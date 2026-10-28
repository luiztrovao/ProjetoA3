-- Notificações: o sino calcula os avisos a partir dos dados (estoque e pedidos);
-- aqui ficam só o momento em que cada usuário marcou tudo como lido e o tempo de atraso dos pedidos.

ALTER TABLE usuario ADD COLUMN notificacoes_lidas_em TIMESTAMP;

ALTER TABLE configuracao ADD COLUMN minutos_alerta_preparo INTEGER NOT NULL DEFAULT 20;
ALTER TABLE configuracao ADD CONSTRAINT ck_configuracao_minutos_alerta
    CHECK (minutos_alerta_preparo BETWEEN 5 AND 240);
