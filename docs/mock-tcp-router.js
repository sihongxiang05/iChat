const net = require('net')

const PORT = process.env.PORT || 8081

const peers = new Map()
const pendingByUser = new Map()

function writeFrame(sock, type, payloadObj) {
  const body = Buffer.from(typeof payloadObj === 'string' ? payloadObj : JSON.stringify(payloadObj))
  const header = Buffer.alloc(5)
  header.writeUInt8(type, 0)
  header.writeInt32BE(body.length, 1)
  try { sock.write(header); sock.write(body) } catch {}
}

function safeJsonParse(str) { try { return JSON.parse(str) } catch { return null } }

const server = net.createServer(sock => {
  let user = null
  let buf = Buffer.alloc(0)

  function cleanup() {
    if (user) {
      const cur = peers.get(user)
      if (cur === sock) peers.delete(user)
      user = null
    }
    try { sock.destroy() } catch {}
  }

  sock.on('data', chunk => {
    buf = Buffer.concat([buf, chunk])
    while (buf.length >= 5) {
      const type = buf.readUInt8(0)
      const len = buf.readInt32BE(1)
      if (buf.length < 5 + len) break
      const payload = buf.slice(5, 5 + len)
      buf = buf.slice(5 + len)

      if (type === 1) {
        const s = payload.toString('utf8')
        const loginObj = safeJsonParse(s)
        user = loginObj && loginObj.user_id ? loginObj.user_id : s
        peers.set(user, sock)
        writeFrame(sock, 1, user)
        const pend = pendingByUser.get(user) || []
        for (const m of pend) {
          writeFrame(sock, 5, m)
          const senderSock = peers.get(m.from_user)
          if (senderSock && senderSock.writable) writeFrame(senderSock, 4, { id: m.id, status: 'delivered' })
        }
        pendingByUser.set(user, [])
        console.log(`[login] ${user}`)
      } else if (type === 2) {
        const msg = safeJsonParse(payload.toString('utf8'))
        if (!msg) continue
        const toSock = peers.get(msg.to_user)
        writeFrame(sock, 3, { id: msg.id, status: 'accepted' })
        if (toSock && toSock.writable) {
          writeFrame(toSock, 2, msg)
          writeFrame(sock, 4, { id: msg.id, status: 'delivered' })
          console.log(`[route] ${msg.from_user} -> ${msg.to_user}: ${msg.content}`)
        } else {
          const list = pendingByUser.get(msg.to_user) || []
          list.push(msg)
          pendingByUser.set(msg.to_user, list)
          console.log(`[queue] ${msg.from_user} -> ${msg.to_user}: ${msg.content}`)
        }
      } else if (type === 6) {
        const ack = safeJsonParse(payload.toString('utf8'))
        // optional: handle read receipts
      }
    }
  })

  sock.on('error', () => cleanup())
  sock.on('close', () => cleanup())
})

server.listen(PORT, () => { console.log('tcp router on', PORT) })
