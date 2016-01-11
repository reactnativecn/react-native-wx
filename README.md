# react-native-wx

React Native的微信插件, 包括登录、分享、react-native版本需要0.17.0及以上
## 如何安装

### 首先安装npm包

```bash
npm install react-native-wx --save
```

### link
```bash
rnpm link
```

#### Note: rnpm requires node version 4.1 or higher


### iOS工程配置

在工程target的`Build Phases->Link Binary with Libraries`中加入`、libsqlite3.tbd、libc++、liz.tbd、CoreTelephony.framework`


在`Info->URL Types` 中增加QQ的scheme： `Identifier` 设置为`weixin`, `URL Schemes` 设置为你注册的微信开发者账号中的APPID

在你工程的`AppDelegate.m`文件中添加如下代码：

```
#import "RCTLinkingManager.h"


- (BOOL)application:(UIApplication *)application openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation {
  return [RCTLinkingManager application:application openURL:url sourceApplication:sourceApplication annotation:annotation];
}

```

### iOS9的适配问题

#### 1.对传输安全的支持
在iOS9中，默认需要为每次网络传输建立SSL，解决方法是在应用plist文件中设置
-
	<key>NSAppTransportSecurity</key>
	<dict>
	<key>NSAllowsArbitraryLoads</key>
	</true>
	</dict>

#### 2.对应用跳转的支持
在iOS9中跳转第三方应用需要在应用的plist文件中添加白名单
-
	<key>LSApplicationQueriesSchemes</key>
	<array>
		<string>weixin</string>
		<string>wechat</string>
	</array>
	


### 安装Android工程

在app的AndroidManifest.xml中`<application>`标签中添加如下代码：

```
	<meta-data android:name="WX_APPID" android:value="${WX_APPID}" />
```

`android/app/build.gradle`里，defaultConfig栏目下添加如下代码：

```
		manifestPlaceholders = [
            WX_APPID: "微信的APPID"		//在此修改微信APPID
        ]
```


确保你的MainActivity.java中有`onActivityResult`的实现：

```java
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        mReactInstanceManager.onActivityResult(requestCode, resultCode, data);
    }
```

## 如何使用

### 引入包

```
import * as WechatAPI from 'react-native-wx';
```

### API

#### WechatAPI.login(config)

```javascript
// 登录参数 
config : {	
	scope: 权限设置, // 默认 'snsapi_userinfo'
}
```

返回一个`Promise`对象。成功时的回调为一个类似这样的对象：

```javascript
{
	"code": "",
	"appid": "",
	"lang": "",
	"country": "",
}
```

#### WeiboAPI.shareToTimeline(data)
分享到朋友圈
#### WeiboAPI.shareToSession(data)
分享到好友

```javascript
// 分享文字
{	
	type: 'text', 
	text: 文字内容,
}
```

```javascript
// 分享图片
{	
	type: 'image',
	imageUrl: 图片地址,
	title : 标题,
	description : 描述,
}
```

```javascript
// 分享网页
{	
	type: 'news',
	title : 标题,
	description : 描述,	
	webpageUrl : 链接地址,
	imageUrl: 缩略图地址,
}
```

#### WeiboAPI.pay(data)
```javascript
// 登录参数 
data : {	
	partnerId: "",
	prepayId: "",
	nonceStr: "",
	timeStamp: "",
	package: "",
	sign: "",
}
```

返回一个`Promise`对象。成功时的回调为一个类似这样的对象：

```javascript
{
	"appid": "",
	"returnKey": ""
}
```

## 常见问题

#### Android: 调用WeiboAPI.login()没有反应

通常出现这个原因是因为Manifest没有配置好，检查Manifest中有关Activity的配置。

#### Android: 已经成功激活微博登录，但回调没有被执行

通常出现这个原因是因为MainActivity.java中缺少onActivityResult的调用。
