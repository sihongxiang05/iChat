const http = require('http')
const port = process.env.PORT || 8082
function send(res, code, obj) { const b = JSON.stringify(obj); res.writeHead(code, { 'Content-Type': 'application/json' }); res.end(b) }
function parse(req, cb) { let d = ''; req.on('data', c => d += c); req.on('end', () => { try { cb(JSON.parse(d || '{}')) } catch { cb({}) } }) }
const users = new Map() // username -> {id, username, email}
const sessions = new Map() // token -> username
const friends = new Map() // username -> Set(friendUsername)

let idSeq = 1
function ensureUser(u, email) {
  if (!users.has(u)) {
    users.set(u, { id: idSeq++, username: u, email: email || `${u}@example.com` })
    friends.set(u, new Set())
  }
}

const srv = http.createServer((req, res) => {
  if (req.method === 'GET' && req.url === '/') return send(res, 200, { ok: true })

  if (req.method === 'POST' && req.url === '/api/auth/login') return parse(req, body => {
    const username = body.username || body.email || null
    if (username && body.password) {
      ensureUser(username, body.email)
      const token = `token-${username}`
      sessions.set(token, username)
      return send(res, 200, { token })
    }
    return send(res, 400, { error: 'invalid' })
  })

  if (req.method === 'POST' && req.url === '/api/auth/register') return parse(req, body => {
    if (body.username && body.email && body.password) {
      ensureUser(body.username, body.email)
      const token = `token-${body.username}`
      sessions.set(token, body.username)
      return send(res, 200, { token })
    }
    return send(res, 400, { error: 'invalid' })
  })

  if (req.method === 'GET' && req.url === '/api/friends') {
    const auth = req.headers['authorization'] || ''
    const token = auth.startsWith('Bearer ') ? auth.slice(7) : null
    const me = token ? sessions.get(token) : null
    if (!me) return send(res, 401, { error: 'unauthorized' })
    const list = Array.from(friends.get(me) || []).map(u => users.get(u)).filter(Boolean)
    return send(res, 200, list)
  }

  if (req.method === 'POST' && req.url === '/api/friends/add') return parse(req, body => {
    const auth = req.headers['authorization'] || ''
    const token = auth.startsWith('Bearer ') ? auth.slice(7) : null
    const me = token ? sessions.get(token) : null
    if (!me) return send(res, 401, { error: 'unauthorized' })
    const target = (body.target || '').trim()
    if (!target) return send(res, 400, { error: 'invalid_target' })
    ensureUser(target)
    ensureUser(me)
    friends.get(me).add(target)
    friends.get(target).add(me) // 双向接受，简化测试
    return send(res, 200, { ok: true })
  })

  send(res, 404, { error: 'not_found' })
})
srv.listen(port, () => { console.log('auth server on', port) })
