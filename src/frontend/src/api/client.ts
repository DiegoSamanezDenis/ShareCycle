//api/client.ts
import { appConfig } from "../config/env";

type ApiRequestOptions = RequestInit & {
  token?: string | null;
  skipAuth?: boolean;
};

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const { token, skipAuth = false, headers, ...rest } = options;
  const requestHeaders = new Headers(headers ?? {});

  if (
    rest.body &&
    typeof rest.body === "string" &&
    !requestHeaders.has("Content-Type")
  ) {
    requestHeaders.set("Content-Type", "application/json");
  }

  // Attach auth token: prefer explicit token, otherwise read from storage
  if (!skipAuth) {
    const resolvedToken = token ?? getTokenFromStorage();
    if (resolvedToken) {
      requestHeaders.set("Authorization", `Bearer ${resolvedToken}`);
    }
  }

  const response = await fetch(`${appConfig.apiUrl}${path}`, {
    ...rest,
    headers: requestHeaders,
  });

  if (!response.ok) {
    const message = await extractErrorMessage(response);
    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    return (await response.json()) as T;
  }

  return undefined as T;
}

async function extractErrorMessage(response: Response): Promise<string> {
  try {
    const data = await response.clone().json();
    if (data && typeof data.message === "string" && data.message.trim()) {
      return data.message.trim();
    }
  } catch {
    // ignore JSON parse failures
  }

  try {
    const text = await response.text();
    if (text.trim()) {
      return text.trim();
    }
  } catch {
    // ignore text parse failures (stream already consumed, etc.)
  }

  if (response.statusText) {
    return response.statusText;
  }

  if (response.status === 401) {
    return "Unauthorized. Please sign in again.";
  }

  return `Request failed with status ${response.status}`;
}

function getTokenFromStorage(): string | null {
  try {
    const raw = localStorage.getItem("sharecycle.auth");
    if (!raw) return null;
    const parsed = JSON.parse(raw) as { token?: string | null };
    return parsed?.token ?? null;
  } catch {
    return null;
  }
}
