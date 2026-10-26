// Tipos dos contratos da API (ver backend/README.md e o Swagger).

export type Perfil = "DONO" | "GERENTE" | "VENDEDOR";
export type FormaPagamento = "DINHEIRO" | "PIX" | "CREDITO" | "DEBITO";
export type StatusVenda = "EM_PREPARO" | "CONCLUIDA" | "CANCELADA";

export type Pagina<T> = {
  conteudo: T[];
  pagina: number;
  tamanho: number;
  totalElementos: number;
  totalPaginas: number;
};

export type ListagemParams = {
  termo?: string;
  ativo?: boolean;
  page?: number;
  size?: number;
  sort?: string;
};

export type Usuario = {
  id: number;
  nome: string;
  username: string;
  email: string | null;
  cpf: string | null;
  telefone: string | null;
  perfil: Perfil;
  ativo: boolean;
  ultimoAcesso: string | null;
  criadoEm: string;
  atualizadoEm: string;
};

export type LoginResponse = {
  token: string;
  tipo: string;
  expiraEm: string;
  usuario: Usuario;
};

export type ContaRequest = {
  nome: string;
  username: string;
  email: string | null;
  telefone: string | null;
};

export type AlterarSenhaRequest = {
  senhaAtual: string;
  novaSenha: string;
  confirmacaoSenha: string;
};

export type UsuarioRequest = {
  nome: string;
  username: string;
  email: string | null;
  cpf: string | null;
  telefone: string | null;
  /** Obrigatória no cadastro; na edição, `null` mantém a senha atual. */
  senha: string | null;
  perfil: Perfil;
};

export type Cliente = {
  id: number;
  nome: string;
  cpf: string | null;
  telefone: string | null;
  email: string | null;
  cep: string | null;
  logradouro: string | null;
  numero: string | null;
  complemento: string | null;
  bairro: string | null;
  cidade: string | null;
  uf: string | null;
  observacoes: string | null;
  ativo: boolean;
  criadoEm: string;
  atualizadoEm: string;
};

export type ClienteRequest = Omit<Cliente, "id" | "ativo" | "criadoEm" | "atualizadoEm">;

export type Categoria = { id: number; nome: string };

export type Produto = {
  id: number;
  codigo: string;
  nome: string;
  descricao: string | null;
  categoria: Categoria;
  preco: number;
  estoqueAtual: number;
  estoqueMinimo: number;
  estoqueBaixo: boolean;
  imagemUrl: string | null;
  ativo: boolean;
  criadoEm: string;
  atualizadoEm: string;
};

export type ProdutoRequest = {
  nome: string;
  codigo: string;
  descricao: string | null;
  categoriaId: number | null;
  preco: number | null;
  estoqueAtual: number | null;
  estoqueMinimo: number | null;
};

/** Identidade pública da lanchonete (GET /api/publico/identidade). */
export type Identidade = {
  nomeFantasia: string;
  logoUrl: string | null;
};

export type Empresa = {
  nomeFantasia: string;
  razaoSocial: string | null;
  cnpj: string | null;
  telefone: string | null;
  email: string | null;
  cep: string | null;
  logradouro: string | null;
  numero: string | null;
  bairro: string | null;
  cidade: string | null;
  uf: string | null;
  /** Somente leitura: o logo é enviado por `configuracoesApi.enviarLogo`. */
  logoUrl?: string | null;
};

export type PreferenciasVenda = {
  controleEstoque: boolean;
  bloquearSemEstoque: boolean;
  alertaEstoqueBaixo: number;
  confirmarCancelamento: boolean;
  permitirVendaSemCliente: boolean;
  statusInicialVenda: StatusVenda;
  formasPagamento: FormaPagamento[];
};

export type Configuracao = {
  empresa: Empresa;
  preferenciasVenda: PreferenciasVenda;
  atualizadoEm: string | null;
};
