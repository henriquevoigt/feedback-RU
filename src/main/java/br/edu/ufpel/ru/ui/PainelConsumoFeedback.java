package br.edu.ufpel.ru.ui;

import br.edu.ufpel.ru.dao.ConsumoFeedbackDAO;
import br.edu.ufpel.ru.dao.ConsumoFeedbackDAO.ComboItem;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.SQLException;

/** Aba de simulação da catraca e de envio/edição de avaliações. */
public class PainelConsumoFeedback extends JPanel {

    private final ConsumoFeedbackDAO dao = new ConsumoFeedbackDAO();

    private final JComboBox<ComboItem> cbUsuario = new JComboBox<>();
    private final JComboBox<ComboItem> cbCardapio = new JComboBox<>();
    private final JComboBox<Integer> cbNota = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
    private final JTextField txtIdConsumo = new JTextField(9);
    private final JTextArea txtComentario = new JTextArea(3, 24);
    private final JLabel lblStatus = new JLabel("Conecte ao PostgreSQL e carregue os dados.");
    private final JButton btnSalvarFeedback = new JButton("Enviar avaliação");
    private final JButton btnTestarGatilho = new JButton("Testar gatilho");

    private final DefaultTableModel modelConsumos = new DefaultTableModel(
        new Object[]{"ID consumo", "Usuário", "Data do cardápio", "Turno", "Entrada na catraca", "Nota", "Comentário", "Situação"},
        0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable tabelaConsumos = new JTable(modelConsumos);

    public PainelConsumoFeedback() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        txtIdConsumo.setEditable(false);
        txtComentario.setLineWrap(true);
        txtComentario.setWrapStyleWord(true);
        btnSalvarFeedback.setEnabled(false);
        btnTestarGatilho.setEnabled(false);

        add(criarAreaDeTrabalho(), BorderLayout.NORTH);
        add(criarTabela(), BorderLayout.CENTER);
        add(criarRodape(), BorderLayout.SOUTH);

        tabelaConsumos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaConsumos.setAutoCreateRowSorter(true);
        tabelaConsumos.setRowHeight(24);
        tabelaConsumos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                preencherFormularioPeloConsumoSelecionado();
            }
        });

        btnSalvarFeedback.addActionListener(e -> salvarFeedback());
        btnTestarGatilho.addActionListener(e -> testarGatilho());

        carregarDados(false);
    }

    private JPanel criarAreaDeTrabalho() {
        JPanel area = new JPanel(new GridLayout(1, 2, 10, 0));
        area.add(criarFormularioConsumo());
        area.add(criarFormularioFeedback());
        return area;
    }

    private JPanel criarFormularioConsumo() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Simular passagem pela catraca — INSERT em consumo"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        painel.add(new JLabel("Aluno/servidor:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        painel.add(cbUsuario, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        painel.add(new JLabel("Cardápio:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        painel.add(cbCardapio, gbc);

        JButton btnRegistrar = new JButton("Registrar entrada");
        btnRegistrar.addActionListener(e -> registrarConsumo());
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.EAST;
        painel.add(btnRegistrar, gbc);
        return painel;
    }

    private JPanel criarFormularioFeedback() {
        JPanel painel = new JPanel(new GridBagLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Avaliação do consumo selecionado — INSERT / UPDATE em feedback"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 5, 4, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        painel.add(new JLabel("ID consumo (selecione na tabela):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        painel.add(txtIdConsumo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
        painel.add(new JLabel("Nota (1 a 5):"), gbc);
        gbc.gridx = 1;
        painel.add(cbNota, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.NORTHWEST;
        painel.add(new JLabel("Comentário:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.weighty = 1;
        painel.add(new JScrollPane(txtComentario), gbc);

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton btnLimpar = new JButton("Limpar seleção");
        btnLimpar.addActionListener(e -> limparSelecao());
        botoes.add(btnTestarGatilho);
        botoes.add(btnLimpar);
        botoes.add(btnSalvarFeedback);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weighty = 0;
        gbc.anchor = GridBagConstraints.EAST;
        painel.add(botoes, gbc);
        return painel;
    }

    private JPanel criarTabela() {
        JPanel painel = new JPanel(new BorderLayout(4, 4));
        painel.setBorder(BorderFactory.createTitledBorder("Histórico de entradas e avaliações (consumo LEFT JOIN feedback)"));
        tabelaConsumos.getColumnModel().getColumn(0).setPreferredWidth(75);
        tabelaConsumos.getColumnModel().getColumn(1).setPreferredWidth(145);
        tabelaConsumos.getColumnModel().getColumn(2).setPreferredWidth(120);
        tabelaConsumos.getColumnModel().getColumn(3).setPreferredWidth(70);
        tabelaConsumos.getColumnModel().getColumn(4).setPreferredWidth(155);
        tabelaConsumos.getColumnModel().getColumn(5).setPreferredWidth(55);
        tabelaConsumos.getColumnModel().getColumn(6).setPreferredWidth(250);
        tabelaConsumos.getColumnModel().getColumn(7).setPreferredWidth(85);
        painel.add(new JScrollPane(tabelaConsumos), BorderLayout.CENTER);
        return painel;
    }

    private JPanel criarRodape() {
        JPanel rodape = new JPanel(new BorderLayout(8, 0));
        JButton btnAtualizar = new JButton("Atualizar dados");
        btnAtualizar.addActionListener(e -> carregarDados(true));
        lblStatus.setFont(new Font("SansSerif", Font.PLAIN, 12));
        rodape.add(lblStatus, BorderLayout.CENTER);
        rodape.add(btnAtualizar, BorderLayout.EAST);
        return rodape;
    }

    private void registrarConsumo() {
        ComboItem usuario = (ComboItem) cbUsuario.getSelectedItem();
        ComboItem cardapio = (ComboItem) cbCardapio.getSelectedItem();
        if (usuario == null || cardapio == null) {
            mostrarAviso("Cadastre usuários e cardápios antes de registrar uma entrada.");
            return;
        }

        try {
            int idConsumo = dao.inserirConsumo(usuario.id(), cardapio.id());
            carregarDados(false);
            selecionarConsumo(idConsumo);
            lblStatus.setText("Entrada registrada no consumo #" + idConsumo + ". O horário veio do DEFAULT do PostgreSQL.");
            lblStatus.setForeground(new Color(0, 110, 0));
        } catch (SQLException ex) {
            mostrarErro("Não foi possível registrar a entrada", ex);
        }
    }

    private void salvarFeedback() {
        String idTexto = txtIdConsumo.getText().trim();
        Integer nota = (Integer) cbNota.getSelectedItem();
        if (idTexto.isEmpty() || nota == null) {
            mostrarAviso("Selecione um consumo na tabela e escolha uma nota.");
            return;
        }

        try {
            int idConsumo = Integer.parseInt(idTexto);
            dao.salvarOuAtualizarFeedback(idConsumo, nota, txtComentario.getText());
            carregarDados(false);
            selecionarConsumo(idConsumo);
            lblStatus.setText("Avaliação do consumo #" + idConsumo + " salva. A data de envio é registrada pelo banco.");
            lblStatus.setForeground(new Color(0, 110, 0));
        } catch (NumberFormatException ex) {
            mostrarAviso("O ID de consumo selecionado não é válido.");
        } catch (SQLException ex) {
            mostrarErro("Não foi possível salvar a avaliação", ex);
        }
    }

    private void testarGatilho() {
        String idTexto = txtIdConsumo.getText().trim();
        if (idTexto.isEmpty()) {
            mostrarAviso("Selecione um consumo na tabela antes de testar o gatilho.");
            return;
        }
        try {
            String resultado = dao.demonstrarValidacaoDoGatilho(Integer.parseInt(idTexto));
            JOptionPane.showMessageDialog(this, resultado, "Demonstração do gatilho", JOptionPane.INFORMATION_MESSAGE);
            lblStatus.setText(resultado);
            lblStatus.setForeground(resultado.startsWith("Gatilho funcionando") ? new Color(0, 110, 0) : new Color(170, 90, 0));
        } catch (NumberFormatException ex) {
            mostrarAviso("O ID de consumo selecionado não é válido.");
        } catch (SQLException ex) {
            mostrarErro("Não foi possível demonstrar o gatilho", ex);
        }
    }

    /** Método público para que outras telas possam pedir a atualização da aba. */
    public void recarregarTudo() {
        carregarDados(false);
    }

    private void carregarDados(boolean mostrarDetalhesDoErro) {
        try {
            atualizarCombo(cbUsuario, dao.listarUsuariosCombo());
            atualizarCombo(cbCardapio, dao.listarCardapiosCombo());

            modelConsumos.setRowCount(0);
            for (Object[] linha : dao.listarConsumosComFeedback()) {
                modelConsumos.addRow(linha);
            }

            lblStatus.setText("Dados carregados: " + modelConsumos.getRowCount() + " entradas na catraca.");
            lblStatus.setForeground(new Color(0, 110, 0));
        } catch (SQLException ex) {
            lblStatus.setText("Sem conexão ou tabelas ainda não criadas. Use 'Atualizar dados' após configurar o PostgreSQL.");
            lblStatus.setForeground(new Color(170, 50, 30));
            if (mostrarDetalhesDoErro) {
                mostrarErro("Não foi possível carregar os usuários, cardápios e consumos", ex);
            }
        }
    }

    private void preencherFormularioPeloConsumoSelecionado() {
        int linhaSelecionada = tabelaConsumos.getSelectedRow();
        if (linhaSelecionada < 0) {
            txtIdConsumo.setText("");
            btnSalvarFeedback.setEnabled(false);
            btnTestarGatilho.setEnabled(false);
            return;
        }

        int linhaModelo = tabelaConsumos.convertRowIndexToModel(linhaSelecionada);
        txtIdConsumo.setText(String.valueOf(modelConsumos.getValueAt(linhaModelo, 0)));
        Object nota = modelConsumos.getValueAt(linhaModelo, 5);
        Object comentario = modelConsumos.getValueAt(linhaModelo, 6);

        if (nota instanceof Number numero) {
            cbNota.setSelectedItem(numero.intValue());
            btnSalvarFeedback.setText("Atualizar avaliação");
        } else {
            cbNota.setSelectedItem(5);
            btnSalvarFeedback.setText("Enviar avaliação");
        }
        txtComentario.setText(comentario == null ? "" : comentario.toString());
        btnSalvarFeedback.setEnabled(true);
        btnTestarGatilho.setEnabled(true);
    }

    private void selecionarConsumo(int idConsumo) {
        for (int linha = 0; linha < modelConsumos.getRowCount(); linha++) {
            if (Integer.parseInt(modelConsumos.getValueAt(linha, 0).toString()) == idConsumo) {
                int linhaVisivel = tabelaConsumos.convertRowIndexToView(linha);
                tabelaConsumos.setRowSelectionInterval(linhaVisivel, linhaVisivel);
                tabelaConsumos.scrollRectToVisible(tabelaConsumos.getCellRect(linhaVisivel, 0, true));
                return;
            }
        }
    }

    private void limparSelecao() {
        tabelaConsumos.clearSelection();
        txtIdConsumo.setText("");
        cbNota.setSelectedItem(5);
        txtComentario.setText("");
        btnSalvarFeedback.setText("Enviar avaliação");
        btnSalvarFeedback.setEnabled(false);
        btnTestarGatilho.setEnabled(false);
    }

    private void atualizarCombo(JComboBox<ComboItem> combo, java.util.List<ComboItem> itens) {
        combo.removeAllItems();
        for (ComboItem item : itens) {
            combo.addItem(item);
        }
    }

    private void mostrarAviso(String mensagem) {
        JOptionPane.showMessageDialog(this, mensagem, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    private void mostrarErro(String titulo, SQLException ex) {
        JOptionPane.showMessageDialog(
            this,
            titulo + ":\n" + ex.getMessage(),
            "Erro no PostgreSQL",
            JOptionPane.ERROR_MESSAGE
        );
    }
}
