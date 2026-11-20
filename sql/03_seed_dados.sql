-- Script 03: dados fictícios de usuários/consumos e snapshot real dos cardápios.
-- O snapshot de cardápios veio do JSON público enviado pelo grupo.
-- Execute depois de 01_ddl_schema.sql e 02_triggers.sql.

BEGIN;

-- 1. Usuários e especializações
INSERT INTO usuario (id_usuario, nome, email) VALUES
    (1, 'Ana Souza',       'ana.souza@example.test'),
    (2, 'Bruno Lima',      'bruno.lima@example.test'),
    (3, 'Camila Rocha',    'camila.rocha@example.test'),
    (4, 'Diego Martins',   'diego.martins@example.test'),
    (5, 'Elisa Ferreira',  'elisa.ferreira@example.test'),
    (6, 'Fernanda Alves',  'fernanda.alves@example.test'),
    (7, 'Gustavo Costa',   'gustavo.costa@example.test'),
    (8, 'Helena Pereira',  'helena.pereira@example.test')
ON CONFLICT (id_usuario) DO NOTHING;

INSERT INTO aluno (id_usuario, matricula) VALUES
    (1, '20260001'), (2, '20260002'), (3, '20260003'), (4, '20260004')
ON CONFLICT (id_usuario) DO NOTHING;

INSERT INTO servidor (id_usuario, siape) VALUES
    (5, '1100001'), (8, '1100002')
ON CONFLICT (id_usuario) DO NOTHING;

INSERT INTO admin (id_usuario, crn) VALUES
    (6, 'CRN-2-EXEMPLO-001'), (7, 'CRN-2-EXEMPLO-002')
ON CONFLICT (id_usuario) DO NOTHING;

-- 2. Unidades, ingredientes e pratos
INSERT INTO restaurante (id_rest, nome) VALUES
    (1, 'CAMPUS CAPÃO DO LEÃO'),
    (2, 'UNIDADE CENTRO'),
    (3, 'ANGLO'),
    (4, 'CEU')
ON CONFLICT (id_rest) DO NOTHING;

INSERT INTO ingrediente (id_ingrediente, nome) VALUES
    (1, 'Arroz'),
    (2, 'Feijão preto'),
    (3, 'Frango'),
    (4, 'Carne bovina'),
    (5, 'Macarrão'),
    (6, 'Molho de tomate'),
    (7, 'Alface'),
    (8, 'Tomate'),
    (9, 'Cenoura'),
    (10, 'Abóbora'),
    (11, 'Ovo'),
    (12, 'Farinha de mandioca'),
    (13, 'Banana'),
    (14, 'Maçã'),
    (15, 'Lentilha'),
    (16, 'Batata')
ON CONFLICT (id_ingrediente) DO NOTHING;

INSERT INTO prato (id_prato, nome, calorias) VALUES
    (1, 'ARROZ BRANCO',                  174),
    (2, 'FEIJÃO PRETO',                   91),
    (3, 'Frango assado',                 280),
    (4, 'Carne moída com legumes',       260),
    (5, 'Macarrão ao molho de tomate',   310),
    (6, 'Omelete de legumes',            220),
    (7, 'Abóbora assada',                110),
    (8, 'Salada de alface e tomate',      45),
    (9, 'Farofa',                        150),
    (10, 'Banana',                        90),
    (11, 'Maçã',                          80),
    (12, 'Lentilha cozida',              180),
    (13, 'Purê de batata',               170)
ON CONFLICT (id_prato) DO NOTHING;

INSERT INTO utiliza (id_prato, id_ingrediente) VALUES
    (1, 1),
    (2, 2),
    (3, 3),
    (4, 4), (4, 9), (4, 8),
    (5, 5), (5, 6),
    (6, 11), (6, 9), (6, 8),
    (7, 10),
    (8, 7), (8, 8), (8, 9),
    (9, 12),
    (10, 13),
    (11, 14),
    (12, 15),
    (13, 16)
ON CONFLICT (id_prato, id_ingrediente) DO NOTHING;

-- 3. Seis cardápios de almoço/jantar em datas recentes.
INSERT INTO cardapio (id_cardapio, data, turno, id_admin) VALUES
    (1, CURRENT_DATE - 10, 'Almoço', 6),
    (2, CURRENT_DATE - 10, 'Jantar', 6),
    (3, CURRENT_DATE - 8,  'Almoço', 7),
    (4, CURRENT_DATE - 7,  'Jantar', 7),
    (5, CURRENT_DATE - 4,  'Almoço', 6),
    (6, CURRENT_DATE - 3,  'Jantar', 7)
