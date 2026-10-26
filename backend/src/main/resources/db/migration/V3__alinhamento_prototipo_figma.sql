-- Alinha o banco ao protótipo do Figma Make (versão 12).
-- Documentação: docs/modelagem/modelo-de-dados.md

-- Cliente: endereço e observações
ALTER TABLE cliente ADD COLUMN cep VARCHAR(8);
ALTER TABLE cliente ADD COLUMN logradouro VARCHAR(150);
ALTER TABLE cliente ADD COLUMN numero VARCHAR(20);
ALTER TABLE cliente ADD COLUMN complemento VARCHAR(100);
ALTER TABLE cliente ADD COLUMN bairro VARCHAR(80);
ALTER TABLE cliente ADD COLUMN cidade VARCHAR(80);
ALTER TABLE cliente ADD COLUMN uf VARCHAR(2);
ALTER TABLE cliente ADD COLUMN observacoes VARCHAR(500);

-- Usuário: login por nome de usuário ou e-mail; CPF e telefone; último acesso
ALTER TABLE usuario ADD COLUMN username VARCHAR(50);
ALTER TABLE usuario ADD COLUMN cpf VARCHAR(11);
ALTER TABLE usuario ADD COLUMN telefone VARCHAR(11);
ALTER TABLE usuario ADD COLUMN ultimo_acesso TIMESTAMP;
UPDATE usuario SET username = 'usuario' || id;
ALTER TABLE usuario ALTER COLUMN username SET NOT NULL;
ALTER TABLE usuario ALTER COLUMN email DROP NOT NULL;
ALTER TABLE usuario ADD CONSTRAINT uk_usuario_username UNIQUE (username);
ALTER TABLE usuario ADD CONSTRAINT uk_usuario_cpf UNIQUE (cpf);

-- Produto: código único e imagem
ALTER TABLE produto ADD COLUMN codigo VARCHAR(20);
ALTER TABLE produto ADD COLUMN imagem_url VARCHAR(500);
UPDATE produto SET codigo = 'PRD' || id;
ALTER TABLE produto ALTER COLUMN codigo SET NOT NULL;
ALTER TABLE produto ADD CONSTRAINT uk_produto_codigo UNIQUE (codigo);

-- Categorias do protótipo
UPDATE categoria SET nome = 'Hambúrgueres' WHERE nome = 'Lanches';
UPDATE categoria SET nome = 'Acompanhamentos' WHERE nome = 'Porções';
INSERT INTO categoria (nome) VALUES ('Combos');

-- Venda: pagamento em dinheiro, confirmação e cancelamento com motivo
ALTER TABLE venda ADD COLUMN valor_recebido NUMERIC(10,2);
ALTER TABLE venda ADD COLUMN troco NUMERIC(10,2);
ALTER TABLE venda ADD COLUMN confirmada_em TIMESTAMP;
ALTER TABLE venda ADD COLUMN cancelada_em TIMESTAMP;
ALTER TABLE venda ADD COLUMN motivo_cancelamento VARCHAR(500);

-- Configurações do sistema (registro único, id = 1)
CREATE TABLE configuracao (
    id                         BIGINT       PRIMARY KEY,
    -- Dados da lanchonete
    nome_fantasia              VARCHAR(120) NOT NULL,
    razao_social               VARCHAR(150),
    cnpj                       VARCHAR(14),
    telefone                   VARCHAR(11),
    email                      VARCHAR(150),
    cep                        VARCHAR(8),
    logradouro                 VARCHAR(150),
    numero                     VARCHAR(20),
    bairro                     VARCHAR(80),
    cidade                     VARCHAR(80),
    uf                         VARCHAR(2),
    logo_url                   VARCHAR(500),
    -- Preferências de vendas
    controle_estoque           BOOLEAN      NOT NULL DEFAULT TRUE,
    bloquear_sem_estoque       BOOLEAN      NOT NULL DEFAULT TRUE,
    alerta_estoque_baixo       INTEGER      NOT NULL DEFAULT 5,
    confirmar_cancelamento     BOOLEAN      NOT NULL DEFAULT TRUE,
    permitir_venda_sem_cliente BOOLEAN      NOT NULL DEFAULT TRUE,
    status_inicial_venda       VARCHAR(20)  NOT NULL DEFAULT 'EM_PREPARO',
    aceita_dinheiro            BOOLEAN      NOT NULL DEFAULT TRUE,
    aceita_credito             BOOLEAN      NOT NULL DEFAULT TRUE,
    aceita_debito              BOOLEAN      NOT NULL DEFAULT TRUE,
    aceita_pix                 BOOLEAN      NOT NULL DEFAULT TRUE,
    atualizado_em              TIMESTAMP    NOT NULL,
    CONSTRAINT ck_configuracao_registro_unico CHECK (id = 1),
    CONSTRAINT ck_configuracao_status_inicial CHECK (status_inicial_venda IN ('EM_PREPARO', 'CONCLUIDA')),
    CONSTRAINT ck_configuracao_alerta_estoque CHECK (alerta_estoque_baixo >= 0)
);

INSERT INTO configuracao (id, nome_fantasia, atualizado_em) VALUES (1, 'Ponto & Sabor', CURRENT_TIMESTAMP);
