package dbg.commands.breakpoint;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import dbg.commands.interfaces.DebugCommand;

import java.util.List;

public class BreakOnceCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;
    private final String fileName;
    private final int lineNumber;

    public BreakOnceCommand(VirtualMachine vm, LocatableEvent event, String fileName, int lineNumber) {
        this.vm = vm;
        this.event = event;
        this.fileName = fileName.replace(".java", "");
        this.lineNumber = lineNumber;
    }

    @Override
    public Object execute() {
        try {
            String targetClass = fileName;
            if (!targetClass.startsWith("dbg.")) {
                targetClass = "dbg." + targetClass;
            }

            for (ReferenceType refType : vm.allClasses()) {
                if (refType.name().equalsIgnoreCase(targetClass)) {
                    List<Location> locations = refType.locationsOfLine(lineNumber);
                    if (!locations.isEmpty()) {
                        Location location = locations.get(0);

                        // Vérifier les breakpoints existants
                        for (BreakpointRequest existingBp : vm.eventRequestManager().breakpointRequests()) {
                            if (existingBp.location().equals(location)) {
                                System.out.printf("Breakpoint deja existant dans %s a la ligne %d%n",
                                        refType.name(), lineNumber);
                                return existingBp;
                            }
                        }

                        BreakpointRequest bpReq = vm.eventRequestManager()
                                .createBreakpointRequest(location);

                        // On ajoute une propriété pour identifier ce breakpoint comme étant "once"
                        bpReq.putProperty("type", "once");
                        bpReq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                        bpReq.enable();

                        System.out.printf("Breakpoint unique ajoute dans %s a la ligne %d%n",
                                refType.name(), lineNumber);
                        return bpReq;
                    }
                }
            }
            System.out.println("Impossible de trouver la classe ou la ligne specifiee (verifier le nom du fichier fourni avec le package)");
            return null;
        } catch (AbsentInformationException e) {
            System.out.println("Impossible d'acceder aux informations de débogage: " + e.getMessage());
            return null;
        }
    }
}