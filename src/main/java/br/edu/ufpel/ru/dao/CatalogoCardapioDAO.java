package br.edu.ufpel.ru.dao;

import br.edu.ufpel.ru.config.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CatalogoCardapioDAO {

    public record ComboItem(int id, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    // OPERAÇÕES DE PRATO, INGREDIENTE E UTILIZA

    public void inserirPrato(String nome, int calorias) throws SQLException {
        String sql = "INSERT INTO prato (nome, calorias) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome.trim());
            ps.setInt(2, calorias);
            ps.executeUpdate();
        }
    }

    public int atualizarPrato(int idPrato, String nome, int calorias) throws SQLException {
        String sql = "UPDATE prato SET nome = ?, calorias = ? WHERE id_prato = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome.trim());
            ps.setInt(2, calorias);
            ps.setInt(3, idPrato);
            return ps.executeUpdate();
        }
    }

    public void inserirIngrediente(String nome) throws SQLException {
        String sql = "INSERT INTO ingrediente (nome) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome.trim());
            ps.executeUpdate();
        }
    }

    public void vincularIngredienteAoPrato(int idPrato, int idIngrediente) throws SQLException {
        String sql = "INSERT INTO utiliza (id_prato, id_ingrediente) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idPrato);
            ps.setInt(2, idIngrediente);
            ps.executeUpdate();
        }
    }

    /* CONSULTA 1 (com JOIN entre prato, utiliza e ingrediente)
    retorna os pratos cadastrados, calorias e lista de ingredientes utilizados.*/

    public List<Object[]> listarPratosComIngredientes() throws SQLException {
        String sql = """
            SELECT
                p.id_prato,
                p.nome AS nome_prato,
                p.calorias,
                COALESCE(STRING_AGG(i.nome, ', ' ORDER BY i.nome), 'Sem ingredientes vinculados') AS ingredientes
            FROM prato p
            LEFT JOIN utiliza u ON u.id_prato = p.id_prato
            LEFT JOIN ingrediente i ON i.id_ingrediente = u.id_ingrediente
            GROUP BY p.id_prato, p.nome, p.calorias
            ORDER BY p.id_prato
            """;
        List<Object[]> linhas = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                linhas.add(new Object[]{
                    rs.getInt("id_prato"),
                    rs.getString("nome_prato"),
                    rs.getInt("calorias"),
                    rs.getString("ingredientes")
                });
            }
        }
        return linhas;
    }

    // OPERAÇÕES DE CARDÁPIO, OFERECE, CONTEM, ADMIN E RESTAURANTE


    public void inserirCardapio(LocalDate data, String turno, int idAdmin) throws SQLException {
        String sql = "INSERT INTO cardapio (data, turno, id_admin) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(data));
            ps.setString(2, turno);
            ps.setInt(3, idAdmin);
            ps.executeUpdate();
        }
    }

    public int atualizarCardapio(int idCardapio, LocalDate data, String turno, int idAdmin) throws SQLException {
        String sql = "UPDATE cardapio SET data = ?, turno = ?, id_admin = ? WHERE id_cardapio = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(data));
            ps.setString(2, turno);
            ps.setInt(3, idAdmin);
            ps.setInt(4, idCardapio);
            return ps.executeUpdate();
        }
    }

    public void vincularPratoAoCardapio(int idCardapio, int idPrato) throws SQLException {
        String sql = "INSERT INTO contem (id_cardapio, id_prato) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCardapio);
            ps.setInt(2, idPrato);
            ps.executeUpdate();
        }
    }

    public void vincularRestauranteAoCardapio(int idCardapio, int idRest) throws SQLException {
        String sql = "INSERT INTO oferece (id_cardapio, id_rest) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCardapio);
            ps.setInt(2, idRest);
            ps.executeUpdate();
        }
    }

    public void inserirRestaurante(String nome) throws SQLException {
        String sql = "INSERT INTO restaurante (nome) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome.trim());
            ps.executeUpdate();
        }
    }

    // cadastra um novo nutricionista (inserindo na supertabela usuario e na subtabela admin em transação)
    
    public void inserirAdmin(String nome, String email, String crn) throws SQLException {
        String sqlUsuario = "INSERT INTO usuario (nome, email) VALUES (?, ?) RETURNING id_usuario";
        String sqlAdmin = "INSERT INTO admin (id_usuario, crn) VALUES (?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement psUser = conn.prepareStatement(sqlUsuario);
                 PreparedStatement psAdmin = conn.prepareStatement(sqlAdmin)) {
                psUser.setString(1, nome.trim());
                psUser.setString(2, email.trim());
                int idGerado;
                try (ResultSet rs = psUser.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("Falha ao obter ID do usuário criado.");
                    }
                    idGerado = rs.getInt(1);
                }

                psAdmin.setInt(1, idGerado);
                psAdmin.setString(2, crn.trim());
                psAdmin.executeUpdate();

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
     * CONSULTA 2 (com JOIN entre cardapio, admin, usuario, oferece, restaurante, contem e prato)
     * retorna os cardápios com nutricionista responsável, restaurantes, pratos e soma de calorias.*/

    public List<Object[]> listarCardapiosDetalhados() throws SQLException {
        String sql = """
            SELECT
                c.id_cardapio,
                c.data,
                c.turno,
                c.id_admin,
                u.nome || ' (CRN: ' || a.crn || ')' AS nutricionista,
                COALESCE((
                    SELECT STRING_AGG(r.nome, ', ' ORDER BY r.nome)
                    FROM oferece o
                    JOIN restaurante r ON r.id_rest = o.id_rest
                    WHERE o.id_cardapio = c.id_cardapio
                ), 'Nenhum restaurante') AS restaurantes,
                COALESCE((
                    SELECT STRING_AGG(p.nome, ', ' ORDER BY p.nome)
                    FROM contem ct
                    JOIN prato p ON p.id_prato = ct.id_prato
                    WHERE ct.id_cardapio = c.id_cardapio
                ), 'Nenhum prato') AS pratos,
                COALESCE((
                    SELECT SUM(p.calorias)
                    FROM contem ct
                    JOIN prato p ON p.id_prato = ct.id_prato
                    WHERE ct.id_cardapio = c.id_cardapio
                ), 0) AS calorias_totais
            FROM cardapio c
            JOIN admin a ON a.id_usuario = c.id_admin
            JOIN usuario u ON u.id_usuario = a.id_usuario
            ORDER BY c.data DESC, c.id_cardapio DESC
            """;
        List<Object[]> linhas = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                linhas.add(new Object[]{
                    rs.getInt("id_cardapio"),
                    rs.getDate("data").toString(),
                    rs.getString("turno"),
                    rs.getString("nutricionista"),
                    rs.getString("restaurantes"),
                    rs.getString("pratos"),
                    rs.getInt("calorias_totais")
                });
            }
        }
        return linhas;
    }

    // LISTAGENS AUXILIARES PARA JCOMBOBOX

    public List<ComboItem> listarPratosCombo() throws SQLException {
        String sql = "SELECT id_prato, nome, calorias FROM prato ORDER BY nome";
        List<ComboItem> itens = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                itens.add(new ComboItem(
                    rs.getInt("id_prato"),
                    String.format("#%d - %s (%d kcal)", rs.getInt("id_prato"), rs.getString("nome"), rs.getInt("calorias"))
                ));
            }
        }
        return itens;
    }

    public List<ComboItem> listarIngredientesCombo() throws SQLException {
        String sql = "SELECT id_ingrediente, nome FROM ingrediente ORDER BY nome";
        List<ComboItem> itens = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                itens.add(new ComboItem(
                    rs.getInt("id_ingrediente"),
                    String.format("#%d - %s", rs.getInt("id_ingrediente"), rs.getString("nome"))
                ));
            }
        }
        return itens;
    }

    public List<ComboItem> listarAdminsCombo() throws SQLException {
        String sql = """
            SELECT a.id_usuario, u.nome, a.crn
            FROM admin a
            JOIN usuario u ON u.id_usuario = a.id_usuario
            ORDER BY u.nome
            """;
        List<ComboItem> itens = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                itens.add(new ComboItem(
                    rs.getInt("id_usuario"),
                    String.format("#%d - %s (CRN: %s)", rs.getInt("id_usuario"), rs.getString("nome"), rs.getString("crn"))
                ));
            }
        }
        return itens;
    }

    public List<ComboItem> listarRestaurantesCombo() throws SQLException {
        String sql = "SELECT id_rest, nome FROM restaurante ORDER BY nome";
        List<ComboItem> itens = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                itens.add(new ComboItem(
                    rs.getInt("id_rest"),
                    String.format("#%d - %s", rs.getInt("id_rest"), rs.getString("nome"))
                ));
            }
        }
        return itens;
    }

    public List<ComboItem> listarCardapiosCombo() throws SQLException {
        String sql = "SELECT id_cardapio, data, turno FROM cardapio ORDER BY data DESC, id_cardapio DESC";
        List<ComboItem> itens = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                itens.add(new ComboItem(
                    rs.getInt("id_cardapio"),
                    String.format("#%d - %s (%s)", rs.getInt("id_cardapio"), rs.getDate("data"), rs.getString("turno"))
                ));
            }
        }
        return itens;
    }
}
