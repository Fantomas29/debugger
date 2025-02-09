package dbg.core;

import dbg.gui.DebuggerGUI;

import javax.swing.SwingUtilities;

public class JDISimpleDebugger {
    public static void main(String[] args) {
        // Lance l'interface graphique dans l'EDT (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            try {
                DebuggerGUI gui = new DebuggerGUI();
                gui.setVisible(true);
                // Démarre le debugger avec la classe de test
                gui.startDebugging(TestDebugger.class);
            } catch (Exception e) {
                System.err.println("Erreur lors du démarrage du debugger: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
}