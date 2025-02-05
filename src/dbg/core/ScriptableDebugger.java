package dbg.core;

import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Scanner;

public class ScriptableDebugger {
    private Class debugClass;
    private VirtualMachine vm;
    private Scanner commandScanner;
    private CommandManager commandManager;

    public ScriptableDebugger() {
        this.commandScanner = new Scanner(System.in);
    }

    public VirtualMachine connectAndLaunchVM() throws IOException, IllegalConnectorArgumentsException, VMStartException {
        LaunchingConnector launchingConnector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> arguments = launchingConnector.defaultArguments();
        arguments.get("main").setValue(debugClass.getName());
        VirtualMachine vm = launchingConnector.launch(arguments);
        return vm;
    }

    public void attachTo(Class debuggeeClass) {
        this.debugClass = debuggeeClass;
        try {
            vm = connectAndLaunchVM();
            commandManager = new CommandManager(vm);
            enableClassPrepareRequest(vm);
            startDebugger();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (IllegalConnectorArgumentsException e) {
            e.printStackTrace();
        } catch (VMStartException e) {
            e.printStackTrace();
            System.out.println(e.toString());
        } catch (VMDisconnectedException e) {
            System.out.println("Virtual Machine is disconnected: " + e.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startDebugger() throws VMDisconnectedException, InterruptedException, AbsentInformationException {
        EventSet eventSet = null;
        while ((eventSet = vm.eventQueue().remove()) != null) {
            for (Event event : eventSet) {
                if (event instanceof VMDisconnectEvent) {
                    handleVMDisconnect();
                    return;
                }

                if (event instanceof ClassPrepareEvent classEvent) {
                    System.out.println("Analyzing ClassPrepareEvent:");
                    System.out.println("Prepared class: " + classEvent.referenceType().name());
                    System.out.println("Class loader: " + classEvent.referenceType().classLoader());
                    System.out.println();
                    setBreakPoint(debugClass.getName(), 27);
                    vm.resume();  // On reprend l'exécution après avoir posé le breakpoint
                }

                if (event instanceof BreakpointEvent || event instanceof StepEvent) {
                    if (event instanceof BreakpointEvent) {
                        BreakpointEvent bpEvent = (BreakpointEvent) event;
                        System.out.println("Breakpoint reached");
                        System.out.println("Thread: " + bpEvent.thread().name());
                        System.out.println("Location: " + bpEvent.location().toString());
                        System.out.println();

                        // Vérifier si c'est un breakpoint "once" et le supprimer si c'est le cas
                        EventRequest request = bpEvent.request();
                        if (request instanceof BreakpointRequest) {
                            BreakpointRequest bpRequest = (BreakpointRequest) request;
                            if ("once".equals(bpRequest.getProperty("type"))) {
                                bpRequest.disable();  // Désactive d'abord
                                vm.eventRequestManager().deleteEventRequest(bpRequest);  // Puis supprime
                                System.out.println("Breakpoint unique supprimé");
                            }
                        }
                    } else {
                        System.out.println("Analyzing StepEvent:");
                        System.out.println("Location: " + ((StepEvent)event).location());
                        ((StepEvent)event).request().disable();
                    }

                    // Boucle de commandes
                    boolean shouldContinue = false;
                    while (!shouldContinue) {
                        String command = getNextCommand((LocatableEvent)event);
                        shouldContinue = commandManager.isControlCommand(command);
                    }
                    continue;
                }

                vm.resume();
            }
        }
    }

    private void handleVMDisconnect() {
        System.out.println("End of program.");
        InputStreamReader reader = new InputStreamReader(vm.process().getInputStream());
        OutputStreamWriter writer = new OutputStreamWriter(System.out);
        try {
            reader.transferTo(writer);
            writer.flush();
        } catch (IOException e) {
            System.out.println("Target VM input stream reading error.");
        }
        commandScanner.close();
    }

    public void enableClassPrepareRequest(VirtualMachine vm) {
        ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
        classPrepareRequest.addClassFilter(debugClass.getName());
        classPrepareRequest.enable();
    }

    public void setBreakPoint(String className, int lineNumber) throws AbsentInformationException {
        for (ReferenceType targetClass : vm.allClasses()) {
            if (targetClass.name().equals(className)) {
                Location location = targetClass.locationsOfLine(lineNumber).getFirst();
                BreakpointRequest bpReq = vm.eventRequestManager().createBreakpointRequest(location);
                bpReq.enable();
            }
        }
    }

    private void handleUserCommand(LocatableEvent event) {
        while (true) {
            System.out.print("Enter command: ");
            String command = commandScanner.nextLine().trim().toLowerCase();

            if (commandManager.isValidCommand(command)) {
                commandManager.executeCommand(command, event);
                break;
            } else {
                System.out.println("Unknown command: '" + command + "'");
                System.out.println("Available commands: " + commandManager.getAvailableCommands());
            }
        }
    }

    private String getNextCommand(LocatableEvent event) {
        System.out.print("Enter command: ");
        String command = commandScanner.nextLine().trim().toLowerCase();

        if (commandManager.isValidCommand(command)) {
            commandManager.executeCommand(command, event);
            return command;
        } else {
            System.out.println("Unknown command: '" + command + "'");
            System.out.println("Available commands: " + commandManager.getAvailableCommands());
            return getNextCommand(event);  // récursion pour obtenir une commande valide
        }
    }
}