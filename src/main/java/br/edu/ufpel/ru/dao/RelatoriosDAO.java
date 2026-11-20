package br.edu.ufpel.ru.dao;

import br.edu.ufpel.ru.config.DatabaseConnection;
import br.edu.ufpel.ru.dao.CatalogoCardapioDAO.ComboItem;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/* aba 3, pedro
consultas gerenciais do script sql/04_consultas.sql, com filtros opcionais.

o SQL de cada método é o mesmo do script (mesma lógica, colunas e ordenação);
só foi acrescentado um WHERE com os filtros, antes do GROUP BY, para não distorcer
médias e contagens. filtro vazio = NULL, e (? IS NULL OR ...) deixa a linha passar,
então sem filtros o resultado é idêntico ao do script.
todo valor digitado entra por PreparedStatement ("?"), nunca concatenado no SQL. */

public class RelatoriosDAO {

    /* resultado genérico, montado com ResultSetMetaData para que a mesma JTable
    sirva para qualquer consulta: nomes das colunas, classe Java de cada coluna
    (para ordenar números como números) e as linhas. */
    public record ResultadoConsulta(List<String> colunas, List<Class<?>> tipos, List<Object[]> linhas) {
        public ResultadoConsulta {
            colunas = Collections.unmodifiableList(colunas);
            tipos = Collections.unmodifiableList(tipos);
            linhas = Collections.unmodifiableList(linhas);
        }
    }

    @FunctionalInterface
    private interface PreencherParametros {
        void aplicar(PreparedStatement ps) throws SQLException;
    }

    /* CONSULTA 1 (prato, utiliza, ingrediente)
    catálogo nutricional de pratos com a lista de ingredientes.
    filtro: nome do prato (busca parcial, sem diferenciar maiúsculas). */

    public ResultadoConsulta catalogoPratos(String nomePrato) throws SQLException {
        String sql = """
            SELECT
                p.id_prato,
                p.nome AS nome_prato,
                p.calorias,
                COALESCE(STRING_AGG(i.nome, ', ' ORDER BY i.nome), 'Sem ingredientes vinculados') AS ingredientes
            FROM prato p
            LEFT JOIN utiliza u ON u.id_prato = p.id_prato
            LEFT JOIN ingrediente i ON i.id_ingrediente = u.id_ingrediente
            WHERE (?::text IS NULL OR p.nome ILIKE ('%' || ?::text || '%'))
            GROUP BY p.id_prato, p.nome, p.calorias
            ORDER BY p.nome
            """;
        return executar(sql, ps -> bindTexto(ps, 1, padraoBusca(nomePrato)));
    }

    /* CONSULTA 2 (cardapio, admin, usuario, oferece, restaurante, contem, prato)
    cardápios planejados com nutricionista, unidades, pratos e calorias.
    filtros: período (data do cardápio), turno e restaurante.
    o restaurante é filtrado com EXISTS em oferece: assim o cardápio continua
    aparecendo uma vez só, com todas as unidades na coluna restaurantes. */

    public ResultadoConsulta cardapiosDetalhados(LocalDate dataInicial, LocalDate dataFinal,
                                                 String turno, Integer idRestaurante) throws SQLException {
        String sql = """
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
            WHERE (?::date IS NULL OR c.data >= ?::date)
              AND (?::date IS NULL OR c.data <= ?::date)
              AND (?::text IS NULL OR c.turno = ?::text)
              AND (?::int IS NULL OR EXISTS (
                    SELECT 1
                    FROM oferece fo
                    WHERE fo.id_cardapio = c.id_cardapio
                      AND fo.id_rest = ?::int
                  ))
            ORDER BY c.data DESC, c.turno
            """;
        return executar(sql, ps -> {
            int i = 1;
            i = bindData(ps, i, dataInicial);
            i = bindData(ps, i, dataFinal);
            i = bindTexto(ps, i, vazioParaNull(turno));
            bindInteiro(ps, i, idRestaurante);
        });
    }

    /* CONSULTA 3 (consumo, usuario, cardapio, aluno, servidor, admin, feedback)
    histórico de entradas na catraca e avaliações; o LEFT JOIN em feedback mantém
    os consumos ainda sem avaliação.
    filtros: período (data da entrada na catraca, data final inclusiva), turno do
    cardápio e nome do usuário (busca parcial). */

