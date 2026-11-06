# dev1 — 员工管理（Vue3 + Spring Boot）

本仓库包含一个员工管理示例应用的完整前后端，批次：`B2026-1106-EMP`。

## 目录结构

```
.
├── backend/                 Spring Boot 员工服务（Java 17 + Maven）
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/example/employee/
│       │   ├── EmployeeServiceApplication.java
│       │   ├── config/       启动种子数据、CORS
│       │   ├── domain/       Employee 实体
│       │   ├── repository/   Spring Data JPA 仓库
│       │   ├── service/      业务逻辑与异常
│       │   └── web/          REST 控制器、统一异常处理、DTO
│       ├── main/resources/application.yml
│       └── test/java/com/example/employee/EmployeeApiTest.java
├── frontend/                Vue 3 + Vite 员工页面
│   ├── index.html
│   ├── package.json
│   ├── vite.config.js       /api → http://localhost:8080 代理
│   └── src/
│       ├── App.vue          列表页
│       ├── api/employees.js fetch 封装
│       ├── components/EmployeeForm.vue  新增／编辑共用表单
│       ├── main.js
│       └── style.css
├── tools/ui-smoke.mjs       页面验收脚本（Playwright 驱动真实浏览器）
└── .opencode/               批次、交付证据
```

## 技术栈与版本

| 组件 | 实际版本 |
| --- | --- |
| Java | 17（JDK 17.0.18） |
| Maven | 3.9.14 |
| Spring Boot | 3.5.9 |
| Spring Cloud（依赖管理 BOM，仅统一版本，未引入具体组件） | 2025.0.3 |
| 数据库 | H2 内存库（`jdbc:h2:mem:employeedb`），Hibernate 6.6 |
| Node.js / npm | 20.20.1 / 10.x |
| Vue | 3.5.45（组合式 API，`<script setup>`） |
| Vite | 6.4.5（`@vitejs/plugin-vue` 5.2.4） |

> Spring Cloud `2025.0.3` 与 Spring Boot `3.5.x` 为官方兼容组合，本工程只导入其 BOM 做版本对齐，未引入任何 Spring Cloud 运行时组件（单服务）。

## 后端

```bash
cd backend
mvn -q test            # 运行集成测试（@SpringBootTest + MockMvc）
mvn -q spring-boot:run # 启动，监听 http://localhost:8080
# 或者：mvn -q -DskipTests package && java -jar target/employee-service-0.0.1-SNAPSHOT.jar
```

接口：

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `http://localhost:8080/api/employees` | 员工列表（JSON 数组） |
| POST | `http://localhost:8080/api/employees` | 新增，成功 201 + 新对象 |
| PUT | `http://localhost:8080/api/employees/{id}` | 编辑，成功 200 + 更新后对象 |
| GET | `http://localhost:8080/api/actuator/health` | 健康探针（200，`status=UP`） |
| GET | `http://localhost:8080/h2-console` | H2 控制台（JDBC URL：`jdbc:h2:mem:employeedb`，用户 `sa`，空密码） |

字段：`id`（服务端生成）、`name`（必填）、`email`（必填、唯一）、`department`、`position`（可选）。

错误约定：校验失败 `400`；email 重复 `409`；员工不存在 `404`；响应体为 `{"message": "..."}`（校验失败额外带 `errors` 数组）。启动时自动写入 2 条示例员工。

## 前端

```bash
cd frontend
npm install     # 或 npm ci（有 package-lock.json 时）
npm run build   # 产物输出到 dist/
npm run dev     # 开发服务器 http://localhost:5173
```

页面地址：<http://localhost:5173> 。开发服务器通过 Vite proxy 把 `/api` 转发到 `http://localhost:8080`，因此需要先启动后端。

页面操作：加载列表 → 「新增员工」填写并保存 → 列表出现新记录 → 行内「编辑」回填当前值并修改 → 列表显示新值；后端返回错误（如邮箱重复、字段校验失败）时页面显示可读错误，不静默失败。

## 页面验收脚本（可选）

```bash
# 需要先启动后端与前端；playwright 可用 PLAYWRIGHT_MODULE_PATH 指定安装位置
PLAYWRIGHT_MODULE_PATH=<playwright 包目录> node tools/ui-smoke.mjs
```

脚本会真实打开浏览器执行「加载列表 → 新增 → 编辑 → 触发一次重复邮箱错误」，输出逐项 PASS/FAIL，失败时退出码非 0；可用 `UI_SHOT_DIR` 指定截图输出目录。

## 交付证据

- 批次需求：`.opencode/batch/current.md` 与 `.opencode/batch/B2026-1106-EMP/specs/`
- 交付记录：`.opencode/delivery/B2026-1106-EMP.md`

## 用户本地改动（保留）

用户本地在途改动：这一行不能被同步或合并覆盖。
Probe: B2026-1106-PROBE
Probe2: B2026-1106-PROBE2
