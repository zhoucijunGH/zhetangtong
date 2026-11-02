<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import api from '@/api'
import type { Customer, CustomerForm, PricePolicy, PricePolicyForm, Variety, VarietyForm } from '@/types'
import { fmtMoney } from '@/utils/format'

const activeTab = ref('customers')

// ========== 通用：客户类型 / 状态文案 ==========
const customerTypeOptions = [
  { label: '蔗农', value: 'FARMER' },
  { label: '经纪人', value: 'BROKER' },
  { label: '合作社', value: 'COOP' }
]

function customerTypeLabel(t?: string) {
  return customerTypeOptions.find((o) => o.value === t)?.label || t || '-'
}

function policyStatusLabel(s?: string) {
  if (s === 'ACTIVE') return '生效中'
  if (s === 'NOT_STARTED') return '未生效'
  if (s === 'EXPIRED') return '已失效'
  return s || '-'
}

function policyTagType(s?: string) {
  if (s === 'ACTIVE') return 'success'
  if (s === 'NOT_STARTED') return 'info'
  return 'warning'
}

// ============================================================
// 客户档案
// ============================================================
const cKeyword = ref('')
const cStatus = ref('')
const cLoading = ref(false)
const customers = ref<Customer[]>([])

async function loadCustomers() {
  cLoading.value = true
  try {
    customers.value = await api.customers.list({ keyword: cKeyword.value, status: cStatus.value })
  } finally {
    cLoading.value = false
  }
}

const cDialog = ref(false)
const cTitle = ref('新增客户')
const cEditingId = ref<number | null>(null)
const cSaving = ref(false)
const cFormRef = ref<FormInstance>()

function cDefault(): CustomerForm {
  return { name: '', type: 'FARMER', phone: '', address: '', remark: '', status: 1 }
}
const cForm = reactive<CustomerForm>(cDefault())

const cRules: FormRules = {
  name: [{ required: true, message: '请输入客户姓名', trigger: 'blur' }],
  type: [{ required: true, message: '请选择客户类型', trigger: 'change' }]
}

function openCCreate() {
  cEditingId.value = null
  cTitle.value = '新增客户'
  Object.assign(cForm, cDefault())
  cDialog.value = true
}

function openCEdit(row: Customer) {
  cEditingId.value = row.id
  cTitle.value = '编辑客户'
  Object.assign(cForm, {
    name: row.name,
    type: row.type,
    phone: row.phone || '',
    address: row.address || '',
    remark: row.remark || '',
    status: row.status == null ? 1 : Number(row.status)
  })
  cDialog.value = true
}

async function saveCustomer() {
  if (!cFormRef.value) return
  try {
    await cFormRef.value.validate()
  } catch {
    return
  }
  cSaving.value = true
  try {
    const payload = { ...cForm, status: Number(cForm.status) ? 1 : 0 }
    if (cEditingId.value != null) {
      await api.customers.update(cEditingId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await api.customers.create(payload)
      ElMessage.success('新增成功')
    }
    cDialog.value = false
    loadCustomers()
  } finally {
    cSaving.value = false
  }
}

function removeCustomer(row: Customer) {
  ElMessageBox.confirm(`确认删除客户「${row.name}」？已有单据的客户建议改为停用。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await api.customers.remove(row.id)
      ElMessage.success('已删除')
      loadCustomers()
    })
    .catch(() => {})
}

// ============================================================
// 甘蔗品种
// ============================================================
const vLoading = ref(false)
const varieties = ref<Variety[]>([])

async function loadVarieties() {
  vLoading.value = true
  try {
    varieties.value = await api.varieties.list()
  } finally {
    vLoading.value = false
  }
}

const vDialog = ref(false)
const vTitle = ref('新增品种')
const vEditingId = ref<number | null>(null)
const vSaving = ref(false)
const vFormRef = ref<FormInstance>()

function vDefault(): VarietyForm {
  return { name: '', remark: '', status: 1 }
}
const vForm = reactive<VarietyForm>(vDefault())

const vRules: FormRules = {
  name: [{ required: true, message: '请输入品种名称', trigger: 'blur' }]
}

function openVCreate() {
  vEditingId.value = null
  vTitle.value = '新增品种'
  Object.assign(vForm, vDefault())
  vDialog.value = true
}

function openVEdit(row: Variety) {
  vEditingId.value = row.id
  vTitle.value = '编辑品种'
  Object.assign(vForm, {
    name: row.name,
    remark: row.remark || '',
    status: row.status == null ? 1 : Number(row.status)
  })
  vDialog.value = true
}

async function saveVariety() {
  if (!vFormRef.value) return
  try {
    await vFormRef.value.validate()
  } catch {
    return
  }
  vSaving.value = true
  try {
    const payload = { ...vForm, status: Number(vForm.status) ? 1 : 0 }
    if (vEditingId.value != null) {
      await api.varieties.update(vEditingId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await api.varieties.create(payload)
      ElMessage.success('新增成功')
    }
    vDialog.value = false
    loadVarieties()
  } finally {
    vSaving.value = false
  }
}

function removeVariety(row: Variety) {
  ElMessageBox.confirm(`确认删除品种「${row.name}」？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await api.varieties.remove(row.id)
      ElMessage.success('已删除')
      loadVarieties()
    })
    .catch(() => {})
}

