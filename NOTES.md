# 测试说明与开发笔记

## 技术栈选型
- **后端**：Java 17 + Spring Boot 3
- **前端**：Vue 3 + Element Plus

## AI 工具使用情况
- **使用的工具**：Cursor (Gemini 3.1 Pro 模型)
- **使用方式**：
  - 辅助理解需求和项目结构
  - 自动生成后端 API、Service 逻辑及 MyBatis/JPA 数据库操作代码
  - 自动生成前端 Vue 页面（入库单表单、库存查询列表等）
  - 辅助排查并修复预埋的 Bug

## 遇到的问题及解决方案
1. **商品删除未校验关联库存**
   - **问题**：`ProductService` 中的 `delete` 方法直接删除了商品，如果该商品在 `Inventory` 中有库存记录，会导致库存数据成为孤儿数据。
   - **解决**：在 `InventoryRepository` 中添加了 `existsByProductId` 方法，并在 `ProductService.delete` 中增加校验，如果存在关联库存则抛出 `BusinessException(400, "该商品存在关联库存，无法删除")`。

2. **前端分页状态未保留**
   - **问题**：在 `ProductsView.vue` 中，编辑商品并提交后，`currentPage.value` 被强制重置为 1，导致用户编辑后跳回第一页，体验不佳。
   - **解决**：修改了 `handleSubmit` 中的逻辑，只有在新增商品（`!form.value.id`）时才将 `currentPage.value` 重置为 1，编辑操作保留当前页码。

## 规范自检（对照 API_SPEC.md / TASKS.md）

### TASKS.md 必做

| 任务 | 结论 |
|------|------|
| 任务 1 入库创建 | 已实现：`POST /api/inbound-orders`，事务内写明细并累加库存；单号格式 `IN-yyyyMMdd-XXX`（当日序号）；前端含商品下拉搜索、仓库→库位级联、多行明细与提交 |
| 任务 2 库存查询 | 已实现：`GET /api/inventory`，分页；keyword + warehouseId；补充 **locationCode** 精确筛选以满足 TASKS「库位编码筛选」；`pageSize` 上限 **100**（与 API_SPEC）；关键词防抖与后端分页 |
| 任务 3 Bug | 已修复：删除商品前校验关联库存；编辑商品后保留列表当前页（仅新增时回到第一页） |

### API_SPEC.md

| 条目 | 结论 |
|------|------|
| 通用响应 code/message/data | 成功：`ApiResponse`；校验/业务异常：`GlobalExceptionHandler` |
| 3.1 创建入库单 | 请求体字段一致；响应 HTTP 201，`code=201`、`message` 与示例一致，`data` 字段对齐 |
| 3.2 / 3.3 入库列表与详情 | 已实现：`GET /api/inbound-orders`、`GET /api/inbound-orders/{id}`（列表文档未给出响应体示例，采用分页结构 `list/total/page/pageSize`，与通用分页约定一致） |
| 4 库存查询 | 查询参数与响应 `data` 结构与文档一致；增加 `locationCode` 以满足 TASKS，与文档不冲突 |
| 1 商品列表 `page/pageSize` | 模板 `GET /api/products` 仍以 keyword 全量列表为主，与文档「可选分页」示例略有出入；若需完全一致可为商品列表补后端分页 |
| 5 出库单 | **未实现 HTTP 接口**（选做 A），方案说明见下文 |

### 环境与代理

`frontend-vue/vite.config.ts` 将 `/api` 代理到 **8080**（Spring Boot），与 README Java 后端端口一致。

---

## 选做任务说明

- **选做 A：出库单 + 库存扣减**：本次未实现 `POST /api/outbound-orders`。若落地，优先考虑 **`Inventory` 乐观锁 `@Version`** 或 **`UPDATE inventory SET quantity = quantity - ? WHERE ... AND quantity >= ?`** 原子扣减，必要时失败重试。
- **选做 B：单元测试**
  - **后端**：Mockito 覆盖 `InboundOrderServiceImpl`（创建成功、商品不存在）。
  - **前端**：已添加 `vitest` 脚本与 `src/utils/inventoryQueryParams.spec.ts`（库存请求参数裁剪与 pageSize 封顶）。若在本地执行 `npm run test` 遇缓存权限问题，可将 npm 缓存目录改到用户目录后重试。
- **选做 C：前端性能优化**：库存列表关键词与库位输入 **防抖**；**后端分页**；并通过工具函数将 **pageSize 限制在 1～100**。

## 如果有更多时间，还会做什么？
- 完善所有 Controller 和 Service 的单元测试，提高测试覆盖率。
- 引入 Redis 缓存，对商品列表、仓库列表等不常变动的数据进行缓存，提高查询性能。
- 完善前端的表单校验规则，增加更多的用户友好提示。
- 实现完整的出库单功能，并结合 Redis + Lua 脚本或数据库乐观锁实现高并发下的库存扣减。
- 增加权限控制（如 Spring Security + JWT），区分管理员和普通用户的操作权限。
