// Teste de sistema da gestão: notificação de estoque baixo leva aos produtos já filtrados.
import { expect, test } from "@playwright/test";
import { entrar, esperarDadosDemo } from "./apoio";

test.beforeAll(async ({ browser }) => {
  const page = await browser.newPage();
  await esperarDadosDemo(page);
  await page.close();
});

test("Gerente abre o aviso de estoque baixo e vê os produtos filtrados", async ({ page }) => {
  await entrar(page, "carlos");

  const sino = page.getByRole("button", { name: /Notificações: \d+ não lidas/ });
  await expect(sino).toBeVisible();
  await sino.click();

  await page.getByRole("region", { name: "Notificações" }).getByRole("button", { name: /Estoque baixo/ }).first().click();

  await expect(page.getByRole("heading", { name: "Cadastros" })).toBeVisible();
  await expect(page.getByLabel("Estoque")).toHaveValue("baixo");
  const linhas = page.locator(".management-table tbody tr");
  await expect(linhas.first()).toContainText("Estoque baixo");
  for (const linha of await linhas.all()) {
    await expect(linha).toContainText("Estoque baixo");
  }
});
