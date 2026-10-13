<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-three-block-compose-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Un plugin qui apporte les interfaces Jetpack Compose et Material 3 aux scripts AutoJs6</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages / Langues

******

Ce document est disponible dans les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/.readme/README-ar.md)

******

### Introduction

******

3-Block Compose est un plugin de rendu d'interface pour AutoJs6. Les scripts déclarent les interfaces via `compose` / `$compose` de l'hôte, puis le plugin les rend dans son processus avec Jetpack Compose et Material 3. L'aperçu prend en charge les activités `"ui";` et les fenêtres flottantes des scripts non UI.

Le plugin intègre un concepteur visuel et une galerie de composants: l'ouvrir depuis le lanceur mène à l'accueil du concepteur, où les projets de script sont créés ou importés puis édités sur le vrai moteur de rendu, tandis que la galerie présente les aperçus et scripts d'exemple de chaque composant; les interfaces des scripts restent rendues dans le processus de l'hôte. L'hôte découvre le plugin via le service INFO, lit sa version et sa compatibilité, puis instancie la fabrique du moteur de rendu dans son propre processus selon le contrat (`org.autojs.plugin.compose.api`).

******

### Etat actuel

******

Aperçu de développement local 1.3.0: nécessite une version de l'hôte AutoJs6 portant la nouvelle identité 3-Block Compose (commit hôte f12665ffde ou ultérieur) avec ce plugin installé et activé; l'entrée de menu de l'éditeur AutoJs6 "Ouvrir dans le concepteur 3-Block Compose" nécessite une version postérieure au commit hôte db7e6a1734. L'accompagnement local fournit les pages UI, les fenêtres flottantes, le concepteur visuel, cinq exemples, la référence API, la page de guide du concepteur et les déclarations TypeScript. Le périmètre vérifié de compatibilité et de performance est consigné dans la feuille de route. Pas encore inscrit à l'index officiel; aucune version officielle. Les icônes utilisent le visuel final du mainteneur.

******

### Fonctionnalites

******

Fonctions de l'aperçu de développement actuel:

