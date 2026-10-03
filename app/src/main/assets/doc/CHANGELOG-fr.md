******

### Historique des versions

******

# v1.0.0

###### 2026/10/03

* `Indication` Aperçu de développement P2: les 30 entrées du catalogue V1 sont implémentées dans un hôte de test dédié, avec 29 composants de noeud et la commande Snackbar. Les champs de texte natifs, listes à chargement différé, Scaffold, dialogues et indicateurs de progression sont disponibles dans cet aperçu. L'API publique de script compose reste prévue pour P3
* `Indication` Necessite AutoJs6 6.8.0 (5316) ou ulterieur
* `Nouveaute` Squelette du depot du plugin : chaine de compilation du plugin de versions de plateforme, dependances Jetpack Compose BOM 2026.09.00, protocole d'activation Wake Activity et service INFO (categorie compose-ui)
* `Nouveaute` README, instruction du centre de plugins et journal des modifications en 10 langues, generes a partir de sources JSON
* `Nouveaute` L'aperçu prend en charge les mises en page, textes, icônes, images, boutons, contrôles de sélection et curseurs; les bitmaps fournis restent la propriété de l'appelant, qui gère leur libération
* `Nouveaute` Les 20 opérations Modifier conservent l'ordre déclaré et prennent en charge la validation du contexte de mise en page, le défilement et les libellés d'accessibilité
* `Nouveaute` Les thèmes Material 3 prennent en charge les couleurs sources, les modes clair et sombre, les couleurs dynamiques du système sur Android 12+, les familles de polices et la taille du texte
* `Nouveaute` Les mises à jour de l'interface sont atomiques et conservent la dernière vue valide en cas de rejet; les entrées contrôlées signalent les changements par des callbacks en file, libérés à la fermeture
* `Nouveaute` Les champs de texte de l'aperçu conservent la sélection et la composition IME, prennent en charge le focus et les modifications explicites, et refusent les modifications retardées qui écraseraient une saisie plus récente
* `Nouveaute` L'aperçu ajoute des listes à chargement différé avec clés stables et défilement par indice, les emplacements de Scaffold et de la barre supérieure, des dialogues contrôlés, des indicateurs de progression et des rappels en file pour les actions ou fermetures de Snackbar
* `Dependance` Ajout de common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, verrouille par empreinte)
* `Dependance` Ajout de Jetpack Compose BOM 2026.09.00 (Apache 2.0)
* `Dependance` Ajout de compose-ui-api.aar V1 aligné sur AutoJs6 6.8.0 (5316) (MPL 2.0, empreinte verrouillée), avec les dépendances partagées alignées sur l'hôte
* `Dependance` Ajout de Compose UI Test géré par BOM 2026.09.00 (Apache 2.0, uniquement pour les tests)
