package dbg.commands.breakpoint;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import dbg.commands.interfaces.DebugCommand;

import java.util.List;

public class BreakOnCountCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;
    private final String fileName;
    private final int lineNumber;
    private final int count;

    public BreakOnCountCommand(VirtualMachine vm, LocatableEvent event, String fileName, int lineNumber, int count) {
        this.vm = vm;
        this.event = event;
        this.fileName = fileName.replace(".java", "");
        this.lineNumber = lineNumber;
        this.count = count;
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
                                System.out.printf("Breakpoint déjà existant dans %s à la ligne %d%n",
                                        refType.name(), lineNumber);
                                return existingBp;
                            }
                        }

                        BreakpointRequest bpReq = vm.eventRequestManager()
                                .createBreakpointRequest(location);

                        // On ajoute une propriété pour compter nous-mêmes
                        bpReq.putProperty("type", "count");
                        bpReq.putProperty("current_count", 0);
                        bpReq.putProperty("target_count", count);
                        bpReq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                        bpReq.enable();

                        System.out.printf("Breakpoint ajouté dans %s à la ligne %d (s'activera après %d passages)%n",
                                refType.name(), lineNumber, count);
                        return bpReq;
                    }
                }
            }
            System.out.println("Impossible de trouver la classe ou la ligne spécifiée");
            return null;
        } catch (AbsentInformationException e) {
            System.out.println("Impossible d'accéder aux informations de débogage: " + e.getMessage());
            return null;
        }
    }
}