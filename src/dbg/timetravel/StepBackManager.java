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
        final String type;  // "STEP", "STEP_OVER", ou "INITIAL"

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
        if (executionHistory.isEmpty()) {
            historyPosition = -1;
        }
    }

    public void recordStep(LocatableEvent event) {
        if (event != null && event.location() != null) {
            String stepType = "STEP";
            if (event.request() instanceof StepRequest) {
                StepRequest stepRequest = (StepRequest) event.request();
                Object type = stepRequest.getProperty("stepType");
                if (type != null && type.equals("STEP_OVER")) {
                    stepType = "STEP_OVER";
                }
            }

            Location location = event.location();
            LocationInfo currentLoc = new LocationInfo(location, stepType);

            // Ne pas enregistrer si nous sommes en train de rejouer
            if (isReplaying) {
                if (location.lineNumber() == targetLine && currentLoc.isMainClass()) {
                    isReplaying = false;
                    targetLine = -1;
                }
                return;
            }

            if (historyPosition < executionHistory.size() - 1) {
                while (executionHistory.size() > historyPosition + 1) {
                    executionHistory.remove(executionHistory.size() - 1);
                }
            }

            // N'enregistrer que si c'est notre classe principale ou si c'est différent du dernier enregistrement
            if (executionHistory.isEmpty() || currentLoc.isMainClass() ||
                    !currentLoc.equals(executionHistory.get(historyPosition))) {
                executionHistory.add(currentLoc);
                historyPosition = executionHistory.size();
            }

            System.out.println("[Debug] Recording step at: " + currentLoc);
        }
    }

    public LocationInfo getPreviousLocation() {
        if (historyPosition > 0 && historyPosition <= executionHistory.size()) {
            // Trouver la dernière position dans notre classe principale
            for (int i = historyPosition - 1; i >= 0; i--) {
                LocationInfo loc = executionHistory.get(i);
                if (loc.isMainClass()) {
                    System.out.println("[Debug] Getting previous location: " + loc);
                    return loc;
                }
            }
        }
        return null;
    }

    public void stepBack() {
        if (historyPosition <= 0) {
            System.out.println("Already at the beginning of execution");
            return;
        }

        try {
            // Chercher la position précédente dans notre classe principale
            LocationInfo targetLocation = null;
            while (historyPosition > 0) {
                historyPosition--;
                LocationInfo loc = executionHistory.get(historyPosition);
                if (loc.isMainClass()) {
                    targetLocation = loc;
                    break;
                }
            }

            if (targetLocation == null) {
                System.out.println("No previous position found in main class");
                return;
            }

            System.out.println("[Debug] Stepping back to: " + targetLocation);
            targetLine = targetLocation.lineNumber;
            debugger.setInitialBreakpoint(targetLine);
            isReplaying = true;

            debugger.restartVM();

        } catch (Exception e) {
            System.out.println("[Debug] Error in step back: " + e.getMessage());
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