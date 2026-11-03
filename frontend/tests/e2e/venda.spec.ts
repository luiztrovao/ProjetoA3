// Teste de aceitação do fluxo principal (risco crítico): o Vendedor registra uma venda do início ao fim
// e o pedido aparece em Pedidos.
import { expect, test } from "@playwright/test";
import { entrar, esperarDadosDemo, menu } from "./apoio";

test.beforeAll(async ({ browser }) => {
  const page = await browser.newPage();
  await esperarDadosDemo(page);
  await page.close();
});

test("Vendedor registra uma venda no PIX e encontra o pedido em Pedidos", async ({ page }) => {
  await entrar(page, "fernanda");
  await expect(page.getByRole("heading", { name: "Nova venda" })).toBeVisible();

  // Seleção de produtos: o primeiro produto disponível, duas vezes
  const adicionar = page.getByRole("button", { name: "Adicionar" }).first();
  await adicionar.click();
  await adicionar.click();
  await expect(page.locator(".order-summary")).toContainText("Quantidade de itens2");

  // Revisão e pagamento
  await page.getByRole("button", { name: /Revisar pedido/ }).click();
  await expect(page.getByRole("heading", { name: "Forma de pagamento" })).toBeVisible();
  await page.getByRole("button", { name: "PIX" }).click();
  await page.getByRole("button", { name: /Confirmar venda/ }).click();

  // Confirmação
  await expect(page.getByRole("heading", { name: "Venda realizada com sucesso!" })).toBeVisible();
  await page.getByRole("button", { name: "Ver pedido" }).click();
  const numero = (await page.getByRole("heading", { name: /^#PV-\d{6}$/ }).textContent())!.trim();

  // O pedido está gravado no banco e aparece na busca de Pedidos
  await menu(page).getByRole("link", { name: "Pedidos" }).click();
  await page.getByPlaceholder(/Pesquisar/).fill(numero);
  await expect(page.getByRole("row").filter({ hasText: numero })).toHaveCount(1);
});

test("Revisar pedido fica desabilitado com o carrinho vazio", async ({ page }) => {
  await entrar(page, "fernanda");

  await expect(page.getByRole("button", { name: /Revisar pedido/ })).toBeDisabled();
});
