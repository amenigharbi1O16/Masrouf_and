# Analyse des Trois Profils Utilisateurs — Masrouf Android Natif

> **Mise au point académique obligatoire** : L'application Android native implémente exactement **3 profils utilisateurs** définis dans [AppRole.java](../app/src/main/java/com/example/masrouf1/AppRole.java) : **`USER`** (`"user"`), **`PARENT`** (`"parent"`) et **`CHILD`** (`"child"`). **Il n'existe aucun profil `Admin` distinct dans le code actuel**, et le profil **`PARENT` ne doit pas être présenté comme un administrateur système global** — il administre uniquement son foyer et les comptes enfants qu'il crée.

---

## 1. Signalement honnête concernant l'absence du rôle `Admin`

| Aspect vérifié dans le code | Constat exact | Fichier de preuve |
|---|---|---|
| Valeurs de l'énumération `AppRole` | Uniquement `PARENT("parent")`, `USER("user")`, `CHILD("child")` | [AppRole.java#L20](../app/src/main/java/com/example/masrouf1/AppRole.java#L20) |
| Traitement d'une éventuelle chaîne `"admin"` | `AppRole.from("admin")` convertit et renvoie `PARENT` ; aucune logique Admin globale n'existe | [AppRole.java#L40-L42](../app/src/main/java/com/example/masrouf1/AppRole.java#L40-L42) |
| Inscription UI (`ChooseAccountTypeActivity` / `RegisterActivity`) | Seuls deux types existent : `"personal"` (`USER`) et `"family"` (`PARENT`) | [RegisterActivity.java#L192](../app/src/main/java/com/masrouf/controller/RegisterActivity.java#L192) |
| Compte de démonstration `DevelopmentSeedManager` | Le compte `admin.masrouf@gmail.com` porte `role = "parent"` et `accountType = "family"` (nom `"Parent Administrateur"`) | [DevelopmentSeedManager.java](../app/src/main/java/com/masrouf/controller/DevelopmentSeedManager.java) |
| Nommage historique `AdminDrawer` / `drawer_admin.xml` | Affiché lorsque `activeRole == AppRole.PARENT` uniquement | [MainActivity.java#L169-L196](../app/src/main/java/com/example/masrouf1/MainActivity.java#L169-L196) |

**Différence avec l'exigence de l'enseignant** : l'exigence demande au moins deux types d'utilisateurs dont un administrateur. Techniquement, Masrouf possède **trois types de comptes** (`USER`, `PARENT`, `CHILD`), mais aucun super-administrateur global pouvant gérer les autres utilisateurs indépendants (`USER`) ou modifier la table `settings` (la table `settings` est un singleton protégé par triggers SQLite).

---

## 2. Profil 1 — `USER` (Utilisateur Normal / Compte Personnel)

### Rôle et permissions
- Gère librement son propre foyer individuel (`accountType = "personal"`, `role = "user"`).
- Peut définir/modifier ses périodes budgétaires, gérer toutes ses catégories (catalogue + personnalisées), enregistrer des achats/dépenses et consulter ses graphiques sur 6 mois.
- **Restrictions** : bloqué sur `FamilyBudgetsAnalyticsActivity` (via `SectionActivity.requiresParentRole()` -> `PARENT` requis), n'a pas accès à `ChildrenManagementActivity` ni aux demandes d'argent (`MoneyRequestActivity` caché dans l'interface).

### Authentification & Onboarding
- Inscription libre depuis `ChooseAccountTypeActivity` (`personal_card`) → `RegisterActivity` (Gmail strict + mot de passe PBKDF2 120 000 itérations).
- Connexion dans `LoginActivity` → si `categoriesSelected == false`, passage obligatoire par `OnboardingActivity` (3 étapes) pour choisir ses catégories système.

### Interface, Dashboard, Side Drawer & Navigation
- **Hôte** : `MainActivity` (`activity_dashboard_host.xml`).
- **Tiroir latéral** : `AppDrawer` (`drawer_personal`, fond clair, badge `@string/drawer_personal`), **7 entrées** : `Accueil`, `Produits`, `Calendrier`, `Analyses`, `Récompenses`, `Notifications`, `Paramètres`.
- **Dashboard (`HomeFragment`)** :
  - Masque `home_children_tracking`, `home_manage_children` et `home_child_summary` (contenant `home_request_money`).
  - Affiche `home_summary` (`@string/dashboard_personal_summary` : total dépensé + nombre de transactions), carte `BudgetCardView`, liste d'enveloppes de catégories (`home_categories_toggle`, **4** visibles par défaut avant déploiement via « Voir tout »), actions `Ajouter un achat` / `Ajouter une dépense` / `Gérer les catégories` / `Répartir`, et 5 dernières transactions récentes.
- **Onglet Analyses** : `AnalyticsFragment` → `AnalyticsRenderer.renderUserOrChild(...)` (synthèse mensuelle, barres de consommation, graphique 6 mois `PillBarChart`, historique).
- **Données & Notifications** : données isolées par `userId` (`budgets`, `budget_lines`, `products`, `purchases`, `money_transactions`, `user_categories`, `notifications`, `points_log`). Reçoit alertes budget 80 % / 100 % / dépassement, alerte enveloppe catégorie, dépense importante (≥ 100 DT ou ≥ 25 % si ≥ 10 DT), clôture et points.

---

## 3. Profil 2 — `PARENT` (Responsable Familial / Compte Famille)

### Rôle et permissions
- Crée un foyer familial (`role = "parent"`, `accountType = "family"`).
- Peut créer des comptes enfants (`createChildAccount`), définir leur dotation mensuelle initiale ou complémentaire (`giveMoneyToChild`, déduite de l'enveloppe parent `Enfants` et du budget actif), réinitialiser leur mot de passe, approuver ou refuser leurs demandes d'argent (`resolveMoneyRequest`).
- **Confidentialité parent/enfant** : consulte uniquement le cumul alloué, dépensé, restant et l'historique budgétaire sur 4 mois de chaque enfant (`ChildSpendingSummary`, `BudgetHistoryRow`). **Ne voit jamais le détail ligne par ligne des dépenses ou achats de l'enfant**.
- **Restrictions** : bloqué sur l'écran `RewardsActivity` si `disallowParentRole()` est actif (selon `SectionActivity`), et ne peut pas passer par `MoneyRequestActivity` (réservé aux comptes `CHILD` dans le Repository).

### Authentification & Catégorie protégée
- Inscription libre via `family_card` → `RegisterActivity` (`TYPE = "family"`).
- Au terme de `OnboardingActivity`, `MasroufDao.finishOnboarding()` appelle `ensureChildrenCategoryRow(userId)` afin de garantir l'existence de la catégorie `Enfants` pour l'imputation des dotations.

### Interface, Dashboard, Side Drawer & Navigation
- **Tiroir latéral** : `AdminDrawer` (`drawer_admin.xml`, fond sombre `@color/forest`, icônes `@color/mint`, texte blanc).
- **7 destinations** : `Accueil` (`home`), `Enfants` (`children` avec badge du nombre d'enfants), `Produits` (`products`), `Calendrier & historique` (`calendar`), `Analyses budgétaires` (`analytics`), `Notifications` (`notifications` avec badge non lues), `Paramètres` (`settings`).
- **Activités spécifiques Parent** :
  - `ChildrenManagementActivity` : liste `ChildAdapter`, carte enveloppe `Enfants`, demandes d'argent en attente (`Approuver` / `Refuser`), dialogues `dialog_add_child` et `dialog_give_money`, Intents implicites Appel (`ACTION_DIAL`) et SMS (`ACTION_SENDTO`).
  - `FamilyBudgetsAnalyticsActivity` : tableau d'analyse familiale dédié avec `ChildrenBudgetAnalyticsView` et `PillBarChartView`.
- **Dashboard (`HomeFragment`)** : affiche `home_children_tracking`, `home_manage_children`, le résumé familial complet `@string/dashboard_family_summary_v2`, les allocations enfants intégrées dans le budget du foyer, et dans `VoiceTransactionDialogFragment`, débloque la 3e pilule d'opération **`Dotation enfant`** (`opChild`).
- **Notifications reçues** : ses propres seuils budgétaires + les alertes enfants (`child_budget_caution` à 80 %, `child_budget_100`, `child_budget_over`, `child_important_tx`, `money_request`).

---

## 4. Profil 3 — `CHILD` (Compte Enfant)

### Rôle et permissions
- Compte subordonné créé exclusivement par un `PARENT` dans `ChildrenManagementActivity` (lié dans `child_profiles`).
- Peut consulter sa dotation, répartir ses enveloppes à l'intérieur de sa dotation (`AllocateBudgetActivity` avec `IS_CHILD = true`), enregistrer ses dépenses/achats, utiliser son catalogue produits, demander de l'argent au parent (`MoneyRequestActivity`) et échanger ses points contre des récompenses (`RewardsActivity`).
- **Restrictions strictes** :
  - **Pas d'inscription autonome** possible depuis `RegisterActivity`.
  - **Ne peut pas créer/modifier librement le plafond du budget familial ni ses dates** (`home_define_budget` / modification budget verrouillés dans `AllocateBudgetActivity` / `BudgetPeriodActivity`).
  - **Choix de catégories restreint** : dans `OnboardingActivity` et `ManageCategoriesActivity`, `allowedSystemCategoriesForChild(userId)` limite ses sélections aux catégories autorisées/activées par son parent (+ `other` protégée).
  - Son solde (`users.balanceMilli`) est décrémenté transactionnellement à chaque achat/dépense et re-crédité lors des dotations/demandes approuvées.

### Interface, Dashboard, Side Drawer & Navigation
- **Tiroir latéral** : `AppDrawer` (`drawer_personal`, badge `@string/drawer_child`).
- **Dashboard (`HomeFragment`)** :
  - Affiche la carte de suivi enfant (`renderChildSummary` / `home_child_summary`) : total de dotation reçue + dernier versement (`home_child_received`), objectif d'épargne principal (`home_child_goal` — affiche `R.string.child_goal_missing` si aucun objectif en table `goals`), bouton `home_request_money` (`Demander de l'argent`) et bouton `home_partition_child_budget` / `Répartir` (`AllocateBudgetActivity` avec `IS_CHILD = true`). Les points accumulés et récompenses s'affichent dans `RewardsActivity`.
- **Navigation spécifique** :
  - `MoneyRequestActivity` : formulaire montant + motif + historique des statuts (`pending`, `accepted`, `rejected`).
  - `SettingsFragment` : affiche `settings_child_allocation_card` (dotation reçue, dépensé, solde actuel), et des raccourcis **Appeler un parent** (`ACTION_DIAL`) / **Envoyer un SMS** (`ACTION_SENDTO`) utilisant `child_profiles.parentPhone`.

---

## 5. Tableau Comparatif Complet des Trois Profils

| Critère | `USER` (Normal User) | `PARENT` (Famille) | `CHILD` (Enfant) |
|---|---|---|---|
| **Valeur `users.role`** | `"user"` | `"parent"` | `"child"` |
| **Mode création de compte** | Auto-inscription (`RegisterActivity`) | Auto-inscription (`RegisterActivity`) | Créé par le Parent (`ChildrenManagementActivity`) |
| **Side Drawer & Style** | `AppDrawer` (fond clair) | `AdminDrawer` (fond `#112B28`) | `AppDrawer` (fond clair) |
| **Badge tiroir** | `"Compte personnel"` (`personal_drawer_badge`) | `"Compte parent"` (`admin_drawer_badge` dans `drawer_admin.xml`) + nom parent | `"Compte enfant"` (`personal_drawer_badge`) |
| **Onglet / Écran Enfants** | Non | **Oui** (`ChildrenManagementActivity` + badge) | Non |
| **Analyses Familiales** | Non (`AnalyticsFragment`) | **Oui** (`AnalyticsFragment` + `FamilyBudgetsAnalyticsActivity`) | Non (`AnalyticsFragment`) |
| **Définir plafond budget** | Oui (`BudgetPeriodActivity`) | Oui (`BudgetPeriodActivity`) | Verrouillé (fixé par dotation parentale) |
| **Répartir les catégories** | Oui (libre sur son budget) | Oui (+ enveloppe `Enfants`) | Oui (dans sa dotation parentale) |
| **Catalogue catégories** | Toutes (16 système + custom) | Toutes (+ `Enfants` forcée) | Sous-ensemble du Parent + `other` |
| **Demander de l'argent** | Non | Non (approuve/refuse) | **Oui** (`MoneyRequestActivity`) |
| **Dictée vocale `Dotation enfant`** | Masquée | **Active** (`opChild`) | Masquée |
| **Récompenses (`RewardsActivity`)** | Points & niveau | Bloqué (`disallowParentRole`) | **Points, niveau & échange récompenses** |
| **Appeler / SMS Parent** | Numéro par défaut si testé | Appeler/SMS chaque enfant | Appeler/SMS son `parentPhone` |
| **Supervision inter-comptes** | Aucune | Totaux/enveloppes enfants (sans détails tx) | Aucune |
