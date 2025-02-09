package dbg.core;

import com.sun.jdi.*;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;
import dbg.timetravel.StepBackManager;

import java.util.Map;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScriptableDebugger {
    private VirtualMachine vm;
    private CommandManager commandManager;
    private StepBackManager stepBackManager;
    private Class<?> debugClass;
    private Process process;
    private Scanner scanner;
    private int initialBreakpointLine = 9;
    private LocatableEvent currentEvent;
    private AtomicBoolean waitingForCommand;
    private volatile boolean isDisposed = false;

    public ScriptableDebugger() {
        this.scanner = new Scanner(System.in);
        this.waitingForCommand = new AtomicBoolean(false);
    }

    public void setInitialBreakpoint(int line) {
        this.initialBreakpointLine = line;
    }

    public void dispose() {
        isDisposed = true;
        if (vm != null) {
            vm.dispose();
        }
        if (process != null) {
            process.destroy();
        }
    }

    public void attachTo(Class<?> debugClass) throws Exception {
        this.debugClass = debugClass;
        if (stepBackManager != null) {
            stepBackManager.clearHistory();
        }
        startDebugee();
        eventLoop();
    }

    public void executeGuiCommand(String command) {
        if (currentEvent != null && waitingForCommand.get()) {
            System.out.println("\n> Executing command: " + command);

            if (command.equals("quit")) {
                dispose();
                return;
            }

            try {
                if (!commandManager.isValidCommand(command)) {
                    System.out.println("Unknown command. Available commands: " +
                            commandManager.getAvailableCommands());
                    return;
                }

                commandManager.executeCommand(command, currentEvent);
                if (commandManager.isControlCommand(command)) {
                    waitingForCommand.set(false);
                    vm.resume();
                }
            } catch (Exception e) {
                System.out.println("Error executing command: " + e.getMessage());
            }
            System.out.println("----------------------------------------");
        }
    }

    public void restartVM() {
        // Exécute le restart dans un nouveau thread pour ne pas bloquer l'interface
        new Thread(() -> {
            try {
                System.out.println("[Debug-VM] Starting VM restart");
                if (vm != null) {
                    System.out.println("[Debug-VM] Disposing old VM");
                    vm.dispose();
                }
                if (process != null) {
                    System.out.println("[Debug-VM] Destroying old process");
                    process.destroy();
                }

                System.out.println("[Debug-VM] Creating new debugee");
                startDebugee();
                Thread.sleep(100);
                System.out.println("[Debug-VM] Starting event loop");
                System.out.println("----------------------------------------");
                eventLoop();
                System.out.println("[Debug-VM] Restart complete");
            } catch (Exception e) {
                System.out.println("[Debug-VM] Error restarting VM: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void startDebugee() throws Exception {
        LaunchingConnector connector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> arguments = connector.defaultArguments();
        arguments.get("main").setValue(debugClass.getName());
        arguments.get("options").setValue("-cp " + System.getProperty("java.class.path"));

        vm = connector.launch(arguments);
        process = vm.process();

        stepBackManager = new StepBackManager(this, vm);
        commandManager = new CommandManager(vm, stepBackManager);

        ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
        classPrepareRequest.addClassFilter(debugClass.getName());
        classPrepareRequest.enable();
    }

    private void setBreakPointStepBack(Class<?> debugClass) {
        try {
            StepBackManager.LocationInfo previousLocation = stepBackManager.getPreviousLocation();
            if (previousLocation != null) {
                for (ReferenceType refType : vm.allClasses()) {
                    if (refType.name().equals(previousLocation.className)) {
                        List<Location> locations = refType.locationsOfLine(previousLocation.lineNumber);
                        if (!locations.isEmpty()) {
                            Location location = locations.get(0);
                            BreakpointRequest bpReq = vm.eventRequestManager().createBreakpointRequest(location);
                            bpReq.enable();
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[Debug] Error setting step back breakpoint: " + e.getMessage());
        }
    }

    private void eventLoop() throws Exception {
        EventQueue eventQueue = vm.eventQueue();
        boolean connected = true;

        while (connected && !isDisposed) {
            EventSet eventSet = eventQueue.remove();
            boolean shouldResume = true;

            for (Event event : eventSet) {
                if (event instanceof ClassPrepareEvent) {
                    handleClassPrepareEvent((ClassPrepareEvent) event);
                } else if (event instanceof BreakpointEvent) {
                    shouldResume = false;
                    handleBreakpointEvent((BreakpointEvent) event);
                } else if (event instanceof StepEvent) {
                    shouldResume = false;
                    handleStepEvent((StepEvent) event);
                } else if (event instanceof VMDeathEvent || event instanceof VMDisconnectEvent) {
                    connected = false;
                }
            }

            if (connected && shouldResume && !isDisposed) {
                eventSet.resume();
            }
        }
    }

    private void handleClassPrepareEvent(ClassPrepareEvent event) throws Exception {
        ClassType classType = (ClassType) event.referenceType();
        try {
            List<Location> locations = classType.locationsOfLine(initialBreakpointLine);
            if (!locations.isEmpty()) {
                Location location = locations.get(0);
                BreakpointRequest bpReq = vm.eventRequestManager().createBreakpointRequest(location);
                bpReq.enable();
            }
        } catch (Exception e) {
            System.out.println("[Debug] Error setting breakpoint: " + e.getMessage());
            throw e;
        }
    }

    private void handleBreakpointEvent(BreakpointEvent event) {
        displayLocation(event);
        if (stepBackManager.isReplaying) {
            setBreakPointStepBack(event.getClass());
        }
        currentEvent = event;
        waitingForCommand.set(true);
        waitForGuiCommand();
    }

    private void handleStepEvent(StepEvent event) {
        displayLocation(event);
        currentEvent = event;
        waitingForCommand.set(true);
        waitForGuiCommand();
    }

    private void displayLocation(LocatableEvent event) {
        Location location = event.location();
        System.out.printf("%s.%s():%d%n",
                location.declaringType().name(),
                location.method().name(),
                location.lineNumber());
    }

    private void waitForGuiCommand() {
        while (waitingForCommand.get() && !isDisposed) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}