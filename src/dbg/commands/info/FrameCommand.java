package dbg.commands.info;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Location;
import dbg.commands.interfaces.DebugCommand;

public class FrameCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;

    public FrameCommand(VirtualMachine vm, LocatableEvent event) {
        this.vm = vm;
        this.event = event;
    }

    @Override
    public Object execute() {
        try {
            ThreadReference thread = event.thread();
            if (thread.frameCount() > 0) {
                StackFrame frame = thread.frame(0);  // Récupère la frame courante (top of stack)
                Location location = frame.location();

                // Affiche les informations de la frame
                System.out.println("Frame courante:");
                System.out.println("  Methode: " + location.method());
                System.out.println("  Classe: " + location.declaringType().name());
                System.out.println("  Fichier: " + location.sourcePath());
                System.out.println("  Ligne: " + location.lineNumber());

                return frame; // Retourne la frame pour une utilisation potentielle par d'autres commandes
            }
            return null;
        } catch (Exception e) {
            System.out.println("Erreur lors de l'accès à la frame: " + e.getMessage());
            return null;
        }
    }
}