/**
 * 页面验收脚本：真实浏览器操作员工页面。
 *
 * 前置：后端 http://localhost:8080 已启动，前端 http://localhost:5173 已启动。
 * 运行：node tools/ui-smoke.mjs
 * 若 playwright 不在本工程的 node_modules 中，可用 PLAYWRIGHT_MODULE_PATH 指向其安装目录。
 */
import { createRequire } from 'node:module'

const require = createRequire(import.meta.url)
const BASE_URL = process.env.UI_BASE_URL || 'http://localhost:5173'
const SHOT_DIR = process.env.UI_SHOT_DIR || ''

function loadChromium() {
  const specs = ['playwright']
  if (process.env.PLAYWRIGHT_MODULE_PATH) {
    specs.unshift(process.env.PLAYWRIGHT_MODULE_PATH)
  }
  const failures = []
  for (const spec of specs) {
    try {
      const mod = require(spec)
      const chromium = mod.chromium || (mod.default && mod.default.chromium)
      if (chromium) {
        return chromium
      }
    } catch (error) {
      failures.push(`${spec}: ${error.message.split('\n')[0]}`)
    }
  }
  throw new Error(`无法加载 playwright（${failures.join(' | ')}）`)
}

const results = []
function check(name, ok, detail = '') {
  results.push({ name, ok, detail })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail ? ` — ${detail}` : ''}`)
}

async function rowByEmail(page, email) {
  return page.locator(`tr[data-testid="employee-row"]:has(td.cell-email:text-is("${email}"))`)
}

async function main() {
  const chromium = loadChromium()
  const browser = await chromium.launch()
  const page = await browser.newPage({ viewport: { width: 1280, height: 900 } })
  const jsErrors = []
  const httpFailures = []
  page.on('console', (msg) => {
    // 被 fetch 处理过的 4xx/5xx 也会在控制台留下 resource 错误，这里只收集脚本自身错误
    if (msg.type() === 'error' && !msg.text().includes('Failed to load resource')) {
      jsErrors.push(msg.text())
    }
  })
  page.on('pageerror', (error) => jsErrors.push(String(error)))
  page.on('response', (response) => {
    if (response.status() >= 400) {
      httpFailures.push(`${response.status()} ${response.request().method()} ${response.url()}`)
    }
  })

  const suffix = Date.now().toString(36)
  const email = `ui.smoke.${suffix}@example.com`
  const createdName = `UI 测试员工 ${suffix}`
  const updatedName = `UI 已更新 ${suffix}`

  try {
    // 1. 加载列表
    await page.goto(BASE_URL, { waitUntil: 'networkidle' })
    await page.waitForSelector('tr[data-testid="employee-row"]', { timeout: 15000 })
    const initialRows = await page.locator('tr[data-testid="employee-row"]').count()
    check('页面加载员工列表', initialRows >= 2, `初始行数=${initialRows}`)
    if (SHOT_DIR) {
      await page.screenshot({ path: `${SHOT_DIR}/ui-01-list.png`, fullPage: true })
    }

    // 2. 新增员工
    await page.click('[data-testid="add-employee"]')
    await page.fill('[data-testid="form-name"]', createdName)
    await page.fill('[data-testid="form-email"]', email)
    await page.fill('[data-testid="form-department"]', '质量部')
    await page.fill('[data-testid="form-position"]', '测试工程师')
    await page.click('[data-testid="form-submit"]')
    await page.waitForSelector('[data-testid="notice"]', { timeout: 15000 })

    const createdRow = await rowByEmail(page, email)
    await createdRow.waitFor({ timeout: 15000 })
    check('新增后列表中可见', (await createdRow.count()) === 1, `email=${email}`)
    check(
      '新增后姓名正确',
      (await createdRow.locator('td.cell-name').innerText()) === createdName,
      createdName,
    )
    const createdId = await createdRow.getAttribute('data-employee-id')
    check('新增后获得服务端 id', Boolean(createdId), `id=${createdId}`)
    if (SHOT_DIR) {
      await page.screenshot({ path: `${SHOT_DIR}/ui-02-created.png`, fullPage: true })
    }

    // 3. 编辑该条（表单需回填当前值）
    await page.click(`[data-testid="edit-${createdId}"]`)
    const prefilledName = await page.inputValue('[data-testid="form-name"]')
    const prefilledEmail = await page.inputValue('[data-testid="form-email"]')
    check('编辑表单回填姓名', prefilledName === createdName, prefilledName)
    check('编辑表单回填邮箱', prefilledEmail === email, prefilledEmail)

    await page.fill('[data-testid="form-name"]', updatedName)
    await page.fill('[data-testid="form-department"]', '平台部')
    await page.fill('[data-testid="form-position"]', '高级测试工程师')
    await page.click('[data-testid="form-submit"]')
    await page.waitForFunction(
      ({ selector, expected }) => {
        const row = document.querySelector(selector)
        return row !== null && row.querySelector('td.cell-name')?.textContent?.trim() === expected
      },
      { selector: `tr[data-testid="employee-row"][data-employee-id="${createdId}"]`, expected: updatedName },
      { timeout: 15000 },
    )

    const updatedRow = page.locator(`tr[data-testid="employee-row"][data-employee-id="${createdId}"]`)
    await updatedRow.waitFor({ timeout: 15000 })
    const nameAfter = (await updatedRow.locator('td.cell-name').innerText()).trim()
    const deptAfter = (await updatedRow.locator('td.cell-department').innerText()).trim()
    check('编辑后列表显示新姓名', nameAfter === updatedName, nameAfter)
    check('编辑后列表显示新部门', deptAfter === '平台部', deptAfter)
    if (SHOT_DIR) {
      await page.screenshot({ path: `${SHOT_DIR}/ui-03-updated.png`, fullPage: true })
    }

    // 4. 失败可见：使用已存在的邮箱新增
    await page.click('[data-testid="add-employee"]')
    await page.fill('[data-testid="form-name"]', '重复邮箱员工')
    await page.fill('[data-testid="form-email"]', 'zhang.wei@example.com')
    await page.click('[data-testid="form-submit"]')
    const formError = page.locator('[data-testid="form-error"]')
    await formError.waitFor({ timeout: 15000 })
    const errorText = (await formError.innerText()).trim()
    check('重复邮箱错误在页面可见', errorText.length > 0, errorText)
    check('表单在失败后保持打开', await page.locator('[data-testid="employee-form"]').isVisible())
    if (SHOT_DIR) {
      await page.screenshot({ path: `${SHOT_DIR}/ui-04-error.png`, fullPage: true })
    }
    await page.click('[data-testid="form-cancel"]')
  } finally {
    await browser.close()
  }

  check('浏览器控制台无脚本错误', jsErrors.length === 0, jsErrors.join(' | '))
  const unexpectedHttp = httpFailures.filter((item) => item !== '409 POST http://localhost:5173/api/employees')
  check('仅重复邮箱用例产生预期 409', unexpectedHttp.length === 0, httpFailures.join(' | '))

  const failed = results.filter((item) => !item.ok)
  console.log(`\n合计 ${results.length} 项检查，通过 ${results.length - failed.length} 项，失败 ${failed.length} 项`)
  if (failed.length > 0) {
    console.log('失败项：' + failed.map((item) => item.name).join('、'))
    process.exitCode = 1
  }
}

main().catch((error) => {
  console.error('页面验收脚本异常：', error)
  process.exitCode = 1
})
