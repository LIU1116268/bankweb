# 投产材料管理系统 - 核心业务逻辑脱敏原型

该项目是对本人实习期间，负责开发的「投产材料管理」模块的后端功能复现与个人总结。  
在开发过程中，保留了模块核心业务逻辑（含递归机构树、旁路缓存、机构数据权限、AOP 审计、状态机、分布式限流、异步导出等），  
同时对所有银行敏感信息、企业级专属配置及私有 API 进行了深度脱敏处理，仅用于个人技术积累与展示。

---

## 核心技术栈

| 类别 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.2.4、Maven |
| 持久层 | MyBatis-Plus 3.5.5 |
| 中间件 | MySQL 8.0+、Redis 8.x+、Redisson（分布式锁 / 限流） |
| 功能组件 | EasyExcel（流式导出）、Spring AOP（审计日志）、Spring `@Async`（异步任务） |
| 工具类 | ZipUtils（多文件压缩）、Jackson（Redis JSON 序列化） |
| 测试支撑 | JUnit 5 + Mockito |

---

## 技术亮点与实际解决方案

### 1. 高性能组织架构检索（递归算法 + Redis 旁路缓存）

针对银行「总行 - 分行 - 支行」多级机构的复杂层级关系，设计了 O(1) 复杂度的子部门 ID 检索方案：

- **递归逻辑**：`fillChildIds` 穿透无限层级，支撑跨机构数据归集与权限范围计算。
- **缓存策略**：Cache Aside，查询优先读 Redis；机构变更时 `clearDeptCache` 主动失效，保证一致性。
- **分布式锁**：机构树重建使用 Redisson `RLock`，避免并发下重复 DFS 与缓存击穿。

### 2. 机构数据权限（Data Scope）

在无完整 SSO 的演示环境下，通过请求头 `X-Dept-Id` 模拟「当前登录用户所属机构」：

- **拦截器**：`DataScopeInterceptor` 解析 Header，写入 `DataScopeContext`（ThreadLocal），请求结束自动清理。
- **过滤规则**：启用后只能访问「本机构 + 全部下级机构」数据；查询参数 `deptId` 超出范围返回 `code=403`。
- **兼容调试**：不传 `X-Dept-Id` 时不做机构过滤，便于本地全量联调。
- **接入范围**：列表、详情、保存/更新、状态流转、附件、同步/异步导出等 PRD 相关接口。

### 3. PRD 投产状态机

清单生命周期通过独立接口流转，禁止在 `/save` 中直接改状态：

```
DRAFT → SUBMITTED → UAT_PASSED → PROD_READY → ARCHIVED
              ↓
           DRAFT（退回）
```

- 新增记录默认 `DRAFT`。
- `POST /prd/transition` 校验合法跳转并写审计日志。
- `GET /prd/transition/allowed/{id}` 查询当前允许的目标状态。

### 4. 非侵入式自动化审计日志（Spring AOP）

- 自定义 `@Log` 注解 + `LogAspect` 切面，捕获 PRD 修改、状态流转、附件删除等操作。
- `@Async` 异步写入 `sys_oper_log`，不阻塞主流程。

### 5. 文件处理与导出（同步 + 异步）

- **流式导出**：EasyExcel 逐行读写，降低万级数据 OOM 风险。
- **同步导出**：`GET /prd/exportExcel`、`GET /prd/exportZip` 立即返回文件流。
- **异步导出**：大文件/慢任务先返回 `taskId`，后台生成后通过任务接口查询与下载，任务状态存 Redis（24h TTL）。
- **多维附件**：批量上传、业务绑定、多线程 Zip 打包；异步 Zip 同样走任务模式。

### 6. 分布式接口限流（Redisson）

基于 `RRateLimiter` 的注解式限流 `@RateLimit`，对列表、上传、导出等高频接口按 IP 限流，超限返回 `code=429`。

---

## 主要 API 一览

### PRD 投产清单 `/prd`

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/prd/save` | 新增/更新清单 |
| GET | `/prd/detail/{id}` | 详情 |
| GET | `/prd/list` | 分页列表（支持 `deptId`、`recursive`） |
| POST | `/prd/transition` | 状态流转 |
| GET | `/prd/transition/allowed/{id}` | 可查允许的下一状态 |
| POST | `/prd/upload` | 上传附件 |
| POST | `/prd/uploadBind` | 上传并绑定记录 |
| DELETE | `/prd/deleteFile/{id}` | 删除附件 |
| GET | `/prd/exportExcel` | 同步导出 Excel |
| GET | `/prd/exportZip` | 同步打包 ZIP |
| GET | `/prd/exportExcel/async` | **异步**提交 Excel 导出 |
| GET | `/prd/exportZip/async` | **异步**提交 ZIP 打包 |
| GET | `/prd/export/task/{taskId}` | 查询异步任务状态 |
| GET | `/prd/export/download/{taskId}` | 下载异步导出结果 |

### 机构与日志

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/depts/tree` | 机构树 |
| POST | `/depts/add` | 新增机构 |
| GET | `/logs/list` | 操作日志分页 |
| GET | `/logs/export` | 日志导出 |

