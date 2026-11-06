<script setup>
import { computed, ref, watch } from 'vue'

const props = defineProps({
  employee: {
    type: Object,
    default: null,
  },
  saving: {
    type: Boolean,
    default: false,
  },
  error: {
    type: String,
    default: '',
  },
})

const emit = defineEmits(['submit', 'cancel'])

const form = ref(emptyForm())
const fieldErrors = ref({})

const isEditing = computed(() => Boolean(props.employee && props.employee.id))

function emptyForm() {
  return { name: '', email: '', department: '', position: '' }
}

// 编辑时回填当前值；新增时清空表单。
function applyEmployee(employee) {
  if (employee) {
    form.value = {
      name: employee.name ?? '',
      email: employee.email ?? '',
      department: employee.department ?? '',
      position: employee.position ?? '',
    }
  } else {
    form.value = emptyForm()
  }
  fieldErrors.value = {}
}

applyEmployee(props.employee)

watch(() => props.employee, (next) => applyEmployee(next))

function validate() {
  const errors = {}
  if (!form.value.name.trim()) {
    errors.name = '姓名不能为空'
  }
  if (!form.value.email.trim()) {
    errors.email = '邮箱不能为空'
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email.trim())) {
    errors.email = '邮箱格式不正确'
  }
  fieldErrors.value = errors
  return Object.keys(errors).length === 0
}

function handleSubmit() {
  if (!validate()) {
    return
  }
  emit('submit', {
    name: form.value.name.trim(),
    email: form.value.email.trim(),
    department: form.value.department.trim(),
    position: form.value.position.trim(),
  })
}
</script>

<template>
  <section class="card form-card" data-testid="employee-form">
    <h2>{{ isEditing ? `编辑员工 #${employee.id}` : '新增员工' }}</h2>

    <form class="employee-form" @submit.prevent="handleSubmit">
      <label class="field">
        <span>姓名 <em>*</em></span>
        <input
          v-model="form.name"
          type="text"
          name="name"
          placeholder="请输入姓名"
          data-testid="form-name"
          :aria-invalid="Boolean(fieldErrors.name)"
        />
        <small v-if="fieldErrors.name" class="field-error" data-testid="form-name-error">
          {{ fieldErrors.name }}
        </small>
      </label>

      <label class="field">
        <span>邮箱 <em>*</em></span>
        <input
          v-model="form.email"
          type="email"
          name="email"
          placeholder="name@example.com"
          data-testid="form-email"
          :aria-invalid="Boolean(fieldErrors.email)"
        />
        <small v-if="fieldErrors.email" class="field-error" data-testid="form-email-error">
          {{ fieldErrors.email }}
        </small>
      </label>

      <label class="field">
        <span>部门</span>
        <input v-model="form.department" type="text" name="department" placeholder="可选" data-testid="form-department" />
      </label>

      <label class="field">
        <span>职位</span>
        <input v-model="form.position" type="text" name="position" placeholder="可选" data-testid="form-position" />
      </label>

      <p v-if="error" class="alert alert-error" data-testid="form-error">{{ error }}</p>

      <div class="form-actions">
        <button type="submit" class="btn btn-primary" :disabled="saving" data-testid="form-submit">
          {{ saving ? '保存中…' : '保存' }}
        </button>
        <button type="button" class="btn" :disabled="saving" data-testid="form-cancel" @click="emit('cancel')">
          取消
        </button>
      </div>
    </form>
  </section>
</template>
