package br.edu.ufpel.ru.ui;

import br.edu.ufpel.ru.dao.CatalogoCardapioDAO.ComboItem;
import br.edu.ufpel.ru.dao.RelatoriosDAO;
import br.edu.ufpel.ru.dao.RelatoriosDAO.ResultadoConsulta;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/* aba 3, pedro
consultas e relatórios gerenciais do RU.

o usuário escolhe uma das 6 consultas de sql/04_consultas.sql, preenche os filtros
que fazem sentido para ela e clica em Executar. a consulta roda fora da thread da
interface (SwingWorker) e o resultado aparece numa JTable montada a partir das
colunas devolvidas pelo banco (ResultSetMetaData), então a mesma tabela serve
para todas as consultas. */

public final class PainelRelatorios extends JPanel {

    private static final long serialVersionUID = 1L;

    // definição das consultas: título, explicação e quais filtros cada uma aceita

    private enum Consulta {
        CATALOGO_PRATOS(
            "1 - Catálogo nutricional de pratos e ingredientes",
            "Lista todos os pratos com as calorias e os ingredientes que cada um utiliza. "
                + "Pratos sem ingrediente vinculado também aparecem (LEFT JOIN).",
            "prato, utiliza, ingrediente",
            "nome do prato (busca parcial).",
            false, false, false, true, "Nome do prato:"),
        CARDAPIOS_DETALHADOS(
            "2 - Cardápios planejados: nutricionista, unidades e pratos",
            "Detalha cada cardápio: data, turno, nutricionista responsável (nome e CRN), "
                + "unidades do RU que o oferecem, pratos da refeição e soma total de calorias.",
            "cardapio, admin, usuario, oferece, restaurante, contem, prato",
            "período (data do cardápio), turno e restaurante que oferece o cardápio.",
            true, true, true, false, "Usuário (nome):"),
        HISTORICO_CONSUMO(
            "3 - Histórico de entradas na catraca e avaliações",
            "Mostra cada entrada na catraca: quem entrou (perfil, matrícula ou SIAPE), em qual cardápio "
                + "e horário, e a avaliação enviada. Consumos ainda sem avaliação aparecem como "
                + "\"Pendente\" (LEFT JOIN em feedback).",
            "consumo, usuario, cardapio, aluno, servidor, admin, feedback",
            "período (data da entrada na catraca), turno e nome do usuário (busca parcial).",
            true, true, false, true, "Usuário (nome):"),
        PARTICIPACAO_USUARIO(
            "4 - Participação de cada usuário nas avaliações",
            "Para cada usuário: quantos consumos teve, quantas avaliações enviou, o percentual de "
                + "consumos avaliados e a média das notas. Usuários sem consumo aparecem com taxa zero.",
            "usuario, aluno, servidor, admin, consumo, feedback",
            "nome do usuário (busca parcial).",
            false, false, false, true, "Usuário (nome):"),
        SATISFACAO_CARDAPIO(
            "5 - Satisfação por cardápio e turno",
            "Por cardápio: total de consumos na catraca, avaliações recebidas, taxa de resposta "
                + "(avaliações ÷ consumos), média, menor e maior nota. Os restaurantes aparecem só como "
                + "informação, porque o consumo não registra em qual unidade a pessoa entrou.",
            "consumo, cardapio, feedback; oferece e restaurante numa subconsulta",
            "período (data do cardápio) e turno. Não há filtro de restaurante: como o consumo não "
                + "registra a unidade de entrada, filtrar por oferece daria números enganosos.",
            true, true, false, false, "Usuário (nome):"),
        GARGALOS_HORARIO(
            "6 - Gargalos por faixa de horário na catraca",
            "Agrupa as entradas na catraca em faixas de 30 minutos, por turno, com o número de "
                + "avaliações, a média das notas e quantas foram negativas (nota até 2). "
                + "Mostra os horários de pico e se as notas caem neles.",
            "consumo, cardapio, feedback",
            "período (data da entrada na catraca) e turno.",
            true, true, false, false, "Usuário (nome):");

