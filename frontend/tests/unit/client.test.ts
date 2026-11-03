// Cliente HTTP (unitário, com o fetch simulado): token, erros da API e sessão expirada.
import { describe, expect, it, vi } from "vitest";
import { ApiError, errorMessage, onUnauthorized, request, tokenStorage } from "../../src/api/client";
import { simularApi } from "./api";

describe("request", () => {
  it("envia o token e o corpo em JSON e monta a query sem valores vazios", async () => {
    tokenStorage.set("token-de-teste", false);
    const fetchFalso = simularApi([{ metodo: "POST", caminho: "/api/vendas", status: 201, corpo: { id: 1 } }]);

    const resposta = await request<{ id: number }>("/api/vendas", {
      method: "POST", body: { total: 10 }, query: { termo: "x", status: "", page: 0, vendedorId: undefined },
    });

    expect(resposta).toEqual({ id: 1 });
    const [url, init] = fetchFalso.mock.calls[0];
    expect(String(url)).toBe("http://api.test/api/vendas?termo=x&page=0");
    expect(init?.headers).toMatchObject({ Authorization: "Bearer token-de-teste", "Content-Type": "application/json" });
    expect(init?.body).toBe(JSON.stringify({ total: 10 }));
  });

  it("converte o erro da API com os campos inválidos", async () => {
    simularApi([{ metodo: "POST", caminho: "/api/clientes", status: 400, corpo: {
      mensagem: "Existem campos inválidos.", campos: [{ campo: "cpf", mensagem: "CPF inválido." }, { campo: "cpf", mensagem: "Outro." }],
    } }]);

    const erro = await request("/api/clientes", { method: "POST", body: {} }).catch((e: unknown) => e);

    expect(erro).toBeInstanceOf(ApiError);
    expect((erro as ApiError).status).toBe(400);
    expect((erro as ApiError).fieldErrors({ cpf: "documento" })).toEqual({ documento: "CPF inválido." });
    expect(errorMessage(erro)).toBe("Existem campos inválidos.");
  });

  it("401 apaga o token e avisa a tela de login", async () => {
    tokenStorage.set("expirado", true);
    const aviso = vi.fn();
    onUnauthorized(aviso);
    simularApi([{ caminho: "/api/conta", status: 401, corpo: { mensagem: "Sessão expirada." } }]);

    await expect(request("/api/conta")).rejects.toThrow("Sessão expirada.");

    expect(aviso).toHaveBeenCalledOnce();
    expect(tokenStorage.get()).toBeNull();
    onUnauthorized(null);
  });

  it("no login, o 401 é só senha errada: não encerra a sessão", async () => {
    tokenStorage.set("atual", true);
    const aviso = vi.fn();
    onUnauthorized(aviso);
    simularApi([{ metodo: "POST", caminho: "/api/auth/login", status: 401, corpo: { mensagem: "Usuário ou senha inválidos." } }]);

    await expect(request("/api/auth/login", { method: "POST", body: {}, skipAuthRedirect: true })).rejects.toThrow("Usuário ou senha inválidos.");

    expect(aviso).not.toHaveBeenCalled();
    expect(tokenStorage.get()).toBe("atual");
    onUnauthorized(null);
  });

  it("403 sem mensagem usa o texto de permissão do protótipo", async () => {
    simularApi([{ caminho: "/api/usuarios", status: 403 }]);
    await expect(request("/api/usuarios")).rejects.toThrow("Você não possui permissão para acessar esta funcionalidade.");
  });

  it("servidor fora do ar vira erro com status 0", async () => {
    vi.stubGlobal("fetch", vi.fn(async () => { throw new TypeError("Failed to fetch"); }));
    const erro = await request("/api/produtos").catch((e: unknown) => e);
    expect((erro as ApiError).status).toBe(0);
    expect((erro as ApiError).message).toContain("Não foi possível conectar ao servidor");
  });

  it("erro que não veio da API usa a mensagem padrão", () => {
    expect(errorMessage(new Error("interno"))).toBe("Não foi possível concluir a operação. Tente novamente.");
    expect(errorMessage("x", "Falhou.")).toBe("Falhou.");
  });
});

describe("tokenStorage", () => {
  it("'Lembrar de mim' guarda no localStorage; sem ele, no sessionStorage", () => {
    tokenStorage.set("a", true);
    expect(window.localStorage.getItem("ponto-e-sabor:token")).toBe("a");
    tokenStorage.set("b", false);
    expect(window.localStorage.getItem("ponto-e-sabor:token")).toBeNull();
    expect(window.sessionStorage.getItem("ponto-e-sabor:token")).toBe("b");
    tokenStorage.clear();
    expect(tokenStorage.get()).toBeNull();
  });
});
