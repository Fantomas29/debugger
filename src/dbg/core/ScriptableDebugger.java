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
import dbg.timetravel.StepByStepDebugger;

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
    private StepByStepDebugger timeTravelDebugger;


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
            timeTravelDebugger = new StepByStepDebugger(vm);
            commandManager = new CommandManager(vm, timeTravelDebugger);
            enableClassPrepareRequest(vm);
            startDebugger();
        } catch (IOException | IllegalConnectorArgumentsException | VMStartException e) {
            e.printStackTrace();
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
                    handleClassPrepareEvent(classEvent);
                }

                if (event instanceof BreakpointEvent || event instanceof StepEvent) {
                    // Enregistrement de l'état pour le time-traveling
                    LocatableEvent locatableEvent = (LocatableEvent) event;
                    timeTravelDebugger.recordStep(locatableEvent);

                    handleDebugEvent(event);
                }

                vm.resume();
            }
        }
    }

    private void handleClassPrepareEvent(ClassPrepareEvent event) throws AbsentInformationException {
        System.out.println("Analyzing ClassPrepareEvent:");
        System.out.println("Prepared class: " + event.referenceType().name());
        System.out.println("Class loader: " + event.referenceType().classLoader());
        System.out.println();
        setBreakPoint(debugClass.getName(), 31);
        vm.resume();
    }

    private void handleDebugEvent(Event event) throws InterruptedException {
        if (event instanceof BreakpointEvent) {
            handleBreakpointEvent((BreakpointEvent) event);
        } else if (event instanceof StepEvent) {
            handleStepEvent((StepEvent) event);
        }

        // Boucle de commandes
        boolean shouldContinue = false;
        while (!shouldContinue) {
            String command = getNextCommand((LocatableEvent)event);
            shouldContinue = commandManager.isControlCommand(command);
        }
    }

    private void handleStepEvent(StepEvent event) {
        System.out.println("Step Event:");
        System.out.println("Location: " + event.location());
        try {
            EventRequest request = event.request();
            if (request != null && request.isEnabled()) {
                request.disable();
                vm.eventRequestManager().deleteEventRequest(request);
            }
        } catch (VMDisconnectedException e) {
            // Ignore if VM is already disconnected
        }
    }

    private String getNextCommand(LocatableEvent event) {
        System.out.print("Enter command: ");
        String command = commandScanner.nextLine().trim().toLowerCase();

        if (commandManager.isValidCommand(command)) {
            Object result = commandManager.executeCommand(command, event);
            if (result != null) {
                System.out.println("Command result: " + result);
            }
            return command;
        } else {
            System.out.println("Unknown command: '" + command + "'");
            System.out.println("Available commands: " + commandManager.getAvailableCommands());
            return getNextCommand(event);  // récursion pour obtenir une commande valide
        }
    }

    private void handleBreakpointEvent(BreakpointEvent event) {
        EventRequest request = event.request();
        if (request instanceof BreakpointRequest) {
            BreakpointRequest bpRequest = (BreakpointRequest) request;

            // Gérer les breakpoints de type count
            if ("count".equals(bpRequest.getProperty("type"))) {
                int currentCount = (Integer) bpRequest.getProperty("current_count");
                int targetCount = (Integer) bpRequest.getProperty("target_count");
                currentCount++;
                bpRequest.putProperty("current_count", currentCount);

                if (currentCount < targetCount) {
                    vm.resume();
                    return;
                }
            }
            // Gérer les breakpoints de type once
            else if ("once".equals(bpRequest.getProperty("type"))) {
                bpRequest.disable();
                vm.eventRequestManager().deleteEventRequest(bpRequest);
                System.out.println("One-time breakpoint removed");
            }
        }

        System.out.println("Breakpoint reached");
        System.out.println("Thread: " + event.thread().name());
        System.out.println("Location: " + event.location().toString());
        System.out.println();
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
}