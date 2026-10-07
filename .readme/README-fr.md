<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-compose-ui-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Un plugin qui apporte les interfaces Jetpack Compose et Material 3 aux scripts AutoJs6</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Compose-UI?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Compose-UI?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages / Langues

******

Ce document est disponible dans les langues suivantes:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-zh-Hant-TW.md)
- [English [en]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-en.md)
- Français [fr] # actuel
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/.readme/README-ar.md)

******

### Introduction

******

Compose UI est un plugin de rendu d'interface pour AutoJs6. Les scripts déclarent les interfaces via `compose` / `$compose` de l'hôte, puis le plugin les rend dans son processus avec Jetpack Compose et Material 3. L'aperçu prend en charge les activités `"ui";` et les fenêtres flottantes des scripts non UI.

Le plugin n'embarque aucun ecran autonome et n'ajoute aucune entree dans le lanceur. L'hote le decouvre via le service INFO, lit sa version et ses informations de compatibilite, puis charge le moteur de rendu dans son propre processus selon le contrat (`org.autojs.plugin.compose.api`). L'arbre d'interface, l'etat et les evenements vivent cote script ; le moteur de rendu se contente d'appliquer les correctifs a la composition Compose et de renvoyer les evenements utilisateur au script.

******

### Etat actuel

******

Aperçu de développement local 1.1.0: une compilation AutoJs6 correspondante et le plugin installé et activé sont nécessaires. Pages UI, fenêtres flottantes, cinq exemples, référence API et déclarations TypeScript sont fournis pour cette intégration locale. La portée des vérifications de compatibilité et de performance figure dans la feuille de route. Le plugin ne figure pas dans l'index officiel et n'a pas de publication officielle. L'icône reste provisoire en attendant les images définitives du mainteneur.

******

### Fonctionnalites

******

Fonctions de l'aperçu de développement actuel:

- Interface declarative : `compose.state` + `compose.mount(render)` redessinent automatiquement a chaque changement d'etat, tandis que des poignees de noeud durables (`compose.Text({...})` et consorts) permettent de modifier directement proprietes et enfants
- Noyau Material 3: 29 fabriques de noeuds pour les dispositions, textes, icônes, images, boutons, saisies, sélections, listes paresseuses, dialogues et indicateurs; Snackbar est une commande de session, pas une fabrique compose.Snackbar
- Modifiers chaines : `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` conserve l'ordre des operations, et les operations a portee limitee sont validees cote hote
- Deux modes d'affichage: `compose.mount` ou `compose` / `$compose` appelables pour le contenu des activités `"ui";`, et `compose.floaty` pour des fenêtres raw ou redimensionnables, y compris dans les scripts non UI
- Rendu dans le processus hôte: les mises à jour UI sont appliquées dans l'hôte et les événements sont mis en file vers le thread propriétaire du script; les workers utilisent compose.post
- Un APK contient arm64-v8a / armeabi-v7a / x86_64 / x86, sans code natif propre au plugin; la bibliothèque auxiliaire AndroidX graphics-path est incluse et les conditions de compatibilité Android/hôte/plugin restent applicables
- La saisie native préserve la sélection et la composition IME, permet le focus et les modifications explicites, et rejette les modifications tardives qui écraseraient une saisie plus récente; les interrupteurs et curseurs restent contrôlés par le script
- Protections d'intégration: les vérifications indiquent indisponible si le plugin manque ou est incompatible, les erreurs utilisent `ComposeError`, et fermer la session ou arrêter le script libère ses fenêtres et rappels
- Cinq exemples exécutables de compteur, validation de formulaire, liste à clés stables de 1000 éléments, HUD flottant hors mode UI et thèmes, avec prérequis et index, synchronisés dans la catégorie Compose UI des exemples de l'hôte correspondant
- TSX prend en charge `<compose.Column>`, `<compose:Text>`, les références aux fabriques de noeuds, les fragments, les emplacements et les rappels réactifs; un même arbre ne peut pas mélanger Compose et les anciens noeuds XML
- Les conteneurs XML `<compose>` et compose.attach intègrent des sessions Compose indépendantes dans les pages UI ou les fenêtres flottantes existantes; compose.AndroidView affiche une View Android existante ou renvoyée par une fabrique synchrone

******

### Utilisation

******