ON CONFLICT (id_cardapio) DO NOTHING;

INSERT INTO oferece (id_cardapio, id_rest) VALUES
    (1, 1), (1, 2),
    (2, 2), (2, 3),
    (3, 1), (3, 3),
    (4, 1), (4, 2),
    (5, 2), (5, 3),
    (6, 1), (6, 2), (6, 3)
ON CONFLICT (id_cardapio, id_rest) DO NOTHING;

INSERT INTO contem (id_cardapio, id_prato) VALUES
    (1, 1), (1, 2), (1, 3), (1, 8), (1, 10),
    (2, 1), (2, 2), (2, 4), (2, 7), (2, 11),
    (3, 1), (3, 6), (3, 8), (3, 9), (3, 12),
    (4, 1), (4, 2), (4, 5), (4, 8), (4, 11),
    (5, 1), (5, 2), (5, 3), (5, 7), (5, 10),
    (6, 1), (6, 6), (6, 8), (6, 9), (6, 13)
ON CONFLICT (id_cardapio, id_prato) DO NOTHING;

-- 4. Passagens na catraca. Alguns consumos ficam sem feedback para demonstrar
-- que uma avaliação é opcional e pode ser enviada depois.
INSERT INTO consumo (id_consumo, id_usuario, id_cardapio, data_hora_entrada) VALUES
    (1, 1, 1, (CURRENT_DATE - 10) + TIME '11:20:00'),
    (2, 2, 1, (CURRENT_DATE - 10) + TIME '11:42:00'),
    (3, 3, 1, (CURRENT_DATE - 10) + TIME '12:05:00'),
    (4, 5, 1, (CURRENT_DATE - 10) + TIME '12:28:00'),
    (5, 1, 2, (CURRENT_DATE - 10) + TIME '19:05:00'),
    (6, 4, 2, (CURRENT_DATE - 10) + TIME '19:31:00'),
    (7, 2, 3, (CURRENT_DATE - 8)  + TIME '11:15:00'),
    (8, 3, 3, (CURRENT_DATE - 8)  + TIME '11:48:00'),
    (9, 5, 3, (CURRENT_DATE - 8)  + TIME '12:12:00'),
    (10, 1, 4, (CURRENT_DATE - 7) + TIME '19:13:00'),
    (11, 4, 4, (CURRENT_DATE - 7) + TIME '19:42:00'),
    (12, 8, 4, (CURRENT_DATE - 7) + TIME '20:04:00'),
    (13, 2, 5, (CURRENT_DATE - 4) + TIME '11:21:00'),
    (14, 3, 5, (CURRENT_DATE - 4) + TIME '11:58:00'),
    (15, 4, 6, (CURRENT_DATE - 3) + TIME '19:10:00'),
    (16, 5, 6, (CURRENT_DATE - 3) + TIME '19:25:00')
ON CONFLICT (id_consumo) DO NOTHING;

-- 5. Avaliações enviadas alguns minutos após a entrada.
INSERT INTO feedback (id_feedback, nota, comentario, data_envio, id_consumo)
SELECT
    v.id_feedback,
    v.nota,
    v.comentario,
    c.data_hora_entrada + v.atraso,
    c.id_consumo
FROM (VALUES
    (1,  5, 'Refeição saborosa e bem servida.',          1, INTERVAL '8 minutes'),
    (2,  4, 'Gostei do frango; a salada podia estar mais fresca.', 2, INTERVAL '12 minutes'),
    (3,  3, 'Refeição boa, mas a fila estava grande.',    4, INTERVAL '18 minutes'),
    (4,  4, 'Boa combinação de pratos.',                 5, INTERVAL '10 minutes'),
    (5,  2, 'A carne estava um pouco seca.',              6, INTERVAL '14 minutes'),
    (6,  5, 'O almoço estava ótimo.',                     7, INTERVAL '7 minutes'),
    (7,  3, 'A omelete estava boa, mas faltou tempero.',  8, INTERVAL '11 minutes'),
    (8,  4, 'Bom atendimento e refeição equilibrada.',   10, INTERVAL '9 minutes'),
    (9,  3, 'A refeição estava razoável.',                11, INTERVAL '16 minutes'),
    (10, 5, 'Tudo bem preparado.',                        13, INTERVAL '6 minutes'),
    (11, 4, 'Gostei da variedade de acompanhamentos.',    14, INTERVAL '13 minutes'),
    (12, 2, 'O purê estava frio.',                        15, INTERVAL '15 minutes')
) AS v(id_feedback, nota, comentario, id_consumo, atraso)
JOIN consumo AS c ON c.id_consumo = v.id_consumo
ON CONFLICT (id_feedback) DO NOTHING;

