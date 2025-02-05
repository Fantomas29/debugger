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
break core.TestDebugger.java 10   # Dans processIteration
break-once core.TestDebugger.java 22   # Dans helperMethod
breakpoints
continue # jusqu'a ligne 10
continue # jusqu'a ligne 22
arguments
continue # jusqu'a ligne 10
breakpoints # Vérifie que le breakpoint-once est supprimé
break-on-count core.TestDebugger.java 14 2
continue # ligne 10 dernier tour
receiver-variables
continue # ligne 14 après 2 passages
breakpoints
receiver-variables
continue
receiver-variables
```



### 2. Test des Commandes d'Information
À l'arrêt sur un breakpoint :
```bash
frame
stack
method
breakpoints
```

### 3. Test du Contrôle d'Exécution
```bash
step
step-over
continue
```

### 4. Test des Variables et Contexte
```bash
receiver
receiver-variables  # Montre counter
temporaries
arguments
```