        final String titulo;
        final String descricao;
        final String tabelas;
        final String filtros;
        final boolean usaDatas;
        final boolean usaTurno;
        final boolean usaRestaurante;
        final boolean usaTexto;
        final String rotuloTexto;

        Consulta(String titulo, String descricao, String tabelas, String filtros,
                 boolean usaDatas, boolean usaTurno, boolean usaRestaurante,
                 boolean usaTexto, String rotuloTexto) {
            this.titulo = titulo;
            this.descricao = descricao;
            this.tabelas = tabelas;
            this.filtros = filtros;
            this.usaDatas = usaDatas;
            this.usaTurno = usaTurno;
            this.usaRestaurante = usaRestaurante;
            this.usaTexto = usaTexto;
            this.rotuloTexto = rotuloTexto;
        }

        @Override
        public String toString() {
            return titulo;
        }
    }

    // títulos legíveis para as colunas do SQL; coluna fora do mapa mantém o nome original
    private static final Map<String, String> TITULOS_COLUNAS = Map.ofEntries(
        Map.entry("id_prato", "ID do prato"),
        Map.entry("nome_prato", "Prato"),
        Map.entry("calorias", "Calorias (kcal)"),
        Map.entry("ingredientes", "Ingredientes"),
        Map.entry("id_cardapio", "ID do cardápio"),
        Map.entry("data", "Data"),
        Map.entry("turno", "Turno"),
        Map.entry("nutricionista_responsavel", "Nutricionista responsável"),
        Map.entry("crn", "CRN"),
        Map.entry("restaurantes", "Restaurantes"),
        Map.entry("pratos_cardapio", "Pratos do cardápio"),
        Map.entry("calorias_totais", "Calorias totais (kcal)"),
        Map.entry("id_consumo", "ID do consumo"),
        Map.entry("id_usuario", "ID do usuário"),
        Map.entry("usuario", "Usuário"),
        Map.entry("perfil", "Perfil"),
        Map.entry("matricula", "Matrícula"),
        Map.entry("siape", "SIAPE"),
        Map.entry("data_cardapio", "Data do cardápio"),
        Map.entry("data_hora_entrada", "Entrada na catraca"),
        Map.entry("id_feedback", "ID do feedback"),
        Map.entry("nota", "Nota"),
        Map.entry("comentario", "Comentário"),
        Map.entry("data_envio", "Envio da avaliação"),
        Map.entry("situacao_feedback", "Situação do feedback"),
        Map.entry("total_consumos", "Total de consumos"),
        Map.entry("total_avaliacoes", "Total de avaliações"),
        Map.entry("percentual_consumos_avaliados", "Consumos avaliados (%)"),
        Map.entry("media_notas", "Média das notas"),
        Map.entry("taxa_resposta_pct", "Taxa de resposta (%)"),
        Map.entry("media_nota", "Média da nota"),
        Map.entry("menor_nota", "Menor nota"),
        Map.entry("maior_nota", "Maior nota"),
        Map.entry("inicio_faixa", "Início da faixa (30 min)"),
        Map.entry("total_entradas", "Total de entradas"),
        Map.entry("avaliacoes_negativas", "Avaliações negativas (nota ≤ 2)")
    );

    private static final DateTimeFormatter FORMATO_DATA =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final ComboItem TODOS_RESTAURANTES = new ComboItem(0, "Todos");
    private static final String TURNO_TODOS = "Todos";
    private static final String DICA_FILTRO_INATIVO = "Este filtro não se aplica à consulta selecionada.";

    private final transient RelatoriosDAO dao = new RelatoriosDAO();

    // seleção da consulta
    private final JComboBox<Consulta> cbConsulta = new JComboBox<>(Consulta.values());
    private final JButton btnExecutar = new JButton("Executar");
    private final JTextArea txtDescricao = new JTextArea();

    // filtros
    private final JLabel lblDataInicial = new JLabel("Data inicial:");
    private final JTextField txtDataInicial = new JTextField(10);
    private final JLabel lblDataFinal = new JLabel("Data final:");
    private final JTextField txtDataFinal = new JTextField(10);
    private final JLabel lblTurno = new JLabel("Turno:");
    private final JComboBox<String> cbTurno = new JComboBox<>(new String[]{TURNO_TODOS, "Almoço", "Jantar"});
    private final JLabel lblRestaurante = new JLabel("Restaurante:");
    private final JComboBox<ComboItem> cbRestaurante = new JComboBox<>();
    private final JLabel lblTexto = new JLabel("Usuário (nome):");
    private final JTextField txtTexto = new JTextField(16);
    private final JButton btnLimpar = new JButton("Limpar filtros");

