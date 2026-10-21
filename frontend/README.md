# Frontend

Aplicação **React 19 + Vite + TypeScript**, com Tailwind CSS 4 e CSS próprio seguindo a
[identidade visual](../docs/design/identidade-visual.md).

## Origem do código

O código atual é o **protótipo do Figma Make**
([Tela de Login Lanchonete](https://www.figma.com/make/XXGbp9NpOByG1oUpCC5PpI/Tela-de-Login-Lanchonete), versão 12),
importado sem alterações:

| Arquivo | Conteúdo |
|---|---|
| `src/App.tsx` | Todas as telas: Login, Dashboard, Vendas (carrinho, revisão e confirmação), Pedidos, Cadastros, Relatórios e Configurações |
| `src/styles/app.css` | Estilos das telas |
| `src/main.tsx` | Ponto de entrada do React |

> **Atenção:** é um protótipo. Os dados ficam em memória e no `localStorage` do navegador, e o login é
> **apenas demonstrativo** (senha `demo`, verificada no próprio navegador). Nas próximas etapas, as telas
> serão divididas em componentes e passarão a usar a API do backend, com login real (JWT).

## Pré-requisitos

- **Node.js 22** ou superior

## Como rodar

Na pasta `frontend/`:

```bash
npm install
npm run dev
```

Acesse http://localhost:5173. Usuários demonstrativos: `matheus` (Dono), `carlos` (Gerente) e
`fernanda` (Vendedor), todos com a senha `demo`.

## Scripts

| Comando | O que faz |
|---|---|
| `npm run dev` | Servidor de desenvolvimento com recarga automática |
| `npm run build` | Gera a versão de produção em `dist/` |
| `npm run typecheck` | Verifica os tipos do TypeScript |
| `npm run preview` | Serve a versão de produção localmente |