-- IDs foram fornecidos para deixar a demonstração previsível. Ajusta as
-- sequências para que próximos cadastros automáticos continuem do maior ID.
-- Cardápios reais extraídos do snapshot JSON enviado pelo grupo (atualizado em 18/11/2026).
-- Fonte: cardapio_ru.json. Descrição, porção e ID externo não existem no modelo lógico atual.
-- Cada unidade/data/turno recebe um cardápio próprio para não misturar pratos de RUs diferentes.
INSERT INTO prato (id_prato, nome, calorias) VALUES
    (14, 'AIPIM SOUTÉ', 125),
    (15, 'ARROZ INTEGRAL', 156),
    (16, 'BATATA PALHA', 545),
    (17, 'BATATA RÚSTICA', 75),
    (18, 'CARRÉ SUÍNO', 191),
    (19, 'CARRETEIRO', 258),
    (20, 'CARRETEIRO DE PROTEINA', 235),
    (21, 'CENOURA E CHUCHU REFOGADOS', 80),
    (22, 'CHARUTO DE LEGUMES COM PROTÉINA DE SOJA', 121),
    (23, 'FRANGO CAMPEIRO', 338),
    (24, 'FRUTA', 0),
    (25, 'GUISADO COM BATATA', 312),
    (26, 'ISCA BOVINA ACEBOLADA', 182),
    (27, 'LENTILHA COM BATATAS', 78),
    (28, 'MASSA AO MOLHO BRANCO COM QUEIJO', 367),
    (29, 'POLENTA CALABRESA', 266),
    (30, 'PROTEÍNA À MODA CAIPIRA', 131),
    (31, 'PROTEÍNA COM ERVILHA', 123),
    (32, 'PROTEÍNA COM MILHO', 125),
    (33, 'REPOLHO COM BACON', 184),
    (34, 'SALADA COZIDA', 0),
    (35, 'SALADA CRUA', 0),
    (36, 'SALADA CRUA OU COZIDA', 15),
    (37, 'SALADA FOLHOSA', 0),
    (38, 'SOBRECOXA ASSADA À MODA CAIPIRA', 260),
    (39, 'STROGONOFF DE FRANGO', 123),
    (40, 'STROGONOFF DE GRÃO DE BICO', 126)
ON CONFLICT (id_prato) DO NOTHING;

INSERT INTO cardapio (id_cardapio, data, turno, id_admin) VALUES
    (7, '2026-11-18', 'Almoço', 6),
    (8, '2026-11-18', 'Almoço', 6),
    (9, '2026-11-18', 'Almoço', 6),
    (10, '2026-11-18', 'Jantar', 6),
    (11, '2026-11-18', 'Jantar', 6),
    (12, '2026-11-19', 'Almoço', 6),
    (13, '2026-11-19', 'Almoço', 6),
    (14, '2026-11-19', 'Almoço', 6),
    (15, '2026-11-19', 'Jantar', 6),
    (16, '2026-11-19', 'Jantar', 6),
    (17, '2026-11-20', 'Almoço', 6),
    (18, '2026-11-20', 'Jantar', 6),
    (19, '2026-11-21', 'Almoço', 6),
    (20, '2026-11-21', 'Jantar', 6),
    (21, '2026-11-22', 'Almoço', 6),
    (22, '2026-11-22', 'Jantar', 6)
ON CONFLICT (id_cardapio) DO NOTHING;

INSERT INTO oferece (id_cardapio, id_rest) VALUES
    (7, 3),
    (8, 1),
    (9, 2),
    (10, 3),
    (11, 2),
    (12, 3),
    (13, 1),
    (14, 2),
    (15, 3),
    (16, 2),
    (17, 2),
    (18, 2),
    (19, 2),
    (20, 2),
    (21, 2),
    (22, 2)
ON CONFLICT (id_cardapio, id_rest) DO NOTHING;