// ============================================================
// 价格政策
// ============================================================
const pVarietyId = ref<number | ''>('')
const pLoading = ref(false)
const policies = ref<PricePolicy[]>([])

async function loadPolicies() {
  pLoading.value = true
  try {
    policies.value = await api.pricePolicies.list(Number(pVarietyId.value) || undefined)
  } finally {
    pLoading.value = false
  }
}

const pDialog = ref(false)
const pTitle = ref('新增价格政策')
const pEditingId = ref<number | null>(null)
const pSaving = ref(false)
const pFormRef = ref<FormInstance>()

function pDefault(): PricePolicyForm {
  return { varietyId: undefined, price: 0, startDate: '', endDate: '', remark: '' }
}
const pForm = reactive<PricePolicyForm>(pDefault())

const pRules: FormRules = {
  varietyId: [{ required: true, message: '请选择品种', trigger: 'change' }],
  price: [{ required: true, message: '请输入单价', trigger: 'blur' }],
  startDate: [{ required: true, message: '请选择生效日期', trigger: 'change' }]
}

function openPCreate() {
  pEditingId.value = null
  pTitle.value = '新增价格政策'
  Object.assign(pForm, pDefault())
  pDialog.value = true
}

function openPEdit(row: PricePolicy) {
  pEditingId.value = row.id
  pTitle.value = '编辑价格政策'
  Object.assign(pForm, {
    varietyId: row.varietyId,
    price: row.price,
    startDate: row.startDate,
    endDate: row.endDate || '',
    remark: row.remark || ''
  })
  pDialog.value = true
}