    public ResultadoConsulta historicoConsumoFeedback(LocalDate dataInicial, LocalDate dataFinal,
                                                      String turno, String nomeUsuario) throws SQLException {
        String sql = """
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
            WHERE (?::date IS NULL OR c.data_hora_entrada >= ?::date)
              AND (?::date IS NULL OR c.data_hora_entrada < ?::date + 1)
              AND (?::text IS NULL OR ca.turno = ?::text)
              AND (?::text IS NULL OR u.nome ILIKE ('%' || ?::text || '%'))
            ORDER BY c.data_hora_entrada DESC, c.id_consumo DESC
            """;
        return executar(sql, ps -> {
            int i = 1;
            i = bindData(ps, i, dataInicial);
            i = bindData(ps, i, dataFinal);
            i = bindTexto(ps, i, vazioParaNull(turno));
            bindTexto(ps, i, padraoBusca(nomeUsuario));
        });
    }

    /* CONSULTA 4 (usuario, aluno, servidor, admin, consumo, feedback)
    participação individual: consumos, avaliações, percentual avaliado e média.
    filtro: nome do usuário (busca parcial). o filtro só escolhe quais usuários
    entram, os números de cada um continuam completos. */

    public ResultadoConsulta participacaoPorUsuario(String nomeUsuario) throws SQLException {
        String sql = """
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
            WHERE (?::text IS NULL OR u.nome ILIKE ('%' || ?::text || '%'))
            GROUP BY u.id_usuario, u.nome, a.id_usuario, s.id_usuario, ad.id_usuario
            ORDER BY total_consumos DESC, u.nome
            """;
        return executar(sql, ps -> bindTexto(ps, 1, padraoBusca(nomeUsuario)));
    }

    /* CONSULTA 5 (consumo, cardapio, feedback; oferece e restaurante só na subconsulta)
    satisfação por cardápio e turno: consumos, avaliações, taxa de resposta e notas.
    filtros: período (data do cardápio) e turno.
    não há filtro de restaurante de propósito: consumo não registra em qual unidade
    a pessoa entrou, então filtrar por oferece atribuiria à unidade os consumos de
    todas as unidades que servem o mesmo cardápio. */

    public ResultadoConsulta satisfacaoPorCardapio(LocalDate dataInicial, LocalDate dataFinal,
                                                   String turno) throws SQLException {
        String sql = """
            SELECT
                c.id_cardapio,
                c.data,
                c.turno,
                COALESCE((
                    SELECT STRING_AGG(r.nome, ', ' ORDER BY r.nome)
                    FROM oferece o
                    JOIN restaurante r ON r.id_rest = o.id_rest
                    WHERE o.id_cardapio = c.id_cardapio
                ), 'Nenhum restaurante vinculado') AS restaurantes,
                COUNT(co.id_consumo) AS total_consumos,
                COUNT(f.id_feedback) AS total_avaliacoes,
                ROUND(100.0 * COUNT(f.id_feedback) / NULLIF(COUNT(co.id_consumo), 0), 1) AS taxa_resposta_pct,
                ROUND(AVG(f.nota), 2) AS media_nota,
                MIN(f.nota) AS menor_nota,
                MAX(f.nota) AS maior_nota
            FROM consumo co
            JOIN cardapio c ON c.id_cardapio = co.id_cardapio
            LEFT JOIN feedback f ON f.id_consumo = co.id_consumo
            WHERE (?::date IS NULL OR c.data >= ?::date)
              AND (?::date IS NULL OR c.data <= ?::date)
              AND (?::text IS NULL OR c.turno = ?::text)
            GROUP BY c.id_cardapio, c.data, c.turno
            ORDER BY c.data DESC, c.turno
            """;
        return executar(sql, ps -> {
            int i = 1;
            i = bindData(ps, i, dataInicial);
            i = bindData(ps, i, dataFinal);
            bindTexto(ps, i, vazioParaNull(turno));
        });
    }

    /* CONSULTA 6 (consumo, cardapio, feedback)
    entradas na catraca em faixas de 30 minutos, por turno, com avaliações e notas baixas.
    filtros: período (data da entrada na catraca, data final inclusiva) e turno. */

