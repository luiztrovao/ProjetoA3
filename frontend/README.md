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
| Dashboard, Vendas, Pedidos, Relatórios | Ainda do protótipo (memória e `localStorage` do navegador) |

## Estrutura

| Pasta/arquivo | Conteúdo |
|---|---|
| `src/api/` | Cliente HTTP (`fetch`) com o token, tratamento de erros da API e os endpoints usados |
| `src/pages/LoginPage.tsx` | Tela de login |
| `src/pages/cadastros/` | Tela Cadastros: tabelas, formulário (modal) e conversão API ↔ tela |
| `src/pages/settings/` | Tela Configurações |
| `src/components/` | Ícones, logo e elementos de tabela compartilhados |
| `src/utils/format.ts` | Máscaras (CPF, telefone, CEP, CNPJ, moeda) e formatação de datas |
| `src/types.ts` | Tipos usados pelas telas |
| `src/App.tsx` | Sessão, navegação e as telas que ainda usam os dados do protótipo |
| `src/styles/app.css` | Estilos das telas |

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
