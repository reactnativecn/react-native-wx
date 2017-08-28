package cn.reactnative.modules.wx;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
import com.facebook.datasource.BaseDataSubscriber;
import com.facebook.datasource.DataSource;
import com.facebook.datasource.DataSubscriber;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.OrientedDrawable;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.RCTNativeAppEventEmitter;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXImageObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXMusicObject;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.modelmsg.WXVideoObject;
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.modelpay.PayResp;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WeChatModule extends ReactContextBaseJavaModule implements IWXAPIEventHandler {
    static private String appId = null;
    static private IWXAPI api = null;
    private static WeChatModule gModule = null;
    private static boolean gIsAppRegistered = false;

    private final static String NOT_REGISTERED = "registerApp required.";
    private final static String INVOKE_FAILED = "WeChat API invoke returns false.";

    private static final String RCTWXShareTypeNews = "news";
    private static final String RCTWXShareTypeImage = "image";
    private static final String RCTWXShareTypeText = "text";
    private static final String RCTWXShareTypeVideo = "video";
    private static final String RCTWXShareTypeAudio = "audio";

    private static final String RCTWXShareType = "type";
    private static final String RCTWXShareText = "text";
    private static final String RCTWXShareTitle = "title";
    private static final String RCTWXShareDescription = "description";
    private static final String RCTWXShareWebpageUrl = "webpageUrl";
    private static final String RCTWXShareImageUrl = "imageUrl";
    private static final String RCTWXShareThumbImageSize = "thumbImageSize";

    public WeChatModule(ReactApplicationContext reactContext) {

        super(reactContext);

        if (appId == null) {
            ApplicationInfo appInfo = null;
            try {
                appInfo = reactContext.getPackageManager().getApplicationInfo(reactContext.getPackageName(), PackageManager.GET_META_DATA);
            } catch (PackageManager.NameNotFoundException e) {
                throw new Error(e);
            }
            if (!appInfo.metaData.containsKey("WX_APPID")){
                throw new Error("meta-data WX_APPID not found in AndroidManifest.xml");
            }
            appId = appInfo.metaData.get("WX_APPID").toString();
        }
        _autoRegisterAppId();
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("isAppRegistered", gIsAppRegistered);
        return constants;
    }

    @Override
    public String getName() {
        return "RCTWeChatAPI";
    }

    @Override
    public void initialize() {
        super.initialize();
        gModule = this;
    }

    @Override
    public void onCatalystInstanceDestroy() {
        super.onCatalystInstanceDestroy();
        gModule = null;
    }

    @ReactMethod
    public void isWXAppInstalled(Callback callback) {
        callback.invoke(null, api.isWXAppInstalled());
    }

    @ReactMethod
    public void isWXAppSupportApi(Callback callback) {
        callback.invoke(null, api.isWXAppSupportAPI());
    }

    @ReactMethod
    public void login(ReadableMap config, Callback callback) {
        SendAuth.Req req = new SendAuth.Req();
        if (config.hasKey("scope")) {
            req.scope = config.getString("scope");
        }
        if (config.hasKey("state")) {
            req.state = config.getString("state");
        }
        else {
            req.state = new Date().toString();
        }
        callback.invoke(api.sendReq(req) ? null : INVOKE_FAILED);
    }

    @ReactMethod
    public void shareToTimeline(ReadableMap data, Callback callback) {
        _share(SendMessageToWX.Req.WXSceneTimeline, data, callback);
    }

    @ReactMethod
    public void shareToSession(ReadableMap data, Callback callback){
        _share(SendMessageToWX.Req.WXSceneSession, data, callback);
    }

    @ReactMethod
    public void pay(ReadableMap data, Callback callback){
        PayReq payReq = new PayReq();
        if (data.hasKey("partnerId")) {
            payReq.partnerId = data.getString("partnerId");
        }
        if (data.hasKey("prepayId")) {
            payReq.prepayId = data.getString("prepayId");
        }
        if (data.hasKey("nonceStr")) {
            payReq.nonceStr = data.getString("nonceStr");
        }
        if (data.hasKey("timeStamp")) {
            payReq.timeStamp = data.getString("timeStamp");
        }
        if (data.hasKey("sign")) {
            payReq.sign = data.getString("sign");
        }
        if (data.hasKey("package")) {
            payReq.packageValue = data.getString("package");
        }
        if (data.hasKey("extData")) {
            payReq.extData = data.getString("extData");
        }
        payReq.appId = appId;
        callback.invoke(api.sendReq(payReq) ? null : INVOKE_FAILED);
    }

    @Override
    public void onReq(BaseReq baseReq) {

    }

    @Override
    public void onResp(BaseResp baseResp) {
        WritableMap map = Arguments.createMap();
        map.putInt("errCode", baseResp.errCode);
        if (baseResp.errStr == null || baseResp.errStr.length()<=0) {
            map.putString("errStr", _getErrorMsg(baseResp.errCode));
        }
        else {
            map.putString("errStr", baseResp.errStr);
        }
        map.putString("transaction", baseResp.transaction);
        if (baseResp instanceof SendAuth.Resp) {
            SendAuth.Resp resp = (SendAuth.Resp)(baseResp);

            map.putString("type", "SendAuth.Resp");
            map.putString("code", resp.code);
            map.putString("state", resp.state);
            map.putString("url", resp.url);
            map.putString("lang", resp.lang);
            map.putString("country", resp.country);
            map.putString("appid", appId);
        }
        else if (baseResp instanceof SendMessageToWX.Resp){
            SendMessageToWX.Resp resp = (SendMessageToWX.Resp)(baseResp);
            map.putString("type", "SendMessageToWX.Resp");
        }
        else if (baseResp instanceof PayResp) {
            PayResp resp = (PayResp)(baseResp);
            map.putString("type", "Pay.Resp");
            map.putString("returnKey", resp.returnKey);
        }

        getReactApplicationContext()
                .getJSModule(RCTNativeAppEventEmitter.class)
                .emit("WeChat_Resp", map);
    }

    public static void handleIntent(Intent intent) {
        if (gModule != null) {
            api.handleIntent(intent, gModule);
        }
    }

    private String _getErrorMsg(int errCode) {
        switch (errCode) {
            case BaseResp.ErrCode.ERR_OK:
                return "成功";
            case BaseResp.ErrCode.ERR_COMM:
                return "普通错误类型";
            case BaseResp.ErrCode.ERR_USER_CANCEL:
                return "用户点击取消并返回";
            case BaseResp.ErrCode.ERR_SENT_FAILED:
                return "发送失败";
            case BaseResp.ErrCode.ERR_AUTH_DENIED:
                return "授权失败";
            case BaseResp.ErrCode.ERR_UNSUPPORT:
                return "微信不支持";
            default:
                return "失败";
        }
    }

    private void _autoRegisterAppId() {
        if (!gIsAppRegistered) {
            api = WXAPIFactory.createWXAPI(getReactApplicationContext(), appId, false);
            gIsAppRegistered = api.registerApp(appId);
        }
    }

    private void _share(final int scene, final ReadableMap data, final Callback callBack){

        if (data.hasKey(RCTWXShareImageUrl)) {
            String imageUrl = data.getString(RCTWXShareImageUrl);
            DataSubscriber<CloseableReference<CloseableImage>> dataSubscriber =
                    new BaseDataSubscriber<CloseableReference<CloseableImage>>() {
                        @Override
                        public void onNewResultImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                            // isFinished must be obtained before image, otherwise we might set intermediate result
                            // as final image.
                            boolean isFinished = dataSource.isFinished();
//                        float progress = dataSource.getProgress();
                            CloseableReference<CloseableImage> image = dataSource.getResult();
                            if (image != null) {
                                Drawable drawable = _createDrawable(image);
                                Bitmap bitmap = _drawable2Bitmap(drawable);
                                _share(scene, data, bitmap, callBack);
                            } else if (isFinished) {
                                _share(scene, data, null, callBack);
                            }
                            dataSource.close();
                        }
                        @Override
                        public void onFailureImpl(DataSource<CloseableReference<CloseableImage>> dataSource) {
                            dataSource.close();
                            _share(scene, data, null, callBack);
                        }

                        @Override
                        public void onProgressUpdate(DataSource<CloseableReference<CloseableImage>> dataSource) {
                        }
                    };
            ResizeOptions resizeOptions = null;
            if (!(data.hasKey(RCTWXShareType) && data.getString(RCTWXShareType).equals(RCTWXShareTypeImage))) {
                int size = 80;
                if (data.hasKey(RCTWXShareThumbImageSize)) {
                    size = data.getInt(RCTWXShareThumbImageSize);
                }
                resizeOptions = new ResizeOptions(size, size);
            }
            _downloadImage(imageUrl, resizeOptions, dataSubscriber);
        }
        else {
            _share(scene, data, null, callBack);
        }
    }

    private void _share(int scene, ReadableMap data, Bitmap image, Callback callback) {
        WXMediaMessage message = new WXMediaMessage();
        if (data.hasKey(RCTWXShareTitle)){
            message.title = data.getString(RCTWXShareTitle);
        }
        if (data.hasKey(RCTWXShareDescription)) {
            message.description = data.getString(RCTWXShareDescription);
        }

        String type = RCTWXShareTypeNews;
        if (data.hasKey(RCTWXShareType)) {
            type = data.getString(RCTWXShareType);
        }

        if (type.equals(RCTWXShareTypeText)) {
            WXTextObject object = new WXTextObject();
            if (data.hasKey(RCTWXShareText)) {
                object.text = data.getString(RCTWXShareText);
            }
            message.mediaObject = object;
        }
        else if (type.equals(RCTWXShareTypeImage)) {
            WXImageObject object = new WXImageObject();
            if (data.hasKey(RCTWXShareImageUrl)) {
                if (image != null) {
                    object.imageData = _Bitmap2Bytes(image);
                    Bitmap thumb = Bitmap.createScaledBitmap(image, 80, 80, true);
                    message.thumbData = _Bitmap2Bytes(thumb);
                }
            }
            message.mediaObject = object;
        }
        else {
            if (type.equals(RCTWXShareTypeNews)) {
                WXWebpageObject object = new WXWebpageObject();
                if (data.hasKey(RCTWXShareWebpageUrl)){
                    object.webpageUrl = data.getString(RCTWXShareWebpageUrl);
                }
                if (data.hasKey("extInfo")){
                    object.extInfo = data.getString("extInfo");
                }
                message.mediaObject = object;
            }
            else if (type.equals(RCTWXShareTypeVideo)) {
                WXMusicObject object = new WXMusicObject();
                if (data.hasKey(RCTWXShareWebpageUrl)) {
                    object.musicUrl = data.getString(RCTWXShareWebpageUrl);
                }
                message.mediaObject = object;
            }
            else if (type.equals(RCTWXShareTypeAudio)) {
                WXVideoObject object = new WXVideoObject();
                if (data.hasKey(RCTWXShareWebpageUrl)) {
                    object.videoUrl = data.getString(RCTWXShareWebpageUrl);
                }
                message.mediaObject = object;
            }

            if (image != null) {
                Log.e("share", "image no null");
                message.setThumbImage(image);
            }else {
                Log.e("share", "image null");
            }
        }

        //TODO: create Thumb Data.
        if (data.hasKey("mediaTagName")) {
            message.mediaTagName = data.getString("mediaTagName");
        }
        if (data.hasKey("messageAction")) {
            message.mediaTagName = data.getString("messageAction");
        }
        if (data.hasKey("messageExt")) {
            message.mediaTagName = data.getString("messageExt");
        }

        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.message = message;
        req.scene = scene;

        boolean success = api.sendReq(req);

        if (success == false) {
            callback.invoke("INVOKE_FAILED");
        }else {
            callback.invoke();
        }
            
    }

    private static @Nullable
    Uri getResourceDrawableUri(Context context, @Nullable String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        name = name.toLowerCase().replace("-", "_");
        int resId = context.getResources().getIdentifier(
                name,
                "drawable",
                context.getPackageName());
        return new Uri.Builder()
                .scheme(UriUtil.LOCAL_RESOURCE_SCHEME)
                .path(String.valueOf(resId))
                .build();
    }

    private void  _downloadImage(String imageUrl, ResizeOptions resizeOptions, DataSubscriber<CloseableReference<CloseableImage>> dataSubscriber) {

        Uri uri = null;
        try {
            uri = Uri.parse(imageUrl);
            // Verify scheme is set, so that relative uri (used by static resources) are not handled.
            if (uri.getScheme() == null) {
                uri = null;
            }
        } catch (Exception e) {
            // ignore malformed uri, then attempt to extract resource ID.
        }
        if (uri == null) {
            uri = getResourceDrawableUri(getReactApplicationContext(), imageUrl);
        } else {
        }

        ImageRequestBuilder builder = ImageRequestBuilder.newBuilderWithSource(uri);
        if (resizeOptions != null) {
            builder = builder.setResizeOptions(resizeOptions);
        }
        ImageRequest imageRequest = builder.build();

        ImagePipeline imagePipeline = Fresco.getImagePipeline();
        DataSource<CloseableReference<CloseableImage>> dataSource = imagePipeline.fetchDecodedImage(imageRequest, null);
        dataSource.subscribe(dataSubscriber, UiThreadImmediateExecutorService.getInstance());
    }

    private Drawable _createDrawable(CloseableReference<CloseableImage> image) {
        Preconditions.checkState(CloseableReference.isValid(image));
        CloseableImage closeableImage = image.get();
        if (closeableImage instanceof CloseableStaticBitmap) {
            CloseableStaticBitmap closeableStaticBitmap = (CloseableStaticBitmap) closeableImage;
            BitmapDrawable bitmapDrawable = new BitmapDrawable(
                    getReactApplicationContext().getResources(),
                    closeableStaticBitmap.getUnderlyingBitmap());
            if (closeableStaticBitmap.getRotationAngle() == 0 ||
                    closeableStaticBitmap.getRotationAngle() == EncodedImage.UNKNOWN_ROTATION_ANGLE) {
                return bitmapDrawable;
            } else {
                return new OrientedDrawable(bitmapDrawable, closeableStaticBitmap.getRotationAngle());
            }
        } else {
            throw new UnsupportedOperationException("Unrecognized image class: " + closeableImage);
        }
    }

    private Bitmap _drawable2Bitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else if (drawable instanceof NinePatchDrawable) {
            Bitmap bitmap = Bitmap
                    .createBitmap(
                            drawable.getIntrinsicWidth(),
                            drawable.getIntrinsicHeight(),
                            drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                                    : Bitmap.Config.RGB_565);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight());
            drawable.draw(canvas);
            return bitmap;
        } else {
            return null;
        }
    }

    private byte[] _Bitmap2Bytes(Bitmap bm){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 70, baos);
        return baos.toByteArray();
    }

}
