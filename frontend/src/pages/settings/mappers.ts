// Conversão entre a API de configurações/conta e os formatos da tela Configurações.
import { imageUrl } from "../../api";
import type { ContaRequest, Empresa, PreferenciasVenda } from "../../api";
import { paymentCodes, type EmployeeRecord, type PaymentMethod } from "../../types";
import { maskCep, maskCnpj, maskPhone, optional, optionalDigits } from "../../utils/format";

export type CompanySettings = {
  tradeName: string;
  legalName: string;
  cnpj: string;
  phone: string;
  email: string;
  cep: string;
  address: string;
  number: string;
  district: string;
  city: string;
  state: string;
  logo: string;
};

export type SalesPreferences = {
  stockControl: boolean;
  blockWithoutStock: boolean;
  lowStockThreshold: number;
  confirmCancellation: boolean;
  allowAnonymousCustomer: boolean;
  initialOrderStatus: "Em preparo" | "Concluído";
  paymentMethods: Record<PaymentMethod, boolean>;
};

export const defaultCompany: CompanySettings = { tradeName: "Ponto & Sabor", legalName: "", cnpj: "", phone: "", email: "", cep: "", address: "", number: "", district: "", city: "", state: "", logo: "" };

export const defaultPreferences: SalesPreferences = { stockControl: true, blockWithoutStock: true, lowStockThreshold: 5, confirmCancellation: true, allowAnonymousCustomer: true, initialOrderStatus: "Em preparo", paymentMethods: { cash: true, credit: true, debit: true, pix: true } };

export const toCompanySettings = (empresa: Empresa): CompanySettings => ({
  tradeName: empresa.nomeFantasia || "",
  legalName: empresa.razaoSocial || "",
  cnpj: empresa.cnpj ? maskCnpj(empresa.cnpj) : "",
  phone: empresa.telefone ? maskPhone(empresa.telefone) : "",
  email: empresa.email || "",
  cep: empresa.cep ? maskCep(empresa.cep) : "",
  address: empresa.logradouro || "",
  number: empresa.numero || "",
  district: empresa.bairro || "",
  city: empresa.cidade || "",
  state: empresa.uf || "",
  logo: imageUrl(empresa.logoUrl),
});

export const toEmpresa = (company: CompanySettings): Empresa => ({
  nomeFantasia: company.tradeName.trim(),
  razaoSocial: optional(company.legalName),
  cnpj: optionalDigits(company.cnpj),
  telefone: optionalDigits(company.phone),
  email: optional(company.email),
  cep: optionalDigits(company.cep),
  logradouro: optional(company.address),
  numero: optional(company.number),
  bairro: optional(company.district),
  cidade: optional(company.city),
  uf: optional(company.state)?.toUpperCase() ?? null,
});

export const companyFieldNames: Record<string, keyof CompanySettings> = {
  nomeFantasia: "tradeName", razaoSocial: "legalName", cnpj: "cnpj", telefone: "phone", email: "email", cep: "cep",
  logradouro: "address", numero: "number", bairro: "district", cidade: "city", uf: "state",
};

export const toSalesPreferences = (preferencias: PreferenciasVenda): SalesPreferences => ({
  stockControl: preferencias.controleEstoque,
  blockWithoutStock: preferencias.bloquearSemEstoque,
  lowStockThreshold: preferencias.alertaEstoqueBaixo,
  confirmCancellation: preferencias.confirmarCancelamento,
  allowAnonymousCustomer: preferencias.permitirVendaSemCliente,
  initialOrderStatus: preferencias.statusInicialVenda === "CONCLUIDA" ? "Concluído" : "Em preparo",
  paymentMethods: {
    cash: preferencias.formasPagamento.includes("DINHEIRO"),
    credit: preferencias.formasPagamento.includes("CREDITO"),
    debit: preferencias.formasPagamento.includes("DEBITO"),
    pix: preferencias.formasPagamento.includes("PIX"),
  },
});

export const toPreferenciasVenda = (preferences: SalesPreferences): PreferenciasVenda => ({
  controleEstoque: preferences.stockControl,
  bloquearSemEstoque: preferences.blockWithoutStock,
  alertaEstoqueBaixo: preferences.lowStockThreshold,
  confirmarCancelamento: preferences.confirmCancellation,
  permitirVendaSemCliente: preferences.allowAnonymousCustomer,
  statusInicialVenda: preferences.initialOrderStatus === "Concluído" ? "CONCLUIDA" : "EM_PREPARO",
  formasPagamento: (Object.keys(paymentCodes) as PaymentMethod[])
    .filter((method) => preferences.paymentMethods[method])
    .map((method) => paymentCodes[method]),
});

export const toContaRequest = (account: EmployeeRecord): ContaRequest => ({
  nome: account.name.trim(),
  username: account.username.trim().toLowerCase(),
  email: optional(account.email),
  telefone: optionalDigits(account.phone),
});

export const accountFieldNames: Record<string, string> = { nome: "name", username: "username", email: "email", telefone: "phone" };
