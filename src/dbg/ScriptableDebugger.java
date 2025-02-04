package dbg;

import com.sun.jdi.*;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.connect.LaunchingConnector;
import com.sun.jdi.connect.VMStartException;
import com.sun.jdi.event.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.StepRequest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Scanner;

public class ScriptableDebugger {

    private Class debugClass;
    private VirtualMachine vm;
    private Scanner commandScanner;


    public ScriptableDebugger() {
        commandScanner = new Scanner(System.in);
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
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startDebugger() throws VMDisconnectedException, InterruptedException, AbsentInformationException {
        EventSet eventSet = null;
        while ((eventSet = vm.eventQueue().remove()) != null) {
            for (Event event : eventSet) {
                // System.out.println(event.toString());
                if(event instanceof VMDisconnectEvent ) {
                    System.out.println("End of program.");
                    InputStreamReader reader = new InputStreamReader(vm.process().getInputStream());
                    OutputStreamWriter writer = new OutputStreamWriter(System.out ) ;
                    try {
                        reader.transferTo(writer) ;
                        writer.flush();
                    }catch( IOException e ) {
                        System.out.println("Target VM input stream reading error.");
                    }
                    commandScanner.close();
                }
                if (event instanceof ClassPrepareEvent classEvent) {
                    System.out.println("Analyzing ClassPrepareEvent:");
                    System.out.println("Prepared class: " + classEvent.referenceType().name());
                    System.out.println("Class loader: " + classEvent.referenceType().classLoader());
                    System.out.println();
                    setBreakPoint(debugClass.getName(), 6);
                }
                if (event instanceof BreakpointEvent breakEvent) {
                    System.out.println("Breakpoint reached");
                    System.out.println("Thread: " + breakEvent.thread().name());
                    System.out.println("Location: " + breakEvent.location().toString());
                    System.out.println();
                    // Au lieu d'activer automatiquement le step, on attend une commande utilisateur
                    handleUserCommand(breakEvent);
                }

                if (event instanceof StepEvent) {
                    StepEvent stepEvent = (StepEvent) event;
                    System.out.println("Analyzing StepEvent:");
                    System.out.println("Location: " + stepEvent.location());
                    // On désactive la requête de step précédente avant d'en créer une nouvelle potentielle
                    stepEvent.request().disable();
                    // On attend une nouvelle commande utilisateur
                    handleUserCommand(stepEvent);
                }
                vm.resume();
            }
        }
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
        System.out.print("Enter command : ");
        String command = commandScanner.nextLine().trim().toLowerCase();

        if (command.equals("step")) {
            StepRequest stepRequest = vm.eventRequestManager().createStepRequest(
                    event.thread(),
                    StepRequest.STEP_MIN,
                    StepRequest.STEP_OVER
            );
            stepRequest.enable();
        }
        // Si ce n'est pas "step", on continue simplement l'exécution
    }
}
