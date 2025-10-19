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

  if (token) {
    requestHeaders.set('Authorization', token);
  }

  const response = await fetch(`${appConfig.apiUrl}${path}`, {
    ...rest,
    headers: requestHeaders
  });

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

async function extractErrorMessage(response: Response): Promise<string> {
  try {
    const data = await response.json();
    if (data && typeof data.message === 'string') {
      return data.message;
    }
  } catch {
    // ignore JSON parse failures
  }

  const text = await response.text();
  if (text) {
    return text;
  }

  return `Request failed with status ${response.status}`;
}
