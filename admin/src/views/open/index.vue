<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import api from '@/api'
import type { Customer, Order, Variety } from '@/types'
import { fmtMoney, fmtWeight, today } from '@/utils/format'

// ========== 表单 ==========
const formRef = ref<FormInstance>()
const saving = ref(false)
const customers = ref<Customer[]>([])
const varieties = ref<Variety[]>([])
const policyPrice = ref<number | null>(null)
const noPolicy = ref(false)

const form = reactive({
  orderDate: today(),
  customerId: undefined as number | undefined,
  varietyId: undefined as number | undefined,
  plateNo: '',
  driver: '',
  grossWeight: undefined as number | undefined,
  tareWeight: undefined as number | undefined,
  impurityRate: 3,
  price: undefined as number | undefined,
  remark: ''
})

const rules: FormRules = {
  orderDate: [{ required: true, message: '请选择收购日期', trigger: 'change' }],
  customerId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  varietyId: [{ required: true, message: '请选择品种', trigger: 'change' }],
  plateNo: [{ required: true, message: '请填写车牌号/司机', trigger: 'blur' }],
  grossWeight: [{ required: true, message: '请输入毛重', trigger: 'blur' }],
  tareWeight: [{ required: true, message: '请输入皮重', trigger: 'blur' }],
  price: [{ required: true, message: '请输入单价', trigger: 'blur' }]
}

/** 计算链路（与后端口径一致）：净重 → 扣杂量 → 计价重量 → 金额（四舍五入 2 位） */
const netWeight = computed(() => Math.max(Number(form.grossWeight || 0) - Number(form.tareWeight || 0), 0))
const deductionWeight = computed(() => Math.round(netWeight.value * Number(form.impurityRate || 0) * 10) / 1000)
const calcWeight = computed(() => Math.round((netWeight.value - deductionWeight.value) * 1000) / 1000)
const amount = computed(() => Math.round(calcWeight.value * Number(form.price || 0) * 100) / 100)

/** 成交价 ≠ 政策价 → 改价（提交后会留痕） */
const priceChanged = computed(
  () => policyPrice.value != null && Number(form.price || 0) !== Number(policyPrice.value)
)

/** 选品种/日期后带出生效中的价格政策单价 */
async function loadPolicy(): Promise<void> {
  noPolicy.value = false
  policyPrice.value = null
  if (!form.varietyId) return
  try {
    const p = await api.pricePolicies.effective(form.varietyId, form.orderDate)
    if (p && p.status === 'ACTIVE') {
      policyPrice.value = Number(p.price)
      form.price = Number(p.price)
    } else {
      noPolicy.value = true
      if (form.price == null) form.price = undefined
    }
  } catch {
    noPolicy.value = true
  }
}

function onVarietyChange(): void {
  loadPolicy()
}

function onDateChange(): void {
  loadPolicy()
}

function resetForm(): void {
  formRef.value?.resetFields()
  form.orderDate = today()
  form.customerId = undefined
  form.varietyId = undefined
  form.plateNo = ''
  form.driver = ''
  form.grossWeight = undefined
  form.tareWeight = undefined
  form.impurityRate = 3
  form.price = undefined
  form.remark = ''
  policyPrice.value = null
  noPolicy.value = false
}

async function save(): Promise<void> {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  if (netWeight.value <= 0) {
    ElMessage.error('净重需大于 0（毛重必须大于皮重）')
    return
  }
  saving.value = true
  try {
    const order = await api.orders.create({
      orderDate: form.orderDate,
      customerId: form.customerId,
      varietyId: form.varietyId,
      plateNo: form.plateNo,
      driver: form.driver,
      grossWeight: Number(form.grossWeight),
      tareWeight: Number(form.tareWeight),
      impurityRate: Number(form.impurityRate || 0),
      price: Number(form.price),
      remark: form.remark
    })
    ElMessage.success(`开单成功：${order.orderNo}，已提交复核`)
    resetForm()
    loadToday()
  } finally {
    saving.value = false
  }
}

// ========== 当日单据快捷列表 ==========
const todayList = ref<Order[]>([])
const todayLoading = ref(false)

async function loadToday(): Promise<void> {
  todayLoading.value = true
  try {
    const res = await api.orders.list({ dateFrom: form.orderDate, dateTo: form.orderDate, page: 1, size: 10 })
    todayList.value = res.list || []
  } finally {
    todayLoading.value = false
  }
}

function statusTag(status?: string) {
  const map: Record<string, { label: string; type: 'success' | 'warning' | 'info' | 'danger' }> = {
    DRAFT: { label: '草稿', type: 'info' },
    PENDING: { label: '待复核', type: 'warning' },
    CONFIRMED: { label: '已确认', type: 'success' },
    VOIDED: { label: '已作废', type: 'info' }
  }
  return map[status || ''] || { label: status || '-', type: 'info' as const }
}

