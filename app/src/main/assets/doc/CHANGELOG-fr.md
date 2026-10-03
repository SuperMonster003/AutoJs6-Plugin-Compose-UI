******

### Historique des versions

******

# v1.0.0

###### 2026/10/03

* `Indication` Aperçu de développement local 1.0.0: une compilation AutoJs6 correspondante et le plugin installé et activé sont nécessaires. Pages UI, fenêtres flottantes, cinq exemples, référence API et déclarations TypeScript sont fournis pour cette intégration locale. La portée des vérifications de compatibilité et de performance figure dans la feuille de route. Le plugin ne figure pas dans l'index officiel et n'a pas de publication officielle. L'icône reste provisoire en attendant les images définitives du mainteneur
* `Indication` Necessite AutoJs6 6.8.0 (5316) ou ulterieur
* `Indication` Les applications empaquetées nécessitent aussi l'installation séparée d'un plugin Compose UI compatible, avec activation/autorisation propres à cette application; la compatibilité porte sur le runtime AutoJs6 intégré, pas sur le versionCode de l'application
* `Indication` Les deux mipmap clair/sombre sont conservés; le dessin actuel est provisoire et sera remplacé par les images noir et blanc définitives du mainteneur
* `Nouveaute` compose / $compose appelables, handles de noeuds persistants, state/render/ref réactifs, modifications groupées, publications en file et contrôle du thème
* `Nouveaute` Noyau Material 3: 29 fabriques de noeuds pour les dispositions, textes, icônes, images, boutons, saisies, sélections, listes paresseuses, dialogues et indicateurs; Snackbar est une commande de session, pas une fabrique compose.Snackbar
* `Nouveaute` Les scripts UI montent le contenu Activity; compose.floaty propose aussi aux scripts hors UI des fenêtres raw ou redimensionnables, une géométrie en pixels, le contrôle du toucher/focus et la libération de leurs ressources
* `Nouveaute` Les 20 opérations Modifier conservent l'ordre déclaré et prennent en charge la validation du contexte de mise en page, le défilement et les libellés d'accessibilité
* `Nouveaute` Les thèmes Material 3 prennent en charge les couleurs sources, les modes clair et sombre, les couleurs dynamiques du système sur Android 12+, les familles de polices et la taille du texte
* `Nouveaute` La saisie native préserve la sélection et la composition IME, permet le focus et les modifications explicites, et rejette les modifications tardives qui écraseraient une saisie plus récente; les interrupteurs et curseurs restent contrôlés par le script
* `Nouveaute` Icônes core et images ImageWrapper/Bitmap, fichiers locaux et drawable de l'hôte; le moteur de rendu ne recycle pas automatiquement les images appartenant à l'appelant
* `Nouveaute` Cinq exemples exécutables de compteur, validation de formulaire, liste à clés stables de 1000 éléments, HUD flottant hors mode UI et thèmes, avec prérequis et index, synchronisés dans la catégorie Compose UI des exemples de l'hôte correspondant
* `Nouveaute` Référence API et déclarations TypeScript associées, ainsi que README, instructions du centre des plugins et historique en 10 langues
* `Nouveaute` Détection dans le centre des plugins avec vérifications de version hôte, de contrat et d'autorisation, sans écran autonome ni entrée de lanceur
* `Correction` Compose UI refuse de monter une autre page ou fenêtre flottante dans un rappel de rendu et conserve la page actuelle; les pages peuvent être remontées après une mise à jour du plugin
* `Amelioration` Les vérifications et ComposeError signalent uniformément les plugins absents, désactivés, non autorisés ou incompatibles, les permissions manquantes et les sessions fermées; la libération couvre aussi les fenêtres annulées avant leur attachement natif; La mise à jour, la désinstallation ou la désactivation du plugin ferme ses sessions actives et signale l'erreur correspondante
* `Dependance` Ajout de common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, verrouille par empreinte)
* `Dependance` Ajout de Jetpack Compose BOM 2026.09.00 (Apache 2.0)
* `Dependance` Ajout de compose-ui-api.aar V1 aligné sur AutoJs6 6.8.0 (5316) (MPL 2.0, empreinte verrouillée), avec les dépendances partagées alignées sur l'hôte
* `Dependance` Ajout de Compose UI Test géré par BOM 2026.09.00 (Apache 2.0, uniquement pour les tests)
* `Dependance` Ajout de JaCoCo version 0.8.14 (couverture des tests facultative uniquement, exclu des paquets de publication)
