package br.edu.ufpel.ru.dao;

import br.edu.ufpel.ru.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/** Acesso ao banco para a aba de catraca e avaliações. */
public class ConsumoFeedbackDAO {

    public record ComboItem(int id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    /** Lista usuários com sua especialização para o seletor de simulação da catraca. */
    public List<ComboItem> listarUsuariosCombo() throws SQLException {
        String sql = """
            SELECT
                u.id_usuario,
                u.nome,
                CASE
                    WHEN a.id_usuario IS NOT NULL THEN 'Aluno - matrícula ' || a.matricula
                    WHEN s.id_usuario IS NOT NULL THEN 'Servidor - SIAPE ' || s.siape
                    WHEN ad.id_usuario IS NOT NULL THEN 'Nutricionista - CRN ' || ad.crn
                    ELSE 'Usuário'
                END AS perfil
            FROM usuario AS u
            LEFT JOIN aluno AS a ON a.id_usuario = u.id_usuario
            LEFT JOIN servidor AS s ON s.id_usuario = u.id_usuario
            LEFT JOIN admin AS ad ON ad.id_usuario = u.id_usuario
            ORDER BY u.nome
            """;

        List<ComboItem> itens = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                itens.add(new ComboItem(
                    rs.getInt("id_usuario"),
                    String.format("%s — %s (#%d)", rs.getString("nome"), rs.getString("perfil"), rs.getInt("id_usuario"))
                ));
            }
        }
        return itens;
    }

    /** Lista cardápios para vincular ao registro de entrada. */
    public List<ComboItem> listarCardapiosCombo() throws SQLException {
        String sql = """
            SELECT id_cardapio, data, turno
            FROM cardapio
            ORDER BY data DESC, turno, id_cardapio DESC
            """;

        List<ComboItem> itens = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                itens.add(new ComboItem(
                    rs.getInt("id_cardapio"),
                    String.format("#%d — %s — %s", rs.getInt("id_cardapio"), rs.getDate("data"), rs.getString("turno"))
                ));
            }
        }
        return itens;
    }

    /** Registra a passagem pela catraca; o horário é preenchido pelo DEFAULT do banco. */
    public int inserirConsumo(int idUsuario, int idCardapio) throws SQLException {
        String sql = """
            INSERT INTO consumo (id_usuario, id_cardapio)
            VALUES (?, ?)
            RETURNING id_consumo
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUsuario);
            ps.setInt(2, idCardapio);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("O PostgreSQL não retornou o ID do consumo criado.");
                }
                return rs.getInt("id_consumo");
            }
        }
    }

    /**
     * Retorna cada entrada da catraca e, quando existe, seu feedback.
     * O LEFT JOIN mantém os consumos pendentes visíveis.
     */
    public List<Object[]> listarConsumosComFeedback() throws SQLException {
        String sql = """
            SELECT
                c.id_consumo,
                u.nome AS usuario,
                ca.data AS data_cardapio,
                ca.turno,
                c.data_hora_entrada,
                f.nota,
                f.comentario,
                CASE WHEN f.id_feedback IS NULL THEN 'Pendente' ELSE 'Avaliado' END AS situacao
            FROM consumo AS c
            JOIN usuario AS u ON u.id_usuario = c.id_usuario
            JOIN cardapio AS ca ON ca.id_cardapio = c.id_cardapio
            LEFT JOIN feedback AS f ON f.id_consumo = c.id_consumo
            ORDER BY c.data_hora_entrada DESC, c.id_consumo DESC
            """;

        List<Object[]> linhas = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                linhas.add(new Object[]{
                    rs.getInt("id_consumo"),
                    rs.getString("usuario"),
                    rs.getDate("data_cardapio").toString(),
                    rs.getString("turno"),
                    rs.getTimestamp("data_hora_entrada").toString(),
                    rs.getObject("nota"),
                    rs.getString("comentario"),
                    rs.getString("situacao")
                });
            }
        }
        return linhas;
    }

    /** Insere um feedback novo ou atualiza nota/comentário do consumo escolhido. */
    public void salvarOuAtualizarFeedback(int idConsumo, int nota, String comentario) throws SQLException {
        String sqlAtualizar = "UPDATE feedback SET nota = ?, comentario = ? WHERE id_consumo = ?";
        String sqlInserir = "INSERT INTO feedback (nota, comentario, id_consumo) VALUES (?, ?, ?)";
        String textoComentario = comentario == null ? "" : comentario.trim();

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int atualizados;
                try (PreparedStatement ps = conn.prepareStatement(sqlAtualizar)) {
                    ps.setInt(1, nota);
                    definirComentario(ps, 2, textoComentario);
                    ps.setInt(3, idConsumo);
                    atualizados = ps.executeUpdate();
                }

                if (atualizados == 0) {
                    try (PreparedStatement ps = conn.prepareStatement(sqlInserir)) {
                        ps.setInt(1, nota);
                        definirComentario(ps, 2, textoComentario);
                        ps.setInt(3, idConsumo);
                        ps.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    /**
     * Demonstra o gatilho sem persistir um feedback de teste: tenta inserir uma
     * data anterior à catraca e encerra a transação com ROLLBACK.
     */
    public String demonstrarValidacaoDoGatilho(int idConsumo) throws SQLException {
        String sql = """
            INSERT INTO feedback (nota, comentario, data_envio, id_consumo)
            SELECT
                1,
                'Registro temporário de demonstração do gatilho',
                c.data_hora_entrada - INTERVAL '1 second',
                c.id_consumo
            FROM consumo AS c
            WHERE c.id_consumo = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idConsumo);
                int inseridos = ps.executeUpdate();
                conn.rollback();
                if (inseridos == 0) {
                    return "Consumo não encontrado; atualize a lista e tente novamente.";
                }
                return "O banco aceitou uma avaliação com data inválida. Confira se 02_triggers.sql foi executado.";
            } catch (SQLException ex) {
                conn.rollback();
                if ("23514".equals(ex.getSQLState())) {
                    return "Gatilho funcionando: o PostgreSQL recusou a avaliação anterior à entrada. Nenhum dado de teste foi salvo.";
                }
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void definirComentario(PreparedStatement ps, int indice, String comentario) throws SQLException {
        if (comentario.isBlank()) {
            ps.setNull(indice, Types.VARCHAR);
        } else {
            ps.setString(indice, comentario);
        }
    }
}
