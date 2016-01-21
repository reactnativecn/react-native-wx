import { NativeAppEventEmitter, NativeModules } from 'react-native';
import promisify from 'es6-promisify';

const WeChatAPI = NativeModules.WeChatAPI;

function translateError(err, result) {
  if (!err) {
    return this.resolve(result);
  }
  if (typeof err === 'object') {
    if (err instanceof Error) {
      return this.reject(ret);
    }
    return this.reject(Object.assign(new Error(err.message), { errCode: err.errCode }));
  } else if (typeof err === 'string') {
    return this.reject(new Error(err));
  }
  this.reject(Object.assign(new Error(), { origin: err }));
}

// Save callback and wait for future event.
let savedCallback = undefined;
function waitForResponse(type) {
  return new Promise((resolve, reject) => {
    if (savedCallback) {
      savedCallback('User canceled.');
    }
    savedCallback = result => {
      if (result.type !== type) {
        //
        //if (__DEV__) {
        //  throw new Error('Unsupported response type: ' + resp.type);
        //}
        return;
      }
      savedCallback = undefined;
      if (result.errCode !== 0) {
        const err = new Error(result.errMsg);
        err.errCode = result.errCode;
        reject(err);
      } else {
        resolve(result);
      }
    };
  });
}

NativeAppEventEmitter.addListener('WeChat_Resp', resp => {
  const callback = savedCallback;
  savedCallback = undefined;
  callback && callback(resp);
});


function wrapCheckApi(nativeFunc) {
  if (!nativeFunc) {
    return undefined;
  }

  const promisified = promisify(nativeFunc, translateError);
  return (...args) => {
    return promisified(...args);
  };
}

export const isWXAppInstalled = wrapCheckApi(WeChatAPI.isWXAppInstalled);
export const isWXAppSupportApi = wrapCheckApi(WeChatAPI.isWXAppSupportApi);

function wrapApi(nativeFunc) {
  if (!nativeFunc) {
    return undefined;
  }

  const promisified = promisify(nativeFunc, translateError);
  return async function (...args) {
    if (!WeChatAPI.isAppRegistered) {
      throw new Error('注册应用失败');
    }
    const checkInstalled = await isWXAppInstalled();
    if (!checkInstalled) {
      throw new Error('没有安装微信!');
    }
    const checkSupport = await isWXAppSupportApi();
    if (!checkSupport) {
      throw new Error('微信版本不支持');
    }
    return await promisified(...args);
  };
}

const nativeSendAuthRequest = wrapApi(WeChatAPI.login);
const nativeShareToTimelineRequest = wrapApi(WeChatAPI.shareToTimeline);
const nativeShareToSessionRequest = wrapApi(WeChatAPI.shareToSession);
const nativePayRequest = wrapApi(WeChatAPI.pay);

export function login(config) {
  const scope = (config && config.scope) || 'snsapi_userinfo';
  return nativeSendAuthRequest({scope})
      .then(() => waitForResponse("SendAuth.Resp"));
}

export function shareToTimeline(data) {
  return nativeShareToTimelineRequest(data)
      .then(() => waitForResponse("SendMessageToWX.Resp"));
}

export function shareToSession(data) {
  return nativeShareToSessionRequest(data)
      .then(() => waitForResponse("SendMessageToWX.Resp"));
}

export function pay(data) {
  return nativePayRequest(data)
      .then(() => waitForResponse("Pay.Resp"));
}

