******

### Historique des versions

******

# v1.0.0

###### 2026/10/03

* `Indication` Aperçu de développement P3: compose / $compose appelables, 29 fabriques de noeuds, handles persistants, state/render/ref réactifs, batch/post/theme, montage UI et fenêtres flottantes raw ou redimensionnables fonctionnent avec une compilation locale compatible de l'hôte AutoJs6. Les vérifications de disponibilité, erreurs typées et libération des sessions sont incluses. Les exemples inclus, la documentation complète, les déclarations de types et la matrice de vérification élargie restent à livrer. Cet aperçu local n'a pas de publication officielle
* `Indication` Necessite AutoJs6 6.8.0 (5316) ou ulterieur
* `Nouveaute` Squelette du depot du plugin : chaine de compilation du plugin de versions de plateforme, dependances Jetpack Compose BOM 2026.09.00, protocole d'activation Wake Activity et service INFO (categorie compose-ui)
* `Nouveaute` README, instruction du centre de plugins et journal des modifications en 10 langues, generes a partir de sources JSON
* `Nouveaute` L'aperçu prend en charge les mises en page, textes, icônes, images, boutons, contrôles de sélection et curseurs; les bitmaps fournis restent la propriété de l'appelant, qui gère leur libération
* `Nouveaute` Les 20 opérations Modifier conservent l'ordre déclaré et prennent en charge la validation du contexte de mise en page, le défilement et les libellés d'accessibilité
* `Nouveaute` Les thèmes Material 3 prennent en charge les couleurs sources, les modes clair et sombre, les couleurs dynamiques du système sur Android 12+, les familles de polices et la taille du texte
* `Nouveaute` Les mises à jour de l'interface sont atomiques et conservent la dernière vue valide en cas de rejet; les entrées contrôlées signalent les changements par des callbacks en file, libérés à la fermeture
* `Nouveaute` Les champs de texte de l'aperçu conservent la sélection et la composition IME, prennent en charge le focus et les modifications explicites, et refusent les modifications retardées qui écraseraient une saisie plus récente
* `Nouveaute` L'aperçu ajoute des listes à chargement différé avec clés stables et défilement par indice, les emplacements de Scaffold et de la barre supérieure, des dialogues contrôlés, des indicateurs de progression et des rappels en file pour les actions ou fermetures de Snackbar
* `Nouveaute` L'aperçu de script fournit compose / $compose appelables, 29 fabriques de noeuds, des handles persistants, state/render/ref réactifs, le regroupement, la planification et le contrôle du thème
* `Nouveaute` Les scripts UI peuvent monter du contenu Compose; remplacer le montage ou arrêter le script libère l'ancienne session et ses rappels
* `Nouveaute` Les scripts non UI peuvent créer des fenêtres Compose raw ou redimensionnables, modifier position et taille en pixels, toucher et focus, puis les fermer via leurs commandes, floaty.closeAll ou l'arrêt du script
* `Amelioration` Les vérifications et ComposeError signalent uniformément les plugins absents, désactivés, non autorisés ou incompatibles, les permissions manquantes et les sessions fermées; la libération couvre aussi les fenêtres annulées avant leur attachement natif
* `Dependance` Ajout de common-plugin-api.aar version 6.8.0 (5307) (MPL 2.0, verrouille par empreinte)
* `Dependance` Ajout de Jetpack Compose BOM 2026.09.00 (Apache 2.0)
* `Dependance` Ajout de compose-ui-api.aar V1 aligné sur AutoJs6 6.8.0 (5316) (MPL 2.0, empreinte verrouillée), avec les dépendances partagées alignées sur l'hôte
* `Dependance` Ajout de Compose UI Test géré par BOM 2026.09.00 (Apache 2.0, uniquement pour les tests)
