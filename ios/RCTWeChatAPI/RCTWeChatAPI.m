//
//  RCTWeChatAPI.m
//  RCTWeChatAPI
//
//  Created by LvBingru on 1/6/16.
//  Copyright © 2016 erica. All rights reserved.
//


#import "RCTLog.h"
#import "RCTWeChatAPI.h"
#import "WXApi.h"
#import "WXApiObject.h"
#import "RCTEventDispatcher.h"
#import "RCTBridge.h"
#import "RCTImageLoader.h"

#define RCTWXShareTypeNews @"news"
#define RCTWXShareTypeImage @"image"
#define RCTWXShareTypeText @"text"
#define RCTWXShareTypeVideo @"video"
#define RCTWXShareTypeAudio @"audio"

#define RCTWXShareType @"type"
#define RCTWXShareTitle @"title"
#define RCTWXShareText @"text"
#define RCTWXShareDescription @"description"
#define RCTWXShareWebpageUrl @"webpageUrl"
#define RCTWXShareImageUrl @"imageUrl"
#define RCTWXShareThumbImageSize @"thumbImageSize"

#define NOT_REGISTERED (@"registerApp required.")
#define INVOKE_FAILED (@"WeChat API invoke returns false.")

@interface RCTWeChatAPI()<WXApiDelegate>
@end

static NSString *gAppID = @"";
static BOOL gIsAppRegistered = false;

@implementation RCTWeChatAPI

@synthesize bridge = _bridge;

RCT_EXPORT_MODULE()

