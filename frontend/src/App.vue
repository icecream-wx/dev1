<script setup>
import { onMounted, ref } from 'vue'
import EmployeeForm from './components/EmployeeForm.vue'
import { createEmployee, fetchEmployees, updateEmployee } from './api/employees'

const employees = ref([])
const loading = ref(false)
const listError = ref('')
const saveError = ref('')
const notice = ref('')
const saving = ref(false)
const formVisible = ref(false)
const editing = ref(null)

async function loadEmployees() {
  loading.value = true
  listError.value = ''
  try {
    employees.value = await fetchEmployees()
  } catch (error) {
    listError.value = error.message
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  saveError.value = ''
  notice.value = ''
  formVisible.value = true
}

function openEdit(employee) {
  editing.value = { ...employee }
  saveError.value = ''
  notice.value = ''
  formVisible.value = true
}

function closeForm() {
  formVisible.value = false
  editing.value = null
  saveError.value = ''
}

async function handleSubmit(payload) {
  saving.value = true
  saveError.value = ''
  try {
    if (editing.value) {
      const updated = await updateEmployee(editing.value.id, payload)
      notice.value = `已更新员工 #${updated.id}`
    } else {
      const created = await createEmployee(payload)
      notice.value = `已新增员工 #${created.id}`
    }
    formVisible.value = false
    editing.value = null
    await loadEmployees()
  } catch (error) {
    saveError.value = error.message
  } finally {
    saving.value = false
  }
}

onMounted(loadEmployees)
</script>

<template>
  <main class="page">
    <header class="page-header">
      <div>
        <h1>员工管理</h1>
        <p class="subtitle">Vue 3 + Vite 前端，数据来自 Spring Boot 后端 /api/employees</p>
      </div>
      <div class="header-actions">
        <button class="btn" :disabled="loading" data-testid="refresh" @click="loadEmployees">
          {{ loading ? '加载中…' : '刷新' }}
        </button>
        <button class="btn btn-primary" data-testid="add-employee" @click="openCreate">新增员工</button>
      </div>
    </header>

    <p v-if="notice" class="alert alert-success" data-testid="notice">{{ notice }}</p>

    <p v-if="listError" class="alert alert-error" role="alert" data-testid="list-error">
      加载员工列表失败：{{ listError }}
    </p>

    <EmployeeForm
      v-if="formVisible"
      :key="editing ? `edit-${editing.id}` : 'create'"
      :employee="editing"
      :saving="saving"
      :error="saveError"
      @submit="handleSubmit"
      @cancel="closeForm"
    />

    <section class="card">
      <table class="employee-table" data-testid="employee-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>姓名</th>
            <th>邮箱</th>
            <th>部门</th>
            <th>职位</th>
            <th class="actions">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="employee in employees"
            :key="employee.id"
            data-testid="employee-row"
            :data-employee-id="employee.id"
          >
            <td class="mono">{{ employee.id }}</td>
            <td class="cell-name">{{ employee.name }}</td>
            <td class="cell-email">{{ employee.email }}</td>
            <td class="cell-department">{{ employee.department || '—' }}</td>
            <td class="cell-position">{{ employee.position || '—' }}</td>
            <td class="actions">
              <button class="btn btn-small" :data-testid="`edit-${employee.id}`" @click="openEdit(employee)">
                编辑
              </button>
            </td>
          </tr>
          <tr v-if="!employees.length">
            <td colspan="6" class="empty" data-testid="empty-state">
              {{ loading ? '正在加载…' : '暂无员工数据' }}
            </td>
          </tr>
        </tbody>
      </table>
    </section>
  </main>
</template>
