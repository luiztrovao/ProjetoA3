# Guia de Contribuição

## Fluxo de trabalho

1. Atualize a `develop`:
   ```bash
   git checkout develop
   git pull
   ```
2. Crie uma branch para a sua tarefa:
   ```bash
   git checkout -b feature/cadastro-clientes
   ```
3. Faça commits pequenos e descritivos (veja o padrão abaixo).
4. Envie a branch e abra um **Pull Request para `develop`**.
5. Outro membro da equipe revisa e aprova o PR antes do merge.
6. Ao final do projeto (ou de uma entrega), é aberto um PR de `develop` para `main`.

> **Nunca** faça push direto na `main`: ela é protegida e só recebe a versão final.

## Nomes de branch

| Prefixo | Uso | Exemplo |
|---|---|---|
| `feature/` | Nova funcionalidade | `feature/dashboard-vendas` |
| `fix/` | Correção de bug | `fix/calculo-total-venda` |
| `docs/` | Documentação | `docs/plano-de-testes` |
| `test/` | Apenas testes | `test/produto-service` |
| `refactor/` | Melhoria de código sem mudar comportamento | `refactor/cliente-controller` |

## Padrão de commits (Conventional Commits)

```
<tipo>: <descrição curta no imperativo>
```

Tipos: `feat`, `fix`, `docs`, `test`, `refactor`, `style`, `chore`.

Exemplos:
- `feat: adiciona cadastro de produtos`
- `fix: corrige cálculo do total da venda`
- `test: adiciona testes do ClienteService`

## Checklist antes de abrir um PR

- [ ] O código compila e roda localmente
- [ ] Testes novos/alterados passam
- [ ] Sem código comentado ou `console.log`/`System.out` esquecidos
- [ ] Descrição do PR explica o que foi feito e como testar
