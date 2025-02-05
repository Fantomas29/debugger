package dbg.commands.breakpoint;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.BreakpointRequest;
import dbg.commands.interfaces.DebugCommand;

import java.util.List;

public class BreakpointsCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;

    public BreakpointsCommand(VirtualMachine vm, LocatableEvent event) {
        this.vm = vm;
        this.event = event;
    }

    @Override
    public Object execute() {
        List<BreakpointRequest> breakpoints = vm.eventRequestManager().breakpointRequests();

        if (breakpoints.isEmpty()) {
            System.out.println("No active breakpoints");
            return null;
        }

        System.out.println("=== Active Breakpoints ===");
        for (BreakpointRequest bp : breakpoints) {
            Location loc = bp.location();
            String status = bp.isEnabled() ? "enabled" : "disabled";
            try {
                System.out.printf("File: %s, Line: %d, Method: %s, Status: %s%n",
                        loc.sourcePath(),
                        loc.lineNumber(),
                        loc.method().name(),
                        status);
            } catch (AbsentInformationException e) {
                System.out.printf("Location: %s, Status: %s%n",
                        loc,
                        status);
            }
        }

        return breakpoints;
    }
}