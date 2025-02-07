package dbg.commands.control;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.StepRequest;
import dbg.commands.interfaces.DebugCommand;

public class StepOverCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;

    public StepOverCommand(VirtualMachine vm, LocatableEvent event) {
        this.vm = vm;
        this.event = event;
    }

    @Override
    public Object execute() {
        vm.eventRequestManager().deleteEventRequests(vm.eventRequestManager().stepRequests());
        StepRequest stepRequest = vm.eventRequestManager().createStepRequest(
                event.thread(),
                StepRequest.STEP_LINE,
                StepRequest.STEP_OVER
        );
        stepRequest.putProperty("stepType", "STEP_OVER");
        stepRequest.enable();
        return stepRequest;
    }
}