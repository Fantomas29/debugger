package dbg.commands.object;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import dbg.commands.interfaces.DebugCommand;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

public class ArgumentsCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;

    public ArgumentsCommand(VirtualMachine vm, LocatableEvent event) {
        this.vm = vm;
        this.event = event;
    }

    @Override
    public Object execute() {
        try {
            ThreadReference thread = event.thread();
            if (thread.frameCount() > 0) {
                StackFrame frame = thread.frame(0);
                Method method = frame.location().method();
                List<LocalVariable> arguments = method.arguments();

                if (arguments.isEmpty()) {
                    System.out.println("Aucun argument");
                    return null;
                }

                Map<LocalVariable, Value> argValues = new LinkedHashMap<>();
                System.out.println("=== Arguments de la méthode ===");
                for (LocalVariable arg : arguments) {
                    Value value = frame.getValue(arg);
                    argValues.put(arg, value);
                    System.out.printf("%s %s = %s%n",
                            arg.typeName(),
                            arg.name(),
                            formatValue(value));
                }
                return argValues;
            }
            return null;
        } catch (IncompatibleThreadStateException | AbsentInformationException e) {
            System.out.println("Erreur d'accès aux arguments: " + e.getMessage());
            return null;
        }
    }

    private String formatValue(Value value) {
        if (value == null) return "null";
        if (value instanceof StringReference) {
            return "\"" + ((StringReference) value).value() + "\"";
        }
        if (value instanceof PrimitiveValue) {
            return value.toString();
        }
        if (value instanceof ArrayReference) {
            ArrayReference array = (ArrayReference) value;
            return String.format("%s[%d]", array.type().name(), array.length());
        }
        return value.toString();
    }
}