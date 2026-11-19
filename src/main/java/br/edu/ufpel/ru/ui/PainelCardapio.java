package br.edu.ufpel.ru.ui;

import br.edu.ufpel.ru.dao.CatalogoCardapioDAO;
import br.edu.ufpel.ru.dao.CatalogoCardapioDAO.ComboItem;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/* aba 1, henrique
gestão de pratos, ingredientes e planejamento de cardápios 
realiza operações de INSERT, UPDATE e SELECT com JOIN no PostgreSQL */

public class PainelCardapio extends JPanel {

    private final CatalogoCardapioDAO dao = new CatalogoCardapioDAO();

    // Componentes da Sub-aba 1: Pratos e Ingredientes
    private final JTextField txtIdPrato = new JTextField(6);
    private final JTextField txtNomePrato = new JTextField(18);
    private final JTextField txtCaloriasPrato = new JTextField(8);
    private final JTextField txtNovoIngrediente = new JTextField(15);
    private final JComboBox<ComboItem> cbPratoUtiliza = new JComboBox<>();
    private final JComboBox<ComboItem> cbIngredienteUtiliza = new JComboBox<>();
    private final DefaultTableModel modelPratos = new DefaultTableModel(
        new Object[]{"ID", "Nome do Prato", "Calorias (kcal)", "Ingredientes (JOIN utiliza/ingrediente)"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabelaPratos = new JTable(modelPratos);

    // cardápios, contém e oferece
    private final JTextField txtIdCardapio = new JTextField(6);
    private final JTextField txtDataCardapio = new JTextField(LocalDate.now().toString(), 10);
    private final JComboBox<String> cbTurnoCardapio = new JComboBox<>(new String[]{"Almoço", "Jantar"});
    private final JComboBox<ComboItem> cbAdminCardapio = new JComboBox<>();

    private final JComboBox<ComboItem> cbCardapioVinculo = new JComboBox<>();
    private final JComboBox<ComboItem> cbPratoContem = new JComboBox<>();
    private final JComboBox<ComboItem> cbRestauranteOferece = new JComboBox<>();

    private final DefaultTableModel modelCardapios = new DefaultTableModel(
        new Object[]{"ID", "Data", "Turno", "Nutricionista (CRN)", "Restaurantes (Oferece)", "Pratos (Contém)", "Total kcal"}, 0
    ) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable tabelaCardapios = new JTable(modelCardapios);

    public PainelCardapio() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTabbedPane subAbas = new JTabbedPane();
        subAbas.addTab("1. Catálogo de Pratos e Ingredientes", criarPainelPratosIngredientes());
        subAbas.addTab("2. Planejamento de Cardápios (Nutricionista)", criarPainelPlanejamentoCardapios());

        add(subAbas, BorderLayout.CENTER);
    }

    // PRATOS E INGREDIENTES

    private JPanel criarPainelPratosIngredientes() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel topo = new JPanel(new GridLayout(1, 2, 10, 0));

        // formulário esquerdo, inserir / atualizar prato
        JPanel formPrato = new JPanel(new GridBagLayout());
        formPrato.setBorder(BorderFactory.createTitledBorder("Cadastro e Atualização de Prato (INSERT / UPDATE)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtIdPrato.setEditable(false);
        gbc.gridx = 0; gbc.gridy = 0;
        formPrato.add(new JLabel("ID (clique na tabela p/ editar):"), gbc);
        gbc.gridx = 1;
        formPrato.add(txtIdPrato, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPrato.add(new JLabel("Nome do Prato:"), gbc);
        gbc.gridx = 1;
        formPrato.add(txtNomePrato, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPrato.add(new JLabel("Calorias (kcal):"), gbc);
        gbc.gridx = 1;
        formPrato.add(txtCaloriasPrato, gbc);

        JPanel botoesPrato = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton btnInserirPrato = new JButton("Inserir Novo Prato");
        JButton btnAtualizarPrato = new JButton("Atualizar Selecionado");
        JButton btnLimparPrato = new JButton("Limpar");

        btnInserirPrato.addActionListener(e -> acaoInserirPrato());
        btnAtualizarPrato.addActionListener(e -> acaoAtualizarPrato());
        btnLimparPrato.addActionListener(e -> limparCamposPrato());

        botoesPrato.add(btnInserirPrato);
        botoesPrato.add(btnAtualizarPrato);
        botoesPrato.add(btnLimparPrato);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        formPrato.add(botoesPrato, gbc);

        // formulário direito, ingredientes e relacionamento utiliza (N:M)
        JPanel formIngrediente = new JPanel(new GridBagLayout());
        formIngrediente.setBorder(BorderFactory.createTitledBorder("Ingredientes e Composição do Prato (Tabela Utiliza)"));
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(4, 4, 4, 4);
        gbc2.anchor = GridBagConstraints.WEST;
        gbc2.fill = GridBagConstraints.HORIZONTAL;

        JButton btnAddIngrediente = new JButton("Cadastrar Ingrediente");
        btnAddIngrediente.addActionListener(e -> acaoInserirIngrediente());

        gbc2.gridx = 0; gbc2.gridy = 0;
        formIngrediente.add(new JLabel("Novo Ingrediente:"), gbc2);
        gbc2.gridx = 1;
        formIngrediente.add(txtNovoIngrediente, gbc2);
        gbc2.gridx = 2;
        formIngrediente.add(btnAddIngrediente, gbc2);

        gbc2.gridx = 0; gbc2.gridy = 1;
        formIngrediente.add(new JLabel("Prato:"), gbc2);
        gbc2.gridx = 1; gbc2.gridwidth = 2;
        formIngrediente.add(cbPratoUtiliza, gbc2);

        gbc2.gridx = 0; gbc2.gridy = 2; gbc2.gridwidth = 1;
        formIngrediente.add(new JLabel("Ingrediente:"), gbc2);
        gbc2.gridx = 1; gbc2.gridwidth = 2;
        formIngrediente.add(cbIngredienteUtiliza, gbc2);

        JButton btnVincularIngrediente = new JButton("Vincular Ingrediente ao Prato (INSERT Utiliza)");
        btnVincularIngrediente.addActionListener(e -> acaoVincularIngrediente());
        gbc2.gridx = 0; gbc2.gridy = 3; gbc2.gridwidth = 3;
        formIngrediente.add(btnVincularIngrediente, gbc2);

        topo.add(formPrato);
        topo.add(formIngrediente);
        painel.add(topo, BorderLayout.NORTH);

        // tabela central (consulta 1 com JOIN)
        tabelaPratos.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaPratos.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabelaPratos.getSelectedRow() != -1) {
                int row = tabelaPratos.getSelectedRow();
                txtIdPrato.setText(String.valueOf(modelPratos.getValueAt(row, 0)));
                txtNomePrato.setText(String.valueOf(modelPratos.getValueAt(row, 1)));
                txtCaloriasPrato.setText(String.valueOf(modelPratos.getValueAt(row, 2)));
            }
        });

        JPanel centro = new JPanel(new BorderLayout(4, 4));
        centro.setBorder(BorderFactory.createTitledBorder("Consulta 1: Catálogo de Pratos e Ingredientes (SELECT com JOIN)"));
        centro.add(new JScrollPane(tabelaPratos), BorderLayout.CENTER);

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRecarregar = new JButton("Carregar / Atualizar Dados do Banco");
        btnRecarregar.addActionListener(e -> recarregarTudo());
        rodape.add(btnRecarregar);
        centro.add(rodape, BorderLayout.SOUTH);

        painel.add(centro, BorderLayout.CENTER);
        return painel;
    }

    // PLANEJAMENTO DE CARDÁPIOS (CARDAPIO, CONTEM, OFERECE)

    private JPanel criarPainelPlanejamentoCardapios() {
        JPanel painel = new JPanel(new BorderLayout(8, 8));
        painel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel topo = new JPanel(new GridLayout(1, 2, 10, 0));

        // esquerda, criar / atualizar cardápio
        JPanel formCardapio = new JPanel(new GridBagLayout());
        formCardapio.setBorder(BorderFactory.createTitledBorder("Cardápio Diário (INSERT / UPDATE Cardápio)"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        txtIdCardapio.setEditable(false);
        gbc.gridx = 0; gbc.gridy = 0;
        formCardapio.add(new JLabel("ID (clique na tabela p/ editar):"), gbc);
        gbc.gridx = 1;
        formCardapio.add(txtIdCardapio, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formCardapio.add(new JLabel("Data (AAAA-MM-DD):"), gbc);
        gbc.gridx = 1;
        formCardapio.add(txtDataCardapio, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formCardapio.add(new JLabel("Turno:"), gbc);
        gbc.gridx = 1;
        formCardapio.add(cbTurnoCardapio, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formCardapio.add(new JLabel("Nutricionista (ADMIN):"), gbc);
        JPanel panelAdmin = new JPanel(new BorderLayout(4, 0));
        panelAdmin.add(cbAdminCardapio, BorderLayout.CENTER);
        JButton btnNovoAdmin = new JButton("+ Admin");
        btnNovoAdmin.setToolTipText("Cadastrar novo Nutricionista (Usuário + Admin)");
        btnNovoAdmin.addActionListener(e -> acaoCadastrarAdminDialog());
        panelAdmin.add(btnNovoAdmin, BorderLayout.EAST);
        gbc.gridx = 1;
        formCardapio.add(panelAdmin, gbc);

        JPanel botoesCardapio = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton btnCriarCardapio = new JButton("Criar Cardápio");
        JButton btnAtualizarCardapio = new JButton("Atualizar Selecionado");
        JButton btnLimparCardapio = new JButton("Limpar");

        btnCriarCardapio.addActionListener(e -> acaoInserirCardapio());
        btnAtualizarCardapio.addActionListener(e -> acaoAtualizarCardapio());
        btnLimparCardapio.addActionListener(e -> {
            txtIdCardapio.setText("");
            txtDataCardapio.setText(LocalDate.now().toString());
            tabelaCardapios.clearSelection();
        });

        botoesCardapio.add(btnCriarCardapio);
        botoesCardapio.add(btnAtualizarCardapio);
        botoesCardapio.add(btnLimparCardapio);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        formCardapio.add(botoesCardapio, gbc);

        // direita, vincular pratos (contém) e restaurantes (oferece)
        JPanel formVinculos = new JPanel(new GridBagLayout());
        formVinculos.setBorder(BorderFactory.createTitledBorder("Associar Pratos (Contém) e Unidades do RU (Oferece)"));
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(4, 4, 4, 4);
        gbc2.anchor = GridBagConstraints.WEST;
        gbc2.fill = GridBagConstraints.HORIZONTAL;

        gbc2.gridx = 0; gbc2.gridy = 0;
        formVinculos.add(new JLabel("Cardápio Alvo:"), gbc2);
        gbc2.gridx = 1; gbc2.gridwidth = 2;
        formVinculos.add(cbCardapioVinculo, gbc2);

        gbc2.gridx = 0; gbc2.gridy = 1; gbc2.gridwidth = 1;
        formVinculos.add(new JLabel("Prato:"), gbc2);
        gbc2.gridx = 1;
        formVinculos.add(cbPratoContem, gbc2);
        JButton btnAddPratoCardapio = new JButton("Vincular Prato (Contém)");
        btnAddPratoCardapio.addActionListener(e -> acaoVincularPratoCardapio());
        gbc2.gridx = 2;
        formVinculos.add(btnAddPratoCardapio, gbc2);

        gbc2.gridx = 0; gbc2.gridy = 2;
        formVinculos.add(new JLabel("Unidade RU:"), gbc2);
        JPanel panelRest = new JPanel(new BorderLayout(4, 0));
        panelRest.add(cbRestauranteOferece, BorderLayout.CENTER);
        JButton btnNovoRest = new JButton("+ RU");
        btnNovoRest.setToolTipText("Cadastrar nova unidade de Restaurante Universitário");
        btnNovoRest.addActionListener(e -> acaoCadastrarRestauranteDialog());
        panelRest.add(btnNovoRest, BorderLayout.EAST);
        gbc2.gridx = 1;
        formVinculos.add(panelRest, gbc2);

        JButton btnAddRestCardapio = new JButton("Vincular RU (Oferece)");
        btnAddRestCardapio.addActionListener(e -> acaoVincularRestauranteCardapio());
        gbc2.gridx = 2;
        formVinculos.add(btnAddRestCardapio, gbc2);

        topo.add(formCardapio);
        topo.add(formVinculos);
        painel.add(topo, BorderLayout.NORTH);

        // tabela central (consulta 2 com JOIN)
        tabelaCardapios.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaCardapios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && tabelaCardapios.getSelectedRow() != -1) {
                int row = tabelaCardapios.getSelectedRow();
                txtIdCardapio.setText(String.valueOf(modelCardapios.getValueAt(row, 0)));
                txtDataCardapio.setText(String.valueOf(modelCardapios.getValueAt(row, 1)));
                cbTurnoCardapio.setSelectedItem(String.valueOf(modelCardapios.getValueAt(row, 2)));
            }
        });

        JPanel centro = new JPanel(new BorderLayout(4, 4));
        centro.setBorder(BorderFactory.createTitledBorder("Consulta 2: Cardápios Planejados, Unidades, Pratos e Calorias (SELECT com JOIN)"));
        centro.add(new JScrollPane(tabelaCardapios), BorderLayout.CENTER);

        JPanel rodape = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnRecarregar = new JButton("Carregar / Atualizar Dados do Banco");
        btnRecarregar.addActionListener(e -> recarregarTudo());
        rodape.add(btnRecarregar);
        centro.add(rodape, BorderLayout.SOUTH);

        painel.add(centro, BorderLayout.CENTER);
        return painel;
    }

    // AÇÕES E EVENTOS DE BANCO DE DADOS

    private void acaoInserirPrato() {
        String nome = txtNomePrato.getText().trim();
        String calStr = txtCaloriasPrato.getText().trim();
        if (nome.isEmpty() || calStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha o nome e as calorias do prato.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int calorias = Integer.parseInt(calStr);
            dao.inserirPrato(nome, calorias);
            limparCamposPrato();
            recarregarTudo();
            JOptionPane.showMessageDialog(this, "Prato inserido com sucesso!");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Informe um número inteiro válido para calorias.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            mostrarErroSQL("Erro ao inserir prato", ex);
        }
    }

    private void acaoAtualizarPrato() {
        String idStr = txtIdPrato.getText().trim();
        if (idStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecione um prato na tabela abaixo para atualizar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int idPrato = Integer.parseInt(idStr);
            int calorias = Integer.parseInt(txtCaloriasPrato.getText().trim());
            String nome = txtNomePrato.getText().trim();
            dao.atualizarPrato(idPrato, nome, calorias);
            recarregarTudo();
            JOptionPane.showMessageDialog(this, "Prato #" + idPrato + " atualizado com sucesso!");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Verifique os campos numéricos.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            mostrarErroSQL("Erro ao atualizar prato", ex);
        }
    }

    private void acaoInserirIngrediente() {
        String nome = txtNovoIngrediente.getText().trim();
        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe o nome do ingrediente.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            dao.inserirIngrediente(nome);
            txtNovoIngrediente.setText("");
            recarregarTudo();
            JOptionPane.showMessageDialog(this, "Ingrediente cadastrado!");
        } catch (SQLException ex) {
            mostrarErroSQL("Erro ao cadastrar ingrediente", ex);
        }
    }

    private void acaoVincularIngrediente() {
        ComboItem prato = (ComboItem) cbPratoUtiliza.getSelectedItem();
        ComboItem ing = (ComboItem) cbIngredienteUtiliza.getSelectedItem();
        if (prato == null || ing == null) {
            JOptionPane.showMessageDialog(this, "Selecione um prato e um ingrediente.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            dao.vincularIngredienteAoPrato(prato.id(), ing.id());
            recarregarTudo();
            JOptionPane.showMessageDialog(this, "Ingrediente vinculado ao prato com sucesso!");
        } catch (SQLException ex) {
            mostrarErroSQL("Erro ao vincular ingrediente ao prato", ex);
        }
    }

    private void acaoInserirCardapio() {
        ComboItem admin = (ComboItem) cbAdminCardapio.getSelectedItem();
        if (admin == null) {
            JOptionPane.showMessageDialog(this, "Cadastre ou selecione um Nutricionista (Admin) primeiro.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            LocalDate data = LocalDate.parse(txtDataCardapio.getText().trim());
            String turno = (String) cbTurnoCardapio.getSelectedItem();
            dao.inserirCardapio(data, turno, admin.id());
            recarregarTudo();
            JOptionPane.showMessageDialog(this, "Cardápio criado com sucesso!");
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Formato de data inválido. Use AAAA-MM-DD (ex: 2026-11-18).", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            mostrarErroSQL("Erro ao criar cardápio", ex);
        }
    }

    private void acaoAtualizarCardapio() {
        String idStr = txtIdCardapio.getText().trim();
        if (idStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecione um cardápio na tabela para atualizar.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ComboItem admin = (ComboItem) cbAdminCardapio.getSelectedItem();
        if (admin == null) {
            JOptionPane.showMessageDialog(this, "Selecione um Nutricionista (Admin).", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int idCardapio = Integer.parseInt(idStr);
            LocalDate data = LocalDate.parse(txtDataCardapio.getText().trim());
            String turno = (String) cbTurnoCardapio.getSelectedItem();
            dao.atualizarCardapio(idCardapio, data, turno, admin.id());
            recarregarTudo();
            JOptionPane.showMessageDialog(this, "Cardápio #" + idCardapio + " atualizado com sucesso!");
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Data inválida. Use o formato AAAA-MM-DD.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (SQLException ex) {
            mostrarErroSQL("Erro ao atualizar cardápio", ex);
        }
    }

    private void acaoVincularPratoCardapio() {
        ComboItem cardapio = (ComboItem) cbCardapioVinculo.getSelectedItem();
        ComboItem prato = (ComboItem) cbPratoContem.getSelectedItem();
        if (cardapio == null || prato == null) {
            JOptionPane.showMessageDialog(this, "Selecione um cardápio e um prato.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            dao.vincularPratoAoCardapio(cardapio.id(), prato.id());
            recarregarTudo();
            JOptionPane.showMessageDialog(this, "Prato adicionado ao cardápio!");
        } catch (SQLException ex) {
            mostrarErroSQL("Erro ao vincular prato ao cardápio", ex);
        }
    }

    private void acaoVincularRestauranteCardapio() {
        ComboItem cardapio = (ComboItem) cbCardapioVinculo.getSelectedItem();
        ComboItem rest = (ComboItem) cbRestauranteOferece.getSelectedItem();
        if (cardapio == null || rest == null) {
            JOptionPane.showMessageDialog(this, "Selecione um cardápio e uma unidade do RU.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            dao.vincularRestauranteAoCardapio(cardapio.id(), rest.id());
            recarregarTudo();
            JOptionPane.showMessageDialog(this, "Restaurante vinculado ao cardápio!");
        } catch (SQLException ex) {
            mostrarErroSQL("Erro ao vincular restaurante ao cardápio", ex);
        }
    }

    private void acaoCadastrarAdminDialog() {
        JTextField txtNome = new JTextField(20);
        JTextField txtEmail = new JTextField(20);
        JTextField txtCrn = new JTextField(15);

        JPanel panel = new JPanel(new GridLayout(3, 2, 6, 6));
        panel.add(new JLabel("Nome do Nutricionista:"));
        panel.add(txtNome);
        panel.add(new JLabel("E-mail Institucional:"));
        panel.add(txtEmail);
        panel.add(new JLabel("Registro CRN:"));
        panel.add(txtCrn);

        int res = JOptionPane.showConfirmDialog(this, panel, "Cadastrar Nutricionista (Usuário + ADMIN)", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            try {
                dao.inserirAdmin(txtNome.getText(), txtEmail.getText(), txtCrn.getText());
                recarregarTudo();
                JOptionPane.showMessageDialog(this, "Nutricionista cadastrado com sucesso!");
            } catch (SQLException ex) {
                mostrarErroSQL("Erro ao cadastrar Nutricionista (Admin)", ex);
            }
        }
    }

    private void acaoCadastrarRestauranteDialog() {
        String nome = JOptionPane.showInputDialog(this, "Nome da Unidade do Restaurante Universitário (ex: RU Centro):", "Cadastrar Restaurante", JOptionPane.PLAIN_MESSAGE);
        if (nome != null && !nome.trim().isEmpty()) {
            try {
                dao.inserirRestaurante(nome);
                recarregarTudo();
                JOptionPane.showMessageDialog(this, "Restaurante cadastrado com sucesso!");
            } catch (SQLException ex) {
                mostrarErroSQL("Erro ao cadastrar Restaurante", ex);
            }
        }
    }

    private void limparCamposPrato() {
        txtIdPrato.setText("");
        txtNomePrato.setText("");
        txtCaloriasPrato.setText("");
        tabelaPratos.clearSelection();
    }

    public void recarregarTudo() {
        try {
            // tabelas
            modelPratos.setRowCount(0);
            for (Object[] row : dao.listarPratosComIngredientes()) {
                modelPratos.addRow(row);
            }

            modelCardapios.setRowCount(0);
            for (Object[] row : dao.listarCardapiosDetalhados()) {
                modelCardapios.addRow(row);
            }

            // combos
            atualizarCombo(cbPratoUtiliza, dao.listarPratosCombo());
            atualizarCombo(cbPratoContem, dao.listarPratosCombo());
            atualizarCombo(cbIngredienteUtiliza, dao.listarIngredientesCombo());
            atualizarCombo(cbAdminCardapio, dao.listarAdminsCombo());
            atualizarCombo(cbRestauranteOferece, dao.listarRestaurantesCombo());
            atualizarCombo(cbCardapioVinculo, dao.listarCardapiosCombo());

        } catch (SQLException ex) {
            mostrarErroSQL("Não foi possível carregar os dados do PostgreSQL.\nVerifique se a conexão (Tailscale / db.properties) e as tabelas (01_ddl_schema.sql) estão ativas.", ex);
        }
    }

    private void atualizarCombo(JComboBox<ComboItem> combo, List<ComboItem> itens) {
        combo.removeAllItems();
        for (ComboItem item : itens) {
            combo.addItem(item);
        }
    }

    private void mostrarErroSQL(String titulo, SQLException ex) {
        JOptionPane.showMessageDialog(
            this,
            titulo + ":\n" + ex.getMessage(),
            "Erro no Banco de Dados",
            JOptionPane.ERROR_MESSAGE
        );
    }
}
