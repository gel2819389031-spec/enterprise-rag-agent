export type HttpMethod = "GET" | "POST" | "PATCH" | "DELETE";

export type ApiCallOptions = {
  baseUrl: string;
  method: HttpMethod;
  path: string;
  query?: Record<string, string | number | undefined>;
  body?: unknown;
  formData?: FormData;
};

export type ApiCallResult = {
  ok: boolean;
  status: number;
  durationMs: number;
  bodyText: string;
  json: unknown;
};

function buildUrl(baseUrl: string, path: string, query?: ApiCallOptions["query"]) {
  const normalizedBase = baseUrl.replace(/\/$/, "");
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const url = new URL(`${normalizedBase}${normalizedPath}`, window.location.origin);

  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      url.searchParams.set(key, String(value));
    }
  });

  return url.toString();
}

export async function callApi(options: ApiCallOptions): Promise<ApiCallResult> {
  const startedAt = performance.now();
  const headers = new Headers();

  let body: BodyInit | undefined;
  if (options.formData) {
    body = options.formData;
  } else if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
    body = JSON.stringify(options.body);
  }

  const response = await fetch(buildUrl(options.baseUrl, options.path, options.query), {
    method: options.method,
    headers,
    body,
  });

  const bodyText = await response.text();
  let json: unknown = null;

  try {
    json = bodyText ? JSON.parse(bodyText) : null;
  } catch {
    json = bodyText;
  }

  return {
    ok: response.ok,
    status: response.status,
    durationMs: Math.round(performance.now() - startedAt),
    bodyText,
    json,
  };
}
