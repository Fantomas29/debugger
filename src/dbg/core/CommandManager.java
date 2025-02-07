package dbg.core;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.LocatableEvent;
import dbg.commands.interfaces.BreakCommandFactory;
import dbg.commands.interfaces.CommandFactory;
import dbg.commands.interfaces.DebugCommand;
import dbg.commands.breakpoint.*;
import dbg.commands.control.ContinueCommand;
import dbg.commands.control.StepCommand;
import dbg.commands.control.StepOverCommand;
import dbg.commands.info.FrameCommand;
import dbg.commands.info.MethodCommand;
import dbg.commands.info.StackCommand;
import dbg.commands.info.TemporariesCommand;
import dbg.commands.object.ArgumentsCommand;
import dbg.commands.object.ReceiverCommand;
import dbg.commands.object.ReceiverVariablesCommand;
import dbg.commands.object.SenderCommand;
import dbg.commands.timetravel.StepBackCommand;
import dbg.timetravel.StepBackManager;
import dbg.timetravel.StepByStepDebugger;

import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

// Mise à jour du CommandManager
public class CommandManager {
    private final Map<String, CommandFactory> commandFactories;
    private final Map<String, BreakCommandFactory> breakCommandFactories;
    private final VirtualMachine vm;
    private final StepBackManager stepBackManager; // Changé de StepByStepDebugger à StepBackManager


    public CommandManager(VirtualMachine vm, StepBackManager stepBackManager) { // Mise à jour du constructeur
        this.vm = vm;
        this.stepBackManager = stepBackManager;
        this.commandFactories = new HashMap<>();
        this.breakCommandFactories = new HashMap<>();
        registerCommands();
    }

    private void registerCommands() {
        // Commandes standard
        commandFactories.put("step", StepCommand::new);
        commandFactories.put("step-over", StepOverCommand::new);
        commandFactories.put("continue", ContinueCommand::new);
        commandFactories.put("frame", FrameCommand::new);
        commandFactories.put("temporaries", TemporariesCommand::new);
        commandFactories.put("stack", StackCommand::new);
        commandFactories.put("receiver", ReceiverCommand::new);
        commandFactories.put("sender", SenderCommand::new);
        commandFactories.put("receiver-variables", ReceiverVariablesCommand::new);
        commandFactories.put("method", MethodCommand::new);
        commandFactories.put("arguments", ArgumentsCommand::new);
        commandFactories.put("breakpoints", BreakpointsCommand::new);

        // Commandes de breakpoint
        breakCommandFactories.put("break", (vm, event, args) ->
                new BreakCommand(vm, event, args[0], Integer.parseInt(args[1])));
        breakCommandFactories.put("break-once", (vm, event, args) ->
                new BreakOnceCommand(vm, event, args[0], Integer.parseInt(args[1])));
        breakCommandFactories.put("break-on-count", (vm, event, args) ->
                new BreakOnCountCommand(vm, event, args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2])));
        breakCommandFactories.put("break-before-method", (vm, event, args) ->
                new BreakBeforeMethodCommand(vm, args[0]));

        commandFactories.put("back", (vm, event) ->
                new StepBackCommand(stepBackManager, event));
    }

    public Object executeCommand(String commandLine, LocatableEvent event) {
        String[] parts = commandLine.trim().split("\\s+");
        String command = parts[0].toLowerCase();

        // Vérifier d'abord les commandes de breakpoint
        if (breakCommandFactories.containsKey(command)) {
            if (command.equals("break-before-method")) {
                if (parts.length < 2) {
                    System.out.println("Usage: " + command + " <methodName>");
                    return null;
                }
            } else if (command.equals("break-on-count")) {
                if (parts.length < 4) {
                    System.out.println("Usage: " + command + " <fileName (with package)> <lineNumber> <count>");
                    return null;
                }
            } else if (parts.length < 3) {
                System.out.println("Usage: " + command + " <fileName (with package)> <lineNumber>");
                return null;
            }

            String[] args = Arrays.copyOfRange(parts, 1, parts.length);
            BreakCommandFactory factory = breakCommandFactories.get(command);
            DebugCommand debugCommand = factory.create(vm, event, args);
            return debugCommand.execute();
        }

        // Sinon, traiter comme une commande standard
        CommandFactory factory = commandFactories.get(command);
        if (factory != null) {
            DebugCommand debugCommand = factory.create(vm, event);
            Object result = debugCommand.execute();

            if (isControlCommand(command)) {
                vm.resume();
            }
            return result;
        }

        System.out.println("Commande inconnue: '" + command + "'");
        System.out.println("Commandes disponibles: " + String.join(", ", getAllCommands()));
        return null;
    }

    private Set<String> getAllCommands() {
        Set<String> allCommands = new HashSet<>(commandFactories.keySet());
        allCommands.addAll(breakCommandFactories.keySet());
        return allCommands;
    }

    public boolean isControlCommand(String command) {
        return command.equals("continue") ||
                command.equals("step") ||
                command.equals("step-over") ||
                command.equals("back");
    }

    public boolean isValidCommand(String command) {
        String cmd = command.trim().split("\\s+")[0].toLowerCase();
        return commandFactories.containsKey(cmd) || breakCommandFactories.containsKey(cmd);
    }

    public String getAvailableCommands() {
        return String.join(", ", getAllCommands());
    }

}