// Conversão entre os registros da API e os formatos usados nas tabelas e no formulário de Cadastros.
import { imageUrl } from "../../api";
import type { Cliente, ClienteRequest, Produto, ProdutoRequest, Usuario, UsuarioRequest } from "../../api";
import { perfilFromLabel, perfilLabels, type ClientRecord, type EmployeeRecord, type FormValues, type ProductRecord, type TabId } from "../../types";
import {
  formatCurrency,
  formatDate,
  formatLastAccess,
  getInitials,
  maskCep,
  maskCpf,
  maskPhone,
  optional,
  optionalDigits,
  parseCurrency,
} from "../../utils/format";

const status = (ativo: boolean) => ativo ? "Ativo" as const : "Inativo" as const;

export const toClientRecord = (cliente: Cliente): ClientRecord => ({
  id: cliente.id,
  initials: getInitials(cliente.nome),
  name: cliente.nome,
  cpf: cliente.cpf ? maskCpf(cliente.cpf) : "",
  phone: cliente.telefone ? maskPhone(cliente.telefone) : "",
  email: cliente.email || "",
  date: formatDate(cliente.criadoEm),
  status: status(cliente.ativo),
  cep: cliente.cep ? maskCep(cliente.cep) : "",
  street: cliente.logradouro || "",
  number: cliente.numero || "",
  complement: cliente.complemento || "",
  district: cliente.bairro || "",
  city: cliente.cidade || "",
  state: cliente.uf || "",
  notes: cliente.observacoes || "",
});

export const toEmployeeRecord = (usuario: Usuario): EmployeeRecord => ({
  id: usuario.id,
  initials: getInitials(usuario.nome),
  name: usuario.nome,
  cpf: usuario.cpf ? maskCpf(usuario.cpf) : "",
  email: usuario.email || "",
  phone: usuario.telefone ? maskPhone(usuario.telefone) : "",
  username: usuario.username,
  role: perfilLabels[usuario.perfil],
  lastAccess: formatLastAccess(usuario.ultimoAcesso),
  status: status(usuario.ativo),
});

export const toProductRecord = (produto: Produto): ProductRecord => ({
  id: produto.id,
  initials: getInitials(produto.nome),
  name: produto.nome,
  category: produto.categoria.nome,
  categoryId: produto.categoria.id,
  code: produto.codigo,
  description: produto.descricao || "",
  price: formatCurrency(produto.preco),
  stock: produto.estoqueAtual,
  minimumStock: produto.estoqueMinimo,
  image: imageUrl(produto.imagemUrl),
  status: status(produto.ativo),
});

export const clientToForm = (record: ClientRecord): FormValues => ({
  name: record.name, cpf: record.cpf, phone: record.phone, email: record.email, cep: record.cep, street: record.street,
  number: record.number, complement: record.complement, district: record.district, city: record.city, state: record.state,
  notes: record.notes, status: record.status,
});

export const employeeToForm = (record: EmployeeRecord): FormValues => ({
  name: record.name, cpf: record.cpf, phone: record.phone, email: record.email, username: record.username,
  role: record.role, status: record.status, password: "", confirmPassword: "",
});

export const productToForm = (record: ProductRecord): FormValues => ({
  name: record.name, code: record.code, categoryId: record.categoryId ? String(record.categoryId) : "", description: record.description,
  price: record.price, stock: String(record.stock), minimumStock: String(record.minimumStock), image: record.image, imageName: "",
  status: record.status,
});

export const clientRequest = (values: FormValues): ClienteRequest => ({
  nome: values.name.trim(),
  cpf: optionalDigits(values.cpf),
  telefone: optionalDigits(values.phone),
  email: optional(values.email),
  cep: optionalDigits(values.cep),
  logradouro: optional(values.street),
  numero: optional(values.number),
  complemento: optional(values.complement),
  bairro: optional(values.district),
  cidade: optional(values.city),
  uf: optional(values.state)?.toUpperCase() ?? null,
  observacoes: optional(values.notes),
});

export const employeeRequest = (values: FormValues): UsuarioRequest => ({
  nome: values.name.trim(),
  username: values.username.trim().toLowerCase(),
  email: optional(values.email),
  cpf: optionalDigits(values.cpf),
  telefone: optionalDigits(values.phone),
  senha: values.password ? values.password : null,
  // O formulário só permite enviar com um perfil escolhido; o fallback nunca chega à API.
  perfil: perfilFromLabel(values.role) || "VENDEDOR",
});

const optionalInteger = (value: string | undefined) => {
  const trimmed = (value || "").trim();
  return trimmed === "" ? null : Number(trimmed);
};

export const productRequest = (values: FormValues): ProdutoRequest => ({
  nome: values.name.trim(),
  codigo: values.code.trim().toUpperCase(),
  descricao: optional(values.description),
  categoriaId: values.categoryId ? Number(values.categoryId) : null,
  preco: values.price ? parseCurrency(values.price) : null,
  estoqueAtual: optionalInteger(values.stock),
  estoqueMinimo: optionalInteger(values.minimumStock),
});

/** Nome do campo na API → nome do campo no formulário, para exibir os erros de validação. */
export const apiFieldNames: Record<TabId, Record<string, string>> = {
  clients: {
    nome: "name", cpf: "cpf", telefone: "phone", email: "email", cep: "cep", logradouro: "street", numero: "number",
    complemento: "complement", bairro: "district", cidade: "city", uf: "state", observacoes: "notes",
  },
  employees: { nome: "name", username: "username", email: "email", cpf: "cpf", telefone: "phone", senha: "password", perfil: "role" },
  products: {
    nome: "name", codigo: "code", descricao: "description", categoriaId: "categoryId", preco: "price",
    estoqueAtual: "stock", estoqueMinimo: "minimumStock",
  },
};
