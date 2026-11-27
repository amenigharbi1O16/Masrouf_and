# Rapport d'Audit et de Mise à Jour — Masrouf Android Natif (Actualisé)

> **Statut** : Document mis à jour d'après le code source actuel de `Masrouf1` (Room **version 11**, **18 tables**, **30** `<activity>` dans `AndroidManifest.xml`).  
> Les écarts signalés dans la version initiale du 24/11/2026 ont été pour l'essentiel résolus dans le code actuel ; ce fichier reflète désormais **l'état exact du code vérifié**.

---

## 1. Résolution des anciens manques signalés le 24/11/2026

| Élément autrefois signalé manquant / cassé | État réel vérifié dans le code actuel | Référence exacte |
|---|---|---|
| Écran de changement de mot de passe | **Implémenté et déclaré** (`ChangePasswordActivity` + `activity_change_password.xml`), appelé depuis `SettingsFragment` | [ChangePasswordActivity.java](app/src/main/java/com/masrouf/controller/ChangePasswordActivity.java) |
| Historique des prix d'un produit | **Implémenté et déclaré** (`ProductPriceHistoryActivity` + `PriceHistoryAdapter`), ouvert depuis `ProductDetailsActivity` | [ProductPriceHistoryActivity.java](app/src/main/java/com/masrouf/controller/ProductPriceHistoryActivity.java) |
| Écran Félicitations (clôture budget) | **Implémenté et déclaré** (`CongratulationsActivity` + `AchievementPlayer` jouant `raw/achievement.mp3`) | [CongratulationsActivity.java](app/src/main/java/com/masrouf/controller/CongratulationsActivity.java) |
| Bascule œil mot de passe | **Unifiée et testée** par `PasswordVisibilityHelper` et `PasswordVisibilityInstrumentedTest` | [PasswordVisibilityHelper.java](app/src/main/java/com/masrouf/controller/PasswordVisibilityHelper.java) |
| Tiroirs latéraux (`AppDrawer` / `AdminDrawer`) | **Harmonisés** : 7 destinations avec icônes vectorielles, badges dynamiques et pilules langue FR/AR/EN | [AppDrawer.java](app/src/main/java/com/example/masrouf1/AppDrawer.java), [AdminDrawer.java](app/src/main/java/com/example/masrouf1/AdminDrawer.java) |
| Thème botanique crème/forêt/émeraude sans violet Material | **Appliqué globalement** dans `themes.xml`, `values-night/themes.xml` et `colors.xml` | [themes.xml](app/src/main/res/values/themes.xml) |
| Saisie vocale multilingue FR/AR/EN/Darija | **Implémentée hors ligne** (`VoiceSpeechController` + `VoiceParser` + `VoiceTransactionDialogFragment` + `VoiceFieldHelper`) | [VoiceParser.java](app/src/main/java/com/masrouf/controller/VoiceParser.java) |
| Sauvegarde SQLite locale et édition profil | **Implémentées** dans `SettingsFragment` (`updateName`, `updateEmail`, `backupDatabase()`, `SystemIntents.shareText()`) | [MasroufDatabase.java](app/src/main/java/com/example/masrouf1/db/MasroufDatabase.java) |

---

## 2. Points ouverts / partiels vérifiés honnêtement

1. **Absence d'un profil `Admin` distinct** : `AppRole` définit uniquement `USER`, `PARENT` et `CHILD`. `AppRole.from("admin")` redirige vers `PARENT`. Les classes/fichiers `AdminDrawer.java` et `drawer_admin.xml` servent en réalité le profil **Parent**.
2. **Détail transaction en lecture seule** : `EditExpenseActivity` et `EditPurchaseActivity` héritent de `TransactionDetailActivity` et affichent uniquement le titre et le montant (sans formulaire de modification ni bouton UI appelant `deleteExpenseTransaction`). `TransactionDetailsActivity` fournit la fiche complète en lecture seule.
3. **Tables `goals` et création de `rewards`** :
   - La table `goals` existe en base et est lue sur le tableau de bord Enfant (`HomeFragment`), mais aucun écran ne permet de créer un objectif d'épargne.
   - `RewardsActivity` permet d'afficher les points et de consommer un `RewardEntity`, mais aucune interface Parent ne permet de créer des récompenses.
4. **BroadcastReceiver `BudgetAllocationReceiver`** : déclaré dans `AndroidManifest.xml` (`exported="false"`), mais aucun composant courant n'appelle `sendBroadcast(ACTION_CHILD_ALLOCATION)` ; `MasroufRepository` notifie directement via `BudgetNotificationManager` / `NotificationEntity`.
5. **Classes historiques inutilisées** : `BudgetRepository.java`, `BudgetDao.java`, `UserDao.java`, `HomeCategoryAdapter.java`, `CategoryAdapter.java`, `RecentTransactionsAdapter.java`, `SafeClickListener.java` et `OnCategorySelectedListener.java` restent présents dans le dépôt sans appelant actif.

---

## 3. Suite documentaire de référence

Voir l'index complet dans [docs/README.md](docs/README.md) et le bilan académique dans [docs/TEACHER_REQUIREMENTS.md](docs/TEACHER_REQUIREMENTS.md).
