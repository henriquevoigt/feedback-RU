-- script 04: consultas SQL
-- requisito: mínimo de 6 consultas, sendo no mínimo 4 delas contendo JOIN

-- CONSULTA 1 (com join) henrique
-- catálogo nutricional de pratos com a lista de ingredientes utilizados em cada prato.
-- tabelas: prato, utiliza, ingrediente (3 tabelas com JOIN)

SELECT
    p.id_prato,
    p.nome AS nome_prato,
    p.calorias,
    COALESCE(STRING_AGG(i.nome, ', ' ORDER BY i.nome), 'Sem ingredientes vinculados') AS ingredientes
FROM prato p
LEFT JOIN utiliza u ON u.id_prato = p.id_prato
LEFT JOIN ingrediente i ON i.id_ingrediente = u.id_ingrediente
GROUP BY p.id_prato, p.nome, p.calorias
ORDER BY p.nome;

-- CONSULTA 2 (com join) henrique
-- detalhamento completo dos cardápios planejados: data, turno, nutricionista responsável (nome e CRN), unidades do RU que oferecem o cardápio, pratos que compõem a refeição e soma total de calorias do cardápio.
-- tabelas: cardapio, admin, usuario, oferece, restaurante, contem, prato

SELECT
    c.id_cardapio,
    c.data,
    c.turno,
    u.nome AS nutricionista_responsavel,
    a.crn,
    COALESCE((
        SELECT STRING_AGG(r.nome, ', ' ORDER BY r.nome)
        FROM oferece o
        JOIN restaurante r ON r.id_rest = o.id_rest
        WHERE o.id_cardapio = c.id_cardapio
    ), 'Nenhum restaurante vinculado') AS restaurantes,
    COALESCE((
        SELECT STRING_AGG(p.nome, ', ' ORDER BY p.nome)
        FROM contem ct
        JOIN prato p ON p.id_prato = ct.id_prato
        WHERE ct.id_cardapio = c.id_cardapio
    ), 'Nenhum prato vinculado') AS pratos_cardapio,
    COALESCE((
        SELECT SUM(p.calorias)
        FROM contem ct
        JOIN prato p ON p.id_prato = ct.id_prato
        WHERE ct.id_cardapio = c.id_cardapio
    ), 0) AS calorias_totais
FROM cardapio c
JOIN admin a ON a.id_usuario = c.id_admin
JOIN usuario u ON u.id_usuario = a.id_usuario
ORDER BY c.data DESC, c.turno;

-- CONSULTA 3 (com join) samuel
-- histórico de consumos (catraca) e respectivos feedbacks dos usuários

-- CONSULTA 4 samuel
-- consulta de filtragem/agrupamento sobre consumos ou avaliações

-- CONSULTA 5 (com join) pedro
-- média de notas e quantidade de avaliações por cardápio, turno e restaurante

-- CONSULTA 6 (com join) pedro
-- análise de satisfação por faixa de horário de entrada na catraca
