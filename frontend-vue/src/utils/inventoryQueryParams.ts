/** 库存列表请求参数规范化（与 API_SPEC：pageSize 最大 100 一致） */

export type InventoryQueryInput = {
  keyword?: string
  warehouseId?: number | undefined
  locationCode?: string
  page?: number
  pageSize?: number
}

export function buildInventoryParams(input: InventoryQueryInput) {
  const page = Math.max(1, input.page ?? 1)
  const rawSize = input.pageSize ?? 20
  const pageSize = Math.min(100, Math.max(1, rawSize))
  const keyword = input.keyword?.trim() ? input.keyword.trim() : undefined
  const locationCode = input.locationCode?.trim() ? input.locationCode.trim() : undefined
  return {
    keyword,
    warehouseId: input.warehouseId,
    locationCode,
    page,
    pageSize,
  }
}
