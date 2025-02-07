// StepBackCommand.java
package dbg.commands.timetravel;

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
        if (stepBackManager.isStepBackInProgress()) {
            System.out.println("Step back already in progress");
            return null;
        }

        stepBackManager.stepBack();
        return null;
    }
}