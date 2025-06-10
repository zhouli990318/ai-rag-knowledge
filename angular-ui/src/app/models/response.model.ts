export interface Response<T> {
  code: string;
  info: string;
  data: T;
}