INSERT INTO contem (id_cardapio, id_prato) VALUES
    (7, 1),
    (7, 15),
    (7, 2),
    (7, 23),
    (7, 17),
    (7, 31),
    (7, 37),
    (7, 35),
    (7, 36),
    (7, 24),
    (8, 1),
    (8, 15),
    (8, 2),
    (8, 23),
    (8, 17),
    (8, 31),
    (8, 37),
    (8, 35),
    (8, 36),
    (8, 24),
    (9, 1),
    (9, 15),
    (9, 2),
    (9, 39),
    (9, 16),
    (9, 40),
    (9, 37),
    (9, 35),
    (9, 34),
    (9, 24),
    (10, 1),
    (10, 15),
    (10, 2),
    (10, 23),
    (10, 17),
    (10, 31),
    (10, 37),
    (10, 35),
    (10, 36),
    (10, 24),
    (11, 1),
    (11, 15),
    (11, 2),
    (11, 39),
    (11, 16),
    (11, 40),
    (11, 37),
    (11, 35),
    (11, 34),
    (11, 24),
    (12, 1),
    (12, 15),
    (12, 2),
    (12, 19),
    (12, 14),
    (12, 20),
    (12, 37),
    (12, 35),
    (12, 36),
    (12, 24),
    (13, 1),
    (13, 15),
    (13, 2),
    (13, 19),
    (13, 14),
    (13, 20),
    (13, 37),
    (13, 35),
    (13, 36),
    (13, 24),
    (14, 1),
    (14, 15),
    (14, 2),
    (14, 26),
    (14, 29),
    (14, 32),
    (14, 37),
    (14, 35),
    (14, 34),
    (14, 24),
    (15, 1),
    (15, 15),
    (15, 2),
    (15, 19),
    (15, 14),
    (15, 20),
    (15, 37),
    (15, 35),
    (15, 36),
    (15, 24),
    (16, 1),
    (16, 15),
    (16, 2),
    (16, 26),
    (16, 29),
    (16, 32),
    (16, 37),
    (16, 35),
    (16, 34),
    (16, 24),
    (17, 1),
    (17, 15),
    (17, 2),
    (17, 18),
    (17, 33),
    (17, 22),
    (17, 37),
    (17, 35),
    (17, 34),
    (17, 24),
    (18, 1),
    (18, 15),
    (18, 2),
    (18, 18),
    (18, 33),
    (18, 22),
    (18, 37),
    (18, 35),
    (18, 34),
    (18, 24),
    (19, 1),
    (19, 15),
    (19, 2),
    (19, 38),
    (19, 21),
    (19, 30),
    (19, 37),
    (19, 35),
    (19, 34),
    (19, 24),
    (20, 1),
    (20, 15),
    (20, 2),
    (20, 38),
    (20, 21),
    (20, 30),
    (20, 37),
    (20, 35),
    (20, 34),
    (20, 24),
    (21, 1),
    (21, 15),
    (21, 2),
    (21, 25),
    (21, 28),
    (21, 27),
    (21, 37),
    (21, 35),
    (21, 34),
    (21, 24),
    (22, 1),
    (22, 15),
    (22, 2),
    (22, 25),
    (22, 28),
    (22, 27),
    (22, 37),
    (22, 35),
    (22, 34),
    (22, 24)
ON CONFLICT (id_cardapio, id_prato) DO NOTHING;

SELECT setval(pg_get_serial_sequence('usuario', 'id_usuario'), (SELECT MAX(id_usuario) FROM usuario), true);
SELECT setval(pg_get_serial_sequence('restaurante', 'id_rest'), (SELECT MAX(id_rest) FROM restaurante), true);
SELECT setval(pg_get_serial_sequence('ingrediente', 'id_ingrediente'), (SELECT MAX(id_ingrediente) FROM ingrediente), true);
SELECT setval(pg_get_serial_sequence('prato', 'id_prato'), (SELECT MAX(id_prato) FROM prato), true);
SELECT setval(pg_get_serial_sequence('cardapio', 'id_cardapio'), (SELECT MAX(id_cardapio) FROM cardapio), true);
SELECT setval(pg_get_serial_sequence('consumo', 'id_consumo'), (SELECT MAX(id_consumo) FROM consumo), true);
SELECT setval(pg_get_serial_sequence('feedback', 'id_feedback'), (SELECT MAX(id_feedback) FROM feedback), true);

COMMIT;
