package br.edu.ufpel.ru.ui;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;

/* aba 2, samuel
módulo de registro de consumo (catraca) e avaliação de refeições (feedback)
 
inserção de consumo (passagem do aluno/servidor na catraca vinculada a um cardápio)
inserção e atualização de feedback (nota de 1 a 5 + comentário vinculado a um consumo)
demonstração do disparo do gatilho (trigger definida em sql/02_triggers.sql) */

public class PainelConsumoFeedback extends JPanel {

    public PainelConsumoFeedback() {
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JLabel lblInfo = new JLabel(
            "<html><div style='text-align:center;'>"
            + "<h2>Módulo 2: Registro de Consumo (Catraca) e Avaliações (Feedback)</h2>"
            + "<p><b>Responsável: Samuel</b></p><br>"
            + "<p>Esta aba está isolada no arquivo <code>PainelConsumoFeedback.java</code> para evitar conflitos no Git.</p>"
            + "<p>Funcionalidades previstas:</p>"
            + "<ul style='text-align:left;'>"
            + "<li><b>INSERT Consumo:</b> Registrar entrada de Aluno/Servidor na catraca em um Cardápio.</li>"
            + "<li><b>INSERT / UPDATE Feedback:</b> Enviar ou editar Nota (1 a 5) e Comentário de um Consumo.</li>"
            + "<li><b>Trigger (BD Ativo):</b> Demonstrar a validação automática configurada em <code>sql/02_triggers.sql</code>.</li>"
            + "</ul>"
            + "</div></html>",
            SwingConstants.CENTER
        );
        lblInfo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        add(lblInfo, BorderLayout.CENTER);
    }
}
