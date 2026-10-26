// Endpoints da API usados pelo frontend.
import { request } from "./client";
import type {
  AlterarSenhaRequest,
  Categoria,
  Cliente,
  ClienteRequest,
  Configuracao,
  ContaRequest,
  Dashboard,
  Empresa,
  Identidade,
  ListagemParams,
  LoginResponse,
  Pagina,
  PeriodoPico,
  Picos,
  PreferenciasVenda,
  Produto,
  ProdutoRequest,
  Relatorio,
  Usuario,
  UsuarioRequest,
  Venda,
  VendaListagemParams,
  VendaRequest,
} from "./types";

export { ApiError, API_URL, errorMessage, imageUrl, onUnauthorized, tokenStorage } from "./client";

/** Corpo multipart com a imagem no campo "arquivo", como a API espera. */
const comArquivo = (arquivo: File) => {
  const dados = new FormData();
  dados.append("arquivo", arquivo);
  return dados;
};
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
export const produtosApi = {
  ...recurso<Produto, ProdutoRequest>("/api/produtos"),
  enviarImagem: (id: number, arquivo: File) =>
    request<Produto>(`/api/produtos/${id}/imagem`, { method: "PUT", body: comArquivo(arquivo) }),
  removerImagem: (id: number) => request<Produto>(`/api/produtos/${id}/imagem`, { method: "DELETE" }),
};

export const categoriasApi = {
  listar: () => request<Categoria[]>("/api/categorias"),
};

export const configuracoesApi = {
  consultar: () => request<Configuracao>("/api/configuracoes"),
  atualizarEmpresa: (dados: Empresa) => request<Configuracao>("/api/configuracoes/empresa", { method: "PUT", body: dados }),
  atualizarVendas: (dados: PreferenciasVenda) => request<Configuracao>("/api/configuracoes/vendas", { method: "PUT", body: dados }),
  enviarLogo: (arquivo: File) => request<Configuracao>("/api/configuracoes/logo", { method: "PUT", body: comArquivo(arquivo) }),
  removerLogo: () => request<Configuracao>("/api/configuracoes/logo", { method: "DELETE" }),
};

/** Rotas sem login: nome e logo da lanchonete para a tela de login. */
export const publicoApi = {
  identidade: () => request<Identidade>("/api/publico/identidade", { skipAuthRedirect: true }),
};

export const vendasApi = {
  listar: (params: VendaListagemParams = {}) => request<Pagina<Venda>>("/api/vendas", { query: params }),
  buscar: (id: number) => request<Venda>(`/api/vendas/${id}`),
  confirmar: (dados: VendaRequest) => request<Venda>("/api/vendas", { method: "POST", body: dados }),
  concluir: (id: number) => request<Venda>(`/api/vendas/${id}/concluir`, { method: "PATCH" }),
  cancelar: (id: number, motivo: string) =>
    request<Venda>(`/api/vendas/${id}/cancelar`, { method: "PATCH", body: { motivo } }),
};

/** Dashboard e Relatórios (Dono e Gerente). Datas no formato aaaa-mm-dd. */
export const relatoriosApi = {
  dashboard: () => request<Dashboard>("/api/dashboard"),
  resumo: (params: { inicio?: string; fim?: string; vendedorId?: number }) =>
    request<Relatorio>("/api/relatorios/resumo", { query: params }),
  picos: (params: { periodo: PeriodoPico; referencia?: string; vendedorId?: number }) =>
    request<Picos>("/api/relatorios/picos", { query: params }),
};
