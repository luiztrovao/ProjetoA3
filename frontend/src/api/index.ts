// Endpoints da API usados pelo frontend.
import { request } from "./client";
import type {
  AlterarSenhaRequest,
  Categoria,
  Cliente,
  ClienteRequest,
  Configuracao,
  ContaRequest,
  Empresa,
  ListagemParams,
  LoginResponse,
  Pagina,
  PreferenciasVenda,
  Produto,
  ProdutoRequest,
  Usuario,
  UsuarioRequest,
} from "./types";

export { ApiError, API_URL, errorMessage, onUnauthorized, tokenStorage } from "./client";
export type * from "./types";

/** CRUD padrão da API: listagem paginada, cadastro, edição e exclusão lógica. */
const recurso = <T, R>(base: string) => ({
  listar: (params: ListagemParams = {}) => request<Pagina<T>>(base, { query: params }),
  buscar: (id: number) => request<T>(`${base}/${id}`),
  criar: (dados: R) => request<T>(base, { method: "POST", body: dados }),
  atualizar: (id: number, dados: R) => request<T>(`${base}/${id}`, { method: "PUT", body: dados }),
  inativar: (id: number) => request<void>(`${base}/${id}`, { method: "DELETE" }),
  ativar: (id: number) => request<void>(`${base}/${id}/ativar`, { method: "PATCH" }),
});

export const authApi = {
  login: (login: string, senha: string) =>
    request<LoginResponse>("/api/auth/login", { method: "POST", body: { login, senha }, skipAuthRedirect: true }),
};

export const contaApi = {
  consultar: () => request<Usuario>("/api/conta"),
  atualizar: (dados: ContaRequest) => request<Usuario>("/api/conta", { method: "PUT", body: dados }),
  alterarSenha: (dados: AlterarSenhaRequest) => request<void>("/api/conta/senha", { method: "PUT", body: dados }),
};

export const clientesApi = recurso<Cliente, ClienteRequest>("/api/clientes");
export const usuariosApi = recurso<Usuario, UsuarioRequest>("/api/usuarios");
export const produtosApi = recurso<Produto, ProdutoRequest>("/api/produtos");

export const categoriasApi = {
  listar: () => request<Categoria[]>("/api/categorias"),
};

export const configuracoesApi = {
  consultar: () => request<Configuracao>("/api/configuracoes"),
  atualizarEmpresa: (dados: Empresa) => request<Configuracao>("/api/configuracoes/empresa", { method: "PUT", body: dados }),
  atualizarVendas: (dados: PreferenciasVenda) => request<Configuracao>("/api/configuracoes/vendas", { method: "PUT", body: dados }),
};
