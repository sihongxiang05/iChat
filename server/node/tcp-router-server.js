const net = require('net')

const PORT = 8081
const HOST = '0.0.0.0'

const clients = new Map()

function now() { return new Date().toISOString() }

const server = net.createServer((socket) => {
  socket.setNoDelay(true)
  socket.setKeepAlive(true, 30000)
  const addr = `${socket.remoteAddress}:${socket.remotePort}`
  let userId = null

  socket.on('data', (data) => {
    let offset = 0
    while (offset + 5 <= data.length) {
      const type = data.readUInt8(offset); offset += 1
      const len = data.readInt32BE(offset); offset += 4
      if (offset + len > data.length) break
      const payload = data.slice(offset, offset + len).toString('utf8'); offset += len
      if (type === 1) {
        userId = payload
        clients.set(userId, socket)
        console.log(`[${now()}] register ${userId} from ${addr}`)
      } else if (type === 2) {
        try {
          const msg = JSON.parse(payload)
          const toSock = clients.get(msg.to_user)
          if (toSock && !toSock.destroyed) {
            const out = Buffer.from(JSON.stringify(msg), 'utf8')
            const frame = Buffer.alloc(1 + 4 + out.length)
            frame.writeUInt8(2, 0)
            frame.writeInt32BE(out.length, 1)
            out.copy(frame, 5)
            toSock.write(frame)
            console.log(`[${now()}] forward ${msg.from_user} -> ${msg.to_user} len=${out.length}`)
          } else {
            console.log(`[${now()}] target ${msg.to_user} not online`)
          }
        } catch (e) {
          console.error(`[${now()}] bad json from ${addr}: ${e.message}`)
        }
      }
    }
  })

  socket.on('close', () => {
    if (userId) clients.delete(userId)
    console.log(`[${now()}] closed ${addr} uid=${userId}`)
  })

  socket.on('error', (e) => {
    console.error(`[${now()}] error ${addr}: ${e.message}`)
  })
})

server.listen(PORT, HOST, () => {
  console.log(`[${now()}] router server listening on ${HOST}:${PORT}`)
})