    public ResultadoConsulta gargalosPorFaixaHorario(LocalDate dataInicial, LocalDate dataFinal,
                                                     String turno) throws SQLException {
        String sql = """
            SELECT
                c.turno,
                TO_CHAR(DATE_TRUNC('hour', co.data_hora_entrada) + FLOOR(EXTRACT(MINUTE FROM co.data_hora_entrada) / 30) * INTERVAL '30 minutes', 'HH24:MI') AS inicio_faixa,
                COUNT(co.id_consumo) AS total_entradas,
                COUNT(f.id_feedback) AS total_avaliacoes,
                ROUND(AVG(f.nota), 2) AS media_nota,
                COUNT(*) FILTER (WHERE f.nota <= 2) AS avaliacoes_negativas
            FROM consumo co
            JOIN cardapio c ON c.id_cardapio = co.id_cardapio
            LEFT JOIN feedback f ON f.id_consumo = co.id_consumo
            WHERE (?::date IS NULL OR co.data_hora_entrada >= ?::date)
              AND (?::date IS NULL OR co.data_hora_entrada < ?::date + 1)
              AND (?::text IS NULL OR c.turno = ?::text)
            GROUP BY c.turno, inicio_faixa
            ORDER BY c.turno, inicio_faixa
            """;
        return executar(sql, ps -> {
            int i = 1;
            i = bindData(ps, i, dataInicial);
            i = bindData(ps, i, dataFinal);
            bindTexto(ps, i, vazioParaNull(turno));
        });
    }

    // LISTAGEM AUXILIAR PARA O JCOMBOBOX DE RESTAURANTES

    public List<ComboItem> listarRestaurantes() throws SQLException {
        String sql = "SELECT id_rest, nome FROM restaurante ORDER BY nome";
        List<ComboItem> itens = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                itens.add(new ComboItem(rs.getInt("id_rest"), rs.getString("nome")));
            }
        }
        return itens;
    }

    // EXECUÇÃO GENÉRICA E MONTAGEM DO RESULTADO

    private ResultadoConsulta executar(String sql, PreencherParametros parametros) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            parametros.aplicar(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return montarResultado(rs);
            }
        }
    }

    private ResultadoConsulta montarResultado(ResultSet rs) throws SQLException {
        ResultSetMetaData meta = rs.getMetaData();
        int total = meta.getColumnCount();

        List<String> colunas = new ArrayList<>(total);
        List<Class<?>> tipos = new ArrayList<>(total);
        for (int c = 1; c <= total; c++) {
            colunas.add(meta.getColumnLabel(c));
            tipos.add(classeDaColuna(meta.getColumnClassName(c)));
        }

        List<Object[]> linhas = new ArrayList<>();
        while (rs.next()) {
            Object[] linha = new Object[total];
            for (int c = 1; c <= total; c++) {
                linha[c - 1] = rs.getObject(c);
            }
            linhas.add(linha);
        }
        return new ResultadoConsulta(colunas, tipos, linhas);
    }

    private static Class<?> classeDaColuna(String nomeClasse) {
        try {
            return Class.forName(nomeClasse);
        } catch (ClassNotFoundException | RuntimeException ex) {
            return Object.class;
        }
    }

    /* cada filtro opcional aparece duas vezes no SQL: (?::tipo IS NULL OR coluna op ?::tipo).
    os métodos abaixo preenchem as duas posições com o mesmo valor e devolvem o
    índice do próximo "?". */

    private static int bindData(PreparedStatement ps, int indice, LocalDate valor) throws SQLException {
        for (int i = indice; i < indice + 2; i++) {
            if (valor == null) {
                ps.setNull(i, Types.DATE);
            } else {
                ps.setDate(i, Date.valueOf(valor));
            }
        }
        return indice + 2;
    }

    private static int bindTexto(PreparedStatement ps, int indice, String valor) throws SQLException {
        for (int i = indice; i < indice + 2; i++) {
            if (valor == null) {
                ps.setNull(i, Types.VARCHAR);
            } else {
                ps.setString(i, valor);
            }
        }
        return indice + 2;
    }

    private static int bindInteiro(PreparedStatement ps, int indice, Integer valor) throws SQLException {
        for (int i = indice; i < indice + 2; i++) {
            if (valor == null) {
                ps.setNull(i, Types.INTEGER);
            } else {
                ps.setInt(i, valor);
            }
        }
        return indice + 2;
    }

    private static String vazioParaNull(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    /* texto da busca parcial com ILIKE. os caracteres especiais do LIKE (% e _) e a
    barra de escape são escapados, para que "50%" procure literalmente "50%". */

    private static String padraoBusca(String texto) {
        String valor = vazioParaNull(texto);
        if (valor == null) {
            return null;
        }
        return valor.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
