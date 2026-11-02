<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import api from '@/api'
import type { Customer, Settlement } from '@/types'
import { fmtMoney, fmtWeight, today } from '@/utils/format'

// ========== 查询条件 ==========
const customerId = ref<number | undefined>()
const dateRange = ref<[string, string] | null>(null)
const customers = ref<Customer[]>([])
const loading = ref(false)
const exporting = ref(false)
const data = ref<Settlement | null>(null)

function monthStart(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`
}

async function load() {
  if (!customerId.value) {
    ElMessage.warning('请先选择客户')
    return
  }
  loading.value = true
  try {
    data.value = await api.settlements.reconcile({
      customerId: customerId.value,
      dateFrom: dateRange.value?.[0] || '',
      dateTo: dateRange.value?.[1] || ''
    })
  } finally {
    loading.value = false
  }
}

async function exportExcel() {
  if (!customerId.value) {
    ElMessage.warning('请先选择客户')
    return
  }
  exporting.value = true
  try {
    const blob = await api.export.settlement({
      customerId: customerId.value,
      dateFrom: dateRange.value?.[0] || '',
      dateTo: dateRange.value?.[1] || ''
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `对账单_${data.value?.customerName || ''}_${dateRange.value ? `${dateRange.value[0]}_${dateRange.value[1]}` : ''}.xlsx`
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)
  } finally {
    exporting.value = false
  }
}

const showOrders = ref(true)
const showPayments = ref(true)

onMounted(async () => {
  dateRange.value = [monthStart(), today()]
  try {
    customers.value = await api.customers.list({})
  } catch {
    customers.value = []
  }
})
</script>

<template>
  <div>
    <div class="page-header">
      <h3>结算对账</h3>
      <div class="page-header-desc">选择客户 + 起止日期，输出期初 / 笔数 / 总吨 / 总额 / 已付 / 期末应付与逐笔明细，可一键导出</div>
    </div>

    <!-- 查询条件 -->
    <div class="page-card query-bar">
      <el-select v-model="customerId" placeholder="选择客户" filterable style="width: 220px">
        <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        value-format="YYYY-MM-DD"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        :clearable="true"
      />
      <el-button type="primary" :loading="loading" @click="load">生成对账单</el-button>
      <div style="flex: 1"></div>
      <el-button type="success" :loading="exporting" :disabled="!data" @click="exportExcel">一键导出 Excel</el-button>
    </div>

    <template v-if="data">
      <!-- 待复核提示 -->
      <el-alert
        v-if="Number(data.pendingCount) > 0"
        type="warning"
        :closable="false"
        show-icon
        :title="`期间内有 ${data.pendingCount} 笔待复核单据未纳入对账口径`"
        style="margin-bottom: 16px"
      />

      <!-- 汇总卡片 -->
      <el-row :gutter="16" class="kpi-row">
        <el-col :xs="12" :sm="4">
          <div class="page-card kpi-card">
            <div class="kpi-label">期初应付</div>
            <div class="kpi-value">¥ {{ fmtMoney(data.openingBalance) }}</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="4">
          <div class="page-card kpi-card">
            <div class="kpi-label">收购笔数</div>
            <div class="kpi-value">{{ data.orderCount }}</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="4">
          <div class="page-card kpi-card">
            <div class="kpi-label">总吨（计价）</div>
            <div class="kpi-value">{{ fmtWeight(data.totalWeight) }}</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="4">
          <div class="page-card kpi-card">
            <div class="kpi-label">收购总额</div>
            <div class="kpi-value">¥ {{ fmtMoney(data.totalAmount) }}</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="4">
          <div class="page-card kpi-card">
            <div class="kpi-label">已付金额</div>
            <div class="kpi-value">¥ {{ fmtMoney(data.paymentTotal) }}</div>
          </div>
        </el-col>
        <el-col :xs="12" :sm="4">
          <div class="page-card kpi-card">
            <div class="kpi-label">期末应付</div>
            <div class="kpi-value warn">¥ {{ fmtMoney(data.closingBalance) }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- 逐笔明细 -->
      <div class="page-card" style="margin-bottom: 16px">
        <div class="section-title" @click="showOrders = !showOrders">
          收购明细（{{ data.orders.length }} 笔）<span class="toggle">{{ showOrders ? '收起' : '展开' }}</span>
        </div>
        <el-table v-if="showOrders" :data="data.orders" stripe style="width: 100%">
          <el-table-column prop="orderNo" label="单号" width="140" />
          <el-table-column prop="orderDate" label="日期" width="110" />
          <el-table-column prop="varietyName" label="品种" min-width="110" />
          <el-table-column prop="plateNo" label="车牌" width="120" />
          <el-table-column label="计价重量(吨)" width="120" align="right">
            <template #default="{ row }">{{ fmtWeight(row.calcWeight) }}</template>
          </el-table-column>
          <el-table-column label="金额(元)" width="130" align="right">
            <template #default="{ row }"><b>{{ fmtMoney(row.amount) }}</b></template>
          </el-table-column>
          <el-table-column prop="createdByName" label="开单人" width="100" />
          <template #empty>
            <el-empty description="该期间无已确认收购单" />
          </template>
        </el-table>
      </div>

      <!-- 付款明细 -->
      <div class="page-card">
        <div class="section-title" @click="showPayments = !showPayments">
          付款明细（{{ data.payments.length }} 笔）<span class="toggle">{{ showPayments ? '收起' : '展开' }}</span>
        </div>
        <el-table v-if="showPayments" :data="data.payments" stripe style="width: 100%">
          <el-table-column prop="payDate" label="付款日期" width="140" />
          <el-table-column label="金额(元)" width="140" align="right">
            <template #default="{ row }"><b>{{ fmtMoney(row.amount) }}</b></template>
          </el-table-column>
          <el-table-column label="方式" width="120">
            <template #default="{ row }">{{ { CASH: '现金', TRANSFER: '转账', OTHER: '其他' }[row.method] || row.method }}</template>
          </el-table-column>
          <el-table-column prop="handler" label="经办人" min-width="120" />
          <template #empty>
            <el-empty description="该期间无付款记录" />
          </template>
        </el-table>
      </div>
    </template>

    <el-empty v-else description="选择客户与日期后点击「生成对账单」" />
  </div>
</template>

<style scoped>
.kpi-row {
  margin-bottom: 16px;
}

.kpi-card {
  margin-bottom: 0;
}

.kpi-value {
  font-size: 17px;
}

.kpi-value.warn {
  color: var(--gzt-warning);
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--gzt-text);
  margin: 0 0 12px;
  cursor: pointer;
}

.toggle {
  font-size: 12px;
  font-weight: 400;
  color: var(--gzt-text-sub);
  margin-left: 8px;
}
</style>