- Concepteur visuel: le fichier de script est le projet, le vrai moteur de rendu affiche l'arbre d'interface pendant l'édition, le plan / la palette de composants / le tiroir de propriétés couvrent les 55 composants, avec annuler / rétablir et restauration des brouillons; le script exporté peut être copié ou envoyé directement à AutoJs6; les dépôts sur le canevas arrivent sous le pointeur, les fenêtres contextuelles ouvertes sont dessinées dans le canevas, Image affiche les images @drawable et celles d'un dossier d'images du projet, et le panneau État avec l'aperçu interactif change temporairement les valeurs d'aperçu des state
- Intégration AutoJs6: un projet peut être envoyé à l'exécution, ouvert dans l'éditeur AutoJs6 (synchronisation bidirectionnelle), importé dans le répertoire de scripts AutoJs6, copié ou exporté; le concepteur ouvre aussi les fichiers .js directement depuis l'éditeur AutoJs6 ou un gestionnaire de fichiers et enregistre dans le même fichier; un projet importé peut, sur demande dans le menu de l'éditeur, être réécrit dans sa source ou réimporté depuis celle-ci
- Interface declarative : `compose.state` + `compose.mount(render)` redessinent automatiquement a chaque changement d'etat, tandis que des poignees de noeud durables (`compose.Text({...})` et consorts) permettent de modifier directement proprietes et enfants
- Noyau Material 3: 29 fabriques de noeuds pour les dispositions, textes, icônes, images, boutons, saisies, sélections, listes paresseuses, dialogues et indicateurs; Snackbar est une commande de session, pas une fabrique compose.Snackbar
- Modifiers chaines : `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` conserve l'ordre des operations, et les operations a portee limitee sont validees cote hote
- Deux modes d'affichage: `compose.mount` ou `compose` / `$compose` appelables pour le contenu des activités `"ui";`, et `compose.floaty` pour des fenêtres raw ou redimensionnables, y compris dans les scripts non UI
- Rendu dans le processus hôte: les mises à jour UI sont appliquées dans l'hôte et les événements sont mis en file vers le thread propriétaire du script; les workers utilisent compose.post
- Un APK contient arm64-v8a / armeabi-v7a / x86_64 / x86, sans code natif propre au plugin; la bibliothèque auxiliaire AndroidX graphics-path est incluse et les conditions de compatibilité Android/hôte/plugin restent applicables
- La saisie native préserve la sélection et la composition IME, permet le focus et les modifications explicites, et rejette les modifications tardives qui écraseraient une saisie plus récente; les interrupteurs et curseurs restent contrôlés par le script
- Protections d'intégration: les vérifications indiquent indisponible si le plugin manque ou est incompatible, les erreurs utilisent `ComposeError`, et fermer la session ou arrêter le script libère ses fenêtres et rappels
- Cinq exemples exécutables de compteur, validation de formulaire, liste à clés stables de 1000 éléments, HUD flottant hors mode UI et thèmes, avec prérequis et index, synchronisés dans la catégorie 3-Block Compose des exemples de l'hôte correspondant
- TSX prend en charge `<compose.Column>`, `<compose:Text>`, les références aux fabriques de noeuds, les fragments, les emplacements et les rappels réactifs; un même arbre ne peut pas mélanger Compose et les anciens noeuds XML
- Les conteneurs XML `<compose>` et compose.attach intègrent des sessions Compose indépendantes dans les pages UI ou les fenêtres flottantes existantes; compose.AndroidView affiche une View Android existante ou renvoyée par une fabrique synchrone
- compose.dialog renvoie une session modifiable et fermable pour les dialogues et panneaux modaux inférieurs dans les scripts UI ou ordinaires
- Composants Material 3 étendus: navigation et tiroirs, onglets, panneaux inférieurs et menus, sélecteurs de date et heure, pagination et grilles, puces, badges, boutons segmentés et flottants, recherche, infobulles et actualisation par glissement
- Galerie de composants: l'entrée du lanceur présente les aperçus Material 3 et des scripts d'exemple exécutables des 55 composants, à copier ou à envoyer à AutoJs6; la page des paramètres suit par défaut la langue, le mode sombre et la couleur de thème d'AutoJs6 et propose quatre icônes de lanceur

******

### Utilisation

******

1. Installez une compilation locale compatible d'AutoJs6 contenant l'entrée compose (minimum 6.8.0 / 5322)
2. Installez l'APK du plugin; ouvrez 3-Block Compose depuis le lanceur pour atteindre l'accueil du concepteur, créer un projet de script ou ouvrir la galerie de composants et les paramètres depuis la barre supérieure
3. Verifiez dans le centre de plugins d'AutoJs6 que 3-Block Compose est reconnu et active
4. Utilisez `compose` ou `$compose` dans les scripts; montez les activités avec `compose.mount`, ou accordez la permission de superposition à l'hôte et utilisez `compose.floaty`
5. Le menu de l'éditeur propose exécuter, ouvrir dans l'éditeur AutoJs6, importer dans les scripts AutoJs6, copier et exporter; choisir 3-Block Compose depuis le menu de l'éditeur AutoJs6 (nécessite une version d'AutoJs6 avec la mise à jour correspondante) ou depuis un gestionnaire de fichiers modifie directement le fichier de script

******

### Demarrage rapide

******

Le compteur et le HUD flottant ci-dessous peuvent s'exécuter avec l'hôte local compatible de l'aperçu. Autorisez l'hôte à s'afficher au-dessus des autres applications avant d'exécuter le HUD:

```js
"ui";

// Compteur (couche render declarative)
let count = compose.state(0);

compose.mount(() => compose.Column({ modifier: compose.modifier().fillMaxSize().padding(16), spacing: 12 }, [
    compose.Text({ key: 'counter', text: `${count.value} clics`, style: 'headlineSmall' }),
    compose.Button({ key: 'inc', onClick: () => { count.value += 1; } }, 'Ajouter un'),
]));
```

