-- Fluxo de vendas do protótipo: quanto de cada item saiu do estoque na confirmação
-- (para devolver exatamente isso no cancelamento) e índices das telas de Pedidos.

ALTER TABLE item_venda ADD COLUMN quantidade_baixada_estoque INTEGER NOT NULL DEFAULT 0;
ALTER TABLE item_venda ADD CONSTRAINT ck_item_venda_baixa_estoque
    CHECK (quantidade_baixada_estoque >= 0 AND quantidade_baixada_estoque <= quantidade);

CREATE INDEX idx_venda_confirmada_em ON venda (confirmada_em);
CREATE INDEX idx_venda_forma_pagamento ON venda (forma_pagamento);
