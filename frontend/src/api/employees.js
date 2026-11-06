const BASE_URL = '/api/employees'

async function readPayload(response) {
  const text = await response.text()
  if (!text) {
    return null
  }
  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

function toErrorMessage(payload, status) {
  if (payload && typeof payload === 'object') {
    if (Array.isArray(payload.errors) && payload.errors.length > 0) {
      return payload.errors.join('；')
    }
    if (payload.message) {
      return payload.message
    }
  }
  if (typeof payload === 'string' && payload.trim()) {
    return payload.trim()
  }
  return `请求失败（HTTP ${status}）`
}

async function request(url, options = {}) {
  let response
  try {
    response = await fetch(url, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    })
  } catch (error) {
    throw new Error(`网络错误：无法连接后端服务（${error.message}）`)
  }

  const payload = await readPayload(response)
  if (!response.ok) {
    throw new Error(toErrorMessage(payload, response.status))
  }
  return payload
}

export function fetchEmployees() {
  return request(BASE_URL)
}

export function createEmployee(employee) {
  return request(BASE_URL, {
    method: 'POST',
    body: JSON.stringify(employee),
  })
}

export function updateEmployee(id, employee) {
  return request(`${BASE_URL}/${id}`, {
    method: 'PUT',
    body: JSON.stringify(employee),
  })
}
