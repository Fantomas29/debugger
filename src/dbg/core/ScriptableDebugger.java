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
    private boolean programFinished = false;

    // Constructeur pour initialiser le scanner et l'état d'attente de commande
    public ScriptableDebugger() {
        this.scanner = new Scanner(System.in);
        this.waitingForCommand = new AtomicBoolean(false);
    }

    // Définir la ligne de point d'arrêt initiale
    public void setInitialBreakpoint(int line) {
        this.initialBreakpointLine = line;
    }

    // Libérer les ressources et arrêter la machine virtuelle et le processus
    public void dispose() {
        isDisposed = true;
        if (vm != null) {
            vm.dispose();
        }
        if (process != null) {
            process.destroy();
        }
    }

    // Attacher le débogueur à une classe spécifique
    public void attachTo(Class<?> debugClass) throws Exception {
        this.debugClass = debugClass;
        if (stepBackManager != null) {
            stepBackManager.clearHistory();
        }
        startDebugee();
        eventLoop();
    }

    // Exécuter une commande GUI
    public void executeGuiCommand(String command) {
        if (programFinished) {
            System.out.println("Le programme est terminé. Veuillez redémarrer le débogueur.");
            return;
        }

        if (currentEvent != null && waitingForCommand.get()) {
            System.out.println("\n> Exécution de la commande : " + command);

            if (command.equals("quit")) {
                dispose();
                return;
            }

            try {
                if (!commandManager.isValidCommand(command)) {
                    System.out.println("Commande inconnue. Commandes disponibles : " +
                            commandManager.getAvailableCommands());
                    return;
                }

                commandManager.executeCommand(command, currentEvent);
                if (commandManager.isControlCommand(command)) {
                    waitingForCommand.set(false);
                    vm.resume();
                }
            } catch (Exception e) {
                System.out.println("Erreur lors de l'exécution de la commande : " + e.getMessage());
            }
            System.out.println("----------------------------------------");
        }
    }

    // Redémarrer la machine virtuelle
    public void restartVM() {
        programFinished = false;
        // Exécute le redémarrage dans un nouveau thread pour ne pas bloquer l'interface
        new Thread(() -> {
            try {
                System.out.println("[Debug-VM] Démarrage du redémarrage de la VM");
                if (vm != null) {
                    System.out.println("[Debug-VM] Libération de l'ancienne VM");
                    vm.dispose();
                }
                if (process != null) {
                    System.out.println("[Debug-VM] Destruction de l'ancien processus");
                    process.destroy();
                }

                System.out.println("[Debug-VM] Création d'un nouveau débogué");
                startDebugee();
                Thread.sleep(100);
                System.out.println("[Debug-VM] Démarrage de la boucle d'événements");
                System.out.println("----------------------------------------");
                eventLoop();
                System.out.println("[Debug-VM] Redémarrage terminé");
            } catch (Exception e) {
                System.out.println("[Debug-VM] Erreur lors du redémarrage de la VM : " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // Démarrer le processus de débogage
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

    // Définir un point d'arrêt pour revenir en arrière
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
            System.out.println("[Debug] Erreur lors de la définition du point d'arrêt pour revenir en arrière : " + e.getMessage());
        }
    }

    // Boucle d'événements pour gérer les événements de débogage
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
                } else if (event instanceof VMDeathEvent) {
                    connected = false;
                    programFinished = true;
                    System.out.println("Programme terminé.");
                    clearStepBack();
                } else if (event instanceof VMDisconnectEvent) {
                    connected = false;
                    if (!programFinished) {
                        System.out.println("Déconnexion de la VM.");
                    }
                }
            }

            if (connected && shouldResume && !isDisposed) {
                eventSet.resume();
            }
        }
    }

    // Effacer l'historique de retour en arrière
    private void clearStepBack() {
        if (stepBackManager != null) {
            stepBackManager.clearHistory();
        }
    }

    // Gérer l'événement de préparation de la classe
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
            System.out.println("[Debug] Erreur lors de la définition du point d'arrêt : " + e.getMessage());
            throw e;
        }
    }

    // Gérer l'événement de point d'arrêt
    private void handleBreakpointEvent(BreakpointEvent event) {
        displayLocation(event);
        if (stepBackManager.isReplaying) {
            setBreakPointStepBack(event.getClass());
        }
        currentEvent = event;
        waitingForCommand.set(true);
        waitForGuiCommand();
    }

    // Gérer l'événement de pas à pas
    private void handleStepEvent(StepEvent event) {
        displayLocation(event);
        currentEvent = event;
        waitingForCommand.set(true);
        waitForGuiCommand();
    }

    // Afficher l'emplacement de l'événement
    private void displayLocation(LocatableEvent event) {
        Location location = event.location();
        System.out.printf("%s.%s():%d%n",
                location.declaringType().name(),
                location.method().name(),
                location.lineNumber());
    }

    // Attendre une commande GUI
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