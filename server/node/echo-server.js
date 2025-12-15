const http = require('http')
const PORT = 8080
const HOST = '0.0.0.0'

const server = http.createServer((req, res) => {
  const { method, url, socket } = req
  const chunks = []
  req.on('data', (c) => chunks.push(c))
  req.on('end', () => {
    const body = Buffer.concat(chunks)
    if (method === 'POST' && url === '/mars/sendText') {
      res.statusCode = 200
      res.setHeader('Content-Type', 'application/octet-stream')
      res.end(body)
      console.log(`[${new Date().toISOString()}] POST ${url} from ${socket.remoteAddress} len=${body.length}`)
      return
    }
    if (method === 'GET' && url === '/') {
      res.statusCode = 200
      res.setHeader('Content-Type', 'text/plain')
      res.end('OK')
      return
    }
    res.statusCode = 404
    res.end()
  })
})

server.listen(PORT, HOST, () => {
  console.log(`[${new Date().toISOString()}] echo server listening on ${HOST}:${PORT}`)
})
