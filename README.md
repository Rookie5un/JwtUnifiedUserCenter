# Atlas ID Workspace

基于 RESTful API 和 JWT 的统一用户中心与企业业绩验证系统完整实现，包含：

- `backend/`: Spring Boot 3 + RESTful API + JWT + OpenAPI
- `frontend/`: Vue 3 + Vite 自定义高质感工作台界面

## 功能范围

- 用户注册、登录、登出、刷新令牌、当前用户信息、修改密码
- 用户管理、角色管理、权限管理、用户角色分配、角色权限分配
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

登录后可在工作台内直接访问：

- `http://localhost:5173/docs`
- `http://localhost:5173/logs`

## 演示账号

- 管理员：`admin / Admin@123`
- 部门经理：`manager / Manager@123`
- 普通员工：`employee / Employee@123`

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
