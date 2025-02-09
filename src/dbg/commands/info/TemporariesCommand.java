package dbg.commands.info;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import dbg.commands.interfaces.DebugCommand;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TemporariesCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;

    public TemporariesCommand(VirtualMachine vm, LocatableEvent event) {
        this.vm = vm;
        this.event = event;
    }

    @Override
    public Object execute() {
        try {
            ThreadReference thread = event.thread();
            if (thread.frameCount() > 0) {
                StackFrame frame = thread.frame(0);
                List<LocalVariable> vars = frame.visibleVariables();

                Map<String, Value> temporaries = new LinkedHashMap<>();
                System.out.println("=== Variables temporaires ===");
                for (LocalVariable var : vars) {
                    Value value = frame.getValue(var);
                    temporaries.put(var.name(), value);
                    System.out.printf("%s -> %s%n", var.name(), value);
                }
                return temporaries;
            }
            return null;
        } catch (IncompatibleThreadStateException | AbsentInformationException e) {
            System.out.println("Impossible d'acceder aux variables temporaires: " + e.getMessage());
            return null;
        }
    }
}