# iChat - 一款基于 Mars 开发的即时通讯 Android App

## 项目亮点
- 端到端即时通讯：基于自定义 TCP 帧协议与腾讯 Mars STN（项目内置示例）
- 离线消息存储并转发：服务端维护离线队列，用户上线即推送未读消息
- 本地持久化与会话回放：Room 存储聊天记录与消息状态，退出/重登保留历史
- 现代 UI：Jetpack Compose 构建聊天气泡、独立聊天页、列表与表单
- 简易多端联测：支持多台 Android 模拟器同时登录进行多端互发

## 技术架构
项目分为客户端和服务端两部分。

### 客户端 (Android App)
- 技术栈：Kotlin、Jetpack Compose、Room、StateFlow/SharedFlow、HttpURLConnection、（示例）Mars STN
- 模块划分：
  - `auth/` 登录注册与服务器地址管理（`AuthApi.kt`、`LoginViewModel.kt`、`LoginScreen.kt`）
  - `friend/` 好友列表与添加、聊天入口（`FriendApi.kt`、`FriendViewModel.kt`、`FriendScreen.kt`）
  - `chat/` 聊天界面与持久化（`ChatScreen.kt`、`ChatMessageEntity.kt`、`ChatDatabase.kt`、`ChatRepository.kt`、`TimeFormatter.kt`）
  - `network/` 原始 TCP 客户端（`RawTcpClient.kt`）
- 即时协议（客户端实现）：
  - 帧格式：`[type(1 byte)][len(4 bytes, BE)][payload(len)]`
  - 消息载荷：JSON
  - Type 1 登录：`{"user_id":"<uid>"}`（兼容纯字符串）
  - Type 2 聊天：`{"id":"<uuid>","from_user":"<from>","to_user":"<to>","content":"<text>","ts":<millis>}`
  - Type 3 发送接受 ACK（server→sender）：`{"id":"...","status":"accepted"}`
  - Type 4 投递 ACK（server→sender）：`{"id":"...","status":"delivered"}`
  - Type 5 未读推送（server→recipient）：同 Type 2 载荷
  - Type 6 已读（recipient→server，可选）：`{"id":"...","status":"read"}`
- 状态流：
  - `RawTcpClient.incoming: SharedFlow<ChatMessage>` 推送新消息
  - `RawTcpClient.acks: SharedFlow<Pair<messageId,status>>` 推送 ACK
  - `FriendViewModel` 收集并入库/更新状态
- 持久化：Room
  - 表 `chat_messages(owner, sender, receiver, content, ts, messageId, status)`
  - 会话查询：`conversation(owner=:me, peer=:user)` 返回 Flow
- UI：
  - `FriendScreen`：联系人列表、添加好友、进入聊天页
  - `ChatScreen`：顶部返回箭头、底部固定输入与发送、气泡样式左/右对齐、时间戳与状态展示

### 服务端 (Node.js)
- 认证服务（示例）：`docs/mock-auth-server.js`
  - `POST /api/auth/login`、`POST /api/auth/register` → 返回 `{"token":"demo-token"}`（示例）
  - 内存用户表与会话映射，便于本地联调
- TCP 路由服务（示例）：`docs/mock-tcp-router.js`
  - 在线转发：对端在线直接转发 Type 2，并回 Type 3/4 给发送方
  - 离线队列：对端离线入队，登录后推送 Type 5，并通知发送方 Type 4（若在线）
  - 仅用于演示；生产建议接入数据库并启用鉴权/ACK持久化

#### 架构图
```
--------------------+        TCP(1/2/3/4/5/6)        +----------------------+
|   Android Client   |  <-------------------------->  |   TCP Router (Node)  |
|  Compose + Room    |                                |  离线队列/ACK/推送     |
|  RawTcpClient      |                                +----------------------+
|  Auth via HTTP     |         HTTP(login/register)   +----------------------+
--------------------+  <-------------------------->  | Auth Server (Node)   |
                                                      +----------------------+
```

## 如何运行

### 1. 环境准备
- Android Studio（JDK 11）、Android SDK、Gradle、Kotlin
- Node.js 16+（用于示例服务端）
- 两台 Android 模拟器（或一台真机 + 一台模拟器）

### 2. 编译 Mars 依赖 
- 项目内已包含演示所需的 Mars 原生库（`libmarsxlog.so`、`libmarsstn.so`），并在 `MainActivity.kt` 中示例调用
- 若需重新编译或替换版本，请参考 Mars 官方文档并更新 `jniLibs`；本项目聊天链路主要基于 `RawTcpClient` 演示，不强制依赖 Mars 编译

### 3. 启动后端服务
- 启动认证服务：
  - `node docs/mock-auth-server.js`
  - 默认监听 `8082`，模拟器端地址为 `http://10.0.2.2:8082`
- 启动 TCP 路由服务：
  - `node docs/mock-tcp-router.js`
  - 默认监听 `8081`，模拟器端地址为 `10.0.2.2:8081`

### 4. 运行客户端 App
- 打开 Android Studio，运行 `app`
- 登录页“服务器地址”填：
  - 模拟器 → `http://10.0.2.2:8082`
  - 真机（同局域网） → `http://<宿主机IP>:8082`
- 登录或注册（示例使用任意非空用户名/邮箱/密码）
- 登录成功后进入好友页：
  - 添加好友：输入对方用户名（例如 `testuser2`）
  - 点击联系人进入聊天页，底部输入消息并发送
- 双机联测：在两台设备分别登录不同账号，互发消息
- 断网与离线测试：关闭一端或路由服务，另一端发送消息；重连/登录后会推送未读并更新状态

## 作业说明
- 目标：在 Android 上实现基础 IM 能力与离线消息演示；完成客户端 UI/协议/持久化与服务端最小原型
- 已实现：
  - 登录/注册（示例后端）与服务器地址管理
  - 好友列表与添加、联系人点击进入独立聊天页
  - TCP 帧协议（type 1/2/3/4/5）与 ACK 状态机
  - Room 持久化、会话查询与时间戳格式化
  - 聊天气泡样式与左右对齐、顶部返回、底部固定输入与滚动列表
- 可拓展：
  - 登录鉴权：登录帧携带 `token`，服务端校验与鉴权拒绝
  - 已读回执（type 6）、重发队列与服务端持久化 ACK
  - 消息加密与安全审计、异常重连与心跳/退避策略
  - 接入真实好友关系与资料服务，替换内存 mock

> 说明：本项目以教学演示为主，示例服务端使用内存队列与简化路由逻辑，生产环境需替换为有持久化与鉴权的后端实现。
