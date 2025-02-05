package dbg.commands.info;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import dbg.commands.interfaces.DebugCommand;

import java.util.List;

public class StackCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;

    public StackCommand(VirtualMachine vm, LocatableEvent event) {
        this.vm = vm;
        this.event = event;
    }

    @Override
    public Object execute() {
        try {
            ThreadReference thread = event.thread();
            List<StackFrame> stack = thread.frames();

            if (stack.isEmpty()) {
                System.out.println("Pile d'appels vide");
                return null;
            }

            System.out.println("=== Pile d'appels ===");
            for (int i = 0; i < stack.size(); i++) {
                StackFrame frame = stack.get(i);
                Location location = frame.location();
                Method method = location.method();

                // Affichage simplifié : juste la classe, la méthode et la ligne
                System.out.printf("%d: %s.%s() - ligne %d%n",
                        i,
                        location.declaringType().name(),
                        method.name(),
                        location.lineNumber());
            }
            return stack;
        } catch (IncompatibleThreadStateException e) {
            System.out.println("Erreur d'accès à la pile: " + e.getMessage());
            return null;
        }
    }
}