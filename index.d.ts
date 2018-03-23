export interface ITextShareData {
  type: "text";
  text: string;
}

export interface IImageShareData {
  type: "image";
  imageUrl: string;
  title: string;
  description: string;
}

export interface IWebShareData {
  type: "news";
  imageUrl: string;
  webpageUrl: string;
  title: string;
  description: string;
}

export interface IPayReq {
  partnerId: string;
  prepayId: string;
  nonceStr: string;
  timeStamp: string;
  package: string;
  sign: string;
}

export interface IPayResp {
  errCode: number;
  errMsg: string;
  appid: string;
  returnKey: string;
}

export interface ILoginResp {
  code: string;
  appid: string;
  lang: string;
  country: string;
  errCode?: number;
  errMsg?: string;
}

export function isWXAppInstalled(): Promise<boolean>;

export function isWXAppSupportApi(): Promise<boolean>;

export function login(data: { scope: string }): Promise<ILoginResp>;

export function shareToTimeline(
  data: ITextShareData | IImageShareData | IWebShareData
): Promise<void>;

export function shareToSession(
  data: ITextShareData | IImageShareData | IWebShareData
): Promise<void>;

export function pay(data: IPayReq): Promise<IPayResp>;
