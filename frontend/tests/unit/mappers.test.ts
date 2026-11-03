// Conversão entre a API e as telas (unitário): o que a tela mostra e o que é enviado à API.
import { describe, expect, it } from "vitest";
import type { Cliente, PreferenciasVenda, Produto, Usuario, Venda } from "../../src/api";
import {
  clientRequest,
  employeeRequest,
  productRequest,
  toClientRecord,
  toEmployeeRecord,
  toProductRecord,
} from "../../src/pages/cadastros/mappers";
import { toPreferenciasVenda, toSalesPreferences } from "../../src/pages/settings/mappers";
import { isoDate, toSaleRecord } from "../../src/pages/vendas/mappers";

const cliente: Cliente = {
  id: 7, nome: "Camila Ferreira", cpf: "12345678901", telefone: "81999998888", email: null, cep: "50000000",
  logradouro: "Rua A", numero: "10", complemento: null, bairro: null, cidade: "Recife", uf: "PE", observacoes: null,
  ativo: false, criadoEm: "2026-10-01T10:00:00", atualizadoEm: "2026-10-01T10:00:00",
} as Cliente;

describe("Cadastros", () => {
  it("mostra o cliente com máscaras e status", () => {
    const registro = toClientRecord(cliente);
    expect(registro).toMatchObject({
      initials: "CF", cpf: "123.456.789-01", phone: "(81) 99999-8888", cep: "50000-000", email: "",
      status: "Inativo", date: "01/10/2026",
    });
  });

  it("envia o cliente só com dígitos, UF maiúscula e vazios como null", () => {
    const corpo = clientRequest({
      name: "  Ana  ", cpf: "123.456.789-01", phone: "(81) 3333-4444", email: " ", cep: "", street: "", number: "",
      complement: "", district: "", city: "Olinda", state: "pe", notes: "",
    });
    expect(corpo).toEqual({
      nome: "Ana", cpf: "12345678901", telefone: "8133334444", email: null, cep: null, logradouro: null, numero: null,
      complemento: null, bairro: null, cidade: "Olinda", uf: "PE", observacoes: null,
    });
  });

  it("na edição do funcionário, senha vazia mantém a atual (null)", () => {
    const base = { name: "Rafael", username: " Rafael.Souza ", email: "", cpf: "", phone: "", role: "Gerente" };
    expect(employeeRequest({ ...base, password: "" })).toMatchObject({ username: "rafael.souza", senha: null, perfil: "GERENTE" });
    expect(employeeRequest({ ...base, password: "segredo1" }).senha).toBe("segredo1");
  });

  it("mostra o perfil e o último acesso do funcionário", () => {
    const usuario = { id: 1, nome: "Matheus Almeida", username: "matheus", email: null, cpf: null, telefone: null,
      perfil: "DONO", ativo: true, ultimoAcesso: null } as Usuario;
    expect(toEmployeeRecord(usuario)).toMatchObject({ role: "Dono", lastAccess: "Ainda não acessou", status: "Ativo" });
  });

  it.each([
    ["", null], // estoque não informado mantém o atual
    ["0", 0], // limite inferior válido
    ["15", 15],
  ])("estoque %j vira %j", (estoque, esperado) => {
    const corpo = productRequest({ name: "Suco", code: " suc001 ", description: "", categoryId: "3", price: "R$ 8,50", stock: estoque, minimumStock: "" });
    expect(corpo).toMatchObject({ codigo: "SUC001", categoriaId: 3, preco: 8.5, estoqueAtual: esperado, estoqueMinimo: null });
  });

  it("produto sem preço e sem categoria envia null (a API valida)", () => {
    expect(productRequest({ name: "X", code: "x", description: "", categoryId: "", price: "", stock: "", minimumStock: "" }))
      .toMatchObject({ categoriaId: null, preco: null });
  });

  it("monta o endereço da foto do produto a partir da API", () => {
    const produto = { id: 2, nome: "X-Burger", codigo: "XBG001", descricao: null, categoria: { id: 1, nome: "Lanches" },
      preco: 18, estoqueAtual: 3, estoqueMinimo: 5, imagemUrl: "/api/imagens/9", ativo: true } as Produto;
    expect(toProductRecord(produto)).toMatchObject({ image: "http://api.test/api/imagens/9", stock: 3, category: "Lanches" });
  });
});

describe("Preferências de vendas", () => {
  const preferencias: PreferenciasVenda = {
    controleEstoque: true, bloquearSemEstoque: false, alertaEstoqueBaixo: 5, confirmarCancelamento: false,
    permitirVendaSemCliente: true, statusInicialVenda: "CONCLUIDA", formasPagamento: ["PIX", "DINHEIRO"], minutosAlertaPreparo: 45,
  };

  it("vai e volta da API sem perder informação", () => {
    const tela = toSalesPreferences(preferencias);
    expect(tela).toMatchObject({ initialOrderStatus: "Concluído", delayAlertMinutes: 45, paymentMethods: { pix: true, cash: true, credit: false, debit: false } });
    const volta = toPreferenciasVenda(tela);
    expect(volta).toMatchObject({ ...preferencias, formasPagamento: expect.arrayContaining(["PIX", "DINHEIRO"]) });
    expect(volta.formasPagamento).toHaveLength(2);
  });

  it("usa 20 minutos quando a API ainda não tem o alerta de atraso", () => {
    const antiga = { ...preferencias, minutosAlertaPreparo: undefined } as unknown as PreferenciasVenda;
    expect(toSalesPreferences(antiga).delayAlertMinutes).toBe(20);
  });
});

describe("Pedidos", () => {
  const venda = {
    id: 123, numero: "#PV-000123", status: "CANCELADA", dataHora: "2026-11-03T12:00:00", confirmadaEm: null,
    cliente: null, vendedor: { id: 3, nome: "Fernanda" }, formaPagamento: "PIX",
    itens: [{ produtoId: 2, codigo: "XBG001", nome: "X-Burger", quantidade: 2, precoUnitario: 18, subtotal: 36 }],
    subtotal: 36, desconto: 0, total: 36, valorRecebido: null, troco: null, motivoCancelamento: "Cliente desistiu",
  } as unknown as Venda;

  it("pedido sem cliente e sem troco usa os valores padrão da tela", () => {
    expect(toSaleRecord(venda)).toMatchObject({
      id: "#PV-000123", clientName: "Cliente não identificado", clientId: null, paymentMethod: "pix",
      status: "Cancelado", cashReceived: 36, change: 0, cancellationReason: "Cliente desistiu",
    });
  });

  it("formata a data dos filtros como aaaa-mm-dd com zeros à esquerda", () => {
    expect(isoDate(new Date(2026, 0, 5))).toBe("2026-01-05");
    expect(isoDate(new Date(2026, 11, 31))).toBe("2026-12-31");
  });
});
