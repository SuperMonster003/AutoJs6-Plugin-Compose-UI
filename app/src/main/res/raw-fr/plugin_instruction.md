Compose UI est un plugin de rendu d'interface pour AutoJs6. Les scripts déclarent les interfaces via `compose` / `$compose` de l'hôte, puis le plugin les rend dans son processus avec Jetpack Compose et Material 3. L'aperçu prend en charge les activités `"ui";` et les fenêtres flottantes des scripts non UI.

Aperçu de développement local 1.1.0: une compilation AutoJs6 correspondante et le plugin installé et activé sont nécessaires. Pages UI, fenêtres flottantes, cinq exemples, référence API et déclarations TypeScript sont fournis pour cette intégration locale. La portée des vérifications de compatibilité et de performance figure dans la feuille de route. Le plugin ne figure pas dans l'index officiel et n'a pas de publication officielle. L'icône reste provisoire en attendant les images définitives du mainteneur.

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
- Les applications empaquetées nécessitent aussi l'installation séparée d'un plugin Compose UI compatible, avec activation/autorisation propres à cette application; la compatibilité porte sur le runtime AutoJs6 intégré, pas sur le versionCode de l'application
- TSX nécessite le build local AutoJs6 6.8.0 / 5319 correspondant et TypeScript Engine avec les déclarations Compose; installer uniquement le moteur de rendu ne suffit pas
- Cette intégration nécessite AutoJs6 6.8.0 / 5320 et un build Compose UI avec extension AndroidView; TSX nécessite aussi TypeScript Engine 0.6.5. Le rendu V1 de base conserve le minimum 5316
- Les fabriques de View tournent sur le thread principal avant le rendu. Un remplacement invalide conserve le contenu courant; une View ne peut appartenir à deux noeuds ni être prise à un autre parent. Ses écouteurs et les ressources du code appelant sont préservés
- Les dialogues indépendants nécessitent AutoJs6 6.8.0 / 5321 et Compose UI avec dialog-v1; TSX nécessite aussi TypeScript Engine 0.6.6. Les scripts hors UI nécessitent la permission de superposition de l'hôte
- cancelable=false désactive la fermeture par retour, clic extérieur et balayage; la fermeture explicite et la fin du script libèrent le dialogue sans remplacer les pages et sessions existantes

### FAQ

- Pourquoi aucune icone de plugin n'apparait apres l'installation ? Le plugin n'a ni interface autonome ni entree dans le lanceur ; consultez le centre de plugins d'AutoJs6
- Pourquoi `compose` est-il absent? L'objet global est fourni par la compilation locale compatible de l'hôte; installer uniquement l'APK du plugin ne l'ajoute pas
- Faut-il desinstaller d'autres plugins d'interface ? Non, Compose UI n'interfere ni avec le module `ui` existant ni avec les autres plugins
- Que se passe-t-il si le plugin change? Sa mise à jour, désinstallation ou désactivation ferme les sessions actives et signale l'erreur correspondante; un plugin compatible et activé permet un nouveau montage
- Que faut-il pour les fenêtres flottantes? Accordez la permission de superposition à l'hôte et appelez `window.requestFocus()` avant la saisie. Si HyperOS ne montre pas la fenêtre, revenez au bureau. Une permission manquante renvoie PERMISSION_REQUIRED sans ouvrir automatiquement de demande d'autorisation
- Peut-on utiliser TSX ou toute fonction Compose? TSX accepte les fabriques Compose documentées avec le bon hôte et TypeScript Engine. Les fonctions Kotlin Composable arbitraires et les composants TSX personnalisés ne sont pas pris en charge
- La rotation perd-elle l'état? L'hôte actuel gère les changements ordinaires d'orientation sans remplacer le moteur de script. Une véritable recréation ou destruction de l'Activity ferme le moteur et ses sessions; l'état métier n'est pas restauré automatiquement
- Comment rechercher les composants? testTag est exposé comme ID brut sans préfixe de paquet. id/testTag et desc/contentDescription sont distincts; le texte d'un Button peut être un enfant, auquel cas suivez parent() vers un ancêtre cliquable

### Permissions et securite

- Protection des composants : la Wake Activity et le service INFO sont tous deux proteges par la permission de signature `org.autojs.permission.PLUGIN`, si bien que seul l'hote AutoJs6 peut les atteindre
- Aucune activite en arriere-plan : le plugin n'a ni service resident, ni recepteur de diffusion, ni tache planifiee, et ne consomme aucune ressource tant que l'hote ne le charge pas
- Frontiere des donnees : le plugin ne lit ni n'ecrit les donnees des scripts ou les fichiers de l'utilisateur ; l'etat de l'interface n'existe que dans la memoire du processus de l'hote
- Politique de sauvegarde : la sauvegarde de l'application et le transfert entre appareils sont desactives, et le plugin ne detient aucune donnee a migrer

Plus d'informations (demarrage rapide, notes de compilation, feuille de route) sur la page du projet : https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI
