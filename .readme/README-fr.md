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

1.7.0 est la version publique actuelle (GitHub Release, inscrite à l'index officiel des plugins). Elle nécessite une version d'AutoJs6 6.8.0 (5322) ou ultérieure portant la nouvelle identité 3-Block Compose (commit hôte f12665ffde ou ultérieur); AutoJs6 6.8.0 n'était pas encore officiellement publié lors de la parution de cette version, une version d'AutoJs6 contenant ce commit est donc nécessaire. L'entrée de menu de l'éditeur AutoJs6 "Ouvrir dans le concepteur 3-Block Compose" nécessite une version postérieure au commit hôte db7e6a1734, et les projets TSX du concepteur nécessitent TypeScript Engine 0.7.0, et l'aperçu de mise en page des projets UI (XML) nécessite une version contenant le service d'aperçu (commit hôte 306a8f7834 ou ultérieur). Le plugin fournit les pages UI, les fenêtres flottantes, le concepteur visuel (projets Compose et UI), cinq exemples, la référence API, la page de guide du concepteur et les déclarations TypeScript. Le périmètre vérifié de compatibilité et de performance est consigné dans la feuille de route. Les icônes utilisent le visuel final du mainteneur. À partir de 1.7.0, l'aperçu interactif des projets UI (XML) fait exécuter tout le script par AutoJs6 pour de vrai; il nécessite une version d'AutoJs6 avec le contrat d'aperçu v2 (commit hôte 4c332c2053 ou ultérieur) et Android 13 ou ultérieur.

******

### Fonctionnalites

******

Fonctions de l'aperçu de développement actuel:

- Concepteur visuel: le fichier de script est le projet, le vrai moteur de rendu affiche l'arbre d'interface pendant l'édition, le plan / la palette de composants / le tiroir de propriétés couvrent les 55 composants, avec annuler / rétablir et restauration des brouillons; le script exporté peut être copié ou envoyé directement à AutoJs6; les dépôts sur le canevas arrivent sous le pointeur, les fenêtres contextuelles ouvertes sont dessinées dans le canevas, Image affiche les images @drawable et celles d'un dossier d'images du projet, le panneau État change temporairement les valeurs d'aperçu des state, et l'aperçu interactif exécute le script d'un projet JavaScript dans un bac à sable restreint (Android 8.0 ou ultérieur): les touches et la saisie mettent à jour le canevas comme le décide le script, avec une console pour sa sortie et ses erreurs; les projets sont en JavaScript ou en TSX, et les projets TSX sont analysés par TypeScript Engine 0.7.0 ou ultérieur puis réécrits en TSX canonique
- Intégration AutoJs6: un projet peut être envoyé à l'exécution, ouvert dans l'éditeur AutoJs6 (synchronisation bidirectionnelle), importé dans le répertoire de scripts AutoJs6, copié ou exporté; le concepteur ouvre aussi les fichiers .js / .tsx directement depuis l'éditeur AutoJs6 ou un gestionnaire de fichiers et enregistre dans le même fichier; un projet importé peut, sur demande dans le menu de l'éditeur, être réécrit dans sa source ou réimporté depuis celle-ci
- Projets UI (XML): le concepteur ouvre aussi les scripts classiques en mode "ui" (mises en page XML E4X) et les modifie sur place en conservant chaque octet non touché (espaces, commentaires, ordre des attributs, guillemets, {expressions}); le canevas est dessiné par le service d'aperçu d'AutoJs6 avec le gonfleur de mise en page du mode ui (vues réelles intégrées à partir d'Android 11, instantané bitmap en dessous), le tiroir de propriétés suit le catalogue de balises et d'attributs du site de documentation, la palette liste 48 balises, un élément peut recevoir un gabarit de gestionnaire `ui.<id>.on`, et le panneau des liaisons remplace temporairement les valeurs `{{ }}`
- Aperçu interactif des projets UI (XML) (1.7.0): l'interrupteur "Interagir" au-dessus du canevas activé, AutoJs6 exécute tout le script comme une exécution normale en mode ui, le canevas affiche le ui.layout du script et reçoit directement les touches, les gestionnaires d'événements, les liaisons `{{ }}` et les autres modules s'exécutent avec la logique réelle du script, et l'onglet console affiche la sortie, les erreurs et la raison de fin; le script est relancé environ 0.4 s après une modification, s'arrête quand l'interrupteur est désactivé ou que le concepteur est quitté, et le document et le fichier restent inchangés
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
- Les projets TSX du concepteur nécessitent TypeScript Engine 0.7.0 ou ultérieur (qui fournit le service d'analyse en arbre syntaxique), et leur exécution laisse AutoJs6 les compiler via TypeScript Engine; sans lui, ou avec une version plus ancienne, un projet TSX s'ouvre en source en lecture seule, consultable et copiable
- Exécuter le script dans l'aperçu interactif du concepteur nécessite Android 8.0 (API 26) ou ultérieur, car le Date JavaScript du bac à sable repose sur java.time; sur un Android plus ancien, un projet JavaScript est prévisualisé comme un projet TSX
- L'aperçu de mise en page des projets UI (XML) nécessite une version d'AutoJs6 6.8.0 contenant le service d'aperçu (commit hôte 306a8f7834 ou ultérieur; le plugin vérifie la présence du service, pas un numéro de version); à partir d'Android 11 (API 30) le canevas intègre les vues réelles dessinées par AutoJs6, les versions plus anciennes affichent un instantané bitmap; sans le service un projet UI reste modifiable et le canevas en indique la raison
- L'aperçu interactif des projets UI (XML) nécessite une version d'AutoJs6 6.8.0 avec le contrat d'aperçu v2 (commit hôte 4c332c2053 ou ultérieur) et Android 13 (API 33) ou ultérieur; sous Android 13 - 14 les champs de saisie du canevas ne reçoivent pas le clavier, à partir d'Android 15 oui; si les conditions ne sont pas remplies, l'interrupteur est masqué, la ligne au-dessus du canevas en indique la raison et "Exécuter dans AutoJs6" reste disponible; une version d'AutoJs6 avec le seul contrat v1 fournit toujours l'aperçu de mise en page

******

### FAQ

******

- Comment exécuter les exemples de la galerie? "Exécuter dans AutoJs6" transmet le script à l'AutoJs6 installé; la galerie n'affiche que des aperçus et du code et n'exécute jamais de script
- Le concepteur peut-il ouvrir n'importe quel script? Il n'interprète que les déclarations `compose.state` de premier niveau et l'unique appel `compose.mount` / `floaty` / `dialog`; toute autre instruction est conservée telle quelle. Les formes qu'il ne peut pas mapper (une racine qui n'est pas une fabrique compose, plusieurs appels de montage, une fonction de rendu sans return) sont refusées avec ligne et colonne. Dans les projets TSX, les balises `compose.X` / `compose:X` sont des nœuds modifiables, les autres balises et les expressions `{}` sont conservées telles quelles, et les balises XML et les attributs de décomposition sont refusés
- Le concepteur exécute-t-il les scripts? Seulement dans l'aperçu interactif. Pendant l'édition, il ne fait que les analyser (JavaScript avec l'analyseur Rhino, TSX avec le service d'analyse de TypeScript Engine) et les prévisualiser avec le moteur de rendu du plugin; les gestionnaires d'événements ne s'exécutent pas et le state affiche sa valeur initiale ou une valeur d'aperçu définie dans le panneau État. Avec Interagir activé, le script d'un projet JavaScript s'exécute dans le plugin, dans un bac à sable restreint (Android 8.0 ou ultérieur): il ne dispose que de compose, console, toast et des minuteurs, ne peut atteindre ni les classes Java / Android, ni les fichiers, ni le réseau, ni les autres modules d'AutoJs6, est arrêté quand une exécution dure plus de 3 secondes environ, et ne modifie que le canevas, jamais le projet. Un projet TSX (ou tout projet sous Android 8.0) n'exécute pas son script; les contrôles liés à un state ne changent que sa valeur d'aperçu. L'exécution réelle revient à AutoJs6. Le canevas d'un projet UI (XML) est seulement mis en page par le service d'aperçu d'AutoJs6 (il crée et dessine les vues) sans exécuter le script du projet; les valeurs `{{ }}` viennent des déclarations littérales de premier niveau du script (évaluées dans le bac à sable) ou des remplacements du panneau Liaisons. À partir de 1.7.0, l'aperçu interactif d'un projet UI (XML) est différent: l'interrupteur activé, AutoJs6 exécute tout le script pour de vrai, exactement comme "Exécuter dans AutoJs6" (sans les limites du bac à sable, les effets secondaires se produisent réellement, chaque projet demande confirmation une fois avant sa première exécution), et seul son contenu ui.layout s'affiche sur le canevas; le résultat n'est toujours pas écrit dans le projet
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
- Bac à sable des scripts: l'aperçu interactif du concepteur exécute le script d'un projet JavaScript dans le plugin avec l'interpréteur de Rhino. Il n'offre que compose, console, toast et les minuteurs; les classes Java / Android, les fichiers, le réseau et les autres modules d'AutoJs6 restent hors de portée, chaque exécution a des limites d'instructions et de durée, et ce qu'il fait reste sur le canevas: le projet n'est jamais écrit
- Politique de sauvegarde : la sauvegarde de l'application et le transfert entre appareils sont desactives, et le plugin ne detient aucune donnee a migrer
- Aperçu de mise en page: le canevas d'un projet UI (XML) est dessiné par l'AutoJs6 installé: le plugin ne lui transmet que le fragment XML et les valeurs de liaison via le service d'aperçu d'AutoJs6 (protégé par la permission de signature PLUGIN), AutoJs6 crée les vues avec son gonfleur de mise en page du mode ui et les dessine en retour, n'exécute aucun script du projet et ne peut pas atteindre les fichiers de projet du plugin; cette exécution d'aperçu apparaît dans la liste des scripts en cours d'AutoJs6 et peut être arrêtée à tout moment
- Aperçu interactif (projets UI): l'interrupteur activé, le plugin transmet le texte du script au service d'aperçu d'AutoJs6 et AutoJs6 l'exécute comme un script normal en mode ui (le même moteur et les mêmes permissions de script que "Exécuter dans AutoJs6", les effets secondaires se produisent réellement), n'affiche que son contenu ui.layout sur le canevas et renvoie la sortie de la console; chaque projet demande confirmation une fois avant sa première exécution, l'exécution apparaît dans la liste des scripts en cours d'AutoJs6 et se termine quand l'interrupteur est désactivé, le concepteur quitté ou l'exécution arrêtée depuis AutoJs6; le script ne s'exécute pas dans le processus du plugin et le plugin n'élargit pas ses permissions

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

#### v1.7.0

_2026/10/15_

- `Indication` L'aperçu interactif des projets UI (XML) fait maintenant exécuter tout le script par AutoJs6 pour de vrai (nécessite une version d'AutoJs6 6.8.0 avec le contrat d'aperçu v2, commit hôte 4c332c2053 ou ultérieur, et Android 13 ou ultérieur): les effets secondaires du script sont les mêmes qu'en l'exécutant dans AutoJs6, et chaque projet demande confirmation une fois avant d'activer l'interrupteur; si les conditions ne sont pas remplies, la ligne au-dessus du canevas en indique la raison et "Exécuter dans AutoJs6" reste disponible
- `Nouveaute` Aperçu interactif des projets UI (XML): l'interrupteur "Interagir" au-dessus du canevas activé, AutoJs6 exécute tout le script comme une exécution en mode ui, le canevas affiche le ui.layout du script et reçoit directement les touches, et les gestionnaires d'événements, les liaisons {{ }}, les minuteurs et les autres modules s'exécutent avec la logique réelle du script; le script est relancé environ 0.4 s après une modification, s'arrête quand l'interrupteur est désactivé, que le concepteur passe en arrière-plan ou que l'éditeur se ferme, et le canevas revient à l'aperçu de mise en page; le document, le fichier du projet et le brouillon ne sont jamais modifiés. Sous Android 13 - 14 les champs de saisie du canevas ne reçoivent pas le clavier, à partir d'Android 15 oui
- `Nouveaute` Pendant l'aperçu interactif, l'onglet console d'un projet UI affiche la sortie du script exécuté par AutoJs6 (colorée par niveau), l'état de l'exécution et sa raison de fin; quand le script échoue, l'erreur (avec sa ligne) s'affiche au-dessus du canevas et une touche ouvre la console; l'exécution apparaît sous le nom du fichier du projet dans la liste des scripts en cours d'AutoJs6 et peut y être arrêtée
- `Correction` Le concepteur affichait parfois "Le service d'aperçu d'AutoJs6 n'est pas disponible pour le moment (at most 2 sessions per client)" à l'ouverture d'un projet UI: quand la taille du canevas changeait deux fois avant que la mise en page se stabilise, le plugin créait deux fois la session d'aperçu dans AutoJs6 et atteignait la limite de sessions par client; désormais une seule session est créée à la fois et la taille ou l'apparence modifiée entre-temps est appliquée ensuite
- `Dependance` Mise à niveau de l'artefact de contrat ui-preview-api.aar vers le contrat d'aperçu ui v2 (ajoute, après v1, les interfaces pour exécuter et arrêter un script et les rappels console / erreur / fin; les versions d'AutoJs6 avec le seul v1 fournissent toujours l'aperçu de mise en page)

#### v1.6.0

_2026/10/15_

- `Indication` Le concepteur gagne les projets UI (XML): les scripts classiques en mode "ui" (mises en page XML E4X) peuvent être ouverts et modifiés, le canevas étant dessiné par AutoJs6. L'aperçu de mise en page nécessite une version d'AutoJs6 6.8.0 contenant le service d'aperçu (commit hôte 306a8f7834 ou ultérieur); à partir d'Android 11 (API 30) le canevas intègre les vues réelles dessinées par AutoJs6, les versions plus anciennes affichent un instantané bitmap; sans le service le projet reste modifiable et le canevas en indique la raison
- `Nouveaute` Le concepteur prend en charge les projets UI (XML): choisissez "UI (XML)" à la création d'un projet, ou importez un script en mode "ui" ou ouvrez-en un depuis l'éditeur AutoJs6 / un gestionnaire de fichiers et il est lu comme projet UI; la liste de l'accueil marque chaque projet JS / TSX / UI. Les mises en page XML du script sont conservées sous forme d'arbre syntaxique avec positions: les parties non touchées (espaces, commentaires, ordre des attributs, guillemets, {expressions}) sont préservées octet par octet, seuls les balises ouvrantes modifiées, le texte et les éléments nouvellement insérés sont réécrits; les autres littéraux XML du script peuvent aussi être placés sur le canevas
- `Nouveaute` L'éditeur de projet UI: plan (repli, monter / descendre, supprimer, glisser par appui long, sélecteur quand il y a plusieurs mises en page), tiroir de propriétés (balise, id, contenu textuel, attributs déjà définis et attributs du catalogue groupés par classe View, états texte / liaison `{{ }}` / {expression}, attributs sans valeur, nom d'attribut libre), palette de composants (les 48 balises de la documentation par catégorie avec attributs par défaut), panneau des liaisons (valeurs évaluées des noms `{{ }}` et remplacements qui ne touchent que le canevas), liste des problèmes (vérifiée contre le catalogue de balises et d'attributs du site de documentation) et console (ce qu'AutoJs6 a signalé en dessinant la mise en page); enregistrer, récupération de brouillon, synchronisation de la source, exécuter, ouvrir dans l'éditeur AutoJs6, importer dans le répertoire de scripts AutoJs6, copier et exporter fonctionnent aussi pour les projets UI
- `Nouveaute` Le canevas d'un projet UI est dessiné par AutoJs6 (aperçu de mise en page): le plugin transmet le fragment XML et les valeurs de liaison au service d'aperçu d'AutoJs6, qui crée des vues réelles avec son gonfleur de mise en page du mode ui et les dessine sur le canevas sans exécuter le script du projet; la sélection et le glisser fonctionnent sur le canevas comme d'habitude, et une image en échec désigne l'élément fautif; l'exécution d'aperçu apparaît sous le nom "3-Block Compose preview" dans la liste des scripts en cours d'AutoJs6 et peut y être arrêtée
- `Nouveaute` Le tiroir de propriétés d'un projet UI ajoute des événements à l'élément sélectionné: un gabarit de gestionnaire `ui.<id>.on('événement', ...)` est ajouté après l'appel ui.layout (un élément sans id en reçoit un), et son corps s'écrit dans l'éditeur de script; les événements déjà enregistrés par le script apparaissent comme "Géré dans le script"
- `Dependance` Ajout de l'artefact de contrat ui-preview-api.aar (contrat d'aperçu de mise en page ui d'AutoJs6 v1), par lequel le concepteur demande à AutoJs6 de dessiner le canevas des projets UI

#### v1.5.1

_2026/10/15_

- `Correction` Le plugin pouvait se fermer de manière inattendue quand un nouveau champ de texte apparaissait sur le canevas du concepteur, par exemple en insérant un champ de texte depuis la palette de composants, quand annuler / rétablir faisait réapparaître un champ de texte, ou quand un script de l'aperçu interactif en affichait un (depuis 1.4.0); ces champs de texte s'affichent maintenant normalement
- `Correction` Compléter les traductions du titre de clé dans l'inspecteur du concepteur

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
