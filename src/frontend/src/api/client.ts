import { appConfig } from '../config/env';

type ApiRequestOptions = RequestInit & {
  token?: string | null;
};

export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {}
): Promise<T> {
  const { token, headers, ...rest } = options;
  const requestHeaders = new Headers(headers ?? {});

  if (rest.body && typeof rest.body === 'string' && !requestHeaders.has('Content-Type')) {
    requestHeaders.set('Content-Type', 'application/json');
  }

  // Attach auth token: prefer explicit token, otherwise read from storage
  const resolvedToken = token ?? getTokenFromStorage();
  if (resolvedToken) requestHeaders.set('Authorization', resolvedToken);

  const response = await fetch(`${appConfig.apiUrl}${path}`, {
    ...rest,
    headers: requestHeaders
  });

  // --- CALL extractErrorMessage HERE ---
  if (!response.ok) {
    const message = await extractErrorMessage(response);
    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get('content-type') ?? '';
  if (contentType.includes('application/json')) {
    return (await response.json()) as T;
  }

  return undefined as T;
}

// Keep only **one** extractErrorMessage here
async function extractErrorMessage(response: Response): Promise<string> {
  try {
    const cloned = response.clone(); // clone to avoid "body already read"
    const data = await cloned.json();
    if (data && typeof data.message === 'string') {
      return data.message;
    }
  } catch {
    // ignore JSON parse failures
  }

  try {
    const text = await response.text();
    if (text) return text;
  } catch {
    // ignore text failures
  }

  return `Request failed with status ${response.status}`;
}

function getTokenFromStorage(): string | null {
  try {
    const raw = localStorage.getItem('sharecycle.auth');
    if (!raw) return null;
    const parsed = JSON.parse(raw) as { token?: string | null };
    return parsed?.token ?? null;
  } catch {
    return null;
  }
}
