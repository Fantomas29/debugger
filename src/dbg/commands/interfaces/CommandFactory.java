package dbg.commands.interfaces;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.LocatableEvent;

@FunctionalInterface
public interface CommandFactory {
    DebugCommand create(VirtualMachine vm, LocatableEvent event);
}