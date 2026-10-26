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

// ===== Vendas e Pedidos =====

export type ItemVendaRequest = { produtoId: number; quantidade: number };

export type VendaRequest = {
  clienteId: number | null;
  itens: ItemVendaRequest[];
  desconto: number;
  formaPagamento: FormaPagamento;
  valorRecebido: number | null;
  observacao?: string | null;
};

export type PessoaResumo = { id: number; nome: string };

export type ItemVenda = {
  produtoId: number;
  codigo: string;
  nome: string;
  quantidade: number;
  precoUnitario: number;
  subtotal: number;
};

export type Venda = {
  id: number;
  numero: string;
  status: StatusVenda;
  dataHora: string;
  confirmadaEm: string | null;
  cliente: PessoaResumo | null;
  vendedor: PessoaResumo;
  formaPagamento: FormaPagamento;
  itens: ItemVenda[];
  quantidadeItens: number;
  subtotal: number;
  desconto: number;
  total: number;
  valorRecebido: number | null;
  troco: number | null;
  observacao: string | null;
  canceladaEm: string | null;
  motivoCancelamento: string | null;
};

export type VendaListagemParams = {
  termo?: string;
  status?: StatusVenda;
  vendedorId?: number;
  clienteId?: number;
  formaPagamento?: FormaPagamento;
  /** aaaa-mm-dd */
  inicio?: string;
  /** aaaa-mm-dd */
  fim?: string;
  page?: number;
  size?: number;
  sort?: string;
};

// ===== Dashboard e Relatórios =====

export type Indicadores = { faturamento: number; pedidos: number; ticketMedio: number };

/** Ponto de um gráfico: `chave` estável (dia, mês, hora ou dia da semana) e `rotulo` para exibir. */
export type Ponto = { chave: string; rotulo: string; pedidos: number; faturamento: number };

export type Granularidade = "HORA" | "DIA" | "MES";
export type PeriodoPico = "SEMANA" | "MES" | "SEMESTRE" | "ANO";

export type Dashboard = {
  hoje: Indicadores;
  produtos: { cadastrados: number; ativos: number; estoqueBaixo: number };
  ultimos7Dias: Ponto[];
  resumo7Dias: Indicadores;
  pedidosRecentes: Venda[];
};

export type ProdutoVendido = { produtoId: number; codigo: string; nome: string; quantidade: number; faturamento: number };
export type DesempenhoVendedor = {
  vendedorId: number;
  nome: string;
  vendas: number;
  faturamento: number;
  ticketMedio: number;
  participacao: number;
};
export type TotalFormaPagamento = { formaPagamento: FormaPagamento; pedidos: number; valor: number; percentual: number };

export type Relatorio = {
  inicio: string;
  fim: string;
  vendedorId: number | null;
  indicadores: Indicadores;
  produtosVendidos: number;
  granularidade: Granularidade;
  evolucao: Ponto[];
  maisVendidos: ProdutoVendido[];
  vendedores: DesempenhoVendedor[];
  formasPagamento: TotalFormaPagamento[];
};

export type Picos = {
  periodo: PeriodoPico;
  inicio: string;
  fim: string;
  indicadores: Indicadores;
  periodoAnterior: Indicadores;
  /** Variação % do faturamento em relação ao período anterior; nula se o anterior não teve vendas. */
  variacaoFaturamento: number | null;
  granularidade: Granularidade;
  serie: Ponto[];
  pico: Ponto | null;
  porDiaDaSemana: Ponto[];
  diaDaSemanaPico: Ponto | null;
  porHora: Ponto[];
  horaPico: Ponto | null;
};
