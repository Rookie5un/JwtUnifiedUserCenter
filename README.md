# 统一用户中心与 SSO 集成门户

基于 RESTful API 和 JWT 的统一用户中心与单点登录集成门户完整实现，包含：

- `backend/`: Spring Boot 3 + RESTful API + JWT + OpenAPI
- `frontend/`: Vue 3 + Vite 统一认证入口、集成门户和业务系统示例页

## 功能范围

- 用户注册、登录、登出、刷新令牌、当前用户信息、修改密码
- 用户管理、角色管理、权限管理、用户角色分配、角色权限分配
- 登录后进入集成门户首页，按角色权限展示可访问业务系统
- 后端提供业务系统目录与 SSO 授权接口，统一记录系统访问审计日志
- OA、仓库、财务等伪业务系统免密跳转示例
- 业绩审批系统复用真实业务接口，演示已登录状态下的跨系统切换
- Swagger / OpenAPI 动态接口文档
- JWT 校验与解析接口
- 企业业绩录入、修改、删除、审批、统计、看板
- 操作日志审计与前端日志查看

## 本地启动

### 后端

```bash
cp .env.example .env
./scripts/bootstrap-mysql.sh
cd backend
mvn spring-boot:run
```

默认使用 MySQL 数据库，连接参数如下：

- `MYSQL_HOST=localhost`
- `MYSQL_PORT=3306`
- `MYSQL_DB=jwt_center`
- `MYSQL_USER=jwt_app`
- `MYSQL_PASSWORD=jwt_app_password`

如果你的本地 MySQL 参数不同，可以用环境变量覆盖：

```bash
cd backend
MYSQL_HOST=127.0.0.1 MYSQL_PORT=3306 MYSQL_DB=jwt_center MYSQL_USER=root MYSQL_PASSWORD=your_password mvn spring-boot:run
```

Swagger 地址：

- `http://localhost:8080/swagger-ui.html`

门户与 SSO 相关接口：

- `GET /portal/apps`：返回当前用户有权访问的业务系统清单
- `POST /portal/apps/{appKey}/authorize`：校验业务系统访问权限，签发短期 SSO 票据，返回 `/systems/{appKey}?ticket=...` 独立系统入口，并记录 `APP_ACCESS` 日志
- `POST /portal/sso/tickets/{ticket}/verify`：独立业务系统页校验 SSO 票据，成功后免密进入系统

MySQL 初始化说明：

- `scripts/bootstrap-mysql.sh` 会创建数据库、应用账号和完整表结构
- 默认使用管理员账号 `root/root` 连接本地 MySQL，可通过 `MYSQL_ADMIN_*` 覆盖
- 运行后会为应用创建 `jwt_app` 账号并授予 `jwt_center` 数据库权限

测试也走 MySQL，请先确保数据库已完成初始化且连接参数正确：

```bash
cd backend
mvn -Dmaven.repo.local=../.m2 test
```

### 前端

```bash
cd frontend
npm install
npm run dev
```

默认访问地址：

- `http://localhost:5173`

登录后进入统一门户，可直接免密访问：

- `http://localhost:5173/systems/oa?ticket=...`
- `http://localhost:5173/systems/warehouse?ticket=...`
- `http://localhost:5173/systems/finance?ticket=...`
- `http://localhost:5173/systems/performance?ticket=...`
- `http://localhost:5173/docs`
- `http://localhost:5173/logs`

## 演示账号

- 管理员：`admin / Admin@123`
- 部门经理：`manager / Manager@123`
- 普通员工：`employee / Employee@123`

门户可见范围：

- 管理员：全部业务系统、权限中心、操作日志、接口文档
- 部门经理：OA、仓库、财务、业绩审批（系统内包含审批队列）
- 普通员工：OA、仓库、财务、个人业绩台账

## 说明

- 后端默认允许来自 `http://localhost:5173` 的跨域请求
- 前端接口地址默认指向 `http://localhost:8080`
- 若需修改前端接口地址，可设置 `VITE_API_BASE`

## 生产配置

生产环境建议启用 `prod` profile：

```bash
cd backend
SPRING_PROFILES_ACTIVE=prod \
MYSQL_HOST=127.0.0.1 \
MYSQL_PORT=3306 \
MYSQL_DB=jwt_center \
MYSQL_USER=jwt_app \
MYSQL_PASSWORD=strong_password \
JWT_SECRET=replace-with-your-secret \
mvn spring-boot:run
```

`prod` profile 的策略：

- 使用 `spring.jpa.hibernate.ddl-auto=validate`
- 不自动修改数据库表结构
- 不写入演示账号和示例业绩数据

首次部署前，请确保目标 MySQL 已先执行 `./scripts/bootstrap-mysql.sh` 或已存在完整表结构。

## 备份与恢复

创建备份：

```bash
./scripts/backup-mysql.sh
```

脚本特性：

- 备份文件默认输出到 `backups/`
- 文件名格式为 `jwt_center_YYYYMMDD_HHMMSS.sql.gz`
- 默认保留 7 天，可通过 `BACKUP_RETENTION_DAYS` 调整

恢复备份：

```bash
./scripts/restore-mysql.sh backups/your-backup.sql.gz
```

定时备份示例：

```bash
0 3 * * * cd /Users/rookie/projects/JWT && ./scripts/backup-mysql.sh >> /Users/rookie/projects/JWT/backups/backup.log 2>&1
```
