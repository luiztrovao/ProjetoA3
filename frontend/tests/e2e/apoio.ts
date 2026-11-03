import { expect, type Page } from "@playwright/test";

// Usuários de demonstração (backend/src/main/resources/application-h2.properties, só para o ambiente local).
export const SENHA_DEMO = "demo123";

export async function entrar(page: Page, usuario: string, senha = SENHA_DEMO) {
  await page.goto("/");
  await page.getByLabel("Usuário ou e-mail").fill(usuario);
  await page.getByLabel("Senha", { exact: true }).fill(senha);
  await page.getByRole("button", { name: /Entrar/ }).click();
}

/** Espera a API terminar de gerar os dados de demonstração (o primeiro login só funciona depois disso). */
export async function esperarDadosDemo(page: Page) {
  await expect(async () => {
    const resposta = await page.request.post("http://localhost:8080/api/auth/login", { data: { login: "matheus", senha: SENHA_DEMO } });
    expect(resposta.status()).toBe(200);
  }).toPass({ timeout: 120_000, intervals: [2_000] });
}

export const menu = (page: Page) => page.getByRole("navigation", { name: "Navegação principal" });
