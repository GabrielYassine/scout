/**
 * A small fetch wrapper that standardizes JSON requests and turns failed
 * HTTP responses into HttpError objects with status, URL, and payload details.
 * @author s230632
 */

export class HttpError extends Error {
  constructor(message, { status = null, url = null, payload = null } = {}) {
    super(message);
    this.name = "HttpError";
    this.status = status;
    this.url = url;
    this.payload = payload;
  }
}

async function parseMaybeJson(res) {
  const contentType = res.headers.get("content-type") ?? "";
  const isJson = contentType.includes("application/json");

  if (!isJson) {
    return null;
  }

  try {
    return await res.json();
  } catch {
    return null;
  }
}

export async function fetchJson(url, init = {}) {
  // Automatically serialize object bodies and add the JSON content type when needed.
  const res = await fetch(url, {
    ...init,
    headers: {
      ...(init.headers ?? {}),
      ...(init.body ? { "Content-Type": "application/json" } : {}),
    },
    body:
      init.body && typeof init.body !== "string"
        ? JSON.stringify(init.body)
        : init.body,
  });

  if (!res.ok) {
    const payload = await parseMaybeJson(res);
    const message = payload?.message ?? `${res.status} ${res.statusText}`.trim();

    throw new HttpError(`Request failed: ${message}`, {
      status: res.status,
      url,
      payload,
    });
  }

  // 204 responses have no body, so trying to parse JSON would fail.
  if (res.status === 204) {
    return null;
  }

  return await res.json();
}