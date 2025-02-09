package dbg.commands.object;

import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import dbg.commands.interfaces.DebugCommand;

import java.util.Map;

public class ReceiverVariablesCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final LocatableEvent event;

    public ReceiverVariablesCommand(VirtualMachine vm, LocatableEvent event) {
        this.vm = vm;
        this.event = event;
    }

    @Override
    public Object execute() {
        try {
            ThreadReference thread = event.thread();
            if (thread.frameCount() > 0) {
                StackFrame frame = thread.frame(0);
                ObjectReference thisObject = frame.thisObject();

                if (thisObject == null) {
                    System.out.println("Pas de variables d'instance (contexte statique)");
                    return null;
                }

                ReferenceType type = thisObject.referenceType();
                Map<Field, Value> values = thisObject.getValues(type.allFields());

                if (values.isEmpty()) {
                    System.out.println("Aucune variable d'instance");
                    return null;
                }

                System.out.println("=== Variables d'instance du receveur ===");
                for (Map.Entry<Field, Value> entry : values.entrySet()) {
                    Field field = entry.getKey();
                    System.out.printf("%s %s = %s%n",
                            field.typeName(),
                            field.name(),
                            formatValue(entry.getValue()));
                }
                return values;
            }
            return null;
        } catch (IncompatibleThreadStateException e) {
            System.out.println("Erreur d'acces aux variables: " + e.getMessage());
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
        return value.toString();
    }
}