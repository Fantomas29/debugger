package dbg.commands.breakpoint;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.BreakpointRequest;
import dbg.commands.interfaces.DebugCommand;

import java.util.List;

public class BreakCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;
    private final String className;
    private final int lineNumber;

    public BreakCommand(VirtualMachine vm, LocatableEvent event, String className, int lineNumber) {
        this.vm = vm;
        this.event = event;
        this.className = className;
        this.lineNumber = lineNumber;
    }

    @Override
    public Object execute() {
        try {
            String targetClass = className.replace(".java", "");
            if (!targetClass.startsWith("dbg.")) {
                targetClass = "dbg." + targetClass;
            }

            // Itérer sur toutes les classes chargées dans la VM
            for (ReferenceType refType : vm.allClasses()) {
                if (refType.name().equalsIgnoreCase(targetClass)) {
                    List<Location> locations = refType.locationsOfLine(lineNumber);
                    if (!locations.isEmpty()) {
                        Location location = locations.get(0);

                        // Vérifier si un breakpoint existe déjà à cet endroit
                        for (BreakpointRequest existingBp : vm.eventRequestManager().breakpointRequests()) {
                            if (existingBp.location().equals(location)) {
                                System.out.printf("Breakpoint deja existant dans %s a la ligne %d%n",
                                        refType.name(), lineNumber);
                                return existingBp;
                            }
                        }

                        // Créer une requête de point d'arrêt à l'emplacement spécifié
                        BreakpointRequest bpReq = vm.eventRequestManager()
                                .createBreakpointRequest(location);
                        bpReq.enable();
                        System.out.printf("Breakpoint ajoute dans %s a la ligne %d%n",
                                refType.name(), lineNumber);
                        return bpReq;
                    }
                }
            }
            System.out.println("Impossible de trouver la classe ou la ligne specifiee (verifier le nom du fichier fourni avec le package)");
            return null;
        } catch (AbsentInformationException e) {
            System.out.println("Erreur : " + e.getMessage());
            return null;
        }
    }
}