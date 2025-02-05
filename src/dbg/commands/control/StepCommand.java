package dbg.commands.control;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.StepRequest;
import dbg.commands.interfaces.DebugCommand;

public class StepCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;

    public StepCommand(VirtualMachine vm, LocatableEvent event) {
        this.vm = vm;
        this.event = event;
    }

    @Override
    public Object execute() {
        // Désactive toute requête de step précédente pour éviter les conflits
        vm.eventRequestManager().deleteEventRequests(vm.eventRequestManager().stepRequests());

        // Crée et active une nouvelle requête de step
        StepRequest stepRequest = vm.eventRequestManager().createStepRequest(
                event.thread(),
                StepRequest.STEP_MIN,
                StepRequest.STEP_OVER
        );
        stepRequest.enable();

        return stepRequest;
    }
}