package dbg.timetravel;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import dbg.core.ScriptableDebugger;

import java.util.ArrayList;
import java.util.List;

public class StepBackManager {
    private final ScriptableDebugger debugger;
    private static List<LocationInfo> executionHistory = new ArrayList<>();
    private static int historyPosition = -1;
    private boolean isReplaying = false;

    private static class LocationInfo {
        final String className;
        final String methodName;
        final int lineNumber;

        LocationInfo(Location location) {
            this.className = location.declaringType().name();
            this.methodName = location.method().name();
            this.lineNumber = location.lineNumber();
        }

        @Override
        public String toString() {
            return String.format("%s.%s():%d", className, methodName, lineNumber);
        }
    }

    public StepBackManager(ScriptableDebugger debugger, VirtualMachine vm) {
        this.debugger = debugger;
        if (executionHistory.isEmpty()) {
            historyPosition = -1;
        }
    }

    public void recordStep(LocatableEvent event) {
        if (event != null && event.location() != null) {
            Location location = event.location();
            LocationInfo currentLoc = new LocationInfo(location);

            if (!isReplaying) {
                // Si nous ne sommes pas en mode replay, ajoutez à l'historique
                if (historyPosition < executionHistory.size() - 1) {
                    // Nous sommes revenus en arrière, donc supprimez l'historique futur
                    while (executionHistory.size() > historyPosition + 1) {
                        executionHistory.remove(executionHistory.size() - 1);
                    }
                }
                executionHistory.add(currentLoc);
                historyPosition = executionHistory.size() - 1;
                System.out.println("[Debug] Recording step at line " + location.lineNumber() +
                        " (position " + historyPosition + ")");
            }
        }
    }

    public void stepBack(LocatableEvent event) {
        if (historyPosition <= 0) {
            System.out.println("Already at the beginning of execution");
            return;
        }

        try {
            historyPosition--;
            LocationInfo targetLocation = executionHistory.get(historyPosition);

            // Configure le point d'arrêt initial pour la nouvelle exécution
            debugger.setInitialBreakpoint(targetLocation.lineNumber);

            isReplaying = true;
            System.out.println("[Debug] Setting initial breakpoint at line " + targetLocation.lineNumber +
                    " (going to position " + historyPosition + ")");

            debugger.restartVM();

        } catch (Exception e) {
            System.out.println("[Debug] Error in step back: " + e.getMessage());
            e.printStackTrace();
            // En cas d'erreur, réinitialiser à un état cohérent
            clearHistory();
        }
    }

    public void clearHistory() {
        executionHistory.clear();
        historyPosition = -1;
        isReplaying = false;
    }
}