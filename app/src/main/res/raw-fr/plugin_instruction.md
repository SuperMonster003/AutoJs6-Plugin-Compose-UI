3-Block Compose est un plugin de rendu d'interface pour AutoJs6. Les scripts déclarent les interfaces via `compose` / `$compose` de l'hôte, puis le plugin les rend dans son processus avec Jetpack Compose et Material 3. L'aperçu prend en charge les activités `"ui";` et les fenêtres flottantes des scripts non UI.

Aperçu de développement local 1.3.0: nécessite une version de l'hôte AutoJs6 portant la nouvelle identité 3-Block Compose (commit hôte f12665ffde ou ultérieur) avec ce plugin installé et activé; l'entrée de menu de l'éditeur AutoJs6 "Ouvrir dans le concepteur 3-Block Compose" nécessite une version postérieure au commit hôte db7e6a1734. L'accompagnement local fournit les pages UI, les fenêtres flottantes, le concepteur visuel, cinq exemples, la référence API, la page de guide du concepteur et les déclarations TypeScript. Le périmètre vérifié de compatibilité et de performance est consigné dans la feuille de route. Pas encore inscrit à l'index officiel; aucune version officielle. Les icônes utilisent le visuel final du mainteneur.

### Utilisation

1. Installez une compilation locale compatible d'AutoJs6 contenant l'entrée compose (minimum 6.8.0 / 5322)
2. Installez l'APK du plugin; ouvrez 3-Block Compose depuis le lanceur pour atteindre l'accueil du concepteur, créer un projet de script ou ouvrir la galerie de composants et les paramètres depuis la barre supérieure
3. Verifiez dans le centre de plugins d'AutoJs6 que 3-Block Compose est reconnu et active
4. Utilisez `compose` ou `$compose` dans les scripts; montez les activités avec `compose.mount`, ou accordez la permission de superposition à l'hôte et utilisez `compose.floaty`
5. Le menu de l'éditeur propose exécuter, ouvrir dans l'éditeur AutoJs6, importer dans les scripts AutoJs6, copier et exporter; choisir 3-Block Compose depuis le menu de l'éditeur AutoJs6 (nécessite une version d'AutoJs6 avec la mise à jour correspondante) ou depuis un gestionnaire de fichiers modifie directement le fichier de script

### Compatibilite

