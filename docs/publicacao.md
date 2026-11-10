# Publicação do sistema (custo zero)

| Parte | Serviço gratuito | Endereço final |
|---|---|---|
| Banco de dados (PostgreSQL) | [Neon](https://neon.tech) | acessado só pela API e pelo SGBD |
| API (Spring Boot) | [Render](https://render.com) — plano Free, via Docker | `https://ponto-e-sabor-api.onrender.com` |
| Frontend (React) | GitHub Pages | `https://luiztrovao.github.io/ProjetoA3/` |

O que já está no repositório:

- `backend/Dockerfile`: empacota a API com o perfil `prod` (PostgreSQL). O CI confere que a imagem é gerada.
- `render.yaml`: o *Blueprint* do Render, com as variáveis de ambiente que a API precisa.
- `.github/workflows/deploy-pages.yml`: gera o build do frontend e publica no GitHub Pages sempre que algo em
  `frontend/` muda na `develop` ou na `main`.

As contas e os segredos ficam com a equipe: **nenhuma senha vai para o repositório** (ele é público).

## 1. Banco de dados no Neon

1. Crie a conta em <https://neon.tech> (pode entrar com o GitHub) e um projeto chamado `ponto-e-sabor`.
   - Versão do PostgreSQL: a padrão.
   - Região: a mesma do Render: **AWS US East 1 (N. Virginia)** no Neon e **Virginia (US East)** no Render (já definida no `render.yaml`).
2. Em **Connect**, desligue a opção **Connection pooling** e copie os dados da conexão direta. A string vem assim:
   ```
   postgresql://neondb_owner:SENHA@ep-xxxx-xxxx.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```
3. Separe as partes para o Render:
   - `DB_URL`: `jdbc:postgresql://ep-xxxx-xxxx.us-east-2.aws.neon.tech/neondb?sslmode=require`.
     É o host e o banco, com `jdbc:` na frente e **sem** usuário e senha.
   - `DB_USERNAME`: `neondb_owner`
   - `DB_PASSWORD`: a senha que aparece na string.

As tabelas não precisam ser criadas à mão: o **Flyway** roda as migrações `V1` a `V6` na primeira vez que a API subir.

## 2. API no Render

1. Crie a conta em <https://render.com> entrando com o GitHub e autorize o acesso ao repositório `ProjetoA3`.
2. **New → Blueprint**, escolha o repositório e confirme. O Render lê o `render.yaml` e cria o serviço `ponto-e-sabor-api` (plano Free).
3. Preencha as variáveis pedidas:
   - `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`: os dados do Neon (passo 1).
   - `APP_DADOS_DEMO_SENHA`: a senha dos usuários de demonstração (matheus, carlos, fernanda…).
     **Escolha uma senha nova**, não a `demo123` do ambiente local: o site ficará aberto na internet.
   - `JWT_SECRET` é gerado sozinho, e `CORS_ORIGENS` já vem preenchido.
4. Aguarde o deploy.
   - Na **primeira subida** a API cria as tabelas e gera os dados de demonstração (cerca de 13 mil vendas).
   - Pode levar alguns minutos. As próximas subidas são rápidas, porque os dados só são gerados com o banco vazio.
5. Teste: `https://ponto-e-sabor-api.onrender.com/api/publico/identidade` deve responder com o nome da lanchonete.
   O Swagger fica em `/swagger-ui.html`.

> O endereço exato aparece no topo da página do serviço no Render. Se for diferente, use-o nos próximos passos.

## 3. Frontend no GitHub Pages

1. No GitHub: **Settings → Secrets and variables → Actions → aba Variables → New repository variable**:
   - Nome: `VITE_API_URL`
   - Valor: o endereço da API, sem barra no final (ex.: `https://ponto-e-sabor-api.onrender.com`).
2. **Settings → Pages → Build and deployment → Source: GitHub Actions**.
3. Publique: o workflow **Publicar frontend (GitHub Pages)** roda sozinho a cada merge na `develop` (ou na `main`)
   que mexa em `frontend/`. Para publicar sem esperar um merge, use **Actions → Publicar frontend (GitHub Pages) →
   Run workflow** e escolha a branch `develop`.
4. Acesse `https://luiztrovao.github.io/ProjetoA3/` e entre com um usuário de demonstração.

## Limites do plano gratuito (importante para a apresentação)

- **Render Free "dorme"** depois de 15 minutos sem acesso, e o primeiro acesso depois disso leva **cerca de 1 minuto**.
  Abra o site uns minutos antes de apresentar.
- **Neon Free** suspende o banco quando ocioso e acorda em segundos. São 0,5 GB de armazenamento, folgado para o projeto.
- As vendas de demonstração se mantêm atualizadas sozinhas: a cada vez que a API sobe (inclusive quando o Render a
  acorda depois de dormir), ela completa as vendas fictícias desde a última venda até o momento atual. Para forçar
  isso antes da apresentação, use **Manual Deploy → Restart service** no Render.
  Para recomeçar do zero (18 meses novos de histórico):
  1. No Neon, em **Branches**, crie um banco vazio ou apague as tabelas.
  2. No Render, use **Manual Deploy → Restart service**.

## Acesso dos alunos na apresentação (perfil Visitante)

O perfil **Visitante** só enxerga a tela **Relatórios**: indicadores, picos de venda, histórico de pedidos e
exportação em CSV. Ele não abre Dashboard, Vendas, Pedidos, Cadastros nem Configurações, não recebe notificações
e não consegue trocar os dados nem a senha da própria conta. Todas as rotas de escrita da API devolvem 403 para ele.

**No dia da apresentação:**

1. Entre no site publicado como Dono.
2. Vá em **Cadastros → Funcionários → Novo funcionário**, com:
   - Nome: `Turma de Qualidade`;
   - Usuário: `turma`;
   - uma senha simples de ditar;
   - **Perfil: Visitante**.
3. Passe aos alunos o endereço <https://luiztrovao.github.io/ProjetoA3/>, o usuário e a senha. Vários alunos podem
   usar a mesma conta ao mesmo tempo.
4. **Depois da apresentação**, desative o usuário em **Cadastros → Funcionários** (menu ⋯ → **Desativar registro**). O acesso
   acaba na hora, inclusive para quem ainda estiver com a tela aberta.

No ambiente local, com os dados de demonstração, já existe o usuário `visitante`, com a mesma senha dos outros
usuários de demonstração.

Abra o site alguns minutos antes de passar o acesso: com muitos alunos entrando ao mesmo tempo, a primeira
chamada à API "dormindo" no Render leva cerca de 50 segundos.

## Mostrando o banco por um SGBD (apresentação de Banco de Dados)

O banco do Neon é um PostgreSQL comum, então qualquer SGBD conecta nele com os dados do passo 1.

**DBeaver** (gratuito):
1. **Nova conexão → PostgreSQL**.
2. Preencha:
   - Host: `ep-xxxx-xxxx.us-east-2.aws.neon.tech`
   - Porta: `5432`
   - Banco: `neondb`
   - Usuário e senha: os do Neon.
3. Na aba **SSL**, marque *Usar SSL* com o modo `require`.
4. Para ver o **diagrama ER**: botão direito no esquema `public` → **Visualizar diagrama**.

**pgAdmin** (gratuito):
1. **Register → Server**.
2. Em **Connection**, use os mesmos dados do DBeaver. Em **Parameters**, defina **SSL mode = require**.

O próprio Neon tem um **SQL Editor** no navegador, útil se não der para instalar nada no computador da apresentação.

Ideia para a apresentação: fazer uma ação no sistema e mostrar o resultado na hora no SGBD.
Por exemplo, cancelar um pedido na tela Pedidos e rodar:

```sql
SELECT id, status, cancelada_em, motivo_cancelamento
FROM venda
ORDER BY cancelada_em DESC NULLS LAST
LIMIT 5;
```
