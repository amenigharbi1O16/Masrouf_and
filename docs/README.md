# Documentation Technique Canonique — Masrouf Android Natif

> **Référence absolue** : Code source Java 11, ressources XML et base SQLite Room **v11** (`18` tables) présents dans `app/src/main`.  
> **Scope strict** : Application Android native uniquement (aucun mélange React Native).

---

## 1. Sommaire de la documentation (`docs/`)

1. **[PROFILES.md](PROFILES.md)** — Documentation indépendante des 3 profils réels (`USER`, `PARENT`, `CHILD`), matrice comparative complète et mise au point sur l'absence du rôle `Admin`.
2. **[LIBRARIES.md](LIBRARIES.md)** — Inventaire exhaustif des bibliothèques Gradle directes, indirectes, API natives du SDK Android et bibliothèques absentes (un fichier unique).
3. **[NAVIGATION.md](NAVIGATION.md)** — Architecture Android hybride, navigation par `FragmentManager` et `Intent` explicites, `SystemIntents` implicites, extras/Bundles et diagrammes Mermaid.
4. **[DATABASE.md](DATABASE.md)** — Base Room / SQLite (`masrouf.db`, 18 entités, migrations `1 -> 11`), threading réel (`ExecutorService` + `Handler` + `LiveData` vs nature synchrone de SQLite), persistance et sauvegarde `VACUUM INTO`.
5. **[TEACHER_REQUIREMENTS.md](TEACHER_REQUIREMENTS.md)** — Vérification point par point des 8 exigences officielles de l'enseignant avec preuves vérifiées.
6. **[COMPOSANTS/](COMPOSANTS/INDEX.md)** — Architecture des composants UI, vues Java vs XML, custom views, listeners, adapters, Activities et Fragments :
   - [COMPOSANTS/INDEX.md](COMPOSANTS/INDEX.md)
   - [COMPOSANTS/COMPOSANTS_REUTILISABLES.md](COMPOSANTS/COMPOSANTS_REUTILISABLES.md)
   - [COMPOSANTS/ACTIVITIES_ET_FRAGMENTS.md](COMPOSANTS/ACTIVITIES_ET_FRAGMENTS.md)
7. **[DESIGN/](DESIGN/CHARTE_ET_QUALIFIERS.md)** — Design system botanique, qualifiers multi-langues / écrans et fiches écran par écran :
   - [DESIGN/CHARTE_ET_QUALIFIERS.md](DESIGN/CHARTE_ET_QUALIFIERS.md)
   - [DESIGN/PAGES_AUTH_ET_ACCUEIL.md](DESIGN/PAGES_AUTH_ET_ACCUEIL.md)
   - [DESIGN/PAGES_METIER_ET_FAMILLE.md](DESIGN/PAGES_METIER_ET_FAMILLE.md)
8. **[ACADEMIC_DOCUMENT/DOCUMENT_ACADEMIQUE.md](ACADEMIC_DOCUMENT/DOCUMENT_ACADEMIQUE.md)** et **[Masrouf_Documentation_Academique.pdf](Masrouf_Documentation_Academique.pdf)** — Rapport académique unifié avec diagrammes de cas d'utilisation, architecture, classes, navigation, séquence et ERD 18 tables.

---

## 2. Réponse claire : MVC, Repository et DAO

**Le projet suit-il le patron MVC avec un composant DAO ou Repository pour gérer la connexion à la base de données ?**

**Oui, sous une forme hybride MVC + MVVM + Repository + DAO :**

| Couche | Implémentation réelle vérifiée | Rôle exact |
|---|---|---|
| **Modèle (`Model`)** | [MasroufEntities.java](../app/src/main/java/com/example/masrouf1/db/MasroufEntities.java) (18 `@Entity`) + projections SQL (`*Row`, `*Summary`) + DTO UI (`*Data`) | Structure relationnelle SQLite et modèles métier immuables/typés. |
| **Vue (`View`)** | 50 layouts XML (`res/layout/`), vues Java (`AnalyticsRenderer`, `DashboardSectionFragment`, `AppDrawer`, `AdminDrawer`) et custom views (`BudgetCardView`, `PillBarChart`...) | Affichage pur et captation des événements utilisateur. |
| **Contrôleur / ViewModel (`Controller` + `MVVM`)** | `com.masrouf.controller.*` (`HomeFragment`, `LoginActivity`, `AddProductActivity`...) reliés à [`MasroufViewModel`](../app/src/main/java/com/example/masrouf1/MasroufViewModel.java) via [`MasroufViewModelFactory`](../app/src/main/java/com/example/masrouf1/MasroufViewModelFactory.java) | Historiquement nommé `com.masrouf.controller`, le contrôle d'écran délègue désormais le chargement asynchrone (`ExecutorService` + `Handler`) et l'état réactif (`LiveData`) à `MasroufViewModel`. |
| **Repository (`Repository`)** | [`MasroufRepository`](../app/src/main/java/com/example/masrouf1/MasroufRepository.java) | Point d'entrée unique métier qui encapsule `MasroufDao`, le hachage PBKDF2, la validation des rôles et les alertes `BudgetNotificationManager`. |
| **DAO & Connexion DB** | [`MasroufDao`](../app/src/main/java/com/example/masrouf1/db/MasroufDao.java) (`@Dao`) + [`MasroufDatabase`](../app/src/main/java/com/example/masrouf1/db/MasroufDatabase.java) (`RoomDatabase` singleton `masrouf.db`) | Exécute toutes les requêtes SQL et transactions atomiques `@Transaction`. |

> **Précision d'audit** :
> 1. `MainActivity` interroge directement `MasroufDatabase.getInstance(...).masroufDao().findUser(userId)` sur un thread `ACCOUNT_GATE` au démarrage pour valider la session avant d'afficher les onglets.
> 2. Les workers/services d'arrière-plan (`BudgetRolloverWorker`, `BudgetEvaluationService`) et `DevelopmentSeedManager` / `BudgetNotificationManager` accèdent également au singleton `MasroufDatabase`.
> 3. Trois façades héritées d'une ancienne structure MVC stricte (`UserDao`, `BudgetDao`, `BudgetRepository`) subsistent dans le dépôt sans aucun appelant actif depuis l'UI.
