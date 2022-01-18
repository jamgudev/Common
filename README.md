
##### 包含的基础库
- ThreadPool 线程池
- widget 小组件
- 零散的拓展工具以及日志
- 状态栏工具

##### Quick Start
- 引用该库
```groovy
    implementation "io.github.jamgudev:common:0.1.0"
```
- 然后在 Application 里初始化
```kotlin
    Common.instance.init(this)
```