onMounted(async () => {
  try {
    const [cs, vs] = await Promise.all([api.customers.list({ status: '1' }), api.varieties.list({ status: '1' })])
    customers.value = cs || []
    varieties.value = vs || []
  } catch {
    // 提示由拦截器统一处理
  }
  loadToday()
})
</script>

<template>
  <div>
    <div class="page-header">
      <h3>收购开单</h3>
      <div class="page-header-desc">按品种带出价格政策单价，自动计算净重 / 扣杂 / 计价重量 / 金额，保存后进入待复核</div>
    </div>

    <el-row :gutter="16">
      <!-- 开单表单 -->
      <el-col :xs="24" :lg="14">
        <div class="page-card">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="收购日期" prop="orderDate">
                  <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" :clearable="false" style="width: 100%" @change="onDateChange" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="客户" prop="customerId">
                  <el-select v-model="form.customerId" placeholder="选择客户" filterable style="width: 100%">
                    <el-option
                      v-for="c in customers"
                      :key="c.id"
                      :label="`${c.name}（${c.type === 'FARMER' ? '蔗农' : c.type === 'BROKER' ? '经纪人' : '合作社'}）`"
                      :value="c.id"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="品种" prop="varietyId">
                  <el-select v-model="form.varietyId" placeholder="选择品种" style="width: 100%" @change="onVarietyChange">
                    <el-option v-for="v in varieties" :key="v.id" :label="v.name" :value="v.id" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="单价(元/吨)" prop="price">
                  <el-input-number v-model="form.price" :min="0" :precision="2" :controls="false" style="width: 100%" />
                  <div class="price-tip">
                    <span v-if="noPolicy" class="warn">该品种当日无生效政策，请手填单价</span>
                    <span v-else-if="policyPrice != null">政策价 ¥{{ fmtMoney(policyPrice) }}/吨</span>
                    <span v-if="priceChanged" class="warn">（改价将留痕：原价 → 新价）</span>
                  </div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="车牌号/司机" prop="plateNo">
                  <el-input v-model="form.plateNo" placeholder="如：桂A·8U921" clearable />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="司机">
                  <el-input v-model="form.driver" placeholder="选填" clearable />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="毛重(吨)" prop="grossWeight">
                  <el-input-number v-model="form.grossWeight" :min="0" :precision="3" :controls="false" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="皮重(吨)" prop="tareWeight">
                  <el-input-number v-model="form.tareWeight" :min="0" :precision="3" :controls="false" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="含杂率(%)">
                  <el-input-number v-model="form.impurityRate" :min="0" :max="100" :precision="2" :controls="false" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="净重(吨)">
                  <div class="calc-box">{{ fmtWeight(netWeight) }}</div>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="扣杂量(吨)">
                  <div class="calc-box">{{ fmtWeight(deductionWeight) }}</div>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="计价重量(吨)">
                  <div class="calc-box">{{ fmtWeight(calcWeight) }}</div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="金额(元)">
                  <div class="calc-amount">¥ {{ fmtMoney(amount) }}</div>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="备注">
                  <el-input v-model="form.remark" placeholder="选填" clearable />
                </el-form-item>
              </el-col>
            </el-row>
            <div class="form-actions">
              <el-button type="primary" :loading="saving" @click="save">保存并提交复核</el-button>
              <el-button @click="resetForm">清空重填</el-button>
            </div>
          </el-form>
        </div>
      </el-col>

      <!-- 当日单据快捷列表 -->
      <el-col :xs="24" :lg="10">
        <div class="page-card">
          <div class="section-title">当日单据（{{ form.orderDate }}）</div>
          <el-table v-loading="todayLoading" :data="todayList" stripe style="width: 100%" size="small">
            <el-table-column prop="orderNo" label="单号" width="130" />
            <el-table-column prop="customerName" label="客户" min-width="90" />
            <el-table-column prop="varietyName" label="品种" min-width="90" />
            <el-table-column label="计价重量" width="90" align="right">
              <template #default="{ row }">{{ fmtWeight(row.calcWeight) }}</template>
            </el-table-column>
            <el-table-column label="金额(元)" width="100" align="right">
              <template #default="{ row }"><b>{{ fmtMoney(row.amount) }}</b></template>
            </el-table-column>
            <el-table-column label="状态" width="90" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status).type" size="small">{{ statusTag(row.status).label }}</el-tag>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="当日暂无单据" />
            </template>
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.price-tip {
  font-size: 12px;
  color: var(--gzt-text-sub);
  line-height: 1.5;
}

.price-tip .warn {
  color: var(--gzt-warning);
}

.calc-box {
  line-height: 32px;
  padding: 0 11px;
  width: 100%;
  background: #f5f7fa;
  border: 1px solid var(--gzt-border);
  border-radius: 4px;
  color: var(--gzt-text);
}

.calc-amount {
  font-size: 18px;
  font-weight: 700;
  color: var(--gzt-danger, #f56c6c);
  line-height: 32px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 4px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--gzt-text);
  margin: 0 0 12px;
}
</style>
