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

Compose UI est un plugin de rendu d'interface pour AutoJs6. Les scripts declarent leur interface via l'objet global `compose` integre a l'hote, et le plugin la rend dans le processus de l'hote avec Jetpack Compose et Material 3, offrant une solution declarative unique au contenu des activites en mode `"ui";` et aux fenetres flottantes.

Le plugin n'embarque aucun ecran autonome et n'ajoute aucune entree dans le lanceur. L'hote le decouvre via le service INFO, lit sa version et ses informations de compatibilite, puis charge le moteur de rendu dans son propre processus selon le contrat (`org.autojs.plugin.compose.api`). L'arbre d'interface, l'etat et les evenements vivent cote script ; le moteur de rendu se contente d'appliquer les correctifs a la composition Compose et de renvoyer les evenements utilisateur au script.

******

### Etat actuel

******

Aperçu de développement P1: le chargeur hôte V1 et les sessions exécutent un compteur Column / Text / Button dans un hôte de test dédié. Le moteur de rendu complet et l'API de script compose restent en développement.

******

### Fonctionnalites

******

Capacites principales que le plugin doit livrer:

- Interface declarative : `compose.state` + `compose.mount(render)` redessinent automatiquement a chaque changement d'etat, tandis que des poignees de noeud durables (`compose.Text({...})` et consorts) permettent de modifier directement proprietes et enfants
- Ensemble de composants Material 3 : dispositions (Column / Row / Box / LazyColumn, etc.), texte, boutons, champs de saisie, interrupteurs, curseurs, indicateurs de progression, cartes, boites de dialogue
- Modifiers chaines : `compose.modifier().padding(16).fillMaxWidth().background('#FFFFFF')` conserve l'ordre des operations, et les operations a portee limitee sont validees cote hote
- Deux surfaces d'affichage : le contenu d'activite des scripts `"ui";` (`compose.mount`) et les fenetres flottantes de n'importe quel script (`compose.floaty`)
- Rendu dans le processus : le moteur de rendu s'execute dans le processus de l'hote sans aucun pont d'interface inter-processus, pour des evenements et des mises a jour d'etat a faible latence
- Paquet unique : aucune variante d'ABI et aucun code natif propre (seulement l'assistant AndroidX graphics-path fourni avec Compose, integre pour les quatre ABI), un seul APK pour tous les appareils

******

### Utilisation

******

1. Installez AutoJs6 6.8.0 (5316) ou une version ulterieure
2. Installez l'APK de ce plugin (rien a ouvrir, le plugin n'a pas d'entree dans le lanceur)
3. Verifiez dans le centre de plugins d'AutoJs6 que Compose UI est reconnu et active
4. Utilisez directement l'objet global `compose` dans vos scripts (le rendu arrive avec la version 1.0.0)

******

### Demarrage rapide

******

Les exemples ci-dessous montrent la forme cible de l'API (definie dans l'annexe A de la feuille de route, non executable tant que le rendu n'est pas livre) :

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
let status = compose.Text({ text: 'Preparation...' });
let win = compose.floaty(compose.Column({ padding: 12, bg: '#CC000000' }, [
    status,
    compose.TextButton({ onClick: () => win.close() }, 'Fermer'),
]), { x: 50, y: 300, raw: true });

threads.start(() => {
    for (let i = 1; i <= 100; i++) {
        sleep(1000);
        compose.post(() => status.set({ text: `Progression ${i}%` }));
    }
});
```

La reference complete de l'API (catalogue de composants, operations de Modifier, objets de session, codes d'erreur) se trouve dans le chapitre du module compose de la documentation AutoJs6.

******

### Compatibilite

******

Exigences d'execution et limites du plugin:

- Version minimale d'AutoJs6 : 6.8.0 (5316) ou ulterieure ; les hotes plus anciens signalent le plugin comme incompatible dans le centre de plugins
- Version d'Android : 7.0 (API 24) ou ulterieure
- Architecture du processeur : arm64-v8a / armeabi-v7a / x86_64 / x86 (les quatre integrees dans l'APK unique, aucun choix par architecture)
- Version de Compose : embarquee dans le plugin (BOM 2026.09.00), independante du runtime Compose de l'hote
- Version du contrat : 1 ; l'hote et le plugin negocient la version du contrat et refusent le chargement avec une erreur explicite en cas de desaccord

******

### FAQ

******

- Pourquoi aucune icone de plugin n'apparait apres l'installation ? Le plugin n'a ni interface autonome ni entree dans le lanceur ; consultez le centre de plugins d'AutoJs6
- Pourquoi `compose` ne fonctionne-t-il pas encore dans les scripts ? Il s'agit d'un apercu de developpement P0 ; le moteur de rendu et l'API de script arrivent dans les prochaines etapes
- Faut-il desinstaller d'autres plugins d'interface ? Non, Compose UI n'interfere ni avec le module `ui` existant ni avec les autres plugins
- Faut-il modifier les scripts apres une mise a jour du plugin ? Non tant que la version du contrat reste la meme ; les montees de version du contrat sont indiquees explicitement dans le journal des modifications

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

#### v1.0.0

_2026/10/03_

- `Indication` Aperçu de développement P1: le chargeur hôte V1 et les sessions exécutent un compteur Column / Text / Button dans un hôte de test dédié. Le moteur de rendu complet et l'API de script compose restent en développement
- `Indication` Necessite AutoJs6 6.8.0 (5316) ou ulterieur
- `Nouveaute` Squelette du depot du plugin : chaine de compilation du plugin de versions de plateforme, dependances Jetpack Compose BOM 2026.09.00, protocole d'activation Wake Activity et service INFO (categorie compose-ui)
- `Nouveaute` README, instruction du centre de plugins et journal des modifications en 10 langues, generes a partir de sources JSON
- `Nouveaute` Le compteur de prévisualisation prend en charge les mises à jour incrémentales et libère les callbacks à la fermeture; une mise à jour rejetée conserve la dernière interface valide
- `Dependance` Ajout de common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, verrouille par empreinte)
- `Dependance` Ajout de Jetpack Compose BOM 2026.09.00 (Apache 2.0)
- `Dependance` Ajout de compose-ui-api.aar V1 aligné sur AutoJs6 6.8.0 (5316) (MPL 2.0, empreinte verrouillée), avec les dépendances partagées alignées sur l'hôte

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
