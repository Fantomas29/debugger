# Guide de Test du Debugger

Ce guide vous permet de tester toutes les fonctionnalités du debugger avec un cas pratique.

## Programme de Test

```java
package dbg.core;

public class TestDebugger {
    private int counter = 0;
    
    public void methodWithLoop() {
        System.out.println("Début de methodWithLoop");
        for(int i = 0; i < 3; i++) {
            counter++;
            processIteration(i);
        }
        System.out.println("Fin de methodWithLoop");
    }

    private void processIteration(int value) {
        String message = "Traitement de l'itération " + value;
        System.out.println(message);
        helperMethod(value);
    }

    private void helperMethod(int value) {
        counter = counter + value;
        System.out.println("Counter = " + counter);
    }

    public static void main(String[] args) {
        TestDebugger test = new TestDebugger();
        test.methodWithLoop();
    }
}
```

## Scénarios de Test

### 1. Test des Breakpoints
```bash
break core.TestDebugger.java 16   # Dans processIteration
break-once core.TestDebugger.java 22   # Dans helperMethod
breakpoints
continue # jusqu'a ligne 16
continue # jusqu'a ligne 22
continue # jusqu'a ligne 16
breakpoints # Vérifie que le breakpoint-once est supprimé
```



### 2. Test des Commandes d'Information
À l'arrêt sur un breakpoint :
```bash
frame  # Affiche la frame courante
stack  # Montre la pile d'appels
method  # Affiche les infos de la méthode courante
breakpoints  # Liste les breakpoints
```

### 3. Test du Contrôle d'Exécution
```bash
step  # Entre dans helperMethod
step-over  # Exécute la ligne sans entrer dans la méthode
continue  # Continue jusqu'au prochain breakpoint
```

### 4. Test des Variables et Contexte
```bash
receiver  # Affiche l'objet courant
receiver-variables  # Montre counter
temporaries  # Affiche les variables locales
arguments  # Montre les arguments de la méthode
```