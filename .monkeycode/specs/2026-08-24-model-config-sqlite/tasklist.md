# 需求实施计划

- [x] 1. core 模块新增配置仓库抽象接口
   - 在 `com.coloop.agent.runtime.config` 下新增 `ConfigRepository` 接口（`load`/`save`/`hasStoredConfig`）
   - 不引入任何数据库依赖，保持 core 精简
   - 参考设计文档 Components 第 1 节

- [x] 2. server 模块引入 SQLite 依赖并实现配置仓库
   - [x] 2.1 在 server/pom.xml 添加 `org.xerial:sqlite-jdbc` 依赖
   - [x] 2.2 实现 `SqliteConfigRepository`：建表 `app_config`、`seedFromSetting()` 引导导入、`load()`/`save()`（版本递增）
   - [x] 2.3 编写 `SqliteConfigRepository` 单元测试（seed 导入、load/save/覆盖、空库回退、损坏 JSON 容错）

- [x] 3. 实现配置 REST 控制器
   - [x] 3.1 实现 `ConfigController`：`GET /api/config`、`PUT /api/config`（含校验）、`GET /api/config/default`
   - [x] 3.2 编写 `ConfigController` 单元测试（正常保存、非法请求 400、defaultModel 不存在 400、seed 返回）

- [x] 4. 检查点 - 确保模块编译通过且单元测试通过,如有疑问请询问用户

- [x] 5. 集成 ConfigRepository 到 AgentService 与启动初始化
   - 将 `AgentService` 改为 Spring 构造注入 `ConfigRepository`，加载优先级 SQLite > JSON seed
   - 新增 `ConfigInitializer`（ApplicationRunner）初始化 schema 与 seed
   - 参考设计文档 Components 第 4 节

- [x] 6. 前端实现设置入口与表单
   - [x] 6.1 `index.html` 侧边栏底部新增「设置」按钮
   - [x] 6.2 新增 `settings.js`：拉取配置、模态框表单、模型/MCP 动态增删、参考备注、保存校验
   - [x] 6.3 在 `index.html` 引入 `settings.js` 并验证前后端交互

- [x] 7. Git 作者校验 hooks
   - [x] 7.1 新增 `.githooks/pre-commit` 与 `.githooks/pre-push` 校验 `user.name`=greatColin、`user.email`=34583190+greatColin@users.noreply.github.com
   - [x] 7.2 配置 `core.hooksPath` 并验证校验生效

- [x] 8. 检查点 - 端到端验证配置保存、重启持久化与新会话生效,如有疑问请询问用户

- [ ] 9. Git 历史提交作者重写并覆盖远程（需用户确认后执行）
   - 备份镜像仓库，重写 author/committer 为 `greatColin <34583190+greatColin@users.noreply.github.com>`
   - 用户确认后 force push 覆盖远程
