# 工具类实例

## [DisplayTool](DisplayTool.kt)

使用 computeMaximumWindowMetrics(activity)方法计算给定 Activity 的最大可能窗口尺寸，将高度和宽度封装成一个Pair对象并返回

为了向下兼容，参考了oppo开放平台的文档：[三方应用开发适配指导书-3.2.1 正确使用应用资源](https://open.oppomobile.com/new/developmentDoc/info?id=11308)

## [IntentTool](IntentTool.kt)

通过 Intent 来携带数据，实现分享 URL 和浏览器打开 URL。

如果希望通过包名筛选分享选择器，可以在[这篇文章](https://zhuanlan.zhihu.com/p/165824744)中查找包名，由于时间关系暂时没有实现自定义选择器（忘了做了，写文档的时候才想起来）。

## [IpAddressTool](IpAddressTool.kt)

获取当前设备的Wi-Fi网络连接，在有可用的IPv4地址时，通过Flow发送

参考[文章](https://blog.csdn.net/linvisf/article/details/133949499)

Flow: [callbackFlow使用心得](https://blog.csdn.net/weixin_44235109/article/details/120824332)

## [NumberConvertTool](NumberConvertTool.kt)

单位转换，通过位移操作代替除法，轻微地提高性能哈哈
