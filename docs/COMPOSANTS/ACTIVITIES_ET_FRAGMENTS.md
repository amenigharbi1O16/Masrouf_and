# Inventaire Complet des Activities, Fragments & Helpers — Masrouf Android Natif

---

## 1. Hôte principal & Fragments d'onglets

| Composant | Package | Rôle, données & communications inter-fichiers |
|---|---|---|
| **`MainActivity`** | `com.example.masrouf1` | Hôte `singleTop` vérifiant la session utilisateur en arrière-plan (`ACCOUNT_GATE` -> `MasroufDao.findUser`), configurant `AppDrawer` ou `AdminDrawer`, la navigation par `FragmentManager` (`Home`, `Products`, `Calendar`, `Analytics`, `Settings`), la permission `POST_NOTIFICATIONS` et le FAB vocal. |
| **`HomeFragment`** | `com.masrouf.controller` | Tableau de bord adapté aux 3 rôles (`USER`, `PARENT`, `CHILD`), observe `MasroufViewModel.home()`, écoute `"masrouf-categories-updated"` & `"masrouf-voice-saved"`, route vers budgets, achats, dépenses, catégories, enfants et demandes d'argent. |
| **`ProductsFragment`** | `com.example.masrouf1` | Catalogue produits recherchable (clavier + voix `VoiceFieldHelper`), filtrable par `ChipGroup` et triable (`A–Z` / `Prix`) via `ProductCatalogAdapter`. |
| **`CalendarFragment`** | `com.example.masrouf1` | Explorateur annuel par année (`calendarYear`), grille 12 mois, périodes chevauchantes (`Consulter` / `Modifier` / `Sélectionner`) et transactions du mois. |
| **`AnalyticsFragment`** | `com.example.masrouf1` | Charge `AnalyticsSummaryData` via `MasroufViewModel.request(...)` et délègue tout le rendu Java à `AnalyticsRenderer`. |
| **`SettingsFragment`** | `com.example.masrouf1` | Profil éditable (Nom/Gmail), carte dotation enfant, puces catégories suivies, changement mot de passe, export partage texte, sauvegarde `MasroufDatabase.backupDatabase()`, manuel PDF, appel/SMS parent, changement de langue instantané `LocaleController.apply()` et déconnexion `SessionStore.clear()`. |

---

## 2. Toutes les Activities secondaires (`app/src/main/AndroidManifest.xml`)

### Entrée, Authentification & Onboarding
- **`WelcomeActivity`** (`LAUNCHER`) : accueil + carrousel 3 slides -> `ChooseAccountTypeActivity` / `LoginActivity`.
- **`ChooseAccountTypeActivity`** : sélection `"personal"` (`USER`) ou `"family"` (`PARENT`) -> `RegisterActivity`.
- **`RegisterActivity`** : validation Gmail + mot de passe fort (5 règles) -> `MasroufRepository.register()` (PBKDF2).
- **`LoginActivity`** : authentification locale -> `OnboardingActivity` ou `MainActivity`.
- **`OnboardingActivity`** : configuration initiale en 3 étapes -> `MasroufDao.finishOnboarding()`.
- **`ChangePasswordActivity`** : vérification ancien mot de passe + mise à jour hachage PBKDF2 dans `users`.

### Budgets, Catégories, Dépenses & Produits
- **`BudgetPeriodActivity`** : sélection dates ISO + plafond + calendrier cliquable -> `AllocateBudgetActivity`.
- **`AllocateBudgetActivity`** : répartition bidirectionnelle `%` / `DT` -> `insertBudgetWithEnvelopes` / `updateBudgetWithEnvelopes` / `replaceChildBudgetEnvelopes`.
- **`ManageCategoriesActivity`** : activation/désactivation multi-catégories et suppression sécurisée -> `updateUserCategorySelections` / `deleteCustomCategory`.
- **`AddExpenseActivity`** : saisie dépense directe + dictée vocale + date -> `MasroufRepository` -> `recordExpense`.
- **`AddProductActivity`** : saisie achat + photo locale + prix unitaire en direct -> `recordPurchase` + `price_history`.
- **`ProductDetailsActivity`** : fiche produit + courbe `PriceChartView` + historique achats -> `ProductPriceHistoryActivity` / `ComparePriceActivity`.
- **`ProductPriceHistoryActivity`** : chronologie détaillée par achat et magasin (`PriceHistoryAdapter`).
- **`ComparePriceActivity`** : comparaison multi-magasins / prix unitaire d'un produit.
- **`TransactionDetailsActivity`**, **`EditExpenseActivity`**, **`EditPurchaseActivity`** : consultation lecture seule des transactions (héritage commun `TransactionDetailActivity` / `BaseFeatureActivity`).

### Famille, Notifications, Récompenses & Utilitaires
- **`ChildrenManagementActivity`** (`PARENT`) : création comptes enfants (`createChildAccount`), dons (`giveMoneyToChild`), réinitialisation mot de passe, validation/refus `money_requests`, appel/SMS (`SystemIntents`).
- **`FamilyBudgetsAnalyticsActivity`** (`PARENT`) : vue analytique familiale dédiée héritant de `SectionActivity` (`requiresParentRole() == true`).
- **`MoneyRequestActivity`** (`CHILD`) : soumission et suivi des demandes d'argent (`requestMoney`).
- **`RewardsActivity`** (`disallowParentRole() == true`) : affichage points, niveaux, historique `points_log` et échange `redeemReward`.
- **`NotificationsActivity`** : boîte de notifications persistées (`NotificationAdapter`), marquage lu et redirection vers l'écran cible.
- **`CongratulationsActivity`** : célébration de clôture budgétaire + son `AchievementPlayer` (`raw/achievement.mp3`).
- **`ConfirmationActivity`** : écran générique de confirmation.
- **`UserManualActivity`** : ouvre `raw/manuel_utilisation.pdf` via `SystemIntents.openManual` puis se termine immédiatement.
- **`ProductsActivity`**, **`CalendarActivity`**, **`AnalyticsActivity`**, **`SettingsActivity`** : 4 redirecteurs rétrocompatibles renvoyant vers `MainActivity` avec `EXTRA_CENTRAL_TAB`.

---

## 3. Classes utilitaires & Persistance

| Classe | Mission |
|---|---|
| `MasroufApplication` | Amorçage Debug (`DevelopmentSeedManager`), canal notification, `AlarmManager` (`BudgetEvaluationService`) et `WorkManager` (`BudgetRolloverWorker`). |
| `MasroufViewModel` / `MasroufViewModelFactory` | Pont MVVM entre écrans et `MasroufRepository`, exécuteur mono-thread `IO`, `DataState<T>`, `MediatorLiveData` et `Handler`. |
| `MasroufRepository` | Logique métier complète, contrôles d'appartenance et rôles, déclenchement `BudgetNotificationManager`. |
| `MoneyUtils` / `MoneyMath` / `CurrencyFormatter` / `BudgetPeriodMath` | Arithmétique entière exacte (`BigInteger` / `BigDecimal`, `millimes`, `bps`) et bornes calendaires. |
| `PasswordHasher` / `InputValidation` / `SessionStore` | Hachage PBKDF2-HMAC-SHA256/SHA1 (120 000 itérations, sel 16 octets), validation Gmail/MDP et session `SharedPreferences`. |
| `DevelopmentSeedManager` | Données de test Debug uniquement (`BuildConfig.DEBUG`) pour `parent` (`admin.masrouf@gmail.com`), `user`, `child`. |
| `UserDao`, `BudgetDao`, `BudgetRepository` | Anciennes façades MVC conservées sans appelant actif. |