```js
// HUD flottant (couche des poignees de noeud)
let worker = null;
let status = compose.Text({ text: 'Preparation...', color: '#FFFFFF' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ contentColor: '#FFFFFF', onClick: () => win.close() }, 'Fermer'),
]), { x: 50, y: 300, raw: true });
win.on('close', () => { if (worker) worker.interrupt(); });

worker = threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => {
            if (!win.isClosed()) status.text = `Progression ${i}%`;
        });
    }
});
```

Les cinq scripts exécutables sont répertoriés dans assets/examples/index.json et synchronisés dans la catégorie 3-Block Compose de l'hôte correspondant. Chaque en-tête précise le mode et les autorisations. Consultez la référence API locale et les déclarations TypeScript/éditeur associées pour les noeuds, modificateurs, thèmes, sessions et fenêtres; le site en ligne peut ne pas encore refléter ces changements locaux.

******

### Compatibilite

******

Exigences d'execution et limites du plugin:

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

******

### FAQ

******

- Comment exécuter les exemples de la galerie? "Exécuter dans AutoJs6" transmet le script à l'AutoJs6 installé; la galerie n'affiche que des aperçus et du code et n'exécute jamais de script
- Le concepteur peut-il ouvrir n'importe quel script? Il n'interprète que les déclarations `compose.state` de premier niveau et l'unique appel `compose.mount` / `floaty` / `dialog`; toute autre instruction est conservée telle quelle. Les formes qu'il ne peut pas mapper (une racine qui n'est pas une fabrique compose, plusieurs appels de montage, une fonction de rendu sans return) sont refusées avec ligne et colonne
- Le concepteur exécute-t-il les scripts? Non. Il ne fait que les analyser avec l'analyseur Rhino et les prévisualiser avec le moteur de rendu du plugin; les gestionnaires d'événements ne s'exécutent pas et le state affiche sa valeur initiale ou une valeur d'aperçu définie dans le panneau État. Dans l'aperçu interactif, les contrôles liés à un state (interrupteurs, curseurs, champs de texte, etc.) changent directement sa valeur d'aperçu, mais les gestionnaires et les fonctions de rendu ne s'exécutent toujours pas. L'exécution revient à AutoJs6
- Pourquoi `compose` est-il absent? L'objet global est fourni par la compilation locale compatible de l'hôte; installer uniquement l'APK du plugin ne l'ajoute pas
- Faut-il desinstaller d'autres plugins d'interface ? Non, 3-Block Compose n'interfere ni avec le module `ui` existant ni avec les autres plugins
- Que se passe-t-il si le plugin change? Sa mise à jour, désinstallation ou désactivation ferme les sessions actives et signale l'erreur correspondante; un plugin compatible et activé permet un nouveau montage
- Que faut-il pour les fenêtres flottantes? Accordez la permission de superposition à l'hôte et appelez `window.requestFocus()` avant la saisie. Si HyperOS ne montre pas la fenêtre, revenez au bureau. Une permission manquante renvoie PERMISSION_REQUIRED sans ouvrir automatiquement de demande d'autorisation
- Peut-on utiliser TSX ou toute fonction Compose? TSX accepte les fabriques Compose documentées avec le bon hôte et TypeScript Engine. Les fonctions Kotlin Composable arbitraires et les composants TSX personnalisés ne sont pas pris en charge
- La rotation perd-elle l'état? L'hôte actuel gère les changements ordinaires d'orientation sans remplacer le moteur de script. Une véritable recréation ou destruction de l'Activity ferme le moteur et ses sessions; l'état métier n'est pas restauré automatiquement
- Comment rechercher les composants? testTag est exposé comme ID brut sans préfixe de paquet. id/testTag et desc/contentDescription sont distincts; le texte d'un Button peut être un enfant, auquel cas suivez parent() vers un ancêtre cliquable

******

### Permissions et securite

******

Le plugin ne demande aucune permission d'execution Android et n'accede jamais au reseau, au stockage ni aux capteurs.

- Protection des composants: la Wake Activity et le service INFO sont protégés par la permission de signature `org.autojs.permission.PLUGIN`, seul l'hôte AutoJs6 peut donc y accéder; l'éditeur du concepteur est un composant exporté sans permission qui n'accepte qu'un Uri de document de script remis par l'utilisateur, la lecture et l'écriture étant limitées à cet Uri
- Comportement en arrière-plan: le plugin n'a ni service résident ni tâche planifiée et ne reçoit qu'une diffusion système lors de sa mise à jour, pour normaliser ses composants d'icône de lanceur; il ne consomme rien tant que l'hôte ne le charge pas et que ni le concepteur ni la galerie ne sont ouverts
- Frontière des données: le moteur de rendu ne lit ni n'écrit jamais les données de script ou les fichiers de l'utilisateur, et l'état de l'interface ne vit que dans la mémoire du processus hôte; les projets et brouillons du concepteur résident dans le répertoire privé du plugin, ne sont importés et exportés que par le sélecteur de documents du système, ou lisent et écrivent un seul fichier de script par un Uri remis par l'utilisateur; le répertoire des projets n'est partagé que par le fournisseur de contenu du plugin, à chaque demande, lors de la remise d'un projet à AutoJs6; un import conserve l'accès au fichier choisi quand son fournisseur le permet, mais ce fichier n'est lu ou écrit que lorsque l'utilisateur choisit "Réécrire dans la source" ou "Réimporter depuis la source"
- Politique de sauvegarde : la sauvegarde de l'application et le transfert entre appareils sont desactives, et le plugin ne detient aucune donnee a migrer

Lors du chargement du moteur de rendu, l'hote conserve son propre modele de permissions de script ; le plugin n'elargit pas les capacites systeme accessibles aux scripts.

******

### Interface du plugin

******

Identifiants exposes a l'hote:

```text
application id: io.github.supermonster003.autojs6.plugin.three.block.compose
plugin id: three-block-compose
engine: compose
variant: default
info action: org.autojs.plugin.INFO
info category: three-block-compose
renderer factory meta-data: org.autojs.plugin.compose.RENDERER_FACTORY
contract package: org.autojs.plugin.compose.api (version 2)
minimum host build: 5322 (6.8.0)
```

L'hote decouvre le plugin via `org.autojs.plugin.INFO` et lit des informations de capacite telles que `requiresHostVersion` ; la classe de fabrique du moteur de rendu est declaree par la meta-donnee `org.autojs.plugin.compose.RENDERER_FACTORY`, et l'hote cree un chargeur de classes a partir du chemin de l'APK du plugin (avec l'hote comme parent) puis l'instancie dans son propre processus.