- (NSDictionary *)constantsToExport
{
    return @{ @"isAppRegistered":@(gIsAppRegistered)};
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

- (instancetype)init
{
    self = [super init];
    if (self) {
        [self _autoRegisterAPI];
        
        [[NSNotificationCenter defaultCenter] addObserver:self selector:@selector(handleOpenURL:) name:@"RCTOpenURLNotification" object:nil];
    }
    return self;
}

- (void)dealloc
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

RCT_EXPORT_METHOD(isWXAppInstalled:(RCTResponseSenderBlock)callback)
{
    callback(@[[NSNull null], @([WXApi isWXAppInstalled])]);
}

RCT_EXPORT_METHOD(isWXAppSupportApi:(RCTResponseSenderBlock)callback)
{
    callback(@[[NSNull null], @([WXApi isWXAppSupportApi])]);
}

RCT_EXPORT_METHOD(login:(NSDictionary *)config
                  :(RCTResponseSenderBlock)callback)
{
    SendAuthReq* req = [[SendAuthReq alloc] init];
    req.scope = config[@"scope"];
    req.state = config[@"state"]?:[NSDate date].description;
    BOOL success = [WXApi sendReq:req];
    callback(@[success ? [NSNull null] : INVOKE_FAILED]);
}

RCT_EXPORT_METHOD(shareToTimeline:(NSDictionary *)data
                  :(RCTResponseSenderBlock)callback)
{
    [self shareToWeixinWithData:data scene:WXSceneTimeline callback:callback];
}

RCT_EXPORT_METHOD(shareToSession:(NSDictionary *)data
                  :(RCTResponseSenderBlock)callback)
{
    [self shareToWeixinWithData:data scene:WXSceneSession callback:callback];
}

RCT_EXPORT_METHOD(pay:(NSDictionary *)data
                  :(RCTResponseSenderBlock)callback)
{
    PayReq* req             = [PayReq new];
    req.partnerId           = data[@"partnerId"];
    req.prepayId            = data[@"prepayId"];
    req.nonceStr            = data[@"nonceStr"];
    req.timeStamp           = [data[@"timeStamp"] unsignedIntValue];
    req.package             = data[@"package"];
    req.sign                = data[@"sign"];
    BOOL success = [WXApi sendReq:req];
    callback(@[success ? [NSNull null] : INVOKE_FAILED]);
}

- (void)handleOpenURL:(NSNotification *)note
{
    NSDictionary *userInfo = note.userInfo;
    NSString *url = userInfo[@"url"];
    [WXApi handleOpenURL:[NSURL URLWithString:url] delegate:self];
}

- (void)shareToWeixinWithData:(NSDictionary *)aData image:(UIImage *)aImage scene:(int)aScene callBack:(RCTResponseSenderBlock)callback
{
    SendMessageToWXReq* req = [SendMessageToWXReq new];
    req.scene = aScene;
    
    NSString *type = aData[RCTWXShareType];
    
    if ([type isEqualToString:RCTWXShareTypeText]) {
        req.bText = YES;
        
        NSString *text = aData[RCTWXShareText];
        if (text && [text isKindOfClass:[NSString class]]) {
            req.text = text;
        }
    }
    else {
        req.bText = NO;
        
        WXMediaMessage* mediaMessage = [WXMediaMessage new];
        
        mediaMessage.title = aData[RCTWXShareTitle];
        mediaMessage.description = aData[RCTWXShareDescription];
        mediaMessage.mediaTagName = aData[@"mediaTagName"];
        mediaMessage.messageAction = aData[@"messageAction"];
        mediaMessage.messageExt = aData[@"messageExt"];
        
        [mediaMessage setThumbImage:aImage];
        if ([type isEqualToString:RCTWXShareTypeImage]) {
            WXImageObject *imageObject = [WXImageObject new];
            imageObject.imageData = UIImageJPEGRepresentation(aImage, 0.7);
            mediaMessage.mediaObject = imageObject;
        }
        else {
            if (type.length <= 0 || [type isEqualToString:RCTWXShareTypeNews]) {
                WXWebpageObject* webpageObject = [WXWebpageObject new];
                webpageObject.webpageUrl = aData[RCTWXShareWebpageUrl];
                mediaMessage.mediaObject = webpageObject;
                
                if (webpageObject.webpageUrl.length<=0) {
                    callback(@[@"webpageUrl不能为空"]);
                    return;
                }
            }
            else if ([type isEqualToString:RCTWXShareTypeAudio]) {
                WXMusicObject *musicObject = [WXMusicObject new];
                musicObject.musicUrl = aData[@"musicUrl"];
                musicObject.musicLowBandUrl = aData[@"musicLowBandUrl"];
                musicObject.musicDataUrl = aData[@"musicDataUrl"];
                musicObject.musicLowBandDataUrl = aData[@"musicLowBandDataUrl"];
                mediaMessage.mediaObject = musicObject;
            }
            else if ([type isEqualToString:RCTWXShareTypeVideo]) {
                WXVideoObject *videoObject = [WXVideoObject new];
                videoObject.videoUrl = aData[@"videoUrl"];
                videoObject.videoLowBandUrl = aData[@"videoLowBandUrl"];
                mediaMessage.mediaObject = videoObject;
            }
        }
        
        req.message = mediaMessage;
    }
    
    BOOL success = [WXApi sendReq:req];
    if (success == NO)
    {
        callback(@[INVOKE_FAILED]);
    }
}


- (void)shareToWeixinWithData:(NSDictionary *)aData scene:(int)aScene callback:(RCTResponseSenderBlock)aCallBack
{
    NSString *imageUrl = aData[RCTWXShareImageUrl];
    if (imageUrl.length && _bridge.imageLoader) {
        CGSize size = CGSizeZero;
        if (![aData[RCTWXShareType] isEqualToString:RCTWXShareTypeImage]) {
            CGFloat thumbImageSize = 80;
            if (aData[RCTWXShareThumbImageSize]) {
                thumbImageSize = [aData[RCTWXShareThumbImageSize] floatValue];
            }
            size = CGSizeMake(thumbImageSize,thumbImageSize);
        }
        [_bridge.imageLoader loadImageWithURLRequest:[RCTConvert NSURLRequest:imageUrl] size:size scale:1 clipped:FALSE resizeMode:UIViewContentModeScaleToFill progressBlock:nil partialLoadBlock: nil completionBlock:^(NSError *error, UIImage *image) {
            [self shareToWeixinWithData:aData image:image scene:aScene callBack:aCallBack];
        }];
    }
    else {
        [self shareToWeixinWithData:aData image:nil scene:aScene callBack:aCallBack];
    }
}

- (void)_autoRegisterAPI
{
    if (gAppID.length > 0 && gIsAppRegistered) {
        return;
    }
    
    NSArray *list = [[[NSBundle mainBundle] infoDictionary] valueForKey:@"CFBundleURLTypes"];
    for (NSDictionary *item in list) {
        NSString *name = item[@"CFBundleURLName"];
        if ([name isEqualToString:@"weixin"]) {
            NSArray *schemes = item[@"CFBundleURLSchemes"];
            if (schemes.count > 0)
            {
                gAppID = schemes[0];
                break;
            }
        }
    }
    gIsAppRegistered = [WXApi registerApp:gAppID];
}


- (NSString *)_getErrorMsg:(int)code {
    switch (code) {
        case WXSuccess:
            return @"成功";
        case WXErrCodeCommon:
            return @"普通错误类型";
        case WXErrCodeUserCancel:
            return @"用户点击取消并返回";
        case WXErrCodeSentFail:
            return @"发送失败";
        case WXErrCodeAuthDeny:
            return @"授权失败";
        case WXErrCodeUnsupport:
            return @"微信不支持";
        default:
            return @"失败";
    }
}

#pragma mark - wx callback
- (void)onReq:(BaseReq*)req
{
    
}

- (void)onResp:(BaseResp*)resp
{
    NSMutableDictionary *body = @{@"errCode":@(resp.errCode)}.mutableCopy;
    body[@"errCode"] = @(resp.errCode);
    
    if (resp.errStr == nil || resp.errStr.length<=0) {
        body[@"errMsg"] = [self _getErrorMsg:resp.errCode];
    }
    else{
        body[@"errMsg"] = resp.errStr;
    }
    
    if([resp isKindOfClass:[SendMessageToWXResp class]])
    {
        SendMessageToWXResp *r = (SendMessageToWXResp *)resp;
        body[@"lang"] = r.lang;
        body[@"country"] =r.country;
        body[@"type"] = @"SendMessageToWX.Resp";
    }
    else if ([resp isKindOfClass:[SendAuthResp class]]) {
        SendAuthResp *r = (SendAuthResp *)resp;
        body[@"state"] = r.state;
        body[@"lang"] = r.lang;
        body[@"country"] =r.country;
        body[@"type"] = @"SendAuth.Resp";
        body[@"appid"] = gAppID;
        body[@"code"]= r.code;
    }
    else if([resp isKindOfClass:[PayResp class]]) {
        PayResp *r = (PayResp *)resp;
        body[@"appid"] = gAppID;
        body[@"returnKey"] = r.returnKey;
        body[@"type"]= @"Pay.Resp";
    }
    
    [self.bridge.eventDispatcher sendAppEventWithName:@"WeChat_Resp" body:body];
}

@end