- Version minimale d'AutoJs6 : 6.8.0 (5322) ou ulterieure ; les hotes plus anciens signalent le plugin comme incompatible dans le centre de plugins
- Version d'Android : 7.0 (API 24) ou ulterieure
- Architecture du processeur : arm64-v8a / armeabi-v7a / x86_64 / x86 (les quatre integrees dans l'APK unique, aucun choix par architecture)
- Version de Compose : embarquee dans le plugin (BOM 2026.09.00), independante du runtime Compose de l'hote
- Version du contrat : 2 ; l'hote et le plugin negocient la version du contrat et refusent le chargement avec une erreur explicite en cas de desaccord
- Les applications empaquetées nécessitent aussi l'installation séparée d'un plugin 3-Block Compose compatible, avec activation/autorisation propres à cette application; la compatibilité porte sur le runtime AutoJs6 intégré, pas sur le versionCode de l'application
- Les fabriques de View tournent sur le thread principal avant le rendu. Un remplacement invalide conserve le contenu courant; une View ne peut appartenir à deux noeuds ni être prise à un autre parent. Ses écouteurs et les ressources du code appelant sont préservés
- cancelable=false désactive la fermeture par retour, clic extérieur et balayage; la fermeture explicite et la fin du script libèrent le dialogue sans remplacer les pages et sessions existantes
- Cette version utilise le contrat 3-Block Compose V2 et nécessite AutoJs6 6.8.0 / 5322; TSX nécessite TypeScript Engine 0.6.7. Les nouveaux hôtes acceptent les composants existants des moteurs V1; les composants étendus nécessitent un moteur V2
- compose.memo est fourni par AutoJs6 6.8.0 / 5323 pour réutiliser les fragments de render dont les dépendances n'ont pas changé; ce plugin n'a pas besoin de mise à jour et TSX nécessite TypeScript Engine 0.6.8
- Depuis 1.2.0 les classes du contrat (org.autojs.plugin.compose.api) sont empaquetées avec le plugin pour son processus autonome; dans AutoJs6 l'hôte fournit les mêmes classes et la copie du plugin est masquée
- Le concepteur s'exécute dans le processus du plugin et ne dépend pas d'AutoJs6; exécuter, ouvrir dans l'éditeur AutoJs6 et importer dans le répertoire de scripts AutoJs6 nécessitent un AutoJs6 installé, et l'entrée de menu de l'éditeur "Ouvrir dans le concepteur 3-Block Compose" nécessite une version d'AutoJs6 qui la contient

### FAQ

- Comment exécuter les exemples de la galerie? "Exécuter dans AutoJs6" transmet le script à l'AutoJs6 installé; la galerie n'affiche que des aperçus et du code et n'exécute jamais de script
- Le concepteur peut-il ouvrir n'importe quel script? Il n'interprète que les déclarations `compose.state` de premier niveau et l'unique appel `compose.mount` / `floaty` / `dialog`; toute autre instruction est conservée telle quelle. Les formes qu'il ne peut pas mapper (une racine qui n'est pas une fabrique compose, plusieurs appels de montage, une fonction de rendu sans return) sont refusées avec ligne et colonne
- Le concepteur exécute-t-il les scripts? Non. Il ne fait que les analyser avec l'analyseur Rhino et les prévisualiser avec le moteur de rendu du plugin; les événements ne se déclenchent pas et le state affiche sa valeur initiale. L'exécution revient à AutoJs6
- Pourquoi `compose` est-il absent? L'objet global est fourni par la compilation locale compatible de l'hôte; installer uniquement l'APK du plugin ne l'ajoute pas
- Faut-il desinstaller d'autres plugins d'interface ? Non, 3-Block Compose n'interfere ni avec le module `ui` existant ni avec les autres plugins
- Que se passe-t-il si le plugin change? Sa mise à jour, désinstallation ou désactivation ferme les sessions actives et signale l'erreur correspondante; un plugin compatible et activé permet un nouveau montage
- Que faut-il pour les fenêtres flottantes? Accordez la permission de superposition à l'hôte et appelez `window.requestFocus()` avant la saisie. Si HyperOS ne montre pas la fenêtre, revenez au bureau. Une permission manquante renvoie PERMISSION_REQUIRED sans ouvrir automatiquement de demande d'autorisation
- Peut-on utiliser TSX ou toute fonction Compose? TSX accepte les fabriques Compose documentées avec le bon hôte et TypeScript Engine. Les fonctions Kotlin Composable arbitraires et les composants TSX personnalisés ne sont pas pris en charge
- La rotation perd-elle l'état? L'hôte actuel gère les changements ordinaires d'orientation sans remplacer le moteur de script. Une véritable recréation ou destruction de l'Activity ferme le moteur et ses sessions; l'état métier n'est pas restauré automatiquement
- Comment rechercher les composants? testTag est exposé comme ID brut sans préfixe de paquet. id/testTag et desc/contentDescription sont distincts; le texte d'un Button peut être un enfant, auquel cas suivez parent() vers un ancêtre cliquable

### Permissions et securite

- Protection des composants: la Wake Activity et le service INFO sont protégés par la permission de signature `org.autojs.permission.PLUGIN`, seul l'hôte AutoJs6 peut donc y accéder; l'éditeur du concepteur est un composant exporté sans permission qui n'accepte qu'un Uri de document de script remis par l'utilisateur, la lecture et l'écriture étant limitées à cet Uri
- Comportement en arrière-plan: le plugin n'a ni service résident ni tâche planifiée et ne reçoit qu'une diffusion système lors de sa mise à jour, pour normaliser ses composants d'icône de lanceur; il ne consomme rien tant que l'hôte ne le charge pas et que ni le concepteur ni la galerie ne sont ouverts
- Frontière des données: le moteur de rendu ne lit ni n'écrit jamais les données de script ou les fichiers de l'utilisateur, et l'état de l'interface ne vit que dans la mémoire du processus hôte; les projets et brouillons du concepteur résident dans le répertoire privé du plugin, ne sont importés et exportés que par le sélecteur de documents du système, ou lisent et écrivent un seul fichier de script par un Uri remis par l'utilisateur; le répertoire des projets n'est partagé que par le fournisseur de contenu du plugin, à chaque demande, lors de la remise d'un projet à AutoJs6
- Politique de sauvegarde : la sauvegarde de l'application et le transfert entre appareils sont desactives, et le plugin ne detient aucune donnee a migrer

Plus d'informations (demarrage rapide, notes de compilation, feuille de route) sur la page du projet : https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose
