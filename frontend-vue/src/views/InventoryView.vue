<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { getInventory, getWarehouses, Warehouse } from '@/api'
import { ElMessage } from 'element-plus'
import { buildInventoryParams } from '@/utils/inventoryQueryParams'

const keyword = ref('')
const locationCode = ref('')
const warehouseId = ref<number | undefined>()
const loading = ref(false)
const inventoryList = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const warehouses = ref<Warehouse[]>([])

let searchTimer: any = null

const loadWarehouses = async () => {
  try {
    const res = await getWarehouses()
    if (res.code === 200) {
      warehouses.value = res.data
    }
  } catch (e) {
    ElMessage.error('加载仓库列表失败')
  }
}

const loadInventory = async () => {
  loading.value = true
  try {
    const params = buildInventoryParams({
      keyword: keyword.value,
      locationCode: locationCode.value,
      warehouseId: warehouseId.value,
      page: page.value,
      pageSize: pageSize.value,
    })
    const res = await getInventory(params)
    if (res.code === 200) {
      inventoryList.value = res.data.list
      total.value = res.data.total
      page.value = res.data.page
      pageSize.value = res.data.pageSize
    }
  } catch (e) {
    ElMessage.error('加载库存失败')
  } finally {
    loading.value = false
  }
}

const debouncedSearch = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    loadInventory()
  }, 300)
}

watch(keyword, () => {
  debouncedSearch()
})

watch(locationCode, () => {
  debouncedSearch()
})

const getRowStyle = ({ row }: { row: any }) => {
  if (row.quantity < 10) {
    return { color: 'red', fontWeight: 'bold' }
  }
  return {}
}

const handleWarehouseChange = () => {
  page.value = 1
  loadInventory()
}

onMounted(() => {
  loadWarehouses()
  loadInventory()
})
</script>

<template>
  <div>
    <h3>库存查询</h3>

    <div style="display: flex; gap: 12px; margin-bottom: 16px">
      <el-input v-model="keyword" placeholder="搜索商品名称/SKU..." style="width: 300px" clearable />
      <el-input v-model="locationCode" placeholder="库位编码精确筛选…" style="width: 180px" clearable />
      <el-select v-model="warehouseId" placeholder="选择仓库" clearable style="width: 200px" @change="handleWarehouseChange">
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>
      <el-button type="primary" @click="() => { page = 1; loadInventory(); }">查询</el-button>
    </div>

    <el-table :data="inventoryList" v-loading="loading" border stripe :row-style="getRowStyle">
      <el-table-column prop="productName" label="商品名称" />
      <el-table-column prop="sku" label="SKU" width="150" />
      <el-table-column prop="locationCode" label="库位编码" width="150" />
      <el-table-column prop="warehouseName" label="仓库" width="120" />
      <el-table-column prop="quantity" label="库存数量" width="100" />
      <el-table-column prop="updatedAt" label="更新时间" width="180">
        <template #default="{ row }">
          {{ row.updatedAt ? new Date(row.updatedAt).toLocaleString() : '-' }}
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 16px; text-align: right">
      <el-pagination
        v-model:current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadInventory"
      />
    </div>

    <el-empty v-if="!loading && inventoryList.length === 0" description="暂无库存数据，请先完成入库操作" />
  </div>
</template>
