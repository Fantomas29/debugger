package dbg.commands.info;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import dbg.commands.interfaces.DebugCommand;

public class MethodCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;

    public MethodCommand(VirtualMachine vm, LocatableEvent event) {
        this.vm = vm;
        this.event = event;
    }

    @Override
    public Object execute() {
        try {
            ThreadReference thread = event.thread();
            if (thread.frameCount() > 0) {
                StackFrame frame = thread.frame(0);
                Method method = frame.location().method();

                System.out.println("=== Methode courante ===");
                System.out.println("Nom: " + method.name());
                System.out.println("Signature: " + method.signature());
                System.out.println("Classe declarante: " + method.declaringType().name());
                System.out.println("Est statique: " + method.isStatic());
                System.out.println("Visibilite: " + getVisibility(method));

                return method;
            }
            return null;
        } catch (IncompatibleThreadStateException e) {
            System.out.println("Erreur d'accès à la méthode: " + e.getMessage());
            return null;
        }
    }

    private String getVisibility(Method method) {
        if (method.isPrivate()) return "private";
        if (method.isProtected()) return "protected";
        if (method.isPublic()) return "public";
        return "package";
    }
}