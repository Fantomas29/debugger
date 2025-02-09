package dbg.commands.object;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import dbg.commands.interfaces.DebugCommand;

public class SenderCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;

    public SenderCommand(VirtualMachine vm, LocatableEvent event) {
        this.vm = vm;
        this.event = event;
    }

    @Override
    public Object execute() {
        try {
            ThreadReference thread = event.thread();
            if (thread.frameCount() > 1) { // Besoin d'au moins 2 frames
                StackFrame callerFrame = thread.frame(1);
                ObjectReference sender = callerFrame.thisObject();
                Location location = callerFrame.location();

                System.out.println("=== Appelant (sender) ===");
                if (sender != null) {
                    System.out.println("Type: " + sender.referenceType().name());
                    System.out.println("Methode appelante: " + location.method().name());
                    System.out.println("Ligne: " + location.lineNumber());
                    return sender;
                } else {
                    System.out.println("Appele depuis un contexte statique");
                    System.out.println("Classe: " + location.declaringType().name());
                    System.out.println("Methode: " + location.method().name());
                    return location;
                }
            }
            System.out.println("Pas d'appelant (frame racine)");
            return null;
        } catch (IncompatibleThreadStateException e) {
            System.out.println("Erreur d'acces à l'appelant: " + e.getMessage());
            return null;
        }
    }
}