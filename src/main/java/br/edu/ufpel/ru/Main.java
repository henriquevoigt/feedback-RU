package br.edu.ufpel.ru;

import br.edu.ufpel.ru.ui.MainFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                    if ("Nimbus".equals(info.getName())) {
                        UIManager.setLookAndFeel(info.getClassName());
                        break;
                    }
                }
            } catch (Exception ignored) {
                // mantém o LookAndFeel padrão caso o nimbus não esteja disponível
            }

            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}
