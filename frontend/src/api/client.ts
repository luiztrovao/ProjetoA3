// Cliente HTTP da API: envia o token, converte os erros e avisa quando a sessão expira.

export const API_URL = (import.meta.env.VITE_API_URL || "http://localhost:8080").replace(/\/+$/, "");

const TOKEN_KEY = "ponto-e-sabor:token";

let memoryToken: string | null = null;

export type CampoErro = { campo: string; mensagem: string };

/** Formato de erro devolvido pela API: `{status, mensagem, campos[]}`. */
export class ApiError extends Error {
  readonly status: number;
  readonly campos: CampoErro[];

  constructor(status: number, mensagem: string, campos: CampoErro[] = []) {
    super(mensagem);
    this.name = "ApiError";
    this.status = status;
    this.campos = campos;
  }

  /** Erros por campo, já com o nome usado no formulário (via `mapa`, quando informado). */
  fieldErrors(mapa: Record<string, string> = {}) {
    return this.campos.reduce<Record<string, string>>((erros, { campo, mensagem }) => {
      const nome = mapa[campo] || campo;
      if (!erros[nome]) erros[nome] = mensagem;
      return erros;
    }, {});
  }
}

export const errorMessage = (error: unknown, fallback = "Não foi possível concluir a operação. Tente novamente.") =>
  error instanceof ApiError ? error.message : fallback;

// Token: localStorage quando "Lembrar de mim" está marcado, senão sessionStorage.
export const tokenStorage = {
  get(): string | null {
    try {
      return window.localStorage.getItem(TOKEN_KEY) || window.sessionStorage.getItem(TOKEN_KEY);
    } catch {
      return memoryToken;
    }
  },
  set(token: string, remember: boolean) {
    memoryToken = token;
    try {
      window.localStorage.removeItem(TOKEN_KEY);
      window.sessionStorage.removeItem(TOKEN_KEY);
      (remember ? window.localStorage : window.sessionStorage).setItem(TOKEN_KEY, token);
    } catch {
      // Sem acesso ao armazenamento do navegador: a sessão fica só em memória.
    }
  },
  clear() {
    memoryToken = null;
    try {
      window.localStorage.removeItem(TOKEN_KEY);
      window.sessionStorage.removeItem(TOKEN_KEY);
    } catch {
      // Nada a limpar.
    }
  },
};

let unauthorizedHandler: (() => void) | null = null;

/** Chamado quando a API responde 401 (token ausente, inválido ou expirado). */
export const onUnauthorized = (handler: (() => void) | null) => {
  unauthorizedHandler = handler;
};

type Query = Record<string, string | number | boolean | null | undefined>;

type RequestOptions = {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  query?: Query;
  /** No login, o 401 significa usuário ou senha incorretos e não deve encerrar a sessão. */
  skipAuthRedirect?: boolean;
};

const buildUrl = (path: string, query?: Query) => {
  const url = new URL(`${API_URL}${path}`);
  Object.entries(query || {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") url.searchParams.set(key, String(value));
  });
  return url.toString();
};

export async function request<T>(path: string, { method = "GET", body, query, skipAuthRedirect }: RequestOptions = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  const token = tokenStorage.get();
  if (token) headers.Authorization = `Bearer ${token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";

  let response: Response;
  try {
    response = await fetch(buildUrl(path, query), {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new ApiError(0, "Não foi possível conectar ao servidor. Verifique se a API está em execução.");
  }

  const text = await response.text();
  let data: unknown = null;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = null;
    }
  }

  if (!response.ok) {
    const erro = (data || {}) as { mensagem?: string; campos?: CampoErro[] };
    const mensagem = erro.mensagem || (response.status === 403
      ? "Você não possui permissão para acessar esta funcionalidade."
      : "Não foi possível concluir a operação. Tente novamente.");
    if (response.status === 401 && !skipAuthRedirect) {
      tokenStorage.clear();
      unauthorizedHandler?.();
    }
    throw new ApiError(response.status, mensagem, Array.isArray(erro.campos) ? erro.campos : []);
  }

  return data as T;
}
