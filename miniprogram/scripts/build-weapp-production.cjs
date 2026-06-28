const path = require('path')
const { spawnSync } = require('child_process')
const { projectDir, validateApiUrl, validateBuiltOutput } = require('./weapp-deploy-utils.cjs')

validateApiUrl()

const bin = process.platform === 'win32' ? 'taro.cmd' : 'taro'
const taroBin = path.join(projectDir, 'node_modules', '.bin', bin)
const result = spawnSync(taroBin, ['build', '--type', 'weapp'], {
  cwd: projectDir,
  env: { ...process.env, NODE_ENV: 'production' },
  shell: process.platform === 'win32',
  stdio: 'inherit',
})

if (result.error) {
  console.error(result.error.message)
}

if (result.status !== 0) {
  process.exit(result.status || 1)
}

validateBuiltOutput()
