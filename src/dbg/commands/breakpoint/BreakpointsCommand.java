package dbg.commands.breakpoint;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.BreakpointRequest;
import dbg.commands.interfaces.DebugCommand;

import java.util.List;

public class BreakpointsCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;

    public BreakpointsCommand(VirtualMachine vm, LocatableEvent event) {
        this.vm = vm;
        this.event = event;
    }

    @Override
    public Object execute() {
        List<BreakpointRequest> breakpoints = vm.eventRequestManager().breakpointRequests();

        if (breakpoints.isEmpty()) {
            System.out.println("Aucun point d'arret actif");
            return null;
        }

        System.out.println("=== Points d'arret actifs ===");
        for (BreakpointRequest bp : breakpoints) {
            Location loc = bp.location();
            String status = bp.isEnabled() ? "activé" : "desactive";
            try {
                System.out.printf("Fichier : %s, Ligne : %d, Methode : %s, Statut : %s%n",
                        loc.sourcePath(),
                        loc.lineNumber(),
                        loc.method().name(),
                        status);
            } catch (AbsentInformationException e) {
                System.out.printf("Emplacement : %s, Statut : %s%n",
                        loc,
                        status);
            }
        }

        return breakpoints;
    }
}