******

### Feuille de route

******

Les jalons, les decisions de conception et les criteres d'acceptation sont suivis dans une seule feuille de route :

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/ROADMAP.md)

******

### Historique des versions

******

#### v1.3.0

_2026/10/13_

- `Nouveaute` Les projets du concepteur créés avec "Importer un script" peuvent utiliser "Réécrire dans la source" et "Réimporter depuis la source" dans le menu de l'éditeur: l'import conserve l'accès au fichier source quand son fournisseur le permet, chaque action demande d'abord confirmation si l'autre côté a changé depuis la dernière synchronisation, et une source qui ne s'ouvre plus peut être choisie à nouveau pour rétablir le lien; enregistrer un projet n'écrit jamais la source automatiquement
- `Nouveaute` Le canevas du concepteur prévisualise les images des Image: '@drawable/name' provient de l'AutoJs6 installé, et les chemins relatifs sont cherchés dans le dossier choisi avec "Dossier d'images" dans le menu de l'éditeur (seul un accès en lecture est conservé); les URL et chemins absolus restent des espaces réservés, avec la raison dans la liste des problèmes
- `Nouveaute` L'éditeur du concepteur propose un nouveau panneau État: il liste les compose.state de premier niveau du script avec leurs valeurs initiales et permet de définir des valeurs d'aperçu qui ne modifient que le canevas, jamais le script, sans ajouter d'étape d'annulation. Avec Interagir activé (en haut à droite du canevas), les composants du canevas peuvent être manipulés, et les interrupteurs, curseurs, champs de texte, sélections, pagers et autres contrôles liés à un état modifient sa valeur d'aperçu (les gestionnaires d'événements du script ne s'exécutent pas); le désactiver rétablit la sélection et le glisser
- `Correction` Lorsqu'un enfant situé au début d'un conteneur était inséré, supprimé ou déplacé ailleurs, les enfants suivants étaient entièrement reconstruits et perdaient leur propre état (par exemple la position de défilement d'une LazyColumn); ils conservent désormais leur état et ne sont plus reconstruits
- `Correction` Après la modification du texte d'un champ (TextField / OutlinedTextField) dans le tiroir de propriétés du concepteur, le champ du canevas affichait toujours l'ancien texte
- `Amelioration` Un glisser après appui long sur le canevas du concepteur affiche une ligne d'insertion selon la disposition du conteneur cible (verticale, horizontale ou en grille) et dépose le noeud à l'emplacement du doigt, au lieu de toujours l'ajouter à la fin du conteneur
- `Amelioration` Sur le canevas du concepteur, AndroidView apparaît désormais comme une zone teintée qui conserve ses modificateurs de taille (comme h et fillMaxWidth) et affiche le source de son expression view
- `Amelioration` La chaîne de modificateurs du tiroir de propriétés du concepteur se réordonne en faisant glisser la poignée à gauche de chaque opération, avec une ligne d'insertion jusqu'au relâchement; les boutons monter / descendre restent disponibles
- `Amelioration` Les composants contextuels ouverts (open vaut true) sont désormais dessinés au plus près de leur rendu réel dans l'écran logique du canevas du concepteur: AlertDialog et ModalBottomSheet recouvrent la page avec un voile, DropdownMenu et Tooltip s'affichent sous ou au-dessus de leur ancre, et les nœuds qu'ils contiennent se sélectionnent d'un appui pour être modifiés; ils ne sont plus insérés dans la page sous forme de cartes et aucune vraie fenêtre ne s'ouvre
- `Amelioration` Après chaque modification, le canevas du concepteur ne recompose que les nœuds modifiés et enregistre les positions des nœuds à moindre coût: un document plus grand (environ 250 nœuds) se redessine nettement plus vite après annuler / rétablir

#### v1.2.0

_2026/10/13_

- `Indication` Le plugin est renommé de Compose UI en 3-Block Compose: l'ID d'application passe de io.github.supermonster003.autojs6.plugin.compose.ui à io.github.supermonster003.autojs6.plugin.three.block.compose. Android le considère comme une nouvelle application, l'ancien paquet peut rester installé, les réglages et autorisations ne sont pas migrés, et le nouveau paquet doit être réactivé et réautorisé dans le centre de plugins
- `Indication` Nécessite une compilation AutoJs6 6.8.0 contenant la nouvelle identité du plugin (commit hôte f12665ffde ou ultérieur); les hôtes antérieurs recherchent le plugin par l'ancien nom de paquet et ne reconnaissent pas cette version
- `Indication` Le concepteur est documenté sur la page "Concepteur 3-Block Compose" du site de documentation; l'entrée de menu de l'éditeur AutoJs6 "Ouvrir dans le concepteur 3-Block Compose" nécessite une version AutoJs6 6.8.0 postérieure au commit hôte db7e6a1734, les autres destinations n'exigeant qu'un AutoJs6 installé
- `Nouveaute` Concepteur visuel: le lanceur ouvre l'accueil du concepteur, où les projets de script du stockage privé sont créés, importés ou ouverts; l'éditeur propose un canevas d'aperçu avec le vrai moteur de rendu, un plan arborescent, une palette de composants et un tiroir de propriétés, avec sélection au toucher, glisser après appui long pour changer de parent, réordonnancement dans le plan, annuler / rétablir, enregistrement et restauration automatiques des brouillons, édition des options d'affichage, rapport de validation selon le catalogue du contrat, et copie du script ou envoi vers AutoJs6; en paysage et sur tablette, le canevas et les panneaux sont côte à côte; la galerie de composants et les paramètres s'ouvrent désormais depuis l'accueil du concepteur
- `Nouveaute` Intégration du concepteur avec AutoJs6: le menu de l'éditeur ouvre le script courant dans l'éditeur AutoJs6 (le concepteur se recharge quand AutoJs6 enregistre), l'importe dans le répertoire de scripts AutoJs6 ou l'exporte n'importe où; le concepteur ouvre aussi les fichiers .js directement depuis l'éditeur AutoJs6 ou un gestionnaire de fichiers et enregistre dans le même fichier, en repliant sur un nouveau projet sans accès en écriture; les fichiers de projet sont partagés par le fournisseur de contenu du plugin avec des autorisations par requête, sans exposer d'autre répertoire
- `Amelioration` Le titre de l'application, l'ID du plugin (three-block-compose), les noms de paquet et de composants, les artefacts de publication, la documentation et le dépôt GitHub (AutoJs6-Plugin-Three-Block-Compose) suivent le renommage; l'API de script compose, la version du contrat et le comportement de rendu restent inchangés
- `Amelioration` Les icônes du lanceur, du centre de plugins et des informations d'application du système utilisent désormais les images définitives du mainteneur, les glyphes clair et sombre partageant une même forme
- `Amelioration` Les exemples fournis et la catégorie d'exemples de l'hôte sont renommés 3-Block Compose
- `Amelioration` L'AAR du contrat compose-ui-api est désormais empaqueté avec le plugin et conservé intact (auparavant en compilation seule) pour le processus propre du plugin; dans AutoJs6 les classes de contrat identiques de l'hôte restent prioritaires, le rendu et la version du contrat ne changent pas
- `Dependance` Ajout du jar allégé de l'analyseur Rhino (org.mozilla.javascript 2.0.0-SNAPSHOT, MPL 2.0, la même compilation que l'hôte AutoJs6), utilisé uniquement par le concepteur visuel pour analyser les scripts, jamais pour les exécuter; les compilations release ne conservent que les classes d'analyse référencées

