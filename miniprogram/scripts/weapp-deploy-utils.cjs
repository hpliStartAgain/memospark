const fs = require('fs')
const os = require('os')
const path = require('path')

const projectDir = path.resolve(__dirname, '..')
const distDir = path.join(projectDir, 'dist')

function readJson(file) {
  return JSON.parse(fs.readFileSync(file, 'utf8'))
}

function loadProjectConfig() {
  const publicConfig = readJson(path.join(projectDir, 'project.config.json'))
  const privateConfigPath = path.join(projectDir, 'project.private.config.json')
  if (!fs.existsSync(privateConfigPath)) return publicConfig
  return { ...publicConfig, ...readJson(privateConfigPath) }
}

function isPlaceholderAppId(appid) {
  return !appid || appid === 'YOUR_WECHAT_APPID' || appid === 'touristappid'
}

function validateAppId() {
  const { appid } = loadProjectConfig()
  if (isPlaceholderAppId(appid)) {
    throw new Error(
      'Missing WeChat AppID. Put it in miniprogram/project.private.config.json as {"appid":"wx..."} or update project.config.json.'
    )
  }
  if (!/^wx[a-zA-Z0-9]{16,}$/.test(appid)) {
    throw new Error(`Invalid-looking WeChat AppID: ${appid}`)
  }
  return appid
}

function isIpHost(hostname) {
  return /^\d{1,3}(\.\d{1,3}){3}$/.test(hostname)
}

function validateApiUrl() {
  const apiUrl = process.env.TARO_APP_API_URL
  if (!apiUrl) {
    throw new Error('TARO_APP_API_URL is required for a production mini-program build.')
  }

  let parsed
  try {
    parsed = new URL(apiUrl)
  } catch {
    throw new Error(`TARO_APP_API_URL is not a valid URL: ${apiUrl}`)
  }

  if (parsed.protocol !== 'https:' && process.env.MINIPROGRAM_ALLOW_HTTP !== '1') {
    throw new Error(
      'WeChat production mini-programs require an HTTPS backend domain. Set MINIPROGRAM_ALLOW_HTTP=1 only for local/dev builds.'
    )
  }
  if (isIpHost(parsed.hostname) && process.env.MINIPROGRAM_ALLOW_IP_HOST !== '1') {
    throw new Error(
      'WeChat production domain whitelist does not accept bare IP hosts. Use an HTTPS domain name.'
    )
  }
  return apiUrl
}

function walkFiles(dir, out = []) {
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) walkFiles(full, out)
    else out.push(full)
  }
  return out
}

function validateBuiltOutput() {
  const appJson = path.join(distDir, 'app.json')
  if (!fs.existsSync(appJson)) {
    throw new Error('Missing dist/app.json. Run npm run build:weapp:prod before upload.')
  }

  const searchable = walkFiles(distDir).filter((file) =>
    /\.(js|json|wxml|wxss)$/.test(file)
  )
  const leakedLocalhost = searchable.find((file) => {
    const text = fs.readFileSync(file, 'utf8')
    return text.includes('http://localhost') || text.includes('localhost:8080')
  })
  if (leakedLocalhost) {
    throw new Error(`Built output still references localhost: ${path.relative(projectDir, leakedLocalhost)}`)
  }
}

function findDevtoolsCli() {
  if (process.env.WECHAT_DEVTOOLS_CLI && fs.existsSync(process.env.WECHAT_DEVTOOLS_CLI)) {
    return process.env.WECHAT_DEVTOOLS_CLI
  }

  const candidates =
    os.platform() === 'win32'
      ? [
          'C:\\Program Files (x86)\\Tencent\\微信web开发者工具\\cli.bat',
          'C:\\Program Files\\Tencent\\微信web开发者工具\\cli.bat',
          'C:\\Program Files (x86)\\Tencent\\微信开发者工具\\cli.bat',
          'C:\\Program Files\\Tencent\\微信开发者工具\\cli.bat',
        ]
      : [
          '/Applications/wechatwebdevtools.app/Contents/MacOS/cli',
          '/Applications/wechatdevtools.app/Contents/MacOS/cli',
        ]

  return candidates.find((candidate) => fs.existsSync(candidate)) || null
}

module.exports = {
  projectDir,
  validateApiUrl,
  validateAppId,
  validateBuiltOutput,
  findDevtoolsCli,
}
