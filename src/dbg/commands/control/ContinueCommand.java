package dbg.commands.control;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.LocatableEvent;
import dbg.commands.interfaces.DebugCommand;

public class ContinueCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;

    public ContinueCommand(VirtualMachine vm, LocatableEvent event) {
        this.vm = vm;
        this.event = event;
    }

    @Override
    public Object execute() {
        // Supprime toutes les requêtes de step existantes car nous n'en avons pas besoin
        vm.eventRequestManager().deleteEventRequests(vm.eventRequestManager().stepRequests());

        // Pas besoin de créer de nouvelle requête, on laisse simplement
        // l'exécution continuer jusqu'au prochain breakpoint
        return null;
    }
}
