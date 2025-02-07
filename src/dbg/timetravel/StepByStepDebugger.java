package dbg.timetravel;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import dbg.core.ScriptableDebugger;
import java.util.*;

public class StepByStepDebugger {
    private final List<Location> executionHistory = new ArrayList<>();
    private int currentPosition = -1;
    private final List<BreakpointRequest> savedBreakpoints = new ArrayList<>();
    private boolean isStepBackInProgress = false;
    private VirtualMachine vm;
    private final ScriptableDebugger scriptableDebugger;

    // Pour le step back
    private int targetLine = -1;
    private String targetClass = null;
    private int stepsNeeded = -1;
    private int currentStep = 0;
    private boolean initialBreakpointHit = false;


    public StepByStepDebugger(VirtualMachine vm, ScriptableDebugger debugger) {
        this.vm = vm;
        this.scriptableDebugger = debugger;
    }

    public void updateVM(VirtualMachine newVM) {
        System.out.println("[Debug] Updating VM reference in StepByStepDebugger");
        this.vm = newVM;
    }

    public void recordStep(LocatableEvent event) {
        if (isStepBackInProgress) {
            if (initialBreakpointHit) {
                currentStep++;
                System.out.println("[Debug] Step back progress: step " + currentStep + "/" + stepsNeeded);
                System.out.println("[Debug] Current line: " + event.location().lineNumber());
            }
            return;
        }

        Location location = event.location();
        executionHistory.add(location);
        currentPosition = executionHistory.size() - 1;
        System.out.println("[Debug] Recording step " + currentPosition +
                " at line " + location.lineNumber() +
                " in " + location.method().name());
        System.out.println("[Debug] History size: " + executionHistory.size());
    }

    public boolean isAtTargetLocation(Location location) {
        boolean isTarget = location.lineNumber() == targetLine &&
                location.declaringType().name().equals(targetClass);
        if (isTarget) {
            System.out.println("[Debug] Target location reached!");
        }
        return isTarget;
    }

    public boolean shouldContinueStepping() {
        return currentStep < stepsNeeded;
    }

    public void markInitialBreakpointHit() {
        System.out.println("[Debug] Initial breakpoint hit marked");
        this.initialBreakpointHit = true;
    }

    public boolean hasHitInitialBreakpoint() {
        return initialBreakpointHit;
    }

    public void setStepBackInProgress(boolean inProgress) {
        System.out.println("[Debug] Setting step back in progress: " + inProgress);
        this.isStepBackInProgress = inProgress;
    }

    public boolean isStepBackInProgress() {
        return isStepBackInProgress;
    }

    public void reset() {
        System.out.println("[Debug] Resetting step back state");
        targetLine = -1;
        targetClass = null;
        stepsNeeded = -1;
        currentStep = 0;
        isStepBackInProgress = false;
        initialBreakpointHit = false;
    }

    public void stepBack() {
        if (currentPosition <= 0) {
            System.out.println("Déjà au début de l'exécution");
            return;
        }

        try {
            // Sauvegarde les breakpoints existants
            savedBreakpoints.clear();
            savedBreakpoints.addAll(vm.eventRequestManager().breakpointRequests());
            for (BreakpointRequest bp : savedBreakpoints) {
                System.out.println("[Debug] Saving and disabling breakpoint at: " + bp.location());
                bp.disable();
            }

            // Calcule la position cible
            Location currentLocation = executionHistory.get(currentPosition - 1);
            targetLine = currentLocation.lineNumber();
            targetClass = currentLocation.declaringType().name();
            stepsNeeded = currentPosition - 1;
            currentStep = 0;
            initialBreakpointHit = false;

            System.out.println("[Debug] Preparing step back:");
            System.out.println("[Debug] - Target class: " + targetClass);
            System.out.println("[Debug] - Target line: " + targetLine);
            System.out.println("[Debug] - Current position: " + currentPosition);
            System.out.println("[Debug] - Steps needed: " + stepsNeeded);

            // Met à jour la position
            currentPosition--;
            isStepBackInProgress = true;

            // Redémarre la VM
            scriptableDebugger.restartVM();

        } catch (Exception e) {
            System.out.println("[Debug] Step back failed: " + e);
            e.printStackTrace();
            reset();
        }
    }

    public List<BreakpointRequest> getSavedBreakpoints() {
        return savedBreakpoints;
    }

    public void clearSavedBreakpoints() {
        savedBreakpoints.clear();
    }

}