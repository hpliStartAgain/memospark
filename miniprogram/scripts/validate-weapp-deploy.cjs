const {
  validateApiUrl,
  validateAppId,
  validateBuiltOutput,
  findDevtoolsCli,
} = require('./weapp-deploy-utils.cjs')

const checks = [
  ['AppID', validateAppId],
  ['API URL', validateApiUrl],
  ['Built output', validateBuiltOutput],
]

let failed = false
for (const [name, check] of checks) {
  try {
    const value = check()
    console.log(`[ok] ${name}${value ? `: ${value}` : ''}`)
  } catch (error) {
    failed = true
    console.error(`[fail] ${name}: ${error.message}`)
  }
}

const cli = findDevtoolsCli()
if (cli) console.log(`[ok] WeChat DevTools CLI: ${cli}`)
else console.error('[fail] WeChat DevTools CLI: set WECHAT_DEVTOOLS_CLI to cli.bat/cli')

if (!cli) failed = true
process.exit(failed ? 1 : 0)
