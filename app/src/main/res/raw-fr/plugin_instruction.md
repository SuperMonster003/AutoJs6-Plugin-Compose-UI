Compose UI est un plugin de rendu d'interface pour AutoJs6. Les scripts declarent leur interface via l'objet global `compose` integre a l'hote, et le plugin la rend dans le processus de l'hote avec Jetpack Compose et Material 3, offrant une solution declarative unique au contenu des activites en mode `"ui";` et aux fenetres flottantes.

Aperçu de développement P2: les 30 entrées du catalogue V1 sont implémentées dans un hôte de test dédié, avec 29 composants de noeud et la commande Snackbar. Les champs de texte natifs, listes à chargement différé, Scaffold, dialogues et indicateurs de progression sont disponibles dans cet aperçu. L'API publique de script compose reste prévue pour P3.

### Utilisation

1. Installez AutoJs6 6.8.0 (5316) ou une version ulterieure
2. Installez l'APK de ce plugin (rien a ouvrir, le plugin n'a pas d'entree dans le lanceur)
3. Verifiez dans le centre de plugins d'AutoJs6 que Compose UI est reconnu et active
4. Essayez cet aperçu avec l'hôte de test dédié; le point d'entrée public des scripts `compose` est prévu pour P3

### Compatibilite

- Version minimale d'AutoJs6 : 6.8.0 (5316) ou ulterieure ; les hotes plus anciens signalent le plugin comme incompatible dans le centre de plugins
- Version d'Android : 7.0 (API 24) ou ulterieure
- Architecture du processeur : arm64-v8a / armeabi-v7a / x86_64 / x86 (les quatre integrees dans l'APK unique, aucun choix par architecture)
- Version de Compose : embarquee dans le plugin (BOM 2026.09.00), independante du runtime Compose de l'hote
- Version du contrat : 1 ; l'hote et le plugin negocient la version du contrat et refusent le chargement avec une erreur explicite en cas de desaccord

### FAQ

- Pourquoi aucune icone de plugin n'apparait apres l'installation ? Le plugin n'a ni interface autonome ni entree dans le lanceur ; consultez le centre de plugins d'AutoJs6
- Pourquoi `compose` ne fonctionne-t-il pas encore dans les scripts ? Cet aperçu de développement P2 fonctionne dans un hôte de test dédié; le point d'entrée des scripts compose est prévu pour P3
- Faut-il desinstaller d'autres plugins d'interface ? Non, Compose UI n'interfere ni avec le module `ui` existant ni avec les autres plugins
- Faut-il modifier les scripts apres une mise a jour du plugin ? Non tant que la version du contrat reste la meme ; les montees de version du contrat sont indiquees explicitement dans le journal des modifications

### Permissions et securite

- Protection des composants : la Wake Activity et le service INFO sont tous deux proteges par la permission de signature `org.autojs.permission.PLUGIN`, si bien que seul l'hote AutoJs6 peut les atteindre
- Aucune activite en arriere-plan : le plugin n'a ni service resident, ni recepteur de diffusion, ni tache planifiee, et ne consomme aucune ressource tant que l'hote ne le charge pas
- Frontiere des donnees : le plugin ne lit ni n'ecrit les donnees des scripts ou les fichiers de l'utilisateur ; l'etat de l'interface n'existe que dans la memoire du processus de l'hote
- Politique de sauvegarde : la sauvegarde de l'application et le transfert entre appareils sont desactives, et le plugin ne detient aucune donnee a migrer

Plus d'informations (demarrage rapide, notes de compilation, feuille de route) sur la page du projet : https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI
