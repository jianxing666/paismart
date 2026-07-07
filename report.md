# PaiSmart 新源码配置迁移报告

## 概述

将旧源码（`other/` 目录）的配置迁移到新源码，使新项目能像旧项目一样正常运行。新源码相对旧源码新增了 53 个 Java 文件（限流、充值、OCR、邀请码、会话管理等功能），旧源码的 74 个文件全部保留。

## 修改清单

### 1. 新建 `.env` 文件（项目根目录）

基于 `.env.example` 创建，填入旧源码中的所有关键配置值：

| 配置项 | 值 | 来源 |
|--------|-----|------|
| `APP_TIMEZONE` | UTC | 旧 application.yml |
| `SERVER_PORT` | 8081 | 旧 application.yml |
| `SPRING_DATASOURCE_URL` | jdbc:mysql://localhost:3306/PaiSmart?...**serverTimezone=UTC**... | 旧 application.yml（时区修正）|
| `SPRING_DATASOURCE_PASSWORD` | PaiSmart2025 | 旧 application.yml |
| `SPRING_DATA_REDIS_PASSWORD` | PaiSmart2025 | 旧 application.yml |
| `MINIO_ENDPOINT` | http://localhost:19000 | docker-compose |
| `MINIO_ACCESS_KEY` | admin | docker-compose |
| `MINIO_SECRET_KEY` | PaiSmart2025 | docker-compose |
| `ELASTICSEARCH_SCHEME` | http | docker-compose（SSL 已关闭）|
| `ELASTICSEARCH_PASSWORD` | PaiSmart2025 | 旧 application.yml |
| `JWT_SECRET_KEY` | PXrQbuCwXwOZzkML/Vm2S5rSwt1iybvmKtGDzVEu+Hc= | 旧 application.yml |
| `ADMIN_BOOTSTRAP_ENABLED` | true | 旧 admin 配置 |
| `ADMIN_BOOTSTRAP_USERNAME` | admin | 旧 admin 配置 |
| `ADMIN_BOOTSTRAP_PASSWORD` | **PaiSmart2025**（见下方说明）| 修改 |
| `APP_AUTH_REGISTRATION_MODE` | OPEN | 旧源码无注册限制 |
| `DEEPSEEK_API_KEY` | sk-301fa98d783a4ce8bce790e1da4c7aea | 旧 application.yml |
| `EMBEDDING_API_KEY` | sk-40c44da4885c472cb78bd27328629873 | 旧 application.yml |

### 2. 修改 `src/main/resources/application.yml`

将所有 `${VAR:}` 占位符的默认值替换为旧源码中的实际值：

- 数据库连接 `serverTimezone`：Asia/Shanghai → **UTC**（匹配旧源码）
- 移除 JPA `hibernate.jdbc.time_zone` 配置（旧源码无此配置）
- `APP_TIMEZONE` 默认值：Asia/Shanghai → **UTC**
- 数据库密码默认值：空 → **PaiSmart2025**
- MinIO 端点端口：9000 → **19000**（匹配 docker-compose）
- MinIO accessKey 默认值：空 → **admin**
- MinIO secretKey 默认值：空 → **PaiSmart2025**
- Elasticsearch scheme：https → **http**
- Elasticsearch password 默认值：空 → **PaiSmart2025**
- JWT secret-key 默认值：空 → **PXrQbuCwXwOZzkML/Vm2S5rSwt1iybvmKtGDzVEu+Hc=**
- Redis 密码默认值：空 → **PaiSmart2025**
- Admin bootstrap enabled：false → **true**
- Admin bootstrap username/password 默认值：空 → **admin** / **PaiSmart2025**
- DeepSeek API key 默认值：空 → **sk-301fa98d783a4ce8bce790e1da4c7aea**
- Embedding API key 默认值：空 → **sk-40c44da4885c472cb78bd27328629873**
- 注册模式：INVITE_ONLY → **OPEN**

### 3. 修改 `src/main/resources/application-dev.yml` 和 `application-docker.yml`

