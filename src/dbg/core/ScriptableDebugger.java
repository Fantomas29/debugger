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
import com.sun.jdi.request.StepRequest;
import dbg.timetravel.StepBackManager;
import dbg.timetravel.StepByStepDebugger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class ScriptableDebugger {
    private Class debugClass;
    private VirtualMachine vm;
    private Scanner commandScanner;
    private CommandManager commandManager;
    private StepBackManager stepBackManager;
    private boolean isFirstLaunch = true;

    public ScriptableDebugger() {
        this.commandScanner = new Scanner(System.in);
    }

    public void restartVM() {
        try {
            System.out.println("[Debug] Starting VM restart process");
            VirtualMachine oldVm = vm;

            // Créer nouvelle VM
            vm = connectAndLaunchVM();
            System.out.println("[Debug] New VM created");

            // Mettre à jour les références
            stepBackManager.updateVM(vm);
            commandManager = new CommandManager(vm, stepBackManager);

            // Configurer la nouvelle VM
            enableClassPrepareRequest(vm);
            stepBackManager.configureInitialBreakpoint();

            // Nettoyer l'ancienne VM
            if (oldVm != null) {
                try {
                    oldVm.dispose();
                    System.out.println("[Debug] Old VM disposed");
                } catch (VMDisconnectedException e) {
                    // Ignoré
                }
            }

            // Redémarrer le debugger
            System.out.println("[Debug] Restarting debugger");
            startDebugger();

        } catch (Exception e) {
            System.out.println("Error restarting VM: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public VirtualMachine connectAndLaunchVM() throws IOException, IllegalConnectorArgumentsException, VMStartException {
        LaunchingConnector launchingConnector = Bootstrap.virtualMachineManager().defaultConnector();
        Map<String, Connector.Argument> arguments = launchingConnector.defaultArguments();
        arguments.get("main").setValue(debugClass.getName());
        return launchingConnector.launch(arguments);
    }

    public void attachTo(Class debuggeeClass) {
        this.debugClass = debuggeeClass;
        try {
            vm = connectAndLaunchVM();
            stepBackManager = new StepBackManager(this, vm);  // Passons la VM
            commandManager = new CommandManager(vm, stepBackManager);
            enableClassPrepareRequest(vm);
            startDebugger();
        } catch (Exception e) {
            System.out.println("Erreur lors de l'initialisation du debugger: " + e.getMessage());
        }
    }

    public void startDebugger() {
        try {
            // Initialiser le ClassPrepareRequest pour la classe cible si on est en step back
            if (stepBackManager.isStepBackInProgress()) {
                System.out.println("[Debug] Setting up class prepare request for step back");
                ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
                classPrepareRequest.addClassFilter(debugClass.getName());
                classPrepareRequest.enable();
            }

            EventSet eventSet;
            boolean waitingForBreakpoint = stepBackManager.isStepBackInProgress();
            long startTime = System.currentTimeMillis();
            final long TIMEOUT = 5000; // 5 secondes

            while ((eventSet = vm.eventQueue().remove()) != null) {
                for (Event event : eventSet) {
                    if (event instanceof VMDisconnectEvent) {
                        System.out.println("[Debug] VM Disconnect detected");
                        handleVMDisconnect();
                        return;
                    }

                    if (event instanceof ClassPrepareEvent) {
                        ClassPrepareEvent cpe = (ClassPrepareEvent) event;
                        System.out.println("[Debug] Class prepared: " + cpe.referenceType().name());

                        if (stepBackManager.isStepBackInProgress()) {
                            System.out.println("[Debug] Setting breakpoint after class prepare");
                            stepBackManager.configureInitialBreakpoint();
                            // On doit continuer l'exécution pour atteindre le breakpoint
                            vm.resume();
                            continue;
                        } else {
                            handleClassPrepareEvent((ClassPrepareEvent) event);
                        }
                    }

                    if (event instanceof BreakpointEvent) {
                        BreakpointEvent bpEvent = (BreakpointEvent) event;
                        System.out.println("[Debug] Breakpoint hit at: " + bpEvent.location());

                        // Vérifier si c'est notre breakpoint de step back
                        EventRequest request = bpEvent.request();
                        if (request != null && request.getProperty("isStepBackBreakpoint") != null) {
                            try {
                                System.out.println("[Debug] Step back breakpoint hit");
                                stepBackManager.handleBreakpointHit(bpEvent);
                                // Important : attendre les commandes utilisateur ici
                                waitForUserCommands(bpEvent);
                                waitingForBreakpoint = false;
                            } catch (VMDisconnectedException e) {
                                System.out.println("[Debug] VM disconnected during step back");
                                return;
                            }
                        } else {
                            handleDebugEvent(event);
                        }
                    } else if (event instanceof StepEvent) {
                        System.out.println("[Debug] Step event at: " + ((StepEvent)event).location());
                        handleDebugEvent(event);
                    }

                    // Vérifier le timeout
                    if (waitingForBreakpoint && System.currentTimeMillis() - startTime > TIMEOUT) {
                        System.out.println("[Debug] Step back timeout reached");
                        stepBackManager.reset();
                        return;
                    }

                    // Si on n'attend pas de breakpoint ou si l'événement n'est pas un breakpoint
                    if (!waitingForBreakpoint || !(event instanceof BreakpointEvent)) {
                        vm.resume();
                    }
                }
            }
        } catch (VMDisconnectedException e) {
            System.out.println("[Debug] VM Disconnected");
            handleVMDisconnect();
        } catch (Exception e) {
            System.out.println("[Debug] Error in debugger: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void handleClassPrepareEvent(ClassPrepareEvent event) {
        try {
            // Si nous sommes dans un step back, configurer le breakpoint cible
            if (stepBackManager != null && stepBackManager.isStepBackInProgress()) {
                System.out.println("[Debug] Class prepared during step back: " + event.referenceType().name());
                stepBackManager.configureInitialBreakpoint();
            } else if (isFirstLaunch) {
                // Configuration normale du premier lancement
                System.out.println("Analyzing ClassPrepareEvent:");
                System.out.println("Prepared class: " + event.referenceType().name());
                System.out.println("Class loader: " + event.referenceType().classLoader());
                setBreakPoint(debugClass.getName(), 31);
                isFirstLaunch = false;
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la préparation de la classe: " + e.getMessage());
        }
    }

    private void handleDebugEvent(Event event) {
        if (event instanceof LocatableEvent) {
            LocatableEvent locEvent = (LocatableEvent) event;
            stepBackManager.recordState(locEvent);

            if (stepBackManager.isStepBackInProgress() &&
                    stepBackManager.isAtTargetState(locEvent.location())) {
                System.out.println("[Debug] Reached step back target");
                stepBackManager.restoreBreakpoints();
                stepBackManager.reset();
            }
        }

        if (event instanceof ClassPrepareEvent) {
            handleClassPrepareEvent((ClassPrepareEvent) event);
        } else if (event instanceof BreakpointEvent) {
            handleBreakpointEvent((BreakpointEvent) event);
        } else if (event instanceof StepEvent) {
            handleStepEvent((StepEvent) event);
        }

        if (event instanceof LocatableEvent && !stepBackManager.isStepBackInProgress()) {
            boolean shouldContinue = false;
            while (!shouldContinue) {
                String command = getNextCommand((LocatableEvent)event);
                if (command.equals("back")) {
                    System.out.println("[Debug] Starting step back process");
                }
                shouldContinue = commandManager.isControlCommand(command);
            }
        }
    }

    private void handleStepEvent(StepEvent event) {
        System.out.println("[Debug] Step Event at: " + event.location());

        try {
            // Désactive la requête de step actuelle
            EventRequest request = event.request();
            if (request != null && request.isEnabled()) {
                request.disable();
                vm.eventRequestManager().deleteEventRequest(request);
            }

            if (stepBackManager.isStepBackInProgress()) {
                if (stepBackManager.isAtTargetState(event.location())) {
                    System.out.println("[Debug] Reached target location for step back!");
                    handleStepBackCompletion(event);
                } else {
                    System.out.println("[Debug] Continuing stepping during step back");
                    // Continue stepping automatically
                    StepRequest stepRequest = vm.eventRequestManager().createStepRequest(
                            event.thread(),
                            StepRequest.STEP_MIN,
                            StepRequest.STEP_INTO
                    );
                    stepRequest.enable();
                    vm.resume();
                }
            }
        } catch (Exception e) {
            System.out.println("[Debug] Error handling step event: " + e);
            e.printStackTrace();
        }
    }

    private void handleStepBackCompletion(LocatableEvent event) {
        System.out.println("[Debug] Step back completed");

        // Réactive les breakpoints sauvegardés
        stepBackManager.restoreBreakpoints();

        // Reset l'état du step back
        stepBackManager.reset();
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
            return getNextCommand(event);
        }
    }

    private void handleBreakpointEvent(BreakpointEvent event) {
        EventRequest request = event.request();
        System.out.println("[Debug] Breakpoint reached at: " + event.location());

        // Vérifie si c'est un breakpoint de step back
        Object isStepBackBp = request.getProperty("isStepBackBreakpoint");
        if (isStepBackBp != null && (Boolean)isStepBackBp) {
            System.out.println("[Debug] Step back breakpoint reached");

            // Supprime le breakpoint temporaire de step back
            vm.eventRequestManager().deleteEventRequest(request);

            // Réactive les breakpoints sauvegardés et reset l'état
            stepBackManager.restoreBreakpoints();
            stepBackManager.reset();
        } else {
            System.out.println("[Debug] Regular breakpoint reached");
            System.out.println("Thread: " + event.thread().name());
            System.out.println("Location: " + event.location().toString());
        }
    }

    private void handleVMDisconnect() {
        System.out.println("End of program.");
        try {
            InputStreamReader reader = new InputStreamReader(vm.process().getInputStream());
            OutputStreamWriter writer = new OutputStreamWriter(System.out);
            reader.transferTo(writer);
            writer.flush();
        } catch (IOException e) {
            System.out.println("Target VM input stream reading error.");
        }
    }

    public void enableClassPrepareRequest(VirtualMachine vm) {
        ClassPrepareRequest classPrepareRequest = vm.eventRequestManager().createClassPrepareRequest();
        classPrepareRequest.addClassFilter(debugClass.getName());
        classPrepareRequest.enable();

        if (stepBackManager != null && stepBackManager.isStepBackInProgress()) {
            System.out.println("[Debug] Configuring step back breakpoint after class prepare");
            stepBackManager.configureInitialBreakpoint();
        }
    }

    public void setBreakPoint(String className, int lineNumber) {
        try {
            for (ReferenceType targetClass : vm.allClasses()) {
                if (targetClass.name().equals(className)) {
                    Location location = targetClass.locationsOfLine(lineNumber).get(0);
                    BreakpointRequest bpReq = vm.eventRequestManager().createBreakpointRequest(location);
                    bpReq.enable();
                }
            }
        } catch (Exception e) {
            System.out.println("Erreur lors de la création du breakpoint: " + e.getMessage());
        }
    }

    private void waitForUserCommands(LocatableEvent event) {
        System.out.println("[Debug] Waiting for user commands after step back");
        boolean shouldContinue = false;
        while (!shouldContinue) {
            String command = getNextCommand(event);
            shouldContinue = commandManager.isControlCommand(command);
        }
    }
}