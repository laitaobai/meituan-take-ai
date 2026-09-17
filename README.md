# meituan外送后端系统 + AI助手

## 📖 项目简介

> 基于本人基于同类项目**重写**的 meituan 外送后端系统，在核心业务逻辑的基础上，
> 集成了 AI Agent 能力，支持自然语言交互完成菜品查询、智能推荐、取消订单、再来一单、催单等操作。系统保留了**正常支付**和**跳过支付**两种模式，便于开发测试与真实场景切换。


**核心特性：**

- 🤖 **AI 智能助手**：用户通过自然语言与 AI 对话，即可完成多种业务操作。
- 🍽️ **菜品展示与推荐**：根据用户口味、预算推荐菜品，并展示菜品详情。
- 📦 **订单管理**：支持取消订单、再来一单、催单等操作。
- 💰 **双支付模式**：保留**正常支付**（对接微信支付）与**跳过支付**（模拟支付成功，便于测试）两种模式。
- 🧠 **对话记忆**：多轮对话上下文保持，体验更自然。
- 🔧 **工具调用**：AI 自主调用后端业务接口，无需人工点击。
- 🛡️ **安全可控**：敏感操作（退款/取消）支持人工确认，防止 AI 误操作。

---

## ✨ AI 功能亮点

| 功能 | 用户示例 | AI 行为 |
| :--- | :--- | :--- |
| **展示菜品** | “你们有什么菜？” | 调用 `listDishes` 工具，返回菜品列表 |
| **推荐菜品** | “我想吃辣的，预算 50 以内” | 调用 `recommendDishes` 工具，结合口味和预算推荐 |
| **取消订单** | “帮我取消订单 123” | 调用 `cancelOrder` 工具，校验权限后取消 |
| **再来一单** | “再来一单上次的” | 调用 `repetition` 工具，复制历史订单 |
| **催单** | “我的订单怎么还没到？订单号 456” | 调用 `reminder` 工具，向商家发送催单提醒 |

> 💡 以上所有功能均通过 AI 对话触发，底层自动调用重写后的 meituan 外送后端系统的业务接口，无需手动操作页面。

---

## 💳 支付功能说明

系统保留了两种支付模式，可根据环境灵活切换：

- **正常支付**：对接微信支付，生成预支付交易单，完成真实支付流程。
- **跳过支付**：通过 `POST /user/order/updateStatus` 接口，直接将订单状态修改为已支付，便于开发测试和演示。

---

## 🧱 技术栈

**后端（重写版 meituan 外送后端系统）：**
- Spring Boot 2.7.3
- MyBatis-Plus
- MySQL 8.0
- Redis
- JWT
- WebSocket
- 阿里云 OSS

**AI 模块（sky-ai）：**
- Spring Boot 3.3.x（独立部署）
- Spring AI 2.0.1
- DeepSeek / 通义千问（OpenAI 兼容接口）
- SimpleVectorStore（RAG 检索）
- ChatMemory（多轮对话）
- Tool Calling（函数调用）
- SSE 流式输出
- RAG 优化（）
**部署与工具：**
- Maven
- Docker（可选）
- Nacos（服务发现，可选）
- Knife4j（接口文档）

---

## 🏗️ 系统架构

```
┌─────────────┐     HTTP      ┌─────────────────────┐
│   前端/小程序 │ ────────────> │ meituan外送后端系统   │
└─────────────┘               │ （重写版）Port: 8080 │
                              └─────────────────────┘
                                      ↑
                                      │ HTTP + Token
                                      │
                              ┌─────────────────────┐
                              │      sky-ai          │
                              │   (AI 智能助手)       │
                              │      Port: 9090      │
                              └─────────────────────┘
                                      ↑
                                      │ 自然语言
                                      │
                              ┌─────────────────────┐
                              │    用户/客户端        │
                              └─────────────────────┘
```

- 重写后的 meituan 外送后端系统提供所有业务接口（菜品、订单、用户等）。
- `sky-ai` 独立启动，通过 HTTP 调用原业务系统的接口，并借助大模型实现智能交互。
- 两者通过 JWT Token 进行安全认证，`sky-ai` 自动登录获取并缓存 Token。

---

## 🖥️ 环境要求

- JDK 17+
- Maven 3.8+
- MySQL 8.0+
- Redis 7.0+
- （可选）Docker Desktop
- 大模型 API Key（如 DeepSeek、通义千问）

---

## 🚀 快速开始

### 1. 克隆项目

```bash
git clone https://github.com/laitaobai/meituan-take-ai.git
cd meituan-take-ai
```

### 2. 初始化数据库

- 创建数据库 `sky_take_out`。
- 导入项目提供的 SQL 脚本（`sky-server/src/main/resources/db` 目录下）。

