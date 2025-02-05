package dbg.commands.breakpoint;

import com.sun.jdi.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import dbg.commands.interfaces.DebugCommand;

public class BreakBeforeMethodCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final String methodName;

    public BreakBeforeMethodCommand(VirtualMachine vm, String methodName) {
        this.vm = vm;
        this.methodName = methodName;
    }

    @Override
    public Object execute() {
        int breakpointsSet = 0;

        for (ReferenceType refType : vm.allClasses()) {
            for (Method method : refType.methods()) {
                if (method.name().equalsIgnoreCase(methodName)) {
                    try {
                        Location methodStart = method.location();
                        BreakpointRequest bpReq = vm.eventRequestManager()
                                .createBreakpointRequest(methodStart);
                        bpReq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                        bpReq.enable();
                        breakpointsSet++;

                        System.out.printf("Breakpoint set at start of method %s in class %s%n",
                                methodName, refType.name());
                    } catch (Exception e) {
                        System.out.printf("Could not set breakpoint for method %s in class %s: %s%n",
                                methodName, refType.name(), e.getMessage());
                    }
                }
            }
        }

        if (breakpointsSet == 0) {
            System.out.printf("No methods named '%s' found%n", methodName);
            return null;
        }

        System.out.printf("Set %d breakpoint(s) for method %s%n", breakpointsSet, methodName);
        return breakpointsSet;
    }
}