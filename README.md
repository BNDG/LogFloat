# LogFloat

#### 介绍
- 简单的、用于查看okhttp请求日志的悬浮窗view，可以展示请求接口和结果，搜索关键字。
- 主要是方便测试的时候查看请求内容。
- 仅用于测试环境。

#### 使用说明

1.  okhttp addInterceptor（示例中有个LoggingInterceptor）
2. start FloatViewService (需要悬浮窗权限）
2. 在Interceptor中，按需发送数据到悬浮窗
``` kotlin
LogManager.instance.logUpdated(
            HttpLogEvent(
                url,
                requestBodyJson,
                hashValue,
                results,
                accessToken
            )
        )
```
#### 截图
<img src="https://foruda.gitee.com/images/1737450328559903224/89f2f31b_854277.jpeg" alt="输入图片说明" title="微信图片_20250121170405.jpg" style="zoom:25%;" align="left"/>
<img src="https://foruda.gitee.com/images/1737450509000636302/3e65687f_854277.jpeg" alt="输入图片说明" title="微信图片_20250121170427.jpg" style="zoom:25%;" align="left"/>
<img src="https://foruda.gitee.com/images/1737450524020963098/5bc33eeb_854277.jpeg" alt="输入图片说明" title="微信图片_20250121170431.jpg" style="zoom:25%;" align="left"/>