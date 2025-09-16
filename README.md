# 自助查询微信公众号服务

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.13-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![WxJava](https://img.shields.io/badge/WxJava-4.7.0-blue.svg)](https://github.com/Wechat-Group/WxJava)
[![Java](https://img.shields.io/badge/Java-18-red.svg)](https://adoptopenjdk.net/)
[![License](https://img.shields.io/badge/License-Apache%202.0-yellow.svg)](https://opensource.org/licenses/Apache-2.0)

## 📝 项目简介

这是一个基于 **WxJava** 和 **Spring Boot** 的微信公众号后端服务项目，专注于为用户提供自助查询功能，包括：

- 🔍 **多源搜索服务** - 集成多个API提供全面搜索结果
- 🎲 **彩票查询服务** - 双色球开奖信息查询与中奖核对
- 🌐 **Web结果展示** - 通过网页展示搜索结果
- ⏰ **定时任务调度** - 支持定时查询与推送
- 📊 **性能监控** - 集成AOP日志与监控

## ✨ 核心功能

### 🔍 搜索服务
- **多API并发查询** - 同时调用多个数据源API，提升查询效率
- **智能缓存机制** - Redis缓存查询结果，减少重复请求
- **结果智能合并** - 自动去重和格式化搜索结果
- **Web页面展示** - 生成短链接，通过网页展示搜索结果

### 🎲 彩票服务
- **实时开奖查询** - 获取最新双色球开奖信息
- **中奖自动核对** - 支持批量号码中奖核对
- **预约查询功能** - Redis存储预约任务，定时查询通知
- **Bark消息推送** - 中奖结果实时推送到手机

### 🤖 微信公众号集成
- **消息自动处理** - 智能路由用户消息到相应处理器
- **多账号支持** - 支持配置多个微信公众号
- **安全验证** - Token和AES加密验证
- **异步处理** - 异步处理复杂查询请求

## 🛠 技术栈

### 后端核心
- **Spring Boot** 2.7.13 - 主应用框架
- **WxJava** 4.7.0 - 微信开发SDK
- **Spring AOP** - 日志记录与性能监控
- **Quartz** 2.3.2 - 定时任务调度
- **FastJSON2** 2.0.51 - JSON处理

### 数据存储
- **Redis** - 缓存与任务存储
- **Jedis** 3.3.0 - Redis客户端
- **分布式锁** - jedis-lock 1.0.0

### 网络请求
- **OkHttp** 4.9.3 - HTTP客户端
- **Hutool** 5.8.10 - 工具库
- **JSoup** 1.16.1 - HTML解析

### 前端展示
- **Thymeleaf** - 模板引擎
- **Swagger** 3.0.0 - API文档
- **Bootstrap** - 页面样式

### 开发工具
- **Lombok** 1.18.36 - 代码简化
- **Spring Boot DevTools** - 开发热重载

## 🚀 快速开始

### 环境要求

- **JDK 18+** (推荐使用JDK 18)
- **Maven 3.6+**
- **Redis 6.x** (可选，用于缓存和任务调度)

### 1. 克隆项目

```bash
git clone <repository-url>
cd SelfServiceInquiry
```

### 2. 配置文件

修改 `src/main/resources/application.yml`：

```yaml
wx:
  mp:
    configs:
      - appId: 你的微信公众号AppId
        secret: 你的微信公众号Secret
        token: 你的Token
        aesKey: 你的AES密钥

server:
  port: 80
  url: http://你的域名

spring:
  redis:
    host: 你的Redis地址
    port: 6379
    password: 你的Redis密码

api:
  search:
    base-url: http://m.kkqws.com
    token: 你的API Token

bark:
  key: 你的Bark推送Key
```

### 3. 编译运行

```bash
# 编译打包
mvn clean package

# 运行应用
java -jar target/wx.jar

# 或直接运行主类
mvn spring-boot:run
```

### 4. 验证部署

- 访问 `http://localhost/swagger-ui/` 查看API文档
- 配置微信公众号回调地址：`http://你的域名/wx/portal/{appid}`

## 📋 API接口

### 搜索相关
- `POST /wx/portal/{appid}` - 微信消息处理
- `GET /result/{key}` - 查看搜索结果页面

### 性能监控
- `GET /actuator/health` - 健康检查
- `GET /actuator/info` - 应用信息

## 🔧 配置说明

### 微信公众号配置
```yaml
wx:
  mp:
    configs:
      - appId: wx738d5cd928816fa0        # 公众号AppId
        secret: 4e262bf21fb03bc20a5140ed2a888f5f  # 公众号Secret
        token: hcliu                     # 自定义Token
        aesKey: LHznZTiUOlwohmxktYHFlFQJYnQMeCFPCw8J2QsUHGK  # AES密钥
```

### Redis配置
```yaml
spring:
  redis:
    database: 0
    host: localhost
    port: 6379
    password: your_password
```

### API服务配置
```yaml
api:
  search:
    base-url: http://m.kkqws.com
    token: i69
    # 其他API配置...
```

## 📁 项目结构

```
src/main/java/com/hc/wx/mp/
├── builder/                 # 消息构建器
│   ├── AbstractBuilder.java
│   └── TextBuilder.java
├── config/                  # 配置类
│   ├── ApiConfig.java      # API配置
│   ├── LogAspect.java      # 日志切面
│   ├── SwaggerConfig.java  # Swagger配置
│   └── WxMpConfiguration.java  # 微信配置
├── controller/             # 控制器
│   ├── SearchController.java   # 搜索结果展示
│   └── WxPortalController.java # 微信消息入口
├── entity/                 # 实体类
│   ├── LotteryResponse.java
│   └── TokenResponse.java
├── handler/                # 消息处理器
│   ├── AbstractHandler.java
│   ├── LogHandler.java
│   ├── MsgHandler.java
│   └── NullHandler.java
├── service/                # 服务层
│   ├── LotteryService.java     # 彩票服务
│   ├── SearchService.java      # 搜索服务
│   ├── ResultStorageService.java  # 结果存储
│   └── UrlService.java         # URL服务
├── task/                   # 定时任务
│   └── LotteryScheduler.java
├── utils/                  # 工具类
│   ├── DateUtils.java
│   ├── JsonUtils.java
│   └── StringUtils.java
└── WxMpDemoApplication.java   # 启动类
```

## 🧪 测试

项目包含完整的测试用例：

```bash
# 运行所有测试
mvn test

# 运行特定测试类
mvn test -Dtest=SearchServiceTest
```

测试覆盖：
- 单元测试：Service层业务逻辑测试
- 集成测试：Controller层接口测试
- 性能测试：并发查询性能测试

## 📊 监控与日志

### 性能监控
- **AOP日志** - 自动记录方法执行时间
- **健康检查** - Spring Boot Actuator
- **自定义指标** - 查询成功率、响应时间等

### 日志配置
项目使用 `logback-spring.xml` 配置日志：
- 开发环境：控制台输出
- 生产环境：文件输出 + 日志轮转

## 🔒 安全说明

### SSL/TLS
- 生产环境建议使用HTTPS
- 已配置信任所有SSL证书（仅开发测试用）

### 微信验证
- Token验证
- 消息加密（AES）
- 签名校验

### 数据安全
- Redis连接加密
- 敏感信息环境变量配置

## 🚀 部署指南

### Docker部署
```bash
# 构建镜像
mvn spring-boot:build-image

# 运行容器
docker run -p 80:80 wechat-mp-demo
```

### 传统部署
```bash
# 使用提供的脚本
./restart.sh

# 或手动部署
nohup java -jar target/wx.jar > app.log 2>&1 &
```

### Nginx配置
```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 🤝 贡献指南

1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开一个 Pull Request

## 📄 许可证

本项目采用 Apache License 2.0 许可证。详情请查看 [LICENSE](LICENSE) 文件。

## 🆘 问题反馈

如果你在使用过程中遇到问题，请：

1. 查看 [Issues](../../issues) 中是否有相似问题
2. 如果没有，请创建新的 Issue
3. 提供详细的错误信息和复现步骤

## 📞 联系方式

- **作者**: LiuHaiCheng
- **项目地址**: [GitHub Repository](../../)

---

⭐ 如果这个项目对你有帮助，请给个 Star 支持一下！