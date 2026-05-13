import { describe, expect, it } from 'vitest'
import { buildInventoryParams } from './inventoryQueryParams'

describe('buildInventoryParams', () => {
  it('trims keyword/locationCode and drops empty strings', () => {
    const p = buildInventoryParams({
      keyword: '  abc  ',
      locationCode: ' \n ',
      warehouseId: 2,
      page: 0,
      pageSize: 500,
    })
    expect(p.keyword).toBe('abc')
    expect(p.locationCode).toBeUndefined()
    expect(p.warehouseId).toBe(2)
    expect(p.page).toBe(1)
    expect(p.pageSize).toBe(100)
  })

  it('defaults page and pageSize when omitted', () => {
    const p = buildInventoryParams({})
    expect(p.page).toBe(1)
    expect(p.pageSize).toBe(20)
  })
})
