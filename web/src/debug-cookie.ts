import fs from 'node:fs'

export function logDebug(msg: string) {
  if (typeof process !== 'undefined') {
    fs.appendFileSync(
      '/tmp/checkpoint_debug.log',
      new Date().toISOString() + ' ' + msg + '\n',
    )
  }
}
