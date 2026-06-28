const { spawnSync } = require('child_process')
const {
  projectDir,
  validateApiUrl,
  validateAppId,
  validateBuiltOutput,
  findDevtoolsCli,
} = require('./weapp-deploy-utils.cjs')

validateAppId()
validateApiUrl()
validateBuiltOutput()

const cli = findDevtoolsCli()
if (!cli) {
  console.error('Missing WeChat DevTools CLI. Install WeChat DevTools or set WECHAT_DEVTOOLS_CLI.')
  process.exit(1)
}

const version = process.env.WEAPP_VERSION || process.env.npm_package_version || '1.0.0'
const desc = process.env.WEAPP_UPLOAD_DESC || `MemoSpark ${version}`
const style = process.env.WEAPP_UPLOAD_CLI_STYLE || 'modern'
const args =
  style === 'legacy'
    ? ['upload', '--project', projectDir, '-v', version, '-d', desc]
    : ['--upload', '--project', projectDir, '--version', version, '--desc', desc]

const result = spawnSync(cli, args, {
  cwd: projectDir,
  shell: process.platform === 'win32',
  stdio: 'inherit',
})
if (result.error) {
  console.error(result.error.message)
}
process.exit(result.status || 0)
