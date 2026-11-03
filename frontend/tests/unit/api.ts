// Simula a API trocando o fetch global: cada chamada recebe a resposta da primeira regra que casar.
import { vi } from "vitest";

export type Rota = { metodo?: string; caminho: string | RegExp; status?: number; corpo?: unknown };

export const respostaJson = (status: number, corpo?: unknown) =>
  new Response(corpo === undefined ? null : JSON.stringify(corpo), { status, headers: { "Content-Type": "application/json" } });

export function simularApi(rotas: Rota[]) {
  const fetchFalso = vi.fn(async (entrada: RequestInfo | URL, init?: RequestInit) => {
    const url = new URL(String(entrada));
    const metodo = init?.method || "GET";
    const rota = rotas.find((r) =>
      (r.metodo || "GET") === metodo && (typeof r.caminho === "string" ? url.pathname === r.caminho : r.caminho.test(url.pathname)));
    if (!rota) return respostaJson(404, { mensagem: `Rota não simulada: ${metodo} ${url.pathname}` });
    return respostaJson(rota.status ?? 200, rota.corpo);
  });
  vi.stubGlobal("fetch", fetchFalso);
  return fetchFalso;
}
