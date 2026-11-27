# Cartographie des Composants — Masrouf Android Natif

Ce dossier recense **l'intégralité des composants logiciels et visuels** présents dans `app/src/main/java` :

- **[COMPOSANTS_REUTILISABLES.md](COMPOSANTS_REUTILISABLES.md)** : Custom views Canvas et Material (`BudgetCardView`, `CategoryProgressCardView`, `AlertBannerView`, `ChildrenBudgetAnalyticsView`, `PillBarChart`, `PillBarChartView`, `PriceChartView`), tiroirs latéraux (`AppDrawer`, `AdminDrawer`), fragments/dialogs vocaux et catégorie, classes abstraites de base, les **12 `RecyclerView.Adapter`** et les listeners/watchers réels.
- **[ACTIVITIES_ET_FRAGMENTS.md](ACTIVITIES_ET_FRAGMENTS.md)** : Fiche technique des **30 `<activity>`** déclarées dans `AndroidManifest.xml`, des **3 classes abstraites d'Activity** (`BaseFeatureActivity`, `SectionActivity`, `TransactionDetailActivity`) et des **5 Fragments centraux** (+ `DashboardSectionFragment` et 2 `DialogFragment`).

---

## Volumétrie exacte vérifiée

| Catégorie | Nombre vérifié | Détail clé |
|---|---|---|
| `<activity>` dans `AndroidManifest.xml` | **30** | Inclut 4 redirecteurs rétrocompatibles (`ProductsActivity`, `CalendarActivity`, `AnalyticsActivity`, `SettingsActivity`) et `UserManualActivity` |
| Classes abstraites d'`Activity` | **3** | `BaseFeatureActivity`, `SectionActivity`, `TransactionDetailActivity` |
| `Fragment` onglets centraux (+ base) | **6** | `DashboardSectionFragment` (abstrait), `HomeFragment`, `ProductsFragment`, `CalendarFragment`, `AnalyticsFragment`, `SettingsFragment` |
| `DialogFragment` & `BottomSheetDialogFragment` | **3** | `CustomCategoryDialogFragment`, `VoiceTransactionDialogFragment`, `PhotoSourceBottomSheetDialog` |
| `RecyclerView.Adapter` | **12** | **9 actifs** (`SlideAdapter`, `CategorySelectionAdapter`, `ManageCategoriesAdapter`, `ProductCatalogAdapter`, `PurchaseHistoryAdapter`, `PriceHistoryAdapter`, `NotificationAdapter`, `MoneyRequestAdapter`, `ChildAdapter`) + **3 historiques inutilisés** (`HomeCategoryAdapter`, `CategoryAdapter`, `RecentTransactionsAdapter`) |
| Vues personnalisées (`ui/` + graphiques Canvas) | **7** | `BudgetCardView`, `CategoryProgressCardView`, `AlertBannerView`, `ChildrenBudgetAnalyticsView`, `PillBarChart`, `PillBarChartView`, `PriceChartView` |
| Composants d'arrière-plan | **3** | `BudgetEvaluationService` (`Service`), `BudgetRolloverWorker` (`Worker`), `BudgetAllocationReceiver` (`BroadcastReceiver`) |
