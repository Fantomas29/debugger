package dbg.timetravel;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.StepRequest;
import dbg.core.ScriptableDebugger;

import java.util.ArrayList;
import java.util.List;

public class StepBackManager {
    private final ScriptableDebugger debugger;
    private static List<LocationInfo> executionHistory = new ArrayList<>();
    private static int historyPosition = -1;
    public boolean isReplaying = false;
    private int targetLine = -1;

    public static class LocationInfo {
        public final String className;
        final String methodName;
        public final int lineNumber;
        final String type;

        LocationInfo(Location location, String type) {
            this.className = location.declaringType().name();
            this.methodName = location.method().name();
            this.lineNumber = location.lineNumber();
            this.type = type;
        }

        @Override
        public String toString() {
            return String.format("%s.%s():%d (%s)",
                    className, methodName, lineNumber, type);
        }

        public boolean isMainClass() {
            return className.contains("JDISimpleDebuggee");
        }
    }

    public StepBackManager(ScriptableDebugger debugger, VirtualMachine vm) {
        this.debugger = debugger;
    }

    public void recordStep(LocatableEvent event) {
        if (event != null && event.location() != null) {
            Location location = event.location();
            LocationInfo currentLoc = new LocationInfo(location, determineStepType(event));

            if (isReplaying) {
                if (location.lineNumber() == targetLine && currentLoc.isMainClass()) {
                    isReplaying = false;
                    targetLine = -1;
                }
                return;
            }

            // Tronquer l'historique si nécessaire
            if (historyPosition < executionHistory.size() - 1) {
                while (executionHistory.size() > historyPosition + 1) {
                    executionHistory.remove(executionHistory.size() - 1);
                }
            }

            // Ajouter la nouvelle position
            executionHistory.add(currentLoc);
            historyPosition = executionHistory.size();
        }
    }

    private String determineStepType(LocatableEvent event) {
        if (event.request() instanceof StepRequest) {
            StepRequest stepRequest = (StepRequest) event.request();
            Object type = stepRequest.getProperty("stepType");
            if (type != null && type.equals("STEP_OVER")) {
                return "STEP_OVER";
            }
        }
        return "STEP";
    }

    public LocationInfo getPreviousLocation() {
        if (historyPosition > 0 && historyPosition <= executionHistory.size()) {
            LocationInfo prevLoc = executionHistory.get(historyPosition - 1);
            return prevLoc;
        }
        return null;
    }

    public void stepBack() {

        if (historyPosition <= 0) {
            System.out.println("Already at the beginning of execution");
            return;
        }

        try {
            LocationInfo targetLocation = getPreviousLocation();
            if (targetLocation == null) {
                System.out.println("No previous position found");
                return;
            }

            historyPosition--; // Décrémenter avant le restart

            targetLine = targetLocation.lineNumber;
            debugger.setInitialBreakpoint(targetLine);
            isReplaying = true;

            debugger.restartVM();

        } catch (Exception e) {
            e.printStackTrace();
            clearHistory();
        }
    }

    public void clearHistory() {
        executionHistory.clear();
        historyPosition = -1;
        isReplaying = false;
        targetLine = -1;
    }
}