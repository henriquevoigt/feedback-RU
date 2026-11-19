package br.edu.ufpel.ru.ui;

import br.edu.ufpel.ru.config.DatabaseConnection;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

public class MainFrame extends JFrame {

    private final JLabel lblStatusConexao = new JLabel();
    private final PainelCardapio painelCardapio = new PainelCardapio();
    private final PainelConsumoFeedback painelConsumoFeedback = new PainelConsumoFeedback();
    private final PainelRelatorios painelRelatorios = new PainelRelatorios();

    public MainFrame() {
        super("Sistema de Avaliação dos Cardápios do RU - UFPel (Grupo 10)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1120, 740);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        add(criarBarraSuperior(), BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("1. Gestão de Pratos e Cardápios (Nutricionista)", painelCardapio);
        tabbedPane.addTab("2. Catraca e Avaliação (Aluno / Servidor)", painelConsumoFeedback);
        tabbedPane.addTab("3. Consultas e Relatórios Gerenciais", painelRelatorios);

        add(tabbedPane, BorderLayout.CENTER);

        verificarConexaoSilenciosa();
    }

    private JPanel criarBarraSuperior() {
        JPanel barra = new JPanel(new BorderLayout(10, 0));
        barra.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JLabel lblTitulo = new JLabel("RU UFPel — Gestão de Cardápios e Avaliações");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 15));
        barra.add(lblTitulo, BorderLayout.WEST);

        JPanel direita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        lblStatusConexao.setFont(new Font("SansSerif", Font.PLAIN, 12));

        JButton btnTestar = new JButton("Testar Conexão");
        btnTestar.addActionListener(e -> testarConexaoComFeedback());

        JButton btnConfig = new JButton("Configurar BD (Tailscale)");
        btnConfig.addActionListener(e -> abrirDialogConfiguracao());

        direita.add(lblStatusConexao);
        direita.add(btnTestar);
        direita.add(btnConfig);
        barra.add(direita, BorderLayout.EAST);

        return barra;
    }

    private void verificarConexaoSilenciosa() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            lblStatusConexao.setText("Conectado: " + DatabaseConnection.getJdbcUrl());
            lblStatusConexao.setForeground(new Color(0, 128, 0));
            painelCardapio.recarregarTudo();
        } catch (SQLException ex) {
            lblStatusConexao.setText("Desconectado (" + DatabaseConnection.getHost() + ":" + DatabaseConnection.getPort() + ")");
            lblStatusConexao.setForeground(new Color(180, 0, 0));
        }
    }

    private void testarConexaoComFeedback() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            lblStatusConexao.setText("Conectado: " + DatabaseConnection.getJdbcUrl());
            lblStatusConexao.setForeground(new Color(0, 128, 0));
            painelCardapio.recarregarTudo();
            JOptionPane.showMessageDialog(
                this,
                "Conexão estabelecida com sucesso em:\n" + DatabaseConnection.getJdbcUrl(),
                "PostgreSQL Conectado",
                JOptionPane.INFORMATION_MESSAGE
            );
        } catch (SQLException ex) {
            lblStatusConexao.setText("Falha na conexão (" + DatabaseConnection.getHost() + ")");
            lblStatusConexao.setForeground(new Color(180, 0, 0));
            JOptionPane.showMessageDialog(
                this,
                "Não foi possível conectar ao PostgreSQL:\n" + ex.getMessage()
                + "\n\nClique em 'Configurar BD (Tailscale)' para ajustar o IP, banco, usuário ou senha.",
                "Erro de Conexão",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void abrirDialogConfiguracao() {
        JTextField txtHost = new JTextField(DatabaseConnection.getHost(), 18);
        JTextField txtPort = new JTextField(DatabaseConnection.getPort(), 6);
        JTextField txtDb = new JTextField(DatabaseConnection.getDbName(), 15);
        JTextField txtUser = new JTextField(DatabaseConnection.getUser(), 15);
        JPasswordField txtPass = new JPasswordField(DatabaseConnection.getPassword(), 15);

        JPanel panel = new JPanel(new GridLayout(5, 2, 6, 6));
        panel.add(new JLabel("Host / IP Tailscale:"));
        panel.add(txtHost);
        panel.add(new JLabel("Porta:"));
        panel.add(txtPort);
        panel.add(new JLabel("Nome do Banco (Database):"));
        panel.add(txtDb);
        panel.add(new JLabel("Usuário:"));
        panel.add(txtUser);
        panel.add(new JLabel("Senha:"));
        panel.add(txtPass);

        int opt = JOptionPane.showConfirmDialog(
            this,
            panel,
            "Configurar Conexão PostgreSQL (db.properties)",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );

        if (opt == JOptionPane.OK_OPTION) {
            try {
                DatabaseConnection.saveConfig(
                    txtHost.getText(),
                    txtPort.getText(),
                    txtDb.getText(),
                    txtUser.getText(),
                    new String(txtPass.getPassword())
                );
                testarConexaoComFeedback();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(
                    this,
                    "Erro ao salvar db.properties: " + ex.getMessage(),
                    "Erro",
                    JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
}
