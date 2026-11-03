# Frontend

Aplicação **React 19 + Vite + TypeScript**, com Tailwind CSS 4 e CSS próprio seguindo a
[identidade visual](../docs/design/identidade-visual.md).

## Origem do código

As telas vieram do **protótipo do Figma Make**
([Tela de Login Lanchonete](https://www.figma.com/make/XXGbp9NpOByG1oUpCC5PpI/Tela-de-Login-Lanchonete), versão 12)
e estão sendo ligadas à API do backend aos poucos, sem mudar o visual:

| Tela | Dados |
|---|---|
| Login | API (`POST /api/auth/login`, token JWT) |
| Cadastros (Clientes, Funcionários, Produtos) | API |
| Configurações (Dados da Lanchonete, Preferências de Vendas, Minha Conta) | API |
| Vendas (PDV, revisão e confirmação) | API (`/api/vendas`) |
| Pedidos (filtros, concluir, cancelar com motivo) | API (`/api/vendas`) |
| Dashboard | API (`/api/dashboard`) |
| Relatórios e **Picos de venda** (semana, mês, semestre e ano) | API (`/api/relatorios`) |

## Estrutura

| Pasta/arquivo | Conteúdo |
|---|---|
| `src/api/` | Cliente HTTP (`fetch`) com o token, tratamento de erros da API e os endpoints usados |
| `src/pages/LoginPage.tsx` | Tela de login |
| `src/pages/cadastros/` | Tela Cadastros: tabelas, formulário (modal) e conversão API ↔ tela |
| `src/pages/vendas/` | Tela Pedidos, detalhes do pedido e conversão dos pedidos da API |
| `src/pages/dashboard/` | Tela Dashboard |
| `src/pages/relatorios/` | Tela Relatórios, seção Picos de venda e exportação em CSV |
| `src/pages/settings/` | Tela Configurações |
| `src/components/` | Ícones, logo, elementos de tabela e os menus do topo (sino de notificações e menu da conta) |
| `src/utils/format.ts` | Máscaras (CPF, telefone, CEP, CNPJ, moeda) e formatação de datas |
| `src/types.ts` | Tipos usados pelas telas |
| `src/App.tsx` | Sessão, navegação e o fluxo de venda (PDV, revisão e confirmação) |
| `src/styles/app.css` | Estilos das telas |

### Topo e navegação

- **Sino**: mostra quantos avisos não lidos existem (consulta a API a cada minuto). Clicar em estoque baixo abre
  Cadastros › Produtos já filtrado; clicar num pedido abre Pedidos com a busca pelo número.
- **Menu da conta** (seta ao lado do nome e "⋯" da barra lateral): Minha conta, Alterar senha, Dados da lanchonete (Dono) e Sair.
- **Celular**: o botão ☰ abre a barra lateral por cima da tela.
- **Configurações › Preferências de Vendas**: "Alerta de pedido atrasado" define os minutos do aviso;
  com "Confirmar antes de cancelar" desligado, o pedido é cancelado sem a janela de motivo.

### Login e sessão

- O token fica no `localStorage` com **Lembrar de mim** marcado; senão, no `sessionStorage` (some ao fechar o navegador).
- Toda requisição envia `Authorization: Bearer <token>`. Se a API responder **401** (token expirado ou inválido),
  o token é apagado e a tela volta para o login.
- Os erros de validação da API (`campos[]`) aparecem embaixo de cada campo do formulário.
- Logo e fotos de produto (PNG ou JPG, até 1 MB) são enviados à API ao salvar e ficam guardados no banco.
  A tela de login mostra o nome e o logo da lanchonete pela rota pública `GET /api/publico/identidade`.

## Pré-requisitos

- **Node.js 22** ou superior

## Como rodar

Suba antes a API (veja [backend/README.md](../backend/README.md)). Depois, na pasta `frontend/`:

```bash
npm install
npm run dev
```

Acesse http://localhost:5173. Com a API no perfil `h2`, entre com `matheus` (Dono), `carlos` (Gerente) ou
`fernanda` (Vendedor); a senha está em `app.dados-demo.senha`, no arquivo
`backend/src/main/resources/application-h2.properties`.

A API é chamada em `http://localhost:8080`. Para usar outro endereço, copie `.env.example` para `.env`
e altere `VITE_API_URL`.

## Scripts

| Comando | O que faz |
|---|---|
| `npm run dev` | Servidor de desenvolvimento com recarga automática |
| `npm run build` | Gera a versão de produção em `dist/` |
| `npm run typecheck` | Verifica os tipos do TypeScript |
| `npm run preview` | Serve a versão de produção localmente |
| `npm run lint` | Análise estática (ESLint) |
| `npm test` | Testes unitários e de componente (Vitest) |
| `npm run test:cobertura` | Os mesmos testes com relatório de cobertura em `coverage/` |
| `npm run test:e2e` | Testes de sistema e aceitação no navegador (Playwright) |

## Testes

Os testes ficam fora do código, em `tests/`, e rodam a cada push e PR no GitHub Actions (jobs **Frontend** e
**Testes de sistema (Playwright)**), com os relatórios nos artefatos `cobertura-frontend` e `relatorio-playwright`.

| Nível | Ferramenta | Arquivos | O que cobrem |
|---|---|---|---|
| Unitário | Vitest | `tests/unit/format.test.ts` | Máscaras de CPF, telefone, CEP, CNPJ e moeda, com valores-limite (10, 11 e 12 dígitos etc.) e datas |
| Unitário | Vitest | `tests/unit/mappers.test.ts` | Conversão API ↔ tela de clientes, funcionários, produtos, preferências e pedidos |
| Unitário | Vitest | `tests/unit/client.test.ts` | Cliente HTTP: token, query, erros com campos, 401 (sessão expirada), 403 e servidor fora do ar |
| Unitário | Vitest | `tests/unit/periodos.test.ts` | Períodos dos Relatórios e "há X min" das notificações, nas fronteiras de minutos, horas e dias |
| Componente | Vitest + Testing Library | `tests/unit/LoginPage.test.tsx` | Login: campos vazios, sucesso, senha errada, mostrar senha e "Esqueci minha senha" |
| Componente | Vitest + Testing Library | `tests/unit/HeaderMenus.test.tsx` | Sino (contador, 9+, marcar como lidas, erro) e menu da conta por perfil |
| Sistema / aceitação | Playwright | `tests/e2e/*.spec.ts` | Com a API e o banco reais: login e permissões por perfil, venda completa até aparecer em Pedidos, aviso de estoque baixo levando aos produtos filtrados |

Nos testes unitários e de componente a API é simulada (`tests/unit/api.ts` troca o `fetch`). Os testes de sistema
sobem a API de verdade (perfil `h2`, com os dados de demonstração) e o frontend compilado. Para rodá-los localmente:

```bash
cd ../backend && ./mvnw -DskipTests package && cd ../frontend
npx playwright install chromium
npm run test:e2e
```
