package dbg.commands.timetravel;

import com.sun.jdi.event.LocatableEvent;
import dbg.commands.interfaces.DebugCommand;
import dbg.timetravel.StepByStepDebugger;

public class StepBackCommand implements DebugCommand {
    private final StepByStepDebugger debugger;

    public StepBackCommand(StepByStepDebugger debugger) {
        this.debugger = debugger;
    }

    @Override
    public Object execute() {
        debugger.stepBack();
        return null;
    }
}