### 统一响应约定

| code | 含义 |
|------|------|
| 200 | 成功 |
| 403 | 机构数据权限不足 |
| 429 | 触发限流 |
| 500 | 业务失败 |

---

## 机构数据权限与 Apifox 调试

演示环境用 **Headers** 模拟登录用户机构（生产可改为 JWT / SSO 解析后写入同一上下文）：

| Header | 说明 | 示例 |
|--------|------|------|
| `X-Dept-Id` | 当前用户所属机构 ID | `101`（四川省分行） |
| `X-User-Id` | 工号（可选，便于审计） | `104356` |

**机构 ID 参考**（见 `sql/sys_dept.sql`）：

- `100` 全国银行总行  
- `101` 四川省分行（下属 `105`–`109`）  
- `102` 广东省分行（下属 `110`–`114`）  

**建议测试矩阵**：

1. `GET /prd/list`，Header `X-Dept-Id: 101` → 仅四川及下属数据。  
2. 同上，Header 改为 `102` → 结果与 1 不同。  
3. Header `101`，URL 加 `?deptId=102` → 应返回 `code: 403`。  
4. 不传 `X-Dept-Id` → 不过滤，便于本地调试。

---

## 异步导出使用流程

适用于数据量大、同步接口易超时的场景：

```text
1. GET  /prd/exportExcel/async          → 返回 taskId（status=PENDING）
2. GET  /prd/export/task/{taskId}       → 轮询直到 status=DONE
3. GET  /prd/export/download/{taskId}   → 下载 xlsx / zip
```

任务状态：`PENDING`（排队）→ `RUNNING`（执行中）→ `DONE`（完成）/ `FAIL`（失败，见 `message`）。  
生成文件目录：`{file.upload-path}/export_tasks/`（默认 `D:/uploads/export_tasks/`）。

ZIP 异步导出：`GET /prd/exportZip/async?ids=id1,id2`，ids 须在机构权限范围内。

---

## 项目目录结构说明

```text
src/main/java/com/example/prd
├── annotation      # @Log 审计、@RateLimit 限流
├── aspect          # LogAspect、RateLimitAspect
├── common          # Result 统一响应
├── config          # Redis、WebMvc（数据权限拦截器）、上传路径
├── context         # DataScopeContext（ThreadLocal）
├── controller      # PRD、部门、操作日志接口
├── dto / vo        # 状态流转、导出任务等传输对象
├── entity          # 数据库实体
├── enums           # 清单状态、导出任务状态/类型
├── exception       # 全局异常（403/429 等）
├── interceptor     # DataScopeInterceptor
├── mapper          # MyBatis 接口
├── service         # 业务实现（含 DataScope、ExportTask）
└── utils           # ZipUtils 等

src/main/resources
├── sql
│   ├── prd_check_list.sql           # 投产清单表 DDL
│   ├── prd_check_list_add_status.sql # 已有库追加 STATUS 列（状态机）
│   ├── sys_dept.sql                 # 机构表 + 初始化数据
│   └── sys_oper_log.sql             # 审计日志表
└── application.yml
```

---

## 快速启动

1. **环境要求**：JDK 21、MySQL 8.0+、Redis 8.x+（本机或 Docker 均可）。

2. **初始化数据库**：按顺序执行 `src/main/resources/sql/` 下脚本。  
   若表 `prd_check_list` 已存在且无 `STATUS` 列，需额外执行 `prd_check_list_add_status.sql`。

3. **配置**：修改 `application.yml` 中的数据源、Redis 连接；无密码的 Redis 请勿保留空的 `password` 字段。

4. **运行**：执行 `BankwebApplication` 启动类，默认 `http://localhost:8080`。

5. **接口调试**：推荐使用 Apifox / Postman；URL 栏只填 `http://...`，HTTP 方法用左侧下拉选择，勿把 `GET` 写进地址栏。

---

## 联系与声明

本仓库代码仅用于个人技术积累与展示，已完成全量脱敏处理，不涉及任何原单位商业秘密、敏感数据及核心配置，请勿用于其他用途。
