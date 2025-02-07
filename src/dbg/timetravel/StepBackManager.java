package dbg.timetravel;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import dbg.core.ScriptableDebugger;

import java.util.ArrayList;
import java.util.List;

public class StepBackManager {
    private final ScriptableDebugger debugger;
    private final List<Location> executionHistory;
    private static List<LocationInfo> persistentHistory = new ArrayList<>();
    private int currentPosition;
    private boolean isReplaying = false;
    private int replayTargetPosition = -1;  // Nouvelle variable pour suivre la position cible

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
        this.executionHistory = new ArrayList<>();
        if (persistentHistory.isEmpty()) {
            this.currentPosition = -1;
            this.isReplaying = false;
        } else {
            this.currentPosition = persistentHistory.size();
        }
    }

    public void recordStep(LocatableEvent event) {
        if (event != null && event.location() != null) {
            Location location = event.location();
            LocationInfo currentLoc = new LocationInfo(location);

            if (isReplaying) {
                // Si on est en mode replay, on vérifie si on a dépassé notre position cible
                if (currentPosition > replayTargetPosition) {
                    // On a dépassé la position cible, on commence à réenregistrer
                    isReplaying = false;
                    // On nettoie l'historique après la position actuelle
                    while (persistentHistory.size() > currentPosition) {
                        persistentHistory.remove(persistentHistory.size() - 1);
                    }
                    persistentHistory.add(currentLoc);
                    currentPosition = persistentHistory.size();
                    System.out.println("[Debug] Recording new step at line " + location.lineNumber());
                }
            } else {
                persistentHistory.add(currentLoc);
                currentPosition = persistentHistory.size();
                System.out.println("[Debug] Recording step at line " + location.lineNumber());
            }
        }
    }

    public void stepBack(LocatableEvent event) {
        if (currentPosition <= 0) {
            System.out.println("Already at the beginning of execution");
            return;
        }

        try {
            LocationInfo targetLocation = persistentHistory.get(currentPosition -1);

            // Configure le point d'arrêt initial pour la nouvelle exécution
            debugger.setInitialBreakpoint(targetLocation.lineNumber);

            // Met à jour la position courante et la cible
            currentPosition--;
            replayTargetPosition = currentPosition;
            isReplaying = true;

            System.out.println("[Debug] Setting initial breakpoint at line " + targetLocation.lineNumber);

            debugger.restartVM();

        } catch (Exception e) {
            System.out.println("[Debug] Error in step back: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void clearHistory() {
        persistentHistory.clear();
        currentPosition = -1;
        isReplaying = false;
        replayTargetPosition = -1;
    }
}