1. Installez une compilation locale compatible d'AutoJs6 contenant l'entrée compose (minimum 6.8.0 / 5316)
2. Installez l'APK de ce plugin (rien a ouvrir, le plugin n'a pas d'entree dans le lanceur)
3. Verifiez dans le centre de plugins d'AutoJs6 que Compose UI est reconnu et active
4. Utilisez `compose` ou `$compose` dans les scripts; montez les activités avec `compose.mount`, ou accordez la permission de superposition à l'hôte et utilisez `compose.floaty`

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

Les cinq scripts exécutables sont répertoriés dans assets/examples/index.json et synchronisés dans la catégorie Compose UI de l'hôte correspondant. Chaque en-tête précise le mode et les autorisations. Consultez la référence API locale et les déclarations TypeScript/éditeur associées pour les noeuds, modificateurs, thèmes, sessions et fenêtres; le site en ligne peut ne pas encore refléter ces changements locaux.

******

### Compatibilite

******

Exigences d'execution et limites du plugin:

- Version minimale d'AutoJs6 : 6.8.0 (5316) ou ulterieure ; les hotes plus anciens signalent le plugin comme incompatible dans le centre de plugins
- Version d'Android : 7.0 (API 24) ou ulterieure
- Architecture du processeur : arm64-v8a / armeabi-v7a / x86_64 / x86 (les quatre integrees dans l'APK unique, aucun choix par architecture)
- Version de Compose : embarquee dans le plugin (BOM 2026.09.00), independante du runtime Compose de l'hote
- Version du contrat : 1 ; l'hote et le plugin negocient la version du contrat et refusent le chargement avec une erreur explicite en cas de desaccord
- Les applications empaquetées nécessitent aussi l'installation séparée d'un plugin Compose UI compatible, avec activation/autorisation propres à cette application; la compatibilité porte sur le runtime AutoJs6 intégré, pas sur le versionCode de l'application
- TSX nécessite le build local AutoJs6 6.8.0 / 5319 correspondant et TypeScript Engine avec les déclarations Compose; installer uniquement le moteur de rendu ne suffit pas
- Cette intégration nécessite AutoJs6 6.8.0 / 5320 et un build Compose UI avec extension AndroidView; TSX nécessite aussi TypeScript Engine 0.6.5. Le rendu V1 de base conserve le minimum 5316
- Les fabriques de View tournent sur le thread principal avant le rendu. Un remplacement invalide conserve le contenu courant; une View ne peut appartenir à deux noeuds ni être prise à un autre parent. Ses écouteurs et les ressources du code appelant sont préservés

******

### FAQ

******

- Pourquoi aucune icone de plugin n'apparait apres l'installation ? Le plugin n'a ni interface autonome ni entree dans le lanceur ; consultez le centre de plugins d'AutoJs6
- Pourquoi `compose` est-il absent? L'objet global est fourni par la compilation locale compatible de l'hôte; installer uniquement l'APK du plugin ne l'ajoute pas
- Faut-il desinstaller d'autres plugins d'interface ? Non, Compose UI n'interfere ni avec le module `ui` existant ni avec les autres plugins
- Que se passe-t-il si le plugin change? Sa mise à jour, désinstallation ou désactivation ferme les sessions actives et signale l'erreur correspondante; un plugin compatible et activé permet un nouveau montage
- Que faut-il pour les fenêtres flottantes? Accordez la permission de superposition à l'hôte et appelez `window.requestFocus()` avant la saisie. Si HyperOS ne montre pas la fenêtre, revenez au bureau. Une permission manquante renvoie PERMISSION_REQUIRED sans ouvrir automatiquement de demande d'autorisation
- Peut-on utiliser TSX ou toute fonction Compose? TSX accepte les fabriques Compose documentées avec le bon hôte et TypeScript Engine. Les fonctions Kotlin Composable arbitraires et les composants TSX personnalisés ne sont pas pris en charge
- La rotation perd-elle l'état? L'hôte actuel gère les changements ordinaires d'orientation sans remplacer le moteur de script. Une véritable recréation ou destruction de l'Activity ferme le moteur et ses sessions; l'état métier n'est pas restauré automatiquement
- Comment rechercher les composants? testTag est exposé comme ID brut sans préfixe de paquet. id/testTag et desc/contentDescription sont distincts; le texte d'un Button peut être un enfant, auquel cas suivez parent() vers un ancêtre cliquable

******

### Permissions et securite

******

Le plugin ne demande aucune permission d'execution Android et n'accede jamais au reseau, au stockage ni aux capteurs.

- Protection des composants : la Wake Activity et le service INFO sont tous deux proteges par la permission de signature `org.autojs.permission.PLUGIN`, si bien que seul l'hote AutoJs6 peut les atteindre
- Aucune activite en arriere-plan : le plugin n'a ni service resident, ni recepteur de diffusion, ni tache planifiee, et ne consomme aucune ressource tant que l'hote ne le charge pas
- Frontiere des donnees : le plugin ne lit ni n'ecrit les donnees des scripts ou les fichiers de l'utilisateur ; l'etat de l'interface n'existe que dans la memoire du processus de l'hote
- Politique de sauvegarde : la sauvegarde de l'application et le transfert entre appareils sont desactives, et le plugin ne detient aucune donnee a migrer

Lors du chargement du moteur de rendu, l'hote conserve son propre modele de permissions de script ; le plugin n'elargit pas les capacites systeme accessibles aux scripts.

******

### Interface du plugin

******

Identifiants exposes a l'hote:

```text
application id: io.github.supermonster003.autojs6.plugin.compose.ui
plugin id: compose-ui
engine: compose
variant: default
info action: org.autojs.plugin.INFO
info category: compose-ui
renderer factory meta-data: org.autojs.plugin.compose.RENDERER_FACTORY
contract package: org.autojs.plugin.compose.api (version 1)
minimum host build: 5316 (6.8.0)
```

L'hote decouvre le plugin via `org.autojs.plugin.INFO` et lit des informations de capacite telles que `requiresHostVersion` ; la classe de fabrique du moteur de rendu est declaree par la meta-donnee `org.autojs.plugin.compose.RENDERER_FACTORY`, et l'hote cree un chargeur de classes a partir du chemin de l'APK du plugin (avec l'hote comme parent) puis l'instancie dans son propre processus.

