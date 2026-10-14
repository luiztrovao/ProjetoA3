# Identidade Visual — Ponto & Sabor

Guia extraído do protótipo no Figma Make
([Tela de Login Lanchonete](https://www.figma.com/make/XXGbp9NpOByG1oUpCC5PpI/Tela-de-Login-Lanchonete)).
Serve de referência para o frontend: toda cor, fonte e medida deve vir daqui.

## Marca

- **Nome:** Ponto & Sabor
- **Subtítulo:** Gestão para lanchonetes
- **Logo:** [logo.svg](logo.svg) — ícone de lanche em traço, usa `currentColor`. Aparece branco sobre um quadrado terracota (`--primary`) com cantos de 14px.

## Paleta

| Token | Cor | Uso |
|---|---|---|
| `--primary` | `#C75A3B` (terracota) | Botões principais, links, item ativo, gráficos |
| `--primary-dark` | `#A9432B` | Hover dos botões, texto de link |
| `--accent` | `#516D5E` (verde) | Ícones secundários, badges de gerente |
| `--sidebar` | `#415C4D` (verde escuro) | Sidebar, painel de apresentação do login, card de totais |
| `--ink` | `#27302B` | Texto principal |
| `--muted` | `#6D756F` | Texto secundário e descrições |
| `--cream` | `#F5F2EB` | Fundo da página |
| `--panel` | `#FFFEFA` | Fundo de cards e painéis |
| `--line` | `#E4E3DC` | Bordas de inputs e divisores |

### Cores de status (badges)

| Status | Texto | Fundo | Ponto |
|---|---|---|---|
| Sucesso / Ativo / Concluído | `#4D765D` | `#EDF5EF` | `#6E9C7E` |
| Em preparo / Alerta / Estoque baixo | `#9B683F` | `#FBF2E7` | `#D48A57` |
| Cancelado / Erro | `#A5584B` | `#F9ECE9` | `#C96D5C` |
| Inativo | `#7B817D` | `#F0F1EF` | `#9BA09C` |

### Perfis de acesso

| Perfil | Texto | Fundo |
|---|---|---|
| Dono | `#A44730` | terracota 10% |
| Gerente | `#496B58` | verde 11% |
| Vendedor | `#8B684E` | dourado (`#D48A57`) 12% |

## Tipografia

Fontes do Google Fonts:

- **Manrope** (600, 700): títulos, nome da marca e números grandes dos indicadores. Usa `letter-spacing` negativo (-0.02em a -0.045em).
- **DM Sans** (400, 500, 600, 700): todo o restante (texto, botões, tabelas).

Pequenos rótulos em caixa alta ("kicker", cabeçalhos de tabela): 11px, peso 700, `letter-spacing: 0.15em`, `uppercase`.

## Formas e sombras

| Elemento | Raio da borda |
|---|---|
| Container do login | 24px |
| Cards | 15px |
| Logo | 14px |
| Inputs e botões | 11px |
| Botões pequenos / filtros | 9–10px |
| Badges | 999px (pílula) |

- **Sombra de card:** `0 5px 18px rgba(54, 49, 38, 0.035)`
- **Sombra do botão principal:** `0 9px 22px rgba(169, 67, 43, 0.2)`
- **Foco:** anel de 3px em terracota com 12–24% de opacidade.
- **Transições:** 0.16s em cor, borda e sombra. Botões sobem 1px no hover.
- Respeitar `prefers-reduced-motion`.

## Componentes identificados

| Componente | Detalhes |
|---|---|
| Input | altura 52px, borda `--line`, foco com borda terracota e anel |
| Botão principal | altura 52px (formulário) ou 40px ("Novo cadastro"), fundo `--primary` |
| Sidebar | 258px, fundo verde escuro; recolhe para 86px abaixo de 1200px e some abaixo de 720px |
| Topbar | 94px, fundo translúcido com blur |
| Card de indicador | ícone em quadrado colorido (terracota, verde, dourado ou cinza), valor em Manrope |
| Tabela | cabeçalho em caixa alta cinza, linhas com hover suave, avatar com iniciais |
| Abas | texto cinza, ativa em `--primary-dark` com sublinhado terracota |
| Paginação | botões de 29px, página atual com fundo terracota |
| Badge | pílula com ponto colorido à esquerda |

## Telas do protótipo

1. **Login:** painel verde de apresentação à esquerda e formulário à direita (usuário/e-mail, senha com botão de mostrar, "Lembrar de mim", "Esqueci a senha").
2. **Dashboard:** 4 cards de indicadores, gráfico de vendas, resumo de faturamento, pedidos recentes e ações rápidas.
3. **Cadastros:** abas Clientes, Funcionários e Produtos, cada uma com busca, filtros, tabela, ações e paginação.

**Navegação da sidebar:** Dashboard, Vendas, Pedidos, Cadastros, Relatórios, Configurações.

## Pontos de corte (responsividade)

| Largura | Comportamento |
|---|---|
| > 1200px | Layout completo |
| ≤ 1200px | Sidebar só com ícones; indicadores em 2 colunas |
| ≤ 880px | Login sem o painel de apresentação |
| ≤ 720px | Sidebar escondida; marca vai para a topbar |
| ≤ 560px | Tudo em 1 coluna |

## Tokens para o Tailwind (frontend)

```css
@theme {
  --color-primary: #c75a3b;
  --color-primary-dark: #a9432b;
  --color-accent: #516d5e;
  --color-sidebar: #415c4d;
  --color-ink: #27302b;
  --color-muted: #6d756f;
  --color-cream: #f5f2eb;
  --color-panel: #fffefa;
  --color-line: #e4e3dc;

  --font-sans: "DM Sans", sans-serif;
  --font-display: "Manrope", sans-serif;
}
```