    // resultado
    private final JTable tabela = new JTable();
    private final JScrollPane scrollTabela = new JScrollPane(tabela);
    private final JLabel lblRodape = new JLabel(" ");

    private transient List<String> colunasSqlAtuais = List.of();
    private int larguraPreferidaTotal;
    private boolean carregandoRestaurantes;

    public PainelRelatorios() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(criarPainelTopo(), BorderLayout.NORTH);
        add(criarPainelResultado(), BorderLayout.CENTER);

        lblRodape.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        add(lblRodape, BorderLayout.SOUTH);

        cbConsulta.addActionListener(e -> aplicarConsultaSelecionada());
        btnExecutar.addActionListener(e -> executarConsulta());
        btnLimpar.addActionListener(e -> limparFiltros());
        // Enter num campo de texto também executa a consulta
        txtDataInicial.addActionListener(e -> executarConsulta());
        txtDataFinal.addActionListener(e -> executarConsulta());
        txtTexto.addActionListener(e -> executarConsulta());

        // a lista de restaurantes é carregada quando a aba aparece, e não no construtor,
        // para o painel abrir normalmente mesmo com o banco desconectado
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && isShowing()) {
                carregarRestaurantes();
            }
        });

        aplicarConsultaSelecionada();
    }

    // MONTAGEM DA TELA

    private JPanel criarPainelTopo() {
        JPanel topo = new JPanel(new BorderLayout(6, 6));

        JPanel linhaConsulta = new JPanel(new BorderLayout(6, 0));
        linhaConsulta.add(new JLabel("Consulta:"), BorderLayout.WEST);
        linhaConsulta.add(cbConsulta, BorderLayout.CENTER);
        linhaConsulta.add(btnExecutar, BorderLayout.EAST);
        topo.add(linhaConsulta, BorderLayout.NORTH);

        txtDescricao.setEditable(false);
        txtDescricao.setFocusable(false);
        txtDescricao.setLineWrap(true);
        txtDescricao.setWrapStyleWord(true);
        txtDescricao.setOpaque(false);
        txtDescricao.setFont(UIManager.getFont("Label.font"));
        txtDescricao.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("Sobre a consulta"),
            BorderFactory.createEmptyBorder(2, 4, 4, 4)
        ));
        topo.add(txtDescricao, BorderLayout.CENTER);

        topo.add(criarPainelFiltros(), BorderLayout.SOUTH);
        return topo;
    }

    private JPanel criarPainelFiltros() {
        JPanel filtros = new JPanel(new GridBagLayout());
        filtros.setBorder(BorderFactory.createTitledBorder("Filtros"));

        String dicaData = "Formato dd/mm/aaaa. Deixe em branco para não limitar o período.";
        txtDataInicial.setToolTipText(dicaData);
        txtDataFinal.setToolTipText(dicaData);
        cbRestaurante.addItem(TODOS_RESTAURANTES);
        cbRestaurante.setPrototypeDisplayValue(new ComboItem(0, "CAMPUS CAPÃO DO LEÃO   "));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // linha 1: período e turno
        gbc.gridy = 0;
        gbc.gridx = 0; filtros.add(lblDataInicial, gbc);
        gbc.gridx = 1; filtros.add(txtDataInicial, gbc);
        gbc.gridx = 2; filtros.add(lblDataFinal, gbc);
        gbc.gridx = 3; filtros.add(txtDataFinal, gbc);
        gbc.gridx = 4; filtros.add(lblTurno, gbc);
        gbc.gridx = 5; filtros.add(cbTurno, gbc);

        // linha 2: restaurante, texto (nome do prato ou do usuário) e limpar
        gbc.gridy = 1;
        gbc.gridx = 0; filtros.add(lblRestaurante, gbc);
        gbc.gridx = 1; filtros.add(cbRestaurante, gbc);
        gbc.gridx = 2; filtros.add(lblTexto, gbc);
        gbc.gridx = 3; filtros.add(txtTexto, gbc);
        gbc.gridx = 4; gbc.gridwidth = 2; filtros.add(btnLimpar, gbc);
        gbc.gridwidth = 1;

        // coluna vazia que absorve a sobra de largura e mantém os filtros à esquerda
        gbc.gridx = 6; gbc.gridy = 0; gbc.weightx = 1.0;
        filtros.add(new JLabel(), gbc);

        return filtros;
    }

    private JPanel criarPainelResultado() {
        tabela.setModel(new DefaultTableModel());
        tabela.setAutoCreateRowSorter(true);
        tabela.setFillsViewportHeight(true);

        CelulaRenderer renderer = new CelulaRenderer();
        tabela.setDefaultRenderer(Object.class, renderer);
        tabela.setDefaultRenderer(Number.class, renderer);
        tabela.setDefaultRenderer(java.util.Date.class, renderer);

        // tooltip no cabeçalho com o nome da coluna no SQL, útil para relacionar com o script
        tabela.setTableHeader(new JTableHeader(tabela.getColumnModel()) {
            @Override
            public String getToolTipText(MouseEvent e) {
                int colunaVisual = columnAtPoint(e.getPoint());
                if (colunaVisual < 0) {
                    return null;
                }
                int colunaModelo = tabela.convertColumnIndexToModel(colunaVisual);
                if (colunaModelo < 0 || colunaModelo >= colunasSqlAtuais.size()) {
                    return null;
                }
                return "Coluna no SQL: " + colunasSqlAtuais.get(colunaModelo) + " (clique para ordenar)";
            }
        });
        tabela.getTableHeader().setReorderingAllowed(false);

        scrollTabela.getViewport().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                ajustarModoRedimensionamento(false);
            }
        });

        JPanel centro = new JPanel(new BorderLayout());
        centro.setBorder(BorderFactory.createTitledBorder("Resultado"));
        centro.add(scrollTabela, BorderLayout.CENTER);
        return centro;
    }

    // TROCA DE CONSULTA E FILTROS

    private void aplicarConsultaSelecionada() {
        Consulta consulta = consultaSelecionada();

        txtDescricao.setText(
            "O que responde: " + consulta.descricao
            + "\nTabelas: " + consulta.tabelas
            + "\nFiltros: " + consulta.filtros
        );

        habilitar(consulta.usaDatas, lblDataInicial, txtDataInicial, lblDataFinal, txtDataFinal);
        habilitar(consulta.usaTurno, lblTurno, cbTurno);
        habilitar(consulta.usaRestaurante, lblRestaurante, cbRestaurante);
        habilitar(consulta.usaTexto, lblTexto, txtTexto);

        // o campo de texto é reaproveitado (nome do prato ou do usuário); se o significado
        // mudou, o que estava digitado não serve mais para a nova consulta
        if (!lblTexto.getText().equals(consulta.rotuloTexto)) {
            lblTexto.setText(consulta.rotuloTexto);
            txtTexto.setText("");
        }
        txtTexto.setToolTipText(consulta.usaTexto
            ? "Busca parcial, sem diferenciar maiúsculas e minúsculas (ILIKE)."
            : DICA_FILTRO_INATIVO);

        if (consulta.usaRestaurante) {
            cbRestaurante.setToolTipText("Mostra só os cardápios oferecidos nessa unidade (tabela oferece).");
        } else if (consulta == Consulta.SATISFACAO_CARDAPIO) {
            cbRestaurante.setToolTipText("Indisponível nesta consulta: a tabela consumo não registra "
                + "em qual restaurante a pessoa entrou.");
        } else {
            cbRestaurante.setToolTipText(DICA_FILTRO_INATIVO);
        }

        limparResultado();
        lblRodape.setText("Preencha os filtros desejados e clique em Executar.");
    }

    private static void habilitar(boolean ativo, JComponent... componentes) {
        for (JComponent componente : componentes) {
            componente.setEnabled(ativo);
        }
    }

    private void limparFiltros() {
        txtDataInicial.setText("");
        txtDataFinal.setText("");
        cbTurno.setSelectedItem(TURNO_TODOS);
        cbRestaurante.setSelectedItem(TODOS_RESTAURANTES);
        txtTexto.setText("");
    }

    // EXECUÇÃO DA CONSULTA

    private void executarConsulta() {
        if (!btnExecutar.isEnabled()) {
            return; // já existe uma consulta em andamento
        }
        Consulta consulta = consultaSelecionada();

        LocalDate dataInicial = null;
        LocalDate dataFinal = null;
        if (consulta.usaDatas) {
            try {
                dataInicial = lerData(txtDataInicial, "Data inicial");
                dataFinal = lerData(txtDataFinal, "Data final");
            } catch (DataInvalidaException ex) {
                ex.campo.requestFocusInWindow();
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Data inválida", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (dataInicial != null && dataFinal != null && dataFinal.isBefore(dataInicial)) {
                txtDataFinal.requestFocusInWindow();
                JOptionPane.showMessageDialog(this,
                    "A data final (" + FORMATO_DATA.format(dataFinal) + ") é anterior à data inicial ("
                        + FORMATO_DATA.format(dataInicial) + ").",
                    "Período inválido", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        final LocalDate inicio = dataInicial;
        final LocalDate fim = dataFinal;
        final String turno = consulta.usaTurno ? turnoSelecionado() : null;
        final ComboItem restaurante = consulta.usaRestaurante ? restauranteSelecionado() : null;
        final Integer idRestaurante = restaurante == null ? null : restaurante.id();
        final String texto = consulta.usaTexto ? textoOuNull(txtTexto.getText()) : null;
        final String resumoFiltros = montarResumoFiltros(consulta, inicio, fim, turno, restaurante, texto);

        if (cbRestaurante.getItemCount() <= 1) {
            carregarRestaurantes(); // a lista ficou vazia (ex.: banco estava fora do ar ao abrir a aba)
        }

        setExecutando(true);
        lblRodape.setText("Carregando...");

        new SwingWorker<ResultadoConsulta, Void>() {
            @Override
            protected ResultadoConsulta doInBackground() throws Exception {
                return switch (consulta) {
                    case CATALOGO_PRATOS -> dao.catalogoPratos(texto);
                    case CARDAPIOS_DETALHADOS -> dao.cardapiosDetalhados(inicio, fim, turno, idRestaurante);
                    case HISTORICO_CONSUMO -> dao.historicoConsumoFeedback(inicio, fim, turno, texto);
                    case PARTICIPACAO_USUARIO -> dao.participacaoPorUsuario(texto);
                    case SATISFACAO_CARDAPIO -> dao.satisfacaoPorCardapio(inicio, fim, turno);
                    case GARGALOS_HORARIO -> dao.gargalosPorFaixaHorario(inicio, fim, turno);
                };
            }

            @Override
            protected void done() {
                setExecutando(false);
                try {
                    exibirResultado(get(), resumoFiltros);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    limparResultado();
                    lblRodape.setText("Não foi possível executar a consulta.");
                    JOptionPane.showMessageDialog(
                        PainelRelatorios.this,
                        "Erro ao executar \"" + consulta.titulo + "\":\n" + causa.getMessage()
                            + "\n\nVerifique a conexão (botão 'Testar Conexão') e se os scripts sql/01 a 03 foram executados.",
                        "Erro no Banco de Dados",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        }.execute();
    }

    private void setExecutando(boolean executando) {
        btnExecutar.setEnabled(!executando);
        cbConsulta.setEnabled(!executando);
        setCursor(executando ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : null);
    }

    private void exibirResultado(ResultadoConsulta resultado, String resumoFiltros) {
        colunasSqlAtuais = resultado.colunas();

        Object[] titulos = resultado.colunas().stream().map(PainelRelatorios::tituloLegivel).toArray();
        List<Class<?>> tipos = resultado.tipos();
        DefaultTableModel modelo = new DefaultTableModel(titulos, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            // a classe real da coluna faz o RowSorter ordenar números e datas corretamente
            @Override
            public Class<?> getColumnClass(int column) {
                return tipos.get(column);
            }
        };
        for (Object[] linha : resultado.linhas()) {
            modelo.addRow(linha);
        }
        tabela.setModel(modelo);
        ajustarLarguras();

        int total = resultado.linhas().size();
        if (total == 0) {
            lblRodape.setText("Nenhum resultado para os filtros escolhidos" + resumoFiltros + ".");
        } else {
            lblRodape.setText(total + (total == 1 ? " linha retornada" : " linhas retornadas") + resumoFiltros + ".");
        }
    }

    private void limparResultado() {
        colunasSqlAtuais = List.of();
        larguraPreferidaTotal = 0;
        tabela.setModel(new DefaultTableModel());
    }

    // LARGURA DAS COLUNAS
    // cada coluna recebe a largura do maior conteúdo (com limite). se tudo cabe na tela,
    // as colunas se esticam para ocupar a largura; se não cabe, aparece a barra horizontal.

    private void ajustarLarguras() {
        TableColumnModel colunas = tabela.getColumnModel();
        TableCellRenderer rendererCabecalho = tabela.getTableHeader().getDefaultRenderer();
        int linhasAmostra = Math.min(tabela.getRowCount(), 200);
        int total = 0;

        for (int c = 0; c < colunas.getColumnCount(); c++) {
            TableColumn coluna = colunas.getColumn(c);
            int largura = rendererCabecalho
                .getTableCellRendererComponent(tabela, coluna.getHeaderValue(), false, false, -1, c)
                .getPreferredSize().width;
            for (int r = 0; r < linhasAmostra; r++) {
                Component celula = tabela.prepareRenderer(tabela.getCellRenderer(r, c), r, c);
                largura = Math.max(largura, celula.getPreferredSize().width);
            }
            largura = Math.min(Math.max(largura + 12, 60), 360);
            coluna.setPreferredWidth(largura);
            total += largura;
        }
        larguraPreferidaTotal = total;
        ajustarModoRedimensionamento(true);
    }

    private void ajustarModoRedimensionamento(boolean forcar) {
        int disponivel = scrollTabela.getViewport().getWidth();
        if (larguraPreferidaTotal == 0 || disponivel <= 0) {
            return;
        }
        int modo = larguraPreferidaTotal <= disponivel ? JTable.AUTO_RESIZE_ALL_COLUMNS : JTable.AUTO_RESIZE_OFF;
        if (!forcar && tabela.getAutoResizeMode() == modo) {
            return;
        }
        tabela.setAutoResizeMode(modo);
        if (modo == JTable.AUTO_RESIZE_OFF) {
            TableColumnModel colunas = tabela.getColumnModel();
            for (int c = 0; c < colunas.getColumnCount(); c++) {
                TableColumn coluna = colunas.getColumn(c);
                coluna.setWidth(coluna.getPreferredWidth());
            }
        }
    }

    // RESTAURANTES (JCOMBOBOX)

    private void carregarRestaurantes() {
        if (carregandoRestaurantes) {
            return;
        }
        carregandoRestaurantes = true;
        new SwingWorker<List<ComboItem>, Void>() {
            @Override
            protected List<ComboItem> doInBackground() throws Exception {
                return dao.listarRestaurantes();
            }

            @Override
            protected void done() {
                carregandoRestaurantes = false;
                try {
                    atualizarComboRestaurantes(get());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ex) {
                    // sem aviso aqui: o banco pode estar desconectado ao abrir a aba.
                    // a lista é recarregada ao clicar em Executar ou ao voltar para a aba.
                }
            }
        }.execute();
    }

    private void atualizarComboRestaurantes(List<ComboItem> restaurantes) {
        ComboItem selecionado = restauranteSelecionado();
        cbRestaurante.removeAllItems();
        cbRestaurante.addItem(TODOS_RESTAURANTES);
        for (ComboItem item : restaurantes) {
            cbRestaurante.addItem(item);
            if (selecionado != null && item.id() == selecionado.id()) {
                selecionado = item;
            }
        }
        cbRestaurante.setSelectedItem(selecionado == null ? TODOS_RESTAURANTES : selecionado);
    }

    // LEITURA DOS FILTROS

    private Consulta consultaSelecionada() {
        return (Consulta) cbConsulta.getSelectedItem();
    }

    private String turnoSelecionado() {
        Object turno = cbTurno.getSelectedItem();
        return turno == null || TURNO_TODOS.equals(turno) ? null : turno.toString();
    }

    private ComboItem restauranteSelecionado() {
        ComboItem item = (ComboItem) cbRestaurante.getSelectedItem();
        return item == null || item.equals(TODOS_RESTAURANTES) ? null : item;
    }

    private static String textoOuNull(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static LocalDate lerData(JTextField campo, String nomeCampo) throws DataInvalidaException {
        String texto = campo.getText().trim();
        if (texto.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(texto, FORMATO_DATA);
        } catch (DateTimeParseException ex) {
            throw new DataInvalidaException(campo,
                nomeCampo + " inválida: \"" + texto + "\".\nUse o formato dd/mm/aaaa (ex.: 17/11/2026) "
                    + "ou deixe o campo em branco.");
        }
    }

    private static String montarResumoFiltros(Consulta consulta, LocalDate inicio, LocalDate fim,
                                              String turno, ComboItem restaurante, String texto) {
        List<String> partes = new ArrayList<>();
        if (inicio != null && fim != null) {
            partes.add("de " + FORMATO_DATA.format(inicio) + " a " + FORMATO_DATA.format(fim));
        } else if (inicio != null) {
            partes.add("a partir de " + FORMATO_DATA.format(inicio));
        } else if (fim != null) {
            partes.add("até " + FORMATO_DATA.format(fim));
        }
        if (turno != null) {
            partes.add("turno " + turno);
        }
        if (restaurante != null) {
            partes.add("restaurante " + restaurante.label());
        }
        if (texto != null) {
            String campo = consulta == Consulta.CATALOGO_PRATOS ? "prato" : "usuário";
            partes.add(campo + " contém \"" + texto + "\"");
        }
        return partes.isEmpty() ? "" : " (filtros: " + String.join(", ", partes) + ")";
    }

    private static String tituloLegivel(String colunaSql) {
        return TITULOS_COLUNAS.getOrDefault(colunaSql, colunaSql);
    }

    private static final class DataInvalidaException extends Exception {
        private static final long serialVersionUID = 1L;
        private final transient JTextField campo;

        DataInvalidaException(JTextField campo, String mensagem) {
            super(mensagem);
            this.campo = campo;
        }
    }

    // RENDERIZAÇÃO DAS CÉLULAS
    // datas no formato brasileiro, decimais com vírgula, números à direita, vazio como "—"
    // e tooltip com o texto completo nas células longas (comentários, listas de pratos).

    private static final class CelulaRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        private static final int LIMITE_TOOLTIP = 40;
        private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        @Override
        public Component getTableCellRendererComponent(JTable table, Object valor, boolean selecionado,
                                                       boolean foco, int linha, int coluna) {
            super.getTableCellRendererComponent(table, formatar(valor), selecionado, foco, linha, coluna);
            // alinha pela classe da coluna, para o "—" de uma coluna numérica ficar à direita também
            boolean numerica = Number.class.isAssignableFrom(table.getColumnClass(coluna));
            setHorizontalAlignment(numerica ? RIGHT : LEFT);
            if (valor instanceof String texto && texto.length() > LIMITE_TOOLTIP) {
                setToolTipText(tooltipHtml(texto));
            } else {
                setToolTipText(null);
            }
            return this;
        }

        private static String formatar(Object valor) {
            if (valor == null) {
                return "—";
            }
            if (valor instanceof Timestamp ts) {
                return DATA_HORA.format(ts.toLocalDateTime());
            }
            if (valor instanceof java.sql.Date data) {
                return DATA.format(data.toLocalDate());
            }
            if (valor instanceof BigDecimal numero) {
                return numero.toPlainString().replace('.', ',');
            }
            return valor.toString();
        }

        private static String tooltipHtml(String texto) {
            String seguro = texto.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            return "<html><body style='width:320px'>" + seguro + "</body></html>";
        }
    }
}
