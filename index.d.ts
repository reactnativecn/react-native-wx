export interface ITextShareData {
  type: 'text';
  text: string;
}

export interface IImageShareData {
  type: 'image';
  imageUrl: string;
  title: string;
  description: string;
}

export interface IWebShareData {
  type: 'news';
  imageUrl: string;
  webpageUrl: string;
  title: string;
  description: string;
}

export interface IPayData {
  partnerId: string;
  prepayId: string;
  nonceStr: string;
  timeStamp: string;
  package: string;
  sign: string;
}

export function isWXAppInstalled(): Promise<boolean>;

export function isWXAppSupportApi(): Promise<boolean>;

export function login(data: {
  scope: string;
}): Promise<{
  code: string;
  appid: string;
  lang: string;
  country: string;
  errCode?: number;
  errMsg?: string;
}>;

export function shareToTimeline(
  data: ITextShareData | IImageShareData | IWebShareData,
): Promise<void>;

export function shareToSession(
  data: ITextShareData | IImageShareData | IWebShareData,
): Promise<void>;

export function pay(
  data: IPayData,
): Promise<{
  errCode: string;
  errMsg: string;
  appid: string;
  returnKey: string;
}>;