******

### Feuille de route

******

Les jalons, les decisions de conception et les criteres d'acceptation sont suivis dans une seule feuille de route :

- [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/ROADMAP.md)

******

### Historique des versions

******

#### v1.1.0

_2026/10/07_

- `Indication` Aperçu de développement local 1.1.0: une compilation AutoJs6 correspondante et le plugin installé et activé sont nécessaires. Pages UI, fenêtres flottantes, cinq exemples, référence API et déclarations TypeScript sont fournis pour cette intégration locale. La portée des vérifications de compatibilité et de performance figure dans la feuille de route. Le plugin ne figure pas dans l'index officiel et n'a pas de publication officielle. L'icône reste provisoire en attendant les images définitives du mainteneur
- `Indication` TSX nécessite le build local AutoJs6 6.8.0 / 5319 correspondant et TypeScript Engine avec les déclarations Compose; installer uniquement le moteur de rendu ne suffit pas
- `Indication` Cette intégration nécessite AutoJs6 6.8.0 / 5320 et un build Compose UI avec extension AndroidView; TSX nécessite aussi TypeScript Engine 0.6.5. Le rendu V1 de base conserve le minimum 5316
- `Nouveaute` TSX prend en charge `<compose.Column>`, `<compose:Text>`, les références aux fabriques de noeuds, les fragments, les emplacements et les rappels réactifs; un même arbre ne peut pas mélanger Compose et les anciens noeuds XML
- `Nouveaute` Les conteneurs XML `<compose>` et compose.attach intègrent des sessions Compose indépendantes dans les pages UI ou les fenêtres flottantes existantes; compose.AndroidView affiche une View Android existante ou renvoyée par une fabrique synchrone
- `Amelioration` Les icônes des informations d'application Android utilisent les illustrations et les fonds clairs et sombres d'Icon Studio, en conservant les images transparentes du centre de plugins et les choix du lanceur
- `Amelioration` Les fabriques de View tournent sur le thread principal avant le rendu. Un remplacement invalide conserve le contenu courant; une View ne peut appartenir à deux noeuds ni être prise à un autre parent. Ses écouteurs et les ressources du code appelant sont préservés
- `Dependance` Mettre à jour compose-ui-api.aar avec une extension AndroidView optionnelle tout en préservant V1

#### v1.0.0

_2026/10/03_

