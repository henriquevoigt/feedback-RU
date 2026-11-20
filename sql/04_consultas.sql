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

-- CONSULTA 3 (com JOIN) — Samuel
-- Histórico geral de entradas na catraca e avaliações. O LEFT JOIN
-- mantém na lista os consumos que ainda não receberam feedback.
SELECT
    c.id_consumo,
    u.id_usuario,
    u.nome AS usuario,
    CASE
        WHEN a.id_usuario IS NOT NULL AND s.id_usuario IS NOT NULL THEN 'Aluno e servidor'
        WHEN a.id_usuario IS NOT NULL AND ad.id_usuario IS NOT NULL THEN 'Aluno e nutricionista'
        WHEN a.id_usuario IS NOT NULL THEN 'Aluno'
        WHEN s.id_usuario IS NOT NULL THEN 'Servidor'
        WHEN ad.id_usuario IS NOT NULL THEN 'Nutricionista'
        ELSE 'Usuário'
    END AS perfil,
    a.matricula,
    s.siape,
    ca.id_cardapio,
    ca.data AS data_cardapio,
    ca.turno,
    c.data_hora_entrada,
    f.id_feedback,
    f.nota,
    f.comentario,
    f.data_envio,
    CASE WHEN f.id_feedback IS NULL THEN 'Pendente' ELSE 'Avaliado' END AS situacao_feedback
FROM consumo AS c
JOIN usuario AS u ON u.id_usuario = c.id_usuario
JOIN cardapio AS ca ON ca.id_cardapio = c.id_cardapio
LEFT JOIN aluno AS a ON a.id_usuario = u.id_usuario
LEFT JOIN servidor AS s ON s.id_usuario = u.id_usuario
LEFT JOIN admin AS ad ON ad.id_usuario = u.id_usuario
LEFT JOIN feedback AS f ON f.id_consumo = c.id_consumo
ORDER BY c.data_hora_entrada DESC, c.id_consumo DESC;

-- CONSULTA 4 — Samuel
-- Participação individual nas avaliações: quantidade de consumos,
-- quantidade de feedbacks, percentual de consumos avaliados e média da nota.
-- Usuários sem consumo também aparecem, com taxa igual a zero.
SELECT
    u.id_usuario,
    u.nome AS usuario,
    CASE
        WHEN a.id_usuario IS NOT NULL AND s.id_usuario IS NOT NULL THEN 'Aluno e servidor'
        WHEN a.id_usuario IS NOT NULL AND ad.id_usuario IS NOT NULL THEN 'Aluno e nutricionista'
        WHEN a.id_usuario IS NOT NULL THEN 'Aluno'
        WHEN s.id_usuario IS NOT NULL THEN 'Servidor'
        WHEN ad.id_usuario IS NOT NULL THEN 'Nutricionista'
        ELSE 'Usuário'
    END AS perfil,
    COUNT(DISTINCT c.id_consumo) AS total_consumos,
    COUNT(DISTINCT f.id_feedback) AS total_avaliacoes,
    COALESCE(
        ROUND(
            100.0 * COUNT(DISTINCT f.id_feedback)
            / NULLIF(COUNT(DISTINCT c.id_consumo), 0),
            2
        ),
        0.00
    ) AS percentual_consumos_avaliados,
    ROUND(AVG(f.nota), 2) AS media_notas
FROM usuario AS u
LEFT JOIN aluno AS a ON a.id_usuario = u.id_usuario
LEFT JOIN servidor AS s ON s.id_usuario = u.id_usuario
LEFT JOIN admin AS ad ON ad.id_usuario = u.id_usuario
LEFT JOIN consumo AS c ON c.id_usuario = u.id_usuario
LEFT JOIN feedback AS f ON f.id_consumo = c.id_consumo
GROUP BY u.id_usuario, u.nome, a.id_usuario, s.id_usuario, ad.id_usuario
ORDER BY total_consumos DESC, u.nome;

-- CONSULTA 5 (com join) pedro
-- média de notas e quantidade de avaliações por cardápio, turno e restaurante

-- CONSULTA 6 (com join) pedro
-- análise de satisfação por faixa de horário de entrada na catraca
