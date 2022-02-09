
##### 包含的基础库
- ThreadPool 线程池
- widget 小组件
- 零散的拓展工具以及日志
- 通用的Toast，LoadingDialog
- 支持ViewBinding的基础Activity类
- EventCenter 全局事件处理器，消息中心
###### util 工具库
- fileUtil 文件管理工具
- bitmapUtil bitmap管理工具
- log 日志管理工具
- rom 管理工具
- statusBar 安卓状态栏管理工具

##### Quick Start
- 引用该库
```groovy
    implementation "io.github.jamgudev:common:0.5.0"
```
- 然后在 Application 里初始化
```kotlin
    Common.instance.init(this)
```