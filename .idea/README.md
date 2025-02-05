# Documentation Technique du Debugger JDI

## Architecture et Choix d'Implémentation

### Organisation des Packages
```
src/dbg/
    ├── commands/
    │   ├── breakpoint/    # Commandes liées aux points d'arrêt
    │   ├── control/       # Commandes de contrôle d'exécution
    │   ├── info/          # Commandes d'information
    │   ├── object/        # Commandes d'inspection d'objets
    │   └── interfaces/    # Interfaces communes
    └── core/              # Noyau du debugger
```

### Pattern Command
L'utilisation du pattern Command a été choisie pour :
- Découpler l'invocation des commandes de leur implémentation
- Faciliter l'ajout de nouvelles commandes sans modifier le code existant
- Permettre une meilleure organisation et maintenance du code

Chaque commande :
- Implémente l'interface `DebugCommand`
- Est autonome et responsable d'une seule fonctionnalité
- Gère ses propres erreurs et retours

### Gestion des Points d'Arrêt
L'implémentation des points d'arrêt utilise deux approches distinctes :

1. **Points d'arrêt standards (`BreakCommand`)**
   - Création via `createBreakpointRequest()`
   - Activation permanente jusqu'à suppression explicite
   - Persistance à travers les itérations et appels multiples

2. **Points d'arrêt uniques (`BreakOnceCommand`)**
   - Utilisation d'une propriété "type" pour l'identification
   - Suppression automatique après le premier déclenchement
   - Nettoyage des ressources post-utilisation

### Gestion des Événements JDI

La gestion des événements suit une approche structurée :
1. Récupération des événements via la queue d'événements de la VM
2. Filtrage par type d'événement
3. Traitement spécifique selon le type
4. Nettoyage des ressources si nécessaire

Points clés :
- Gestion explicite du contrôle d'exécution
- Nettoyage systématique des requêtes obsolètes
- Reprise de l'exécution uniquement sur commande utilisateur

### Gestion de la Mémoire

Plusieurs mécanismes sont en place :
1. Suppression des anciennes requêtes de step avant d'en créer de nouvelles
2. Désactivation des points d'arrêt uniques après utilisation
3. Nettoyage des ressources lors de la déconnexion de la VM

### Retour d'Information

Standardisation des retours pour toutes les commandes :
- Messages d'erreur explicites en français
- Confirmation des actions effectuées
- Retour des objets pertinents pour chaînage possible