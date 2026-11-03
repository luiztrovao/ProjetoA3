// Máscaras e formatação (caixa-preta): particionamento de equivalência e valores-limite de cada campo.
import { describe, expect, it, vi } from "vitest";
import {
  formatCurrency,
  formatDate,
  formatLastAccess,
  getInitials,
  maskCep,
  maskCnpj,
  maskCpf,
  maskCurrency,
  maskPhone,
  onlyDigits,
  optional,
  optionalDigits,
  parseCurrency,
} from "../../src/utils/format";

/** O Intl usa espaço não separável depois do "R$". */
const semNbsp = (texto: string) => texto.replace(/\s/g, " ");

describe("maskCpf", () => {
  it.each([
    ["1234567890", "123.456.789-0"], // 10 dígitos: logo abaixo do limite
    ["12345678901", "123.456.789-01"], // 11 dígitos: no limite
    ["123456789012", "123.456.789-01"], // 12 dígitos: o excedente é descartado
    ["123.456.789-01", "123.456.789-01"], // já mascarado
    ["", ""],
  ])("%s → %s", (entrada, esperado) => {
    expect(maskCpf(entrada)).toBe(esperado);
  });
});

describe("maskPhone", () => {
  it.each([
    ["8133334444", "(81) 3333-4444"], // fixo: 10 dígitos
    ["81999998888", "(81) 99999-8888"], // celular: 11 dígitos
    ["819999988887", "(81) 99999-8888"], // 12 dígitos: corta no 11º
    ["81", "81"],
  ])("%s → %s", (entrada, esperado) => {
    expect(maskPhone(entrada)).toBe(esperado);
  });
});

describe("maskCep e maskCnpj", () => {
  it("formata o CEP com 8 dígitos e ignora o excedente", () => {
    expect(maskCep("50000000")).toBe("50000-000");
    expect(maskCep("500000001")).toBe("50000-000");
    expect(maskCep("5000")).toBe("5000");
  });

  it("formata o CNPJ com 14 dígitos", () => {
    expect(maskCnpj("12345678000199")).toBe("12.345.678/0001-99");
    expect(maskCnpj("123456780001999")).toBe("12.345.678/0001-99");
  });
});

describe("moeda", () => {
  it("mascara o que é digitado como centavos", () => {
    expect(semNbsp(maskCurrency("1"))).toBe("R$ 0,01");
    expect(semNbsp(maskCurrency("100"))).toBe("R$ 1,00");
    expect(semNbsp(maskCurrency("123456"))).toBe("R$ 1.234,56");
    expect(maskCurrency("")).toBe("");
    expect(maskCurrency("abc")).toBe("");
  });

  it("converte o texto de volta em número", () => {
    expect(parseCurrency("R$ 1.234,56")).toBe(1234.56);
    expect(parseCurrency("R$ 0,01")).toBe(0.01);
    expect(parseCurrency("")).toBe(0);
  });

  it("formata valores numéricos", () => {
    expect(semNbsp(formatCurrency(0))).toBe("R$ 0,00");
    expect(semNbsp(formatCurrency(27.5))).toBe("R$ 27,50");
  });
});

describe("textos", () => {
  it("monta as iniciais com no máximo duas palavras", () => {
    expect(getInitials("Fernanda Lima Souza")).toBe("FL");
    expect(getInitials("  carlos  ")).toBe("C");
    expect(getInitials("")).toBe("");
  });

  it("campos opcionais vazios viram null para a API", () => {
    expect(optional("  ")).toBeNull();
    expect(optional(undefined)).toBeNull();
    expect(optional("  Rua A ")).toBe("Rua A");
    expect(optionalDigits("(81) 99999-8888")).toBe("81999998888");
    expect(optionalDigits("( ) -")).toBeNull();
    expect(onlyDigits("a1b2")).toBe("12");
  });
});

describe("datas", () => {
  it("formata a data e ignora valores inválidos", () => {
    expect(formatDate("2026-05-12T10:00:00")).toBe("12/05/2026");
    expect(formatDate("não é data")).toBe("");
    expect(formatDate(null)).toBe("");
  });

  it("descreve o último acesso como hoje, ontem ou a data", () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 10, 3, 20, 0));
    expect(formatLastAccess("2026-11-03T19:48:00")).toBe("Hoje, 19:48");
    expect(formatLastAccess("2026-11-02T21:10:00")).toBe("Ontem, 21:10");
    expect(formatLastAccess("2026-11-01T08:30:00")).toBe("01/11/2026, 08:30");
    expect(formatLastAccess(null)).toBe("Ainda não acessou");
    expect(formatLastAccess("inválido")).toBe("Ainda não acessou");
  });
});
