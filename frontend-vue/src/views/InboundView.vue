<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { createInboundOrder, getProducts, getWarehouses, getLocations, Product, Warehouse, Location } from '@/api'

const supplierName = ref('')
const items = ref<any[]>([])
const submitting = ref(false)

const products = ref<Product[]>([])
const warehouses = ref<Warehouse[]>([])

onMounted(async () => {
  try {
    const [pRes, wRes] = await Promise.all([
      getProducts(),
      getWarehouses()
    ])
    if (pRes.code === 200) {
      products.value = pRes.data
    }
    if (wRes.code === 200) {
      warehouses.value = wRes.data
    }
  } catch (e) {
    ElMessage.error('加载基础数据失败')
  }
})

const addItem = () => {
  items.value.push({
    productId: undefined,
    warehouseId: undefined,
    locationCode: '',
    quantity: 1,
    locations: [] // 用于存储该行选中的仓库对应的库位列表
  })
}

const removeItem = (index: number) => {
  items.value.splice(index, 1)
}

const handleWarehouseChange = async (val: number, index: number) => {
  items.value[index].locationCode = ''
  items.value[index].locations = []
  if (!val) return
  
  try {
    const res = await getLocations(val)
    if (res.code === 200) {
      items.value[index].locations = res.data
    }
  } catch (e) {
    ElMessage.error('加载库位失败')
  }
}

const handleSubmit = async () => {
  if (!supplierName.value) {
    ElMessage.warning('请输入供应商名称')
    return
  }
  
  for (let i = 0; i < items.value.length; i++) {
    const item = items.value[i]
    if (!item.productId) return ElMessage.warning(`第 ${i + 1} 行请选择商品`)
    if (!item.locationCode) return ElMessage.warning(`第 ${i + 1} 行请选择库位`)
    if (!item.quantity || item.quantity <= 0) return ElMessage.warning(`第 ${i + 1} 行数量必须大于0`)
  }

  submitting.value = true
  try {
    const payload = {
      supplierName: supplierName.value,
      items: items.value.map(item => ({
        productId: item.productId,
        quantity: item.quantity,
        locationCode: item.locationCode
      }))
    }
    const res = await createInboundOrder(payload)
    if (res.code === 201 || res.code === 200) {
      ElMessage.success('入库单创建成功')
      supplierName.value = ''
      items.value = []
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '服务器内部错误')
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div>
    <h3>入库管理</h3>

    <el-form label-width="100px" style="max-width: 800px">
      <el-form-item label="供应商名称" required>
        <el-input v-model="supplierName" placeholder="请输入供应商名称" />
      </el-form-item>

      <el-form-item label="入库明细">
        <el-button type="primary" @click="addItem">+ 添加明细</el-button>
      </el-form-item>
    </el-form>

    <div v-for="(item, index) in items" :key="index" style="margin-bottom: 12px; display: flex; gap: 12px; align-items: center">
      <el-select v-model="item.productId" placeholder="选择商品" filterable style="width: 200px">
        <el-option v-for="p in products" :key="p.id" :label="p.name + ' (' + p.sku + ')'" :value="p.id" />
      </el-select>

      <el-select v-model="item.warehouseId" placeholder="选择仓库" @change="(val: number) => handleWarehouseChange(val, index)" style="width: 150px">
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>

      <el-select v-model="item.locationCode" placeholder="选择库位" style="width: 150px">
        <el-option v-for="loc in item.locations" :key="loc.code" :label="loc.code" :value="loc.code" />
      </el-select>

      <el-input-number v-model="item.quantity" :min="1" placeholder="数量" style="width: 120px" />

      <el-button type="danger" size="small" @click="removeItem(index)">删除</el-button>
    </div>

    <el-button type="success" :loading="submitting" @click="handleSubmit" :disabled="items.length === 0" style="margin-top: 20px">
      提交入库单
    </el-button>

    <el-empty v-if="items.length === 0" description="请点击“添加明细”按钮添加入库商品" />
  </div>
</template>
