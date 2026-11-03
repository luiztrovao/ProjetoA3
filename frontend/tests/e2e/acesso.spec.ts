// Teste de sistema: login e permissões por perfil, com a API e o banco reais.
import { expect, test } from "@playwright/test";
import { entrar, esperarDadosDemo, menu } from "./apoio";

test.beforeAll(async ({ browser }) => {
  const page = await browser.newPage();
  await esperarDadosDemo(page);
  await page.close();
});

test("senha errada mostra a mensagem e não entra", async ({ page }) => {
  await entrar(page, "fernanda", "senha-errada");

  await expect(page.getByRole("alert")).toBeVisible();
  await expect(page.getByRole("heading", { name: "Acesse sua conta" })).toBeVisible();
});

test("Vendedor entra direto em Vendas e não vê Dashboard nem Relatórios", async ({ page }) => {
  await entrar(page, "fernanda");

  await expect(page.getByRole("heading", { name: "Nova venda" })).toBeVisible();
  await expect(menu(page).getByRole("link", { name: "Vendas" })).toBeVisible();
  await expect(menu(page).getByRole("link", { name: "Dashboard" })).toHaveCount(0);
  await expect(menu(page).getByRole("link", { name: "Relatórios" })).toHaveCount(0);
});

test("Dono entra no Dashboard e sai pelo menu da conta", async ({ page }) => {
  await entrar(page, "matheus");

  await expect(page.getByRole("heading", { name: /Olá, Matheus/ })).toBeVisible();
  await expect(page.getByText("Vendas de hoje", { exact: true })).toBeVisible();

  await page.getByRole("button", { name: "Abrir menu do usuário" }).click();
  await expect(page.getByRole("menuitem", { name: "Dados da lanchonete" })).toBeVisible();
  await page.getByRole("menuitem", { name: "Sair" }).click();
  await expect(page.getByRole("heading", { name: "Acesse sua conta" })).toBeVisible();
});