#### v1.1.0

_2026/10/08_

- `Indication` Aperçu de développement local 1.1.0: une compilation AutoJs6 correspondante et le plugin installé et activé sont nécessaires. Pages UI, fenêtres flottantes, cinq exemples, référence API et déclarations TypeScript sont fournis pour cette intégration locale. La portée des vérifications de compatibilité et de performance figure dans la feuille de route. Le plugin ne figure pas dans l'index officiel et n'a pas de publication officielle. L'icône reste provisoire en attendant les images définitives du mainteneur
- `Indication` Cette version utilise le contrat Compose UI V2 et nécessite AutoJs6 6.8.0 / 5322; TSX nécessite TypeScript Engine 0.6.7. Les nouveaux hôtes acceptent les composants existants des moteurs V1; les composants étendus nécessitent un moteur V2
- `Indication` compose.memo est fourni par AutoJs6 6.8.0 / 5323 pour réutiliser les fragments de render dont les dépendances n'ont pas changé; ce plugin n'a pas besoin de mise à jour et TSX nécessite TypeScript Engine 0.6.8
- `Indication` La galerie et les paramètres s'exécutent dans le processus du plugin et ne modifient ni le rendu dans l'hôte ni la version minimale de l'hôte; l'exécution des exemples nécessite AutoJs6 avec ce plugin installé et activé
- `Nouveaute` TSX prend en charge `<compose.Column>`, `<compose:Text>`, les références aux fabriques de noeuds, les fragments, les emplacements et les rappels réactifs; un même arbre ne peut pas mélanger Compose et les anciens noeuds XML
- `Nouveaute` Les conteneurs XML `<compose>` et compose.attach intègrent des sessions Compose indépendantes dans les pages UI ou les fenêtres flottantes existantes; compose.AndroidView affiche une View Android existante ou renvoyée par une fabrique synchrone
- `Nouveaute` compose.dialog renvoie une session modifiable et fermable pour les dialogues et panneaux modaux inférieurs dans les scripts UI ou ordinaires
- `Nouveaute` Composants Material 3 étendus: navigation et tiroirs, onglets, panneaux inférieurs et menus, sélecteurs de date et heure, pagination et grilles, puces, badges, boutons segmentés et flottants, recherche, infobulles et actualisation par glissement
- `Nouveaute` Galerie de composants: ouvrez-la depuis le lanceur pour parcourir les aperçus Material 3 et les scripts d'exemple des 55 composants, les copier dans le presse-papiers ou les envoyer à l'AutoJs6 installé
- `Nouveaute` Page des paramètres: la langue, le mode sombre et la couleur de thème suivent AutoJs6 par défaut et se règlent séparément; l'icône de lanceur propose des variantes adaptatives (claire / sombre / automatique) et à fond transparent
- `Correction` Correction de l'échec de compilation après modification du fond des icônes du lanceur dans Icon Studio
- `Amelioration` Les icônes des informations d'application Android utilisent les illustrations et les fonds clairs et sombres d'Icon Studio, en conservant les images transparentes du centre de plugins et les choix du lanceur
- `Amelioration` Les fabriques de View tournent sur le thread principal avant le rendu. Un remplacement invalide conserve le contenu courant; une View ne peut appartenir à deux noeuds ni être prise à un autre parent. Ses écouteurs et les ressources du code appelant sont préservés
- `Amelioration` cancelable=false désactive la fermeture par retour, clic extérieur et balayage; la fermeture explicite et la fin du script libèrent le dialogue sans remplacer les pages et sessions existantes
- `Amelioration` L'icône du centre de plugins et l'icône d'informations système utilisent les fonds #FAFAFA clair / #212121 sombre partagés avec les autres plugins autonomes
- `Dependance` Mettre à jour compose-ui-api.aar avec une extension AndroidView optionnelle tout en préservant V1
- `Dependance` Ajout de la capacité de dialogue optionnelle à compose-ui-api.aar en conservant les contrats V1 et AndroidView existants
- `Dependance` Ajout du catalogue V2 de compose-ui-api.aar en conservant les modèles de noeuds et la sémantique des composants V1
- `Dependance` Ajout de material-color-utilities 4.1.1 (MIT) pour la dérivation de couleur de thème partagée avec les autres plugins autonomes
- `Dependance` Ajout de copies d'AndroidX activity, core, lifecycle, savedstate, emoji2, window, kotlinx-coroutines et de leurs compléments aux versions verrouillées par l'hôte pour la galerie dans le processus du plugin; dans l'hôte, les copies de l'hôte restent prioritaires