async function savePolicy() {
  if (!pFormRef.value) return
  try {
    await pFormRef.value.validate()
  } catch {
    return
  }
  pSaving.value = true
  try {
    const payload: PricePolicyForm = {
      varietyId: Number(pForm.varietyId),
      price: Number(pForm.price),
      startDate: pForm.startDate,
      endDate: pForm.endDate || undefined,
      remark: pForm.remark
    }
    if (pEditingId.value != null) {
      await api.pricePolicies.update(pEditingId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await api.pricePolicies.create(payload)
      ElMessage.success('新增成功')
    }
    pDialog.value = false
    loadPolicies()
  } finally {
    pSaving.value = false
  }
}

function removePolicy(row: PricePolicy) {
  ElMessageBox.confirm(`确认删除「${row.varietyName}」的价格政策？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
    .then(async () => {
      await api.pricePolicies.remove(row.id)
      ElMessage.success('已删除')
      loadPolicies()
    })
    .catch(() => {})
}

// 切换页签时懒加载
watch(activeTab, (tab) => {
  if (tab === 'customers') loadCustomers()
  else if (tab === 'varieties') loadVarieties()
  else loadPolicies()
})

onMounted(() => {
  loadCustomers()
  loadVarieties()
  loadPolicies()
})
</script>

<template>
  <div>
    <div class="page-header">
      <h3>档案管理</h3>
      <div class="page-header-desc">维护客户、甘蔗品种与价格政策（仅老板/管理员可操作）</div>
    </div>

    <div class="page-card">
      <el-tabs v-model="activeTab">
        <!-- ============ 客户档案 ============ -->
        <el-tab-pane label="客户档案" name="customers">
          <div class="query-bar">
            <el-input
              v-model="cKeyword"
              placeholder="姓名 / 电话"
              clearable
              style="width: 200px"
              @keyup.enter="loadCustomers"
            />
            <el-select v-model="cStatus" style="width: 130px" placeholder="全部状态">
              <el-option label="全部状态" value="" />
              <el-option label="启用" value="1" />
              <el-option label="停用" value="0" />
            </el-select>
            <el-button type="primary" @click="loadCustomers">查询</el-button>
            <div style="flex: 1"></div>
            <el-button type="primary" :icon="Plus" @click="openCCreate">新增客户</el-button>
          </div>

          <el-table :data="customers" stripe style="width: 100%">
            <el-table-column prop="name" label="姓名" min-width="110" />
            <el-table-column label="类型" width="100" align="center">
              <template #default="{ row }">
                <el-tag size="small" :type="row.type === 'COOP' ? 'warning' : row.type === 'BROKER' ? 'primary' : 'success'">
                  {{ customerTypeLabel(row.type) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="phone" label="电话" min-width="130">
              <template #default="{ row }">{{ row.phone || '-' }}</template>
            </el-table-column>
            <el-table-column prop="address" label="地址" min-width="160">
              <template #default="{ row }">{{ row.address || '-' }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="120">
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80" align="center">
              <template #default="{ row }">
                <el-tag :type="Number(row.status) ? 'success' : 'info'" size="small">
                  {{ Number(row.status) ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openCEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="removeCustomer(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无客户" />
            </template>
          </el-table>
        </el-tab-pane>

        <!-- ============ 品种档案 ============ -->
        <el-tab-pane label="甘蔗品种" name="varieties">
          <div class="query-bar">
            <div style="flex: 1"></div>
            <el-button type="primary" :icon="Plus" @click="openVCreate">新增品种</el-button>
          </div>

          <el-table :data="varieties" stripe style="width: 100%">
            <el-table-column prop="name" label="品种名称" min-width="160" />
            <el-table-column prop="remark" label="备注" min-width="200">
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="Number(row.status) ? 'success' : 'info'" size="small">
                  {{ Number(row.status) ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="130">
              <template #default="{ row }">
                <el-button link type="primary" @click="openVEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="removeVariety(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无品种" />
            </template>
          </el-table>
        </el-tab-pane>

        <!-- ============ 价格政策 ============ -->
        <el-tab-pane label="价格政策" name="policies">
          <div class="query-bar">
            <el-select v-model="pVarietyId" placeholder="全部品种" clearable style="width: 200px" @change="loadPolicies">
              <el-option v-for="v in varieties" :key="v.id" :label="v.name" :value="v.id" />
            </el-select>
            <el-button type="primary" @click="loadPolicies">查询</el-button>
            <div style="flex: 1"></div>
            <el-button type="primary" :icon="Plus" @click="openPCreate">新增政策</el-button>
          </div>

          <el-table :data="policies" stripe style="width: 100%">
            <el-table-column prop="varietyName" label="品种" min-width="140" />
            <el-table-column label="单价(元/吨)" width="130" align="right">
              <template #default="{ row }">
                <b>{{ fmtMoney(row.price) }}</b>
              </template>
            </el-table-column>
            <el-table-column prop="startDate" label="生效日期" width="120" />
            <el-table-column label="失效日期" width="120">
              <template #default="{ row }">{{ row.endDate || '长期' }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="policyTagType(row.status)" size="small">{{ policyStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="120">
              <template #default="{ row }">{{ row.remark || '-' }}</template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openPEdit(row)">编辑</el-button>
                <el-button link type="danger" @click="removePolicy(row)">删除</el-button>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无价格政策" />
            </template>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 客户弹窗 -->
    <el-dialog v-model="cDialog" :title="cTitle" width="520px" destroy-on-close>
      <el-form ref="cFormRef" :model="cForm" :rules="cRules" label-width="80px">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="cForm.name" placeholder="客户姓名" clearable />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="cForm.type">
            <el-radio v-for="o in customerTypeOptions" :key="o.value" :value="o.value">{{ o.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="cForm.phone" placeholder="联系电话（可选）" clearable />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="cForm.address" placeholder="地址（可选）" clearable />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="cForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="cForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cDialog = false">取消</el-button>
        <el-button type="primary" :loading="cSaving" @click="saveCustomer">保存</el-button>
      </template>
    </el-dialog>

    <!-- 品种弹窗 -->
    <el-dialog v-model="vDialog" :title="vTitle" width="460px" destroy-on-close>
      <el-form ref="vFormRef" :model="vForm" :rules="vRules" label-width="80px">
        <el-form-item label="品种名称" prop="name">
          <el-input v-model="vForm.name" placeholder="如：桂糖42号、粤糖93-159" clearable />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="vForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="vForm.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="vDialog = false">取消</el-button>
        <el-button type="primary" :loading="vSaving" @click="saveVariety">保存</el-button>
      </template>
    </el-dialog>

    <!-- 价格政策弹窗 -->
    <el-dialog v-model="pDialog" :title="pTitle" width="500px" destroy-on-close>
      <el-form ref="pFormRef" :model="pForm" :rules="pRules" label-width="90px">
        <el-form-item label="品种" prop="varietyId">
          <el-select v-model="pForm.varietyId" placeholder="选择品种" style="width: 100%">
            <el-option v-for="v in varieties" :key="v.id" :label="v.name" :value="v.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="单价(元/吨)" prop="price">
          <el-input-number v-model="pForm.price" :min="0" :precision="2" :controls="false" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生效日期" prop="startDate">
          <el-date-picker v-model="pForm.startDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="失效日期">
          <el-date-picker v-model="pForm.endDate" type="date" value-format="YYYY-MM-DD" placeholder="留空表示长期有效" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="pForm.remark" type="textarea" :rows="2" placeholder="备注（可选）" />
        </el-form-item>
        <el-alert type="info" :closable="false" show-icon title="同一品种的生效期间不可重叠，保存时自动校验" />
      </el-form>
      <template #footer>
        <el-button @click="pDialog = false">取消</el-button>
        <el-button type="primary" :loading="pSaving" @click="savePolicy">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped></style>
