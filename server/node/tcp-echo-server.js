// TCP Echo Server using Node.js net module
// - Listens on 0.0.0.0:8081
// - Logs connections, errors, closes
// - Echoes any received bytes back to the originating client
// - Supports multiple concurrent clients and applies sane socket options

const net = require('net')

const PORT = 8081
const HOST = '0.0.0.0'

const clients = new Map() // socket -> {addr, bytesRx, bytesTx, connectedAt}

function now() {
  return new Date().toISOString()
}

const server = net.createServer((socket) => {
  socket.setNoDelay(true)
  socket.setKeepAlive(true, 30_000)

  const addr = `${socket.remoteAddress}:${socket.remotePort}`
  clients.set(socket, { addr, bytesRx: 0, bytesTx: 0, connectedAt: Date.now() })
  console.log(`[${now()}] [+] client connected ${addr}`)

  socket.on('data', (buf) => {
    const stat = clients.get(socket)
    if (!stat) return
    stat.bytesRx += buf.length
    // Echo back exactly what we received
    try {
      socket.write(buf, (err) => {
        if (err) {
          console.error(`[${now()}] [!] write error to ${addr}:`, err.message)
          return
        }
        stat.bytesTx += buf.length
        console.log(`[${now()}] [>] echo ${buf.length}B to ${addr}`)
      })
    } catch (e) {
      console.error(`[${now()}] [!] write exception to ${addr}:`, e.message)
    }
  })

  socket.on('error', (err) => {
    console.error(`[${now()}] [!] socket error ${addr}:`, err.message)
  })

  socket.on('close', (hadErr) => {
    const stat = clients.get(socket)
    clients.delete(socket)
    const alive = Date.now() - (stat?.connectedAt || Date.now())
    console.log(`[${now()}] [-] client closed ${addr} hadErr=${hadErr} alive=${alive}ms rx=${stat?.bytesRx || 0} tx=${stat?.bytesTx || 0}`)
  })
})

server.on('error', (err) => {
  console.error(`[${now()}] [!] server error:`, err.stack || err.message)
})

server.on('listening', () => {
  const addr = server.address()
  console.log(`[${now()}] [*] echo server listening on ${addr.address}:${addr.port}`)
})

server.maxConnections = 1024
server.listen(PORT, HOST)

// Periodic stats (memory & connections)
setInterval(() => {
  const mem = process.memoryUsage()
  console.log(`[${now()}] [#] connections=${clients.size} rss=${(mem.rss/1024/1024).toFixed(2)}MB heapUsed=${(mem.heapUsed/1024/1024).toFixed(2)}MB`)
}, 30_000)