##### Pour un historique plus complet, voir

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

******

### Compilation

******

Apres le clonage, compilez directement avec le Gradle Wrapper ; les versions du plugin Android Gradle et de Kotlin sont choisies automatiquement par le plugin de versions de plateforme selon l'environnement IDE courant.

Compiler l'APK de debogage:

```powershell
.\gradlew.bat :app:assembleDebug
```

Executer les tests unitaires JVM et empaqueter les tests de contrat sur appareil:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Compiler l'APK de publication (necessite `sign.properties` et la cle de signature):

```powershell
.\gradlew.bat :app:assembleRelease
```

Verifier la signature et produire le fichier de publication suffixe par son empreinte:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Verifier que les documents localises correspondent a leurs sources:

```powershell
py .python\generate_markdown.py --check
```

La compilation necessite JDK 21 ou ulterieur. Apres avoir modifie les sources sous `.readme` ou `.changelog`, executez `py .python\generate_markdown.py` pour regenerer tous les documents.

******

### Organisation de la documentation

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

Le README, l'instruction du centre de plugins et le journal des modifications sont tous generes a partir des sources JSON situees sous `.readme` et `.changelog` ; ne modifiez pas directement les fichiers Markdown generes.

******

### Licence

******

Ce projet est publie sous la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/LICENSE). Les informations de licence des composants tiers figurent dans [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md).

******

### Liens

******

- Projet AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentation AutoJs6: https://docs.autojs6.com
- Documentation du module compose: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- Mentions tierces: https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Block-Compose/blob/master/THIRD_PARTY_NOTICES.md
