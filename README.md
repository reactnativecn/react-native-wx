# react-native-wx

React Native的微信插件, 包括登录、分享
#### note: react-native版本需要0.17.0及以上

## 如何安装

### 1.首先安装npm包

```bash
npm install react-native-wx --save
```

### 2.link
#### 自动link方法~rnpm requires node version 4.1 or higher

```bash
rnpm link
```
link成功命令行会提示

```bash
rnpm info Linking react-native-wx android dependency 
rnpm info Linking react-native-wx ios dependency
```

#### 手动link~（如果不能够自动link）
#####ios
a.打开XCode's工程中, 右键点击Libraries文件夹 ➜ Add Files to <...>
b.去node_modules ➜ react-native-wx ➜ ios ➜ 选择 RCTWeChat.xcodeproj
c.在工程Build Phases ➜ Link Binary With Libraries中添加libRCTWeChat.a

#####Android

```
// file: android/settings.gradle
...

include ':react-native-wx'
project(':react-native-wx').projectDir = new File(settingsDir, '../node_modules/react-native-wx/android')
```

```
// file: android/app/build.gradle
...

dependencies {
    ...
    compile project(':react-native-wx')
}
```
`android/app/src/main/java/<你的包名>/MainActivity.java`中，`public class MainActivity`之前增加：

```java
import cn.reactnative.modules.weibo.WeiboPackage;
```

如果react-native版本 <0.18.0
`.addPackage(new MainReactPackage())`之后增加：

```java
.addPackage(new WeChatPackage())
```
如果react-native版本 >=0.18.0
在`new MainReactPackage()`之后增加
```java
,new WeChatPackage()
```


### 3.工程配置
#### iOS配置

在工程target的`Build Phases->Link Binary with Libraries`中加入`、libsqlite3.tbd、libc++、liz.tbd、CoreTelephony.framework`


在`Info->URL Types` 中增加微信的scheme： `Identifier` 设置为`weixin`（这个拼写不能错哦）, `URL Schemes` 设置为你注册的微信开发者账号中的APPID

如果react-native版本>=0.17.0, 在你工程的`AppDelegate.m`文件中添加如下代码：

```
#import "../Libraries/LinkingIOS/RCTLinkingManager.h"

- (BOOL)application:(UIApplication *)application openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation {
  return [RCTLinkingManager application:application openURL:url sourceApplication:sourceApplication annotation:annotation];
}
```

如果升级有困难，react-native版本实在是<0.17.0, 在你工程的`AppDelegate.m`文件中添加如下代码：

```
- (BOOL)application:(UIApplication *)application openURL:(NSURL *)url sourceApplication:(NSString *)sourceApplication annotation:(id)annotation {
  [[NSNotificationCenter defaultCenter] postNotificationName:@"RCTOpenURLNotification" object:nil userInfo:@{@"url": url.absoluteString}];
  return YES;
}

```


##### iOS9的适配问题

###### a.对传输安全的支持
在iOS9中，默认需要为每次网络传输建立SSL，解决方法是在应用plist文件中设置
-
	<key>NSAppTransportSecurity</key>
	<dict>
	<key>NSAllowsArbitraryLoads</key>
	</true>
	</dict>

###### b.对应用跳转的支持
在iOS9中跳转第三方应用需要在应用的plist文件中添加白名单
-
	<key>LSApplicationQueriesSchemes</key>
	<array>
		<string>weixin</string>
		<string>wechat</string>
	</array>
	


#### Android配置

在`android/app/build.gradle`里，defaultConfig栏目下添加如下代码：

```
manifestPlaceholders = [
	// 如果有多项，每一项之间需要用逗号分隔
    WX_APPID: "微信的APPID"		//在此修改微信APPID
]
```


如果react-native版本<0.18.0,确保你的MainActivity.java中有`onActivityResult`的实现：

```java
private ReactInstanceManager mReactInstanceManager;
@Override
public void onActivityResult(int requestCode, int resultCode, Intent data){
    super.onActivityResult(requestCode, resultCode, data);
    mReactInstanceManager.onActivityResult(requestCode, resultCode, data);
}
```
在你的包名相应目录下新建一个wxapi目录，并在该wxapi目录下新增一个WXEntryActivity类，该类继承自Activity（例如应用程序的包名为net.sourceforge.simcpux，则新添加的类的包名为net.sourceforge.simcpux.wxapi）

```java
package net.sourceforge.simcpux.wxapi; // net.sourceforge.simcpux处为你的包名

import android.app.Activity;
import android.os.Bundle;

import cn.reactnative.modules.wx.WeChatModule;

public class WXEntryActivity extends Activity{
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WeChatModule.handleIntent(getIntent());
        finish();
    }
}
		
```


##### note:Android还需要对keystore文件进行md5签名之后，在开放平台的应用设置中设置相应签名才可以成功使用，具体方法见开放平台说明


## 如何使用

### 引入包

```
import * as WechatAPI from 'react-native-wx';
```

### API
#### WechatAPI.isWXAppInstalled()
返回一个`Promise`对象

#### WechatAPI.isWXAppSupportApi()
返回一个`Promise`对象

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
	errCode: "",
	errMsg: "",
	appid: "",
	returnKey: "",
}
```
