
##### 包含的基础库
- ThreadPool 线程池
- widget 小组件
- 零散的拓展工具以及日志
- 通用的Toast，LoadingDialog
- 支持ViewBinding的基础Activity，Fragment类
- EventCenter 全局事件处理器，消息中心
- MVP 架构基础类
###### util 工具库
- fileUtil 文件管理工具
- bitmapUtil bitmap管理工具
- log 日志管理工具
- rom 管理工具
- statusBar 安卓状态栏管理工具
- 计时器 RoughTimer(低精度)，VATimer(高精度)
- ReflectHelper 反射工具类
- KeyBoardUtils 键盘管理工具
- PreferenceUtil Preference管理工具
- StrokeTextView 精准描边TextView

##### Quick Start
- 引用该库
```groovy
    implementation "io.github.jamgudev:common:0.6.0"
```
- 然后在 Application 里初始化
```kotlin
    Common.instance.init(this)
```