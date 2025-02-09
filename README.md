# Java Debugger avec Interface Graphique

Ce projet est un débogueur Java implémenté avec une interface graphique Swing. Le choix de Swing a été fait pour rester dans l'écosystème Java et pour sa simplicité d'implémentation tout en gardant une cohérence avec le projet de débogage.

## Comment utiliser le débogueur

### Lancement
Pour lancer le débogueur, exécutez la classe `dbg.core.JDISimpleDebugger`. Deux classes de test sont fournies :
- `TestDebugger.java` : Exemple avec des boucles et méthodes
- `JDISimpleDebuggee.java` : Exemple manipulant une ArrayList

### Adapter pour votre code
Pour déboguer votre propre classe, trois modifications sont nécessaires :

1. Dans `StepBackManager.java`, modifiez la méthode `isMainClass()` pour reconnaître votre classe :
```java
public boolean isMainClass() {
    return className.contains("VotreClasse");  // Remplacez "TestDebugger"
}
```

2. Dans `JDISimpleDebugger.java`, modifiez la classe à déboguer :
```java
public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
        try {
            DebuggerGUI gui = new DebuggerGUI();
            gui.setVisible(true);
            gui.startDebugging(core.VotreClasse.class);  // Remplacez core.TestDebugger.class
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du debugger: " + e.getMessage());
            e.printStackTrace();
        }
    });
}
```

3. Dans `DebuggerGUI.java`, les boîtes de dialogue des breakpoints ont comme valeur par défaut "JDISimpleDebuggee.java". Modifiez cette valeur pour correspondre à votre fichier :
```java
JTextField fileField = new JTextField("core.VotreClasse.java");  // Dans les méthodes showBreakpointDialog et showBreakOnCountDialog
```

### Position du breakpoint initial
Par défaut, le débogueur place le premier point d'arrêt à la ligne 9. Pour modifier cette ligne, dans `ScriptableDebugger.java`, modifiez la variable :
```java
private int initialBreakpointLine = 9;  // Changez cette valeur
```

## Améliorations possibles

1. Intégration du code source dans l'interface graphique pour une meilleure visualisation et interaction
2. Gestion des instructions non déterministes pour un débogage plus robuste