package dbg.gui;

import dbg.core.ScriptableDebugger;

import javax.swing.*;
import java.awt.*;
import java.io.PrintStream;
import java.io.OutputStream;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DebuggerGUI extends JFrame {
    private JTextArea outputArea;
    private ScriptableDebugger debugger;
    private JPanel buttonPanel;

    public DebuggerGUI() {
        super("Java Debugger");
        initializeComponents();
        redirectSystemOut();
        setupDebugger();
    }

    private void initializeComponents() {
        // Configuration de la fenêtre principale
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLayout(new BorderLayout());

        // Zone de texte pour la sortie
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(outputArea);
        add(scrollPane, BorderLayout.CENTER);

        // Panel pour les boutons
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        add(buttonPanel, BorderLayout.NORTH);

        // Création des boutons pour chaque commande
        createCommandButtons();
    }

    private void createCommandButtons() {
        // Commandes de contrôle
        addCommandButton("Step", "step");
        addCommandButton("Step Over", "step-over");
        addCommandButton("Continue", "continue");
        addCommandButton("Back", "back");

        // Commandes d'information
        addCommandButton("Frame", "frame");
        addCommandButton("Stack", "stack");
        addCommandButton("Method", "method");
        addCommandButton("Temporaries", "temporaries");

        // Commandes d'objet
        addCommandButton("Receiver", "receiver");
        addCommandButton("Sender", "sender");
        addCommandButton("Arguments", "arguments");
        addCommandButton("Receiver Variables", "receiver-variables");

        // Commandes de breakpoint
        addCommandButton("Breakpoints", "breakpoints");
    }

    private void addCommandButton(String label, String command) {
        JButton button = new JButton(label);
        button.addActionListener(e -> {
            if (debugger != null) {
                outputArea.append("\n"); // Ajoute une ligne vide pour la lisibilité
                debugger.executeGuiCommand(command);
            }
        });
        buttonPanel.add(button);
    }

    private void redirectSystemOut() {
        // Redirection de la sortie standard vers la zone de texte
        PrintStream printStream = new PrintStream(new CustomOutputStream(outputArea));
        System.setOut(printStream);
        System.setErr(printStream);
    }

    private void setupDebugger() {
        debugger = new ScriptableDebugger();

        // Gestion de la fermeture propre du debugger
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (debugger != null) {
                    debugger.dispose();
                }
            }
        });
    }

    public void startDebugging(Class<?> debugClass) {
        // Démarre le debugger dans un thread séparé pour ne pas bloquer l'interface
        new Thread(() -> {
            try {
                debugger.attachTo(debugClass);
            } catch (Exception e) {
                System.err.println("Erreur lors de l'attachement au debugger: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // Classe interne pour rediriger la sortie vers le JTextArea
    private static class CustomOutputStream extends OutputStream {
        private JTextArea textArea;
        private StringBuilder buffer;

        public CustomOutputStream(JTextArea textArea) {
            this.textArea = textArea;
            this.buffer = new StringBuilder();
        }

        @Override
        public void write(int b) {
            buffer.append((char) b);
            if (b == '\n') {
                final String text = buffer.toString();
                SwingUtilities.invokeLater(() -> {
                    textArea.append(text);
                    textArea.setCaretPosition(textArea.getDocument().getLength());
                });
                buffer.setLength(0);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DebuggerGUI gui = new DebuggerGUI();
            gui.setVisible(true);
        });
    }
}