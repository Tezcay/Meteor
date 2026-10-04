import axe, { type AxeResults, type RunOptions } from 'axe-core'

const wcagOptions: RunOptions = {
  runOnly: {
    type: 'tag',
    values: ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'],
  },
}

export async function runDevA11yAudit(): Promise<AxeResults> {
  const result = await axe.run(document, wcagOptions)
  const previousOutput = document.querySelector('#meteor-a11y-result')
  const output = previousOutput ?? document.createElement('script')

  output.id = 'meteor-a11y-result'
  output.setAttribute('type', 'application/json')
  output.textContent = JSON.stringify({
    url: window.location.pathname,
    testedAt: new Date().toISOString(),
    violations: result.violations,
  })
  if (!previousOutput) document.body.append(output)

  return result
}