与 `application.yml` 相同的默认值修改（数据库时区、MinIO 端口、各服务密码、API Key 等），确保所有 profile 下配置一致。

### 4. 修改 `frontend/.env`

新增后端 API 地址配置（dev 模式下前端代理需要）：

```
VITE_SERVICE_BASE_URL=http://localhost:8081/api/v1
VITE_OTHER_SERVICE_BASE_URL= `{
  "api": "http://localhost:8081/api/v1",
  "ws": "ws://localhost:8081"
}`
```

### 5. 修改 `frontend/package.json`

新增 `pnpm.overrides` 配置，修复 `vite-plugin-vue-devtools@7.7.6` 依赖链中引用了不存在的 `@vue/shared@3.5.36` 的问题：

```json
"pnpm": {
  "overrides": {
    "@vue/compiler-dom": "3.5.13",
    "@vue/shared": "3.5.13"
  }
}
```

### 6. 重新生成 `frontend/pnpm-lock.yaml`

旧锁文件引用了不存在的 `@vue/shared@3.5.36`，删除后重新生成。

## 重要注意事项

### Java 版本

新源码使用了 Java 14+ 的 arrow-switch 语法和 Java 16+ 的 `record` 类型，**必须使用 Java 17** 编译运行。

当前系统 `JAVA_HOME` 指向 Java 8（`D:\ide+jdk\hspjdk8`），需要切换到 Java 17：

```powershell
# 临时切换（当前终端会话）
$env:JAVA_HOME = "D:\ide+jdk\jdk17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# 或者使用 nvm for Java，永久修改系统环境变量 JAVA_HOME 指向 D:\ide+jdk\jdk17
```

### 管理员密码变更

旧源码管理员密码为 `admin123`（8位），但新源码 `AdminUserInitializer` 增加了安全校验：
- 密码长度必须 >= 12 位
- 不能使用弱口令（admin123、admin、password 等）

因此将管理员密码改为 **`PaiSmart2025`**（与数据库等其他密码保持一致）。

### 前置基础设施

项目需要以下服务运行（可通过 `docs/docker-compose.yaml` 一键启动）：

| 服务 | 端口 | 凭证 |
|------|------|------|
| MySQL 8 | 3306 | root / PaiSmart2025 |
| Redis | 6379 | 密码 PaiSmart2025 |
| MinIO | 19000 (API), 19001 (Console) | admin / PaiSmart2025 |
| Kafka | 9092 | 无认证 |
| Elasticsearch 8.10 | 9200 | elastic / PaiSmart2025 (HTTP) |

### 前端 pnpm 问题

系统 pnpm 未全局安装，可通过以下路径调用：
```
D:\Soft\nvm\v20.20.0\node_modules\corepack\shims\pnpm.ps1
```

建议使用 `nvm use 20.20.0` 后执行 `corepack enable pnpm` 来启用 pnpm（可能需要管理员权限）。

## 启动步骤

```bash
# 1. 启动基础设施
cd docs && docker-compose up -d

# 2. 启动后端（确保 JAVA_HOME 指向 JDK 17）
mvn spring-boot:run

# 3. 启动前端
cd frontend && pnpm dev
```

## 代码差异说明

新源码相比旧源码新增的功能模块（53 个新 Java 文件）：

- **限流系统**: RateLimitService, RateLimitConfigService, RateLimitProperties 等
- **用量配额**: UsageQuotaService, UsageDashboardService, UserTokenService 等
- **充值系统**: RechargeController, RechargeService, WxPayService 等
- **邀请码**: InviteCode, InviteCodeService, RegistrationMode 等
- **OCR 集成**: AliyunOcrService, LiteParseOcrAdapterService, InternalOcrController
- **会话管理**: ConversationSession, ConversationSessionController
- **多模型路由**: ModelProviderConfig, LlmProviderRouter
- **启动引导**: BootstrapKnowledgeInitializer, AdminUserInitializer, OrgTagInitializer

所有旧源码的 74 个 Java 文件均完整保留，未删除任何文件。
