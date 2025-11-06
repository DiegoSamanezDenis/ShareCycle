export const API_BASE_URL =
  import.meta.env.VITE_API_URL ?? "http://localhost:8081/api";

export type AppConfig = {
  apiUrl: string;
};

export const appConfig: AppConfig = Object.freeze({
  apiUrl: API_BASE_URL,
});
