错误原因与修复方案

概述
- 工程内引入的是腾讯 Mars Java API（老版本），而 Kotlin 代码按新/伪 API 编写，导致大量类型与方法签名不匹配的编译错误。

主要差异
- `Mars.init(Context, Handler)` 与 `Mars.onCreate(boolean)`，无 `Mars.CallBack`、无 `Mars.onForeground`。
- `AppLogic.ICallBack` 提供 `AccountInfo`、`DeviceInfo`、版本与路径回调。
- `StnLogic.ICallBack` 方法签名包含 `host`、`ByteArrayOutputStream`、`int[]` 等；`Task` 是 `StnLogic` 的内部类，`startTask(task)` 仅接收一个参数。
- 前后台通知应使用 `BaseEvent.onForeground(boolean)`。

修复内容
- 在 `iChatApplication.kt`：对齐 `AppLogic.setCallBack` 与 `StnLogic.setCallBack` 全量方法实现；`Mars.init(context, Handler)` + `Mars.onCreate(true)`；设置长/短链接地址。
- 在 `MainActivity.kt`：使用 `BaseEvent.onForeground(true/false)`；以 `StnLogic.Task` 构建任务并通过 `userContext` 传递请求体；调用 `StnLogic.startTask(task)`。
- 新增 `network/MarsClient.kt`：统一 CMD 常量与任务构造、请求打包。
- 新增 `MarsClientTest.kt`：覆盖基本构造与打包逻辑的单元测试。

验证建议
- 在设备/模拟器上进行前后台切换与任务发送冒烟测试。
- 后端未就绪时，`makesureAuthed` 可暂时返回 `true`，`onNewDns` 返回 `null` 走底层解析。
