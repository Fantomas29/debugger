package dbg.timetravel;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import java.util.*;

public class StepByStepDebugger {
    private final VirtualMachine vm;
    private final List<ExecutionState> states = new ArrayList<>();
    private int currentPosition = -1;

    public StepByStepDebugger(VirtualMachine vm) {
        this.vm = vm;
    }

    // Classe représentant un état d'exécution
    private static class ExecutionState {
        final Map<String, Value> variables;
        final Location location;
        final int lineNumber;

        ExecutionState(Map<String, Value> variables, Location location, int lineNumber) {
            this.variables = variables;
            this.location = location;
            this.lineNumber = lineNumber;
        }
    }

    // Enregistre un état lors d'une étape d'exécution
    public void recordStep(LocatableEvent event) {
        if (event == null) return;

        try {
            ThreadReference thread = event.thread();
            if (thread != null && thread.frameCount() > 0) {
                StackFrame frame = thread.frame(0);
                Map<String, Value> vars = new HashMap<>();

                try {
                    // Capture des variables locales
                    for (LocalVariable var : frame.visibleVariables()) {
                        vars.put(var.name(), frame.getValue(var));
                    }

                    // Capture des variables d'instance
                    ObjectReference thisObject = frame.thisObject();
                    if (thisObject != null) {
                        for (Field field : thisObject.referenceType().allFields()) {
                            vars.put("this." + field.name(), thisObject.getValue(field));
                        }
                    }
                } catch (AbsentInformationException e) {
                    // Si pas d'info de debug disponible, on continue avec un map vide
                }

                states.add(new ExecutionState(vars, event.location(),
                        event.location().lineNumber()));
                currentPosition = states.size() - 1;

                System.out.printf("Recorded step at line %d (state %d)%n",
                        event.location().lineNumber(), currentPosition);
            }
        } catch (IncompatibleThreadStateException e) {
            // Ignore si le thread n'est pas dans un état compatible
        } catch (Exception e) {
            System.out.println("Error recording step: " + e.getMessage());
        }
    }

    // Retourne en arrière d'un pas
    public void stepBack() {
        if (currentPosition <= 0) {
            System.out.println("Already at the beginning of recorded execution");
            return;
        }

        try {
            ExecutionState previousState = states.get(--currentPosition);
            System.out.printf("Stepped back to line %d (state %d)%n",
                    previousState.lineNumber, currentPosition);
        } catch (Exception e) {
            System.out.println("Error stepping back: " + e.getMessage());
        }
    }
}