### 3. 配置重写后的业务系统

修改 `sky-server/src/main/resources/application-dev.yml`：

```yaml
sky:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    host: localhost
    port: 3306
    database: sky_take_out
    username: root
    password: 你的密码
  redis:
    host: localhost
    port: 6379
    database: 0
  alioss:
    endpoint: oss-cn-xxx.aliyuncs.com
    access-key-id: ${ALIYUN_ACCESS_KEY_ID}
    access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET}
    bucket-name: your-bucket
```

> 建议将密钥配置为环境变量，不要硬编码在代码中。

### 4. 启动业务系统

运行 `SkyApplication` 主类，端口 `8080`。

### 5. 配置 sky-ai

修改 `sky-ai/src/main/resources/application.yml`：

```yaml
server:
  port: 9090

spring:
  ai:
    openai:
      api-key: ${DEEPSEEK_API_KEY}
      base-url: https://api.deepseek.com/v1
      chat:
        options:
          model: deepseek-chat

sky:
  server:
    base-url: http://localhost:8080
    username: admin      # 业务系统管理员账号
    password: 123456     # 业务系统管理员密码
  ai:
    enabled: true
```

### 6. 启动 sky-ai

运行 `SkyAiApplication` 主类，端口 `9090`。

### 7. 测试 AI 对话

```bash
curl "http://localhost:9090/ai/chat?message=我想吃辣的，预算50以内"
```

AI 将自动调用菜品查询、推荐等工具，并返回自然语言结果。

---

## 📡 AI 功能使用说明

### 对话接口

```
GET /ai/chat?message={用户消息}
```

支持 **SSE 流式输出**，前端可逐字显示。

### 功能示例

#### 1. 展示菜品

**用户**：你们有什么菜？  
**AI**：调用 `listDishes`，返回：“我们目前有：老坛酸菜鱼、剁椒鱼头、清炒小油菜……”

#### 2. 推荐菜品

**用户**：我想吃辣的，预算 50 以内。  
**AI**：调用 `recommendDishes(taste="辣", budget=50.0)`，返回：“为您推荐：麻婆豆腐（18元）、水煮肉片（38元），总价 56 元，略超预算，可以换成……”

#### 3. 取消订单

**用户**：帮我取消订单 123。  
**AI**：调用 `cancelOrder(orderId=123)`，先校验权限，若订单状态允许取消，则返回：“订单 123 已成功取消。”

#### 4. 再来一单

**用户**：再来一单上次的。  
**AI**：调用 `repetition(orderId=xxx)`，复制历史订单并返回：“已为您重新下单，订单号 789。”

#### 5. 催单

**用户**：我的订单怎么还没到？订单号 456。  
**AI**：调用 `reminder(orderId=456)`，返回：“已为您催单，商家会尽快处理。”

> 所有操作均通过 Tool Calling 自动完成，无需手动点击。

---

## 📁 项目结构

```
meituan-take-ai
├── sky-common          # 公共模块（工具类、常量、异常等）
├── sky-pojo            # 实体类、DTO、VO
├── sky-server          # 重写版 meituan 外送后端系统（端口 8080）
│   ├── src/main/java/com/sky
│   │   ├── controller
│   │   ├── service
│   │   ├── mapper
│   │   └── ...
│   └── src/main/resources
│       ├── application-dev.yml
│       └── mapper
├── sky-ai              # AI 智能助手模块（端口 9090）
│   ├── src/main/java/com/sky/taobai
│   │   ├── controller  # AI 对话接口
│   │   ├── service     # Tool 实现、Token 管理
│   │   ├── config      # RestTemplate、ChatClient 配置
│   │   └── ...
│   └── src/main/resources
│       ├── application.yml
│       └── file/prompt.st   # 系统提示词
└── pom.xml             # 父 POM
```

---

## 🤝 贡献指南

欢迎提交 Issue 或 Pull Request。

1. Fork 本仓库
2. 新建分支 `feature/xxx`
3. 提交代码
4. 发起 Pull Request

---

## 📄 开源协议

本项目基于 [MIT License](LICENSE) 开源，可自由使用、修改、分发。

---

## 🙏 致谢

- 感谢原 meituan 外送系统提供的业务参考。
- 感谢 [Spring AI](https://spring.io/projects/spring-ai) 提供的 AI 集成能力。
- 感谢 DeepSeek / 通义千问 提供的大模型服务。

---

> **重写系统 + AI 赋能，让点外卖也能对话。**  
> 如果这个项目对你有帮助，欢迎点个 ⭐ Star 支持一下！
> --致歉：该项目还有甚多可以优化的点，大家可以自行修改完善------
