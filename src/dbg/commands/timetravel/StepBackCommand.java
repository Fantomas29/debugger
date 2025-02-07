package dbg.commands.timetravel;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.LocatableEvent;
import dbg.commands.interfaces.DebugCommand;
import dbg.timetravel.StepBackManager;

public class StepBackCommand implements DebugCommand {
    private final StepBackManager stepBackManager;
    private final LocatableEvent event;

    public StepBackCommand(StepBackManager stepBackManager, LocatableEvent event) {
        this.stepBackManager = stepBackManager;
        this.event = event;
    }

    @Override
    public Object execute() {
        stepBackManager.stepBack();
        return null;
    }
}