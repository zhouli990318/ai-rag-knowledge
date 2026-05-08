export interface ApiResponse<T> {
  code: number;
  data: T;
  message: string | null;
  timestamp?: string;
}