- `Indication` Aperçu de développement local 1.0.0: une compilation AutoJs6 correspondante et le plugin installé et activé sont nécessaires. Pages UI, fenêtres flottantes, cinq exemples, référence API et déclarations TypeScript sont fournis pour cette intégration locale. La portée des vérifications de compatibilité et de performance figure dans la feuille de route. Le plugin ne figure pas dans l'index officiel et n'a pas de publication officielle. L'icône reste provisoire en attendant les images définitives du mainteneur
- `Indication` Necessite AutoJs6 6.8.0 (5316) ou ulterieur
- `Indication` Les applications empaquetées nécessitent aussi l'installation séparée d'un plugin Compose UI compatible, avec activation/autorisation propres à cette application; la compatibilité porte sur le runtime AutoJs6 intégré, pas sur le versionCode de l'application
- `Indication` Les deux mipmap clair/sombre sont conservés; le dessin actuel est provisoire et sera remplacé par les images noir et blanc définitives du mainteneur
- `Nouveaute` compose / $compose appelables, handles de noeuds persistants, state/render/ref réactifs, modifications groupées, publications en file et contrôle du thème
- `Nouveaute` Noyau Material 3: 29 fabriques de noeuds pour les dispositions, textes, icônes, images, boutons, saisies, sélections, listes paresseuses, dialogues et indicateurs; Snackbar est une commande de session, pas une fabrique compose.Snackbar
- `Nouveaute` Les scripts UI montent le contenu Activity; compose.floaty propose aussi aux scripts hors UI des fenêtres raw ou redimensionnables, une géométrie en pixels, le contrôle du toucher/focus et la libération de leurs ressources
- `Nouveaute` Les 20 opérations Modifier conservent l'ordre déclaré et prennent en charge la validation du contexte de mise en page, le défilement et les libellés d'accessibilité
- `Nouveaute` Les thèmes Material 3 prennent en charge les couleurs sources, les modes clair et sombre, les couleurs dynamiques du système sur Android 12+, les familles de polices et la taille du texte
- `Nouveaute` La saisie native préserve la sélection et la composition IME, permet le focus et les modifications explicites, et rejette les modifications tardives qui écraseraient une saisie plus récente; les interrupteurs et curseurs restent contrôlés par le script
- `Nouveaute` Icônes core et images ImageWrapper/Bitmap, fichiers locaux et drawable de l'hôte; le moteur de rendu ne recycle pas automatiquement les images appartenant à l'appelant
- `Nouveaute` Cinq exemples exécutables de compteur, validation de formulaire, liste à clés stables de 1000 éléments, HUD flottant hors mode UI et thèmes, avec prérequis et index, synchronisés dans la catégorie Compose UI des exemples de l'hôte correspondant
- `Nouveaute` Référence API et déclarations TypeScript associées, ainsi que README, instructions du centre des plugins et historique en 10 langues
- `Nouveaute` Détection dans le centre des plugins avec vérifications de version hôte, de contrat et d'autorisation, sans écran autonome ni entrée de lanceur
- `Correction` Compose UI refuse de monter une autre page ou fenêtre flottante dans un rappel de rendu et conserve la page actuelle; les pages peuvent être remontées après une mise à jour du plugin
- `Amelioration` Les vérifications et ComposeError signalent uniformément les plugins absents, désactivés, non autorisés ou incompatibles, les permissions manquantes et les sessions fermées; la libération couvre aussi les fenêtres annulées avant leur attachement natif; La mise à jour, la désinstallation ou la désactivation du plugin ferme ses sessions actives et signale l'erreur correspondante
- `Dependance` Ajout de common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, verrouille par empreinte)
- `Dependance` Ajout de Jetpack Compose BOM 2026.09.00 (Apache 2.0)
- `Dependance` Ajout de compose-ui-api.aar V1 aligné sur AutoJs6 6.8.0 (5316) (MPL 2.0, empreinte verrouillée), avec les dépendances partagées alignées sur l'hôte
- `Dependance` Ajout de Compose UI Test géré par BOM 2026.09.00 (Apache 2.0, uniquement pour les tests)
- `Dependance` Ajout de JaCoCo version 0.8.14 (couverture des tests facultative uniquement, exclu des paquets de publication)

##### Pour un historique plus complet, voir

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/app/src/main/assets/doc/CHANGELOG-fr.md)

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

Ce projet est publie sous la [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/LICENSE). Les informations de licence des composants tiers figurent dans [THIRD_PARTY_NOTICES.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md).

******

### Liens

******

- Projet AutoJs6: https://github.com/SuperMonster003/AutoJs6
- Documentation AutoJs6: https://docs.autojs6.com
- Documentation du module compose: https://docs.autojs6.com/#/compose
- Jetpack Compose: https://developer.android.com/compose
- Mentions tierces: https://github.com/SuperMonster003/AutoJs6-Plugin-Compose-UI/blob/master/THIRD_PARTY_NOTICES.md
