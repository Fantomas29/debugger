package dbg.commands.object;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import dbg.commands.interfaces.DebugCommand;

public class ReceiverCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;

    public ReceiverCommand(VirtualMachine vm, LocatableEvent event) {
        this.vm = vm;
        this.event = event;
    }

    @Override
    public Object execute() {
        try {
            ThreadReference thread = event.thread();
            if (thread.frameCount() > 0) {
                StackFrame frame = thread.frame(0);
                ObjectReference thisObject = frame.thisObject();

                if (thisObject == null) {
                    System.out.println("Pas de receveur (contexte statique)");
                    return null;
                }

                System.out.println("=== Receveur (this) ===");
                System.out.println("Type: " + thisObject.referenceType().name());
                System.out.println("Identité: " + thisObject.uniqueID());

                return thisObject;
            }
            return null;
        } catch (IncompatibleThreadStateException e) {
            System.out.println("Erreur d'accès au receveur: " + e.getMessage());
            return null;
        }
    }
}