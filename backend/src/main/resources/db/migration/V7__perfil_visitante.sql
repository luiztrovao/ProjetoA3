-- Perfil Visitante: acesso só de leitura à tela Relatórios (ex.: alunos na apresentação do projeto).

ALTER TABLE usuario DROP CONSTRAINT ck_usuario_perfil;
ALTER TABLE usuario ADD CONSTRAINT ck_usuario_perfil
    CHECK (perfil IN ('DONO', 'GERENTE', 'VENDEDOR', 'VISITANTE'));
