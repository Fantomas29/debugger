// StepBackManager.java
package dbg.timetravel;

import com.sun.jdi.*;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import dbg.core.ScriptableDebugger;

import java.util.ArrayList;
import java.util.List;

public class StepBackManager {
    private final List<ExecutionState> executionHistory;
    private int currentPosition;
    private final List<BreakpointRequest> savedBreakpoints;
    private boolean isStepBackInProgress;
    private ExecutionState targetState;
    private VirtualMachine vm;
    private final ScriptableDebugger debugger;

    public StepBackManager(ScriptableDebugger debugger, VirtualMachine initialVM) {
        this.debugger = debugger;
        this.vm = initialVM;
        this.executionHistory = new ArrayList<>();
        this.savedBreakpoints = new ArrayList<>();
        this.currentPosition = -1;
        this.isStepBackInProgress = false;
    }

    public void updateVM(VirtualMachine newVM) {
        this.vm = newVM;
        System.out.println("[Debug] Updated VM reference in StepBackManager");

        // Important : configurer le breakpoint initial immédiatement après la mise à jour de la VM
        if (isStepBackInProgress && targetState != null) {
            configureInitialBreakpoint();
        }
    }

    public void recordState(LocatableEvent event) {
        if (isStepBackInProgress) {
            return;
        }

        ExecutionState state = new ExecutionState(
                event.location().declaringType().name(),
                event.location().lineNumber(),
                event.thread().name(),
                event.location().method().name()
        );

        executionHistory.add(state);
        currentPosition = executionHistory.size() - 1;
        System.out.println("[Debug] Recording state " + currentPosition + ": " + state);
    }

    public void stepBack() {
        if (currentPosition <= 0) {
            System.out.println("Already at the beginning of execution");
            return;
        }

        try {
            if (vm == null) {
                System.out.println("[Debug] Error: VM is null before step back");
                return;
            }

            System.out.println("[Debug] Starting step back from position: " + currentPosition);

            // Save current breakpoints
            saveBreakpoints();

            // Set target state
            currentPosition--;
            targetState = executionHistory.get(currentPosition);
            isStepBackInProgress = true;

            System.out.println("[Debug] Step back to: " + targetState);

            // Restart VM and set initial breakpoint
            debugger.restartVM();

        } catch (Exception e) {
            System.out.println("[Debug] Step back failed: " + e);
            e.printStackTrace();
            reset();
        }
    }

    private void saveBreakpoints() {
        if (vm == null) {
            System.out.println("[Debug] Error: VM is null when trying to save breakpoints");
            return;
        }

        savedBreakpoints.clear();
        for (BreakpointRequest bp : vm.eventRequestManager().breakpointRequests()) {
            if (bp.isEnabled()) {
                savedBreakpoints.add(bp);
                bp.disable();
                System.out.println("[Debug] Saved and disabled breakpoint at: " + bp.location());
            }
        }
    }

    public void restoreBreakpoints() {
        if (vm == null || !vm.canBeModified()) {
            System.out.println("[Debug] Cannot restore breakpoints - VM not available");
            return;
        }

        try {
            for (BreakpointRequest bp : savedBreakpoints) {
                bp.enable();
                System.out.println("[Debug] Restored breakpoint at: " + bp.location());
            }
            savedBreakpoints.clear();
        } catch (VMDisconnectedException e) {
            System.out.println("[Debug] VM disconnected while restoring breakpoints");
        } catch (Exception e) {
            System.out.println("[Debug] Error restoring breakpoints: " + e.getMessage());
        }
    }

    public boolean isAtTargetState(Location location) {
        if (targetState == null) return false;

        return location.declaringType().name().equals(targetState.className()) &&
                location.lineNumber() == targetState.lineNumber() &&
                location.method().name().equals(targetState.methodName());
    }

    public void configureInitialBreakpoint() {
        if (!isStepBackInProgress || targetState == null || vm == null) {
            System.out.println("[Debug] Cannot configure initial breakpoint - invalid state");
            return;
        }

        try {
            System.out.println("[Debug] Attempting to set breakpoint for " + targetState.className() +
                    " at line " + targetState.lineNumber());

            ReferenceType targetType = null;
            for (ReferenceType refType : vm.allClasses()) {
                if (refType.name().equals(targetState.className())) {
                    targetType = refType;
                    break;
                }
            }

            if (targetType == null) {
                System.out.println("[Debug] Class not found, cannot set breakpoint");
                return;
            }

            List<Location> locations = targetType.locationsOfLine(targetState.lineNumber());
            if (locations.isEmpty()) {
                System.out.println("[Debug] No location found for line " + targetState.lineNumber());
                return;
            }

            Location targetLocation = locations.get(0);
            BreakpointRequest bpReq = vm.eventRequestManager().createBreakpointRequest(targetLocation);
            bpReq.putProperty("isStepBackBreakpoint", true);
            bpReq.enable();

            System.out.println("[Debug] Successfully set breakpoint at " + targetLocation);

        } catch (AbsentInformationException e) {
            System.out.println("[Debug] Cannot set breakpoint - missing debug information: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("[Debug] Error setting breakpoint: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean isStepBackInProgress() {
        return isStepBackInProgress;
    }

    public void reset() {
        targetState = null;
        isStepBackInProgress = false;
        System.out.println("[Debug] Reset step back state");
    }

    public boolean isWaitingForBreakpoint() {
        return isStepBackInProgress && targetState != null;
    }

    public void handleBreakpointHit(BreakpointEvent event) {
        if (!isStepBackInProgress) {
            return;
        }

        try {
            // D'abord, on désactive le step back
            isStepBackInProgress = false;

            // Supprimons le breakpoint temporaire de step back AVANT de restaurer les autres
            EventRequest request = event.request();
            if (request != null && request.isEnabled()) {
                request.disable();
                vm.eventRequestManager().deleteEventRequest(request);
            }

            // Ensuite seulement on restaure les breakpoints
            restoreBreakpoints();

            System.out.println("[Debug] Step back completed, restored original breakpoints");
        } catch (VMDisconnectedException e) {
            System.out.println("[Debug] VM disconnected while handling breakpoint");
        } catch (Exception e) {
            System.out.println("[Debug] Error handling breakpoint hit: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
