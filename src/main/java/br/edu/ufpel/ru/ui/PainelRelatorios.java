package br.edu.ufpel.ru.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/* aba 3, pedro
painel de consultas analíticas e relatórios gerenciais do RU

implementar seletor (JComboBox) e JTable para exibir as 6+ consultas obrigatórias (sendo pelo menos 4 com JOIN) definidas em sql/04_consultas.sql */

public class PainelRelatorios extends JPanel {

    public PainelRelatorios() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel lblInfo = new JLabel(
            "<html><div style='text-align:center;'>"
            + "<h2>Módulo 3: Consultas e Relatórios Gerenciais (JOINs e Agregações)</h2>"
            + "<p><b>Responsável: Pedro</b></p><br>"
            + "<p>Esta aba está isolada no arquivo <code>PainelRelatorios.java</code> para evitar conflitos no Git.</p>"
            + "<p>Funcionalidades previstas:</p>"
            + "<ul style='text-align:left;'>"
            + "<li>Executar e exibir em <code>JTable</code> as consultas do script <code>sql/04_consultas.sql</code> (mínimo 6 consultas, sendo 4+ com JOIN).</li>"
            + "<li>Média de notas por cardápio, turno e restaurante.</li>"
            + "<li>Análise de notas por faixa de horário de entrada na catraca (identificação de gargalos).</li>"
            + "<li>Histórico detalhado de acessos e avaliações por aluno/servidor.</li>"
            + "</ul>"
            + "</div></html>",
            SwingConstants.CENTER
        );
        lblInfo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        add(lblInfo, BorderLayout.CENTER);
    }
}
