// Tipos compartilhados pelas telas (formato usado na interface, não o da API).
import type { Perfil } from "./api";

export type UserRole = "owner" | "manager" | "seller";

export const roleKey = (role: EmployeeRecord["role"]): UserRole => role === "Dono" ? "owner" : role === "Gerente" ? "manager" : "seller";

export const perfilLabels: Record<Perfil, EmployeeRecord["role"]> = {
  DONO: "Dono",
  GERENTE: "Gerente",
  VENDEDOR: "Vendedor",
};

export const perfilFromLabel = (role: string): Perfil | null =>
  role === "Dono" ? "DONO" : role === "Gerente" ? "GERENTE" : role === "Vendedor" ? "VENDEDOR" : null;

export type TabId = "clients" | "employees" | "products";
export type ModalMode = "create" | "view" | "edit";
export type FormValues = Record<string, string>;

export type ClientRecord = {
  id: number;
  initials: string;
  name: string;
  cpf: string;
  phone: string;
  email: string;
  date: string;
  status: "Ativo" | "Inativo";
  cep: string;
  street: string;
  number: string;
  complement: string;
  district: string;
  city: string;
  state: string;
  notes: string;
};

export type EmployeeRecord = {
  id: number;
  initials: string;
  name: string;
  cpf: string;
  email: string;
  phone: string;
  username: string;
  role: "Dono" | "Gerente" | "Vendedor";
  lastAccess: string;
  status: "Ativo" | "Inativo";
};

export type ProductRecord = {
  id: number;
  initials: string;
  name: string;
  category: string;
  /** Presente nos produtos vindos da API. */
  categoryId?: number;
  code: string;
  description: string;
  price: string;
  stock: number;
  minimumStock: number;
  image: string;
  status: "Ativo" | "Inativo";
};

export type PaymentMethod = "cash" | "credit" | "debit" | "pix";

export const paymentLabels: Record<PaymentMethod, string> = {
  cash: "Dinheiro",
  credit: "Cartão de crédito",
  debit: "Cartão de débito",
  pix: "PIX",
};
