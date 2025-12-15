const http = require('http')
const { URL } = require('url')
const crypto = require('crypto')
const { Client } = require('pg')

const PORT = process.env.PORT || 8082
const HOST = process.env.HOST || '0.0.0.0'
const DB_URL = process.env.DATABASE_URL || 'postgres://postgres:postgres@localhost:5432/chat'
const JWT_SECRET = process.env.JWT_SECRET || 'secret'

function json(res, code, obj) {
  const data = Buffer.from(JSON.stringify(obj))
  res.statusCode = code
  res.setHeader('Content-Type', 'application/json')
  res.end(data)
}

function hmac(data) {
  return crypto.createHmac('sha256', JWT_SECRET).update(data).digest('base64url')
}

function jwt(payload) {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url')
  const body = Buffer.from(JSON.stringify(payload)).toString('base64url')
  const sig = hmac(header + '.' + body)
  return `${header}.${body}.${sig}`
}

async function hashPassword(password, salt) {
  return new Promise((resolve, reject) => {
    crypto.pbkdf2(password, salt, 100000, 32, 'sha256', (err, derived) => {
      if (err) reject(err); else resolve(derived.toString('base64'))
    })
  })
}

async function bootstrap(client) {
  await client.query(require('fs').readFileSync(require('path').join(__dirname, '../sql/init.sql'), 'utf8'))
}

const client = new Client({ connectionString: DB_URL })
client.connect().then(() => bootstrap(client))

const server = http.createServer(async (req, res) => {
  const u = new URL(req.url, `http://${req.headers.host}`)
  const method = req.method
  const chunks = []
  req.on('data', c => chunks.push(c))
  req.on('end', async () => {
    const body = chunks.length ? JSON.parse(Buffer.concat(chunks).toString('utf8')) : {}
    try {
      if (u.pathname === '/api/auth/register' && method === 'POST') {
        const { username, email, password } = body
        if (!username || !email || !password) return json(res, 400, { error: 'invalid' })
        const salt = crypto.randomBytes(16).toString('base64')
        const hash = await hashPassword(password, salt)
        const r = await client.query('INSERT INTO users(username,email,password_hash,salt) VALUES($1,$2,$3,$4) RETURNING id', [username, email, hash, salt])
        const token = jwt({ uid: r.rows[0].id })
        const exp = new Date(Date.now() + 7 * 24 * 3600 * 1000)
        await client.query('INSERT INTO sessions(user_id, token, expired_at) VALUES($1,$2,$3)', [r.rows[0].id, token, exp])
        return json(res, 200, { token })
      }
      if (u.pathname === '/api/auth/login' && method === 'POST') {
        const { username, email, password } = body
        const q = username ? 'username=$1' : 'email=$1'
        const v = username ? username : email
        const r = await client.query(`SELECT id,password_hash,salt FROM users WHERE ${q}`, [v])
        if (r.rowCount === 0) return json(res, 401, { error: 'not_found' })
        const hash = await hashPassword(password || '', r.rows[0].salt)
        if (hash !== r.rows[0].password_hash) return json(res, 401, { error: 'bad_password' })
        const token = jwt({ uid: r.rows[0].id })
        const exp = new Date(Date.now() + 7 * 24 * 3600 * 1000)
        await client.query('INSERT INTO sessions(user_id, token, expired_at) VALUES($1,$2,$3)', [r.rows[0].id, token, exp])
        return json(res, 200, { token })
      }
      if (u.pathname === '/api/auth/logout' && method === 'POST') {
        const { token } = body
        if (!token) return json(res, 400, { error: 'invalid' })
        await client.query('DELETE FROM sessions WHERE token=$1', [token])
        return json(res, 200, { ok: true })
      }
      json(res, 404, { error: 'not_found' })
    } catch (e) {
      json(res, 500, { error: 'server_error' })
    }
  })
})

server.listen(PORT, HOST)
