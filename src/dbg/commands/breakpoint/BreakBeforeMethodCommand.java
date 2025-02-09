package dbg.commands.breakpoint;

import com.sun.jdi.*;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import dbg.commands.interfaces.DebugCommand;

public class BreakBeforeMethodCommand implements DebugCommand {
    private final VirtualMachine vm;
    private final String methodName;

    public BreakBeforeMethodCommand(VirtualMachine vm, String methodName) {
        this.vm = vm;
        this.methodName = methodName;
    }

    @Override
    public Object execute() {
        int breakpointsSet = 0;

        // Itérer sur toutes les classes chargées dans la VM
        for (ReferenceType refType : vm.allClasses()) {
            // Itérer sur toutes les méthodes de la classe
            for (Method method : refType.methods()) {
                // Vérifier si le nom de la méthode correspond au nom spécifié
                if (method.name().equalsIgnoreCase(methodName)) {
                    try {
                        // Obtenir l'emplacement du début de la méthode
                        Location methodStart = method.location();
                        // Créer une requête de point d'arrêt à l'emplacement du début de la méthode
                        BreakpointRequest bpReq = vm.eventRequestManager()
                                .createBreakpointRequest(methodStart);
                        // Définir la politique de suspension pour suspendre le thread de l'événement
                        bpReq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                        // Activer la requête de point d'arrêt
                        bpReq.enable();
                        breakpointsSet++;

                        System.out.printf("Point d'arret defini au debut de la methode %s dans la classe %s%n",
                                methodName, refType.name());
                    } catch (Exception e) {
                        System.out.printf("Impossible de definir un point d'arret pour la methode %s dans la classe %s : %s%n (verifier le nom du fichier fourni avec le package)",
                                methodName, refType.name(), e.getMessage());
                    }
                }
            }
        }

        // Si aucun point d'arrêt n'a été défini, afficher un message
        if (breakpointsSet == 0) {
            System.out.printf("Aucune methode nommée '%s' trouvee%n", methodName);
            return null;
        }

        System.out.printf("Defini %d point(s) d'arret pour la methode %s%n", breakpointsSet, methodName);
        return breakpointsSet;
    }
}