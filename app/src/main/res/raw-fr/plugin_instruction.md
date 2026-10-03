Compose UI est un plugin de rendu d'interface pour AutoJs6. Les scripts déclarent les interfaces via `compose` / `$compose` de l'hôte, puis le plugin les rend dans son processus avec Jetpack Compose et Material 3. L'aperçu prend en charge les activités `"ui";` et les fenêtres flottantes des scripts non UI.

Aperçu de développement P3: compose / $compose appelables, 29 fabriques de noeuds, handles persistants, state/render/ref réactifs, batch/post/theme, montage UI et fenêtres flottantes raw ou redimensionnables fonctionnent avec une compilation locale compatible de l'hôte AutoJs6. Les vérifications de disponibilité, erreurs typées et libération des sessions sont incluses. Les exemples inclus, la documentation complète, les déclarations de types et la matrice de vérification élargie restent à livrer. Cet aperçu local n'a pas de publication officielle.

### Utilisation

1. Installez une compilation locale compatible d'AutoJs6 contenant l'entrée compose (minimum 6.8.0 / 5316)
2. Installez l'APK de ce plugin (rien a ouvrir, le plugin n'a pas d'entree dans le lanceur)
3. Verifiez dans le centre de plugins d'AutoJs6 que Compose UI est reconnu et active
4. Utilisez `compose` ou `$compose` dans les scripts; montez les activités avec `compose.mount`, ou accordez la permission de superposition à l'hôte et utilisez `compose.floaty`

### Compatibilite

- Version minimale d'AutoJs6 : 6.8.0 (5316) ou ulterieure ; les hotes plus anciens signalent le plugin comme incompatible dans le centre de plugins
- Version d'Android : 7.0 (API 24) ou ulterieure
- Architecture du processeur : arm64-v8a / armeabi-v7a / x86_64 / x86 (les quatre integrees dans l'APK unique, aucun choix par architecture)
- Version de Compose : embarquee dans le plugin (BOM 2026.09.00), independante du runtime Compose de l'hote
- Version du contrat : 1 ; l'hote et le plugin negocient la version du contrat et refusent le chargement avec une erreur explicite en cas de desaccord

### FAQ

- Pourquoi aucune icone de plugin n'apparait apres l'installation ? Le plugin n'a ni interface autonome ni entree dans le lanceur ; consultez le centre de plugins d'AutoJs6
- Pourquoi `compose` est-il absent? L'objet global est fourni par la compilation locale compatible de l'hôte; installer uniquement l'APK du plugin ne l'ajoute pas
- Faut-il desinstaller d'autres plugins d'interface ? Non, Compose UI n'interfere ni avec le module `ui` existant ni avec les autres plugins
- Faut-il modifier les scripts apres une mise a jour du plugin ? Non tant que la version du contrat reste la meme ; les montees de version du contrat sont indiquees explicitement dans le journal des modifications
- Que faut-il pour les fenêtres flottantes? Accordez la permission de superposition à l'hôte et appelez `window.requestFocus()` avant la saisie. Si HyperOS ne montre pas la fenêtre, revenez au bureau. Une permission manquante renvoie PERMISSION_REQUIRED sans ouvrir automatiquement de demande d'autorisation

### Permissions et securite

- Protection des composants : la Wake Activity et le service INFO sont tous deux proteges par la permission de signature `org.autojs.permission.PLUGIN`, si bien que seul l'hote AutoJs6 peut les atteindre
- Aucune activite en arriere-plan : le plugin n'a ni service resident, ni recepteur de diffusion, ni tache planifiee, et ne consomme aucune ressource tant que l'hote ne le charge pas
- Frontiere des donnees : le plugin ne lit ni n'ecrit les donnees des scripts ou les fichiers de l'utilisateur ; l'etat de l'interface n'existe que dans la memoire du processus de l'hote
- Politique de sauvegarde : la sauvegarde de l'application et le transfert entre appareils sont desactives, et le plugin ne detient aucune donnee a migrer

Plus d'informations (demarrage rapide, notes de compilation, feuille de route) sur la page du projet : https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI
