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
        JPanel controlPanel = new JPanel(new FlowLayout());
        JPanel breakpointPanel = new JPanel(new FlowLayout());
        JPanel infoPanel = new JPanel(new FlowLayout());

        // Panel du haut pour les contrôles principaux
        addCommandButton(controlPanel, "Step", "step");
        addCommandButton(controlPanel, "Step Over", "step-over");
        addCommandButton(controlPanel, "Continue", "continue");
        addCommandButton(controlPanel, "Back", "back");

        // Panel du milieu pour les breakpoints
        addBreakpointButton(breakpointPanel, "Break", "break");
        addBreakpointButton(breakpointPanel, "Break Once", "break-once");
        addBreakpointButton(breakpointPanel, "Break on Count", "break-on-count");
        addBreakpointButton(breakpointPanel, "Break Before Method", "break-before-method");
        addCommandButton(breakpointPanel, "List Breakpoints", "breakpoints");

        // Panel du bas pour les infos
        addCommandButton(infoPanel, "Frame", "frame");
        addCommandButton(infoPanel, "Stack", "stack");
        addCommandButton(infoPanel, "Method", "method");
        addCommandButton(infoPanel, "Temporaries", "temporaries");
        addCommandButton(infoPanel, "Receiver", "receiver");
        addCommandButton(infoPanel, "Sender", "sender");
        addCommandButton(infoPanel, "Arguments", "arguments");
        addCommandButton(infoPanel, "Receiver Variables", "receiver-variables");

        // Organisation verticale des panels
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.add(controlPanel);
        buttonPanel.add(breakpointPanel);
        buttonPanel.add(infoPanel);
    }

    // Ajoute un bouton de commande avec le label et la commande associée
    private void addCommandButton(JPanel panel, String label, String command) {
        JButton button = new JButton(label);
        button.addActionListener(e -> {
            if (debugger != null) {
                outputArea.append("\n");
                debugger.executeGuiCommand(command);
            }
        });
        panel.add(button);
    }

    // Ajoute un bouton de breakpoint avec le label et la commande associée
    private void addBreakpointButton(JPanel panel, String label, String command) {
        JButton button = new JButton(label);
        button.addActionListener(e -> {
            if (debugger != null) {
                switch(command) {
                    case "break":
                    case "break-once":
                        showBreakpointDialog(command);
                        break;
                    case "break-on-count":
                        showBreakOnCountDialog();
                        break;
                    case "break-before-method":
                        showBreakBeforeMethodDialog();
                        break;
                }
            }
        });
        panel.add(button);
    }

    // Affiche une boîte de dialogue pour définir un breakpoint
    private void showBreakpointDialog(String command) {
        JDialog dialog = new JDialog(this, "Set Breakpoint", true);
        dialog.setLayout(new GridLayout(3, 2, 5, 5));

        JTextField fileField = new JTextField("core.TestDebugger.java");
        JTextField lineField = new JTextField();

        dialog.add(new JLabel("File:"));
        dialog.add(fileField);
        dialog.add(new JLabel("Line:"));
        dialog.add(lineField);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            try {
                int line = Integer.parseInt(lineField.getText());
                String cmd = String.format("%s %s %d", command, fileField.getText(), line);
                debugger.executeGuiCommand(cmd);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter a valid line number");
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.add(okButton);
        dialog.add(cancelButton);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // Affiche une boîte de dialogue pour définir un breakpoint sur un compteur
    private void showBreakOnCountDialog() {
        JDialog dialog = new JDialog(this, "Set Break on Count", true);
        dialog.setLayout(new GridLayout(4, 2, 5, 5));

        JTextField fileField = new JTextField("core.TestDebugger.java");
        JTextField lineField = new JTextField();
        JTextField countField = new JTextField();

        dialog.add(new JLabel("File:"));
        dialog.add(fileField);
        dialog.add(new JLabel("Line:"));
        dialog.add(lineField);
        dialog.add(new JLabel("Count:"));
        dialog.add(countField);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            try {
                int line = Integer.parseInt(lineField.getText());
                int count = Integer.parseInt(countField.getText());
                String cmd = String.format("break-on-count %s %d %d",
                        fileField.getText(), line, count);
                debugger.executeGuiCommand(cmd);
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid numbers");
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.add(okButton);
        dialog.add(cancelButton);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // Affiche une boîte de dialogue pour définir un breakpoint avant une méthode
    private void showBreakBeforeMethodDialog() {
        JDialog dialog = new JDialog(this, "Break Before Method", true);
        dialog.setLayout(new GridLayout(2, 2, 5, 5));

        JTextField methodField = new JTextField();

        dialog.add(new JLabel("Method name:"));
        dialog.add(methodField);

        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            String method = methodField.getText().trim();
            if (!method.isEmpty()) {
                String cmd = String.format("break-before-method %s", method);
                debugger.executeGuiCommand(cmd);
                dialog.dispose();
            } else {
                JOptionPane.showMessageDialog(dialog, "Please enter a method name");
            }
        });

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dialog.dispose());

        dialog.add(okButton);
        dialog.add(cancelButton);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    // Redirige la sortie standard vers la zone de texte
    private void redirectSystemOut() {
        // Redirection de la sortie standard vers la zone de texte
        PrintStream printStream = new PrintStream(new CustomOutputStream(outputArea));
        System.setOut(printStream);
        System.setErr(printStream);
    }

    // Initialise le debugger
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

    // Démarre le debugger avec la classe spécifiée
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