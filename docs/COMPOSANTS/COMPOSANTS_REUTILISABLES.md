# Composants Réutilisables, Graphiques Canvas, Drawers, Adapters & Listeners

Ce document centralise la documentation des briques réutilisables pour ne pas répéter leur fonctionnement dans chaque fiche d'écran.

---

## 1. Custom Views Material & Canvas 2D (`app/src/main/java/com/example/masrouf1/`)

| Composant | Hérite de / Tech | Méthode clé & comportement | Utilisé dans |
|---|---|---|---|
| [`BudgetCardView`](../../app/src/main/java/com/example/masrouf1/ui/BudgetCardView.java) | `MaterialCardView` | `bind(...)` affiche le budget actif, le total dépensé, le reste, l'enveloppe enfants (Parent), les dates formatées et la jauge de couleur (vert / ambre / corail). | `HomeFragment` |
| [`CategoryProgressCardView`](../../app/src/main/java/com/example/masrouf1/ui/CategoryProgressCardView.java) | `MaterialCardView` | `bind(...)` associe icône vectorielle (`CategoryIconResources`), nom localisé, montant dépensé / plafond et barre de consommation. | `HomeFragment`, `HomeCategoryAdapter` |
| [`AlertBannerView`](../../app/src/main/java/com/example/masrouf1/ui/AlertBannerView.java) | `MaterialCardView` | `setAlert(Tone, title, detail)` applique l'un des 3 styles `SUCCESS` (`#EBF5EA`), `WARNING` (`#FDF6E2`) ou `DANGER` (`#FCEAE9`). | `AddExpenseActivity`, `AddProductActivity`, `BudgetPeriodActivity`, `AllocateBudgetActivity`, `ManageCategoriesActivity`, `SettingsFragment`, `VoiceFieldHelper`, `VoiceTransactionDialogFragment`, `CustomCategoryDialogFragment` |
| [`ChildrenBudgetAnalyticsView`](../../app/src/main/java/com/example/masrouf1/ChildrenBudgetAnalyticsView.java) | `View` (`Canvas`) | Trace en 2D natif les barres comparatives (alloué vs dépensé) des enfants du foyer `PARENT`. | `FamilyBudgetsAnalyticsActivity`, `AnalyticsRenderer` |
| [`PillBarChart`](../../app/src/main/java/com/example/masrouf1/PillBarChart.java) | `View` (`Canvas`) | Dessine 6 pilules verticales arrondies sur les 6 derniers mois (`setBarItems`), vert `forest` ou `coral` en cas de dépassement. | `AnalyticsRenderer` |
| [`PillBarChartView`](../../app/src/main/java/com/example/masrouf1/PillBarChartView.java) | `View` (`Canvas`) | Variante comparative mensuelle avec étiquettes de montants pour la vue familiale. | `FamilyBudgetsAnalyticsActivity`, `AnalyticsRenderer` |
| [`PriceChartView`](../../app/src/main/java/com/example/masrouf1/PriceChartView.java) | `View` (`Canvas`) | Courbe chronologique d'évolution des prix unitaires d'un produit à partir de `price_history`. | `ProductDetailsActivity` |

---

## 2. Tiroirs latéraux (`AppDrawer` & `AdminDrawer`)

- **[`AppDrawer`](../../app/src/main/java/com/example/masrouf1/AppDrawer.java)** (`USER` et `CHILD`) : construit en Java 7 lignes (`home`, `products`, `calendar`, `analytics`, `rewards`, `notifications`, `settings`) avec `LinearLayout` + `ImageView` + `TextView` + badge vert émeraude, met en évidence la ligne active (`mint`), et interroge `MasroufViewModel.request(repo -> repo.unreadNotificationCount(userId))` dans `refreshBadges(...)`.
- **[`AdminDrawer`](../../app/src/main/java/com/example/masrouf1/AdminDrawer.java)** (`PARENT` uniquement, malgré son nom historique) : fond sombre `forest`, icônes `mint`, texte blanc `alabaster`, 7 destinations (`home`, `children`, `products`, `calendar`, `analytics`, `notifications`, `settings`) + 2 badges dynamiques (`children` et `unreadNotifications` via `parentDrawerBadges(userId)`).

---

## 3. Modules vocaux & Modales partagées (`com.masrouf.controller.*`)

- **[`VoiceSpeechController`](../../app/src/main/java/com/masrouf/controller/VoiceSpeechController.java)** : gère la permission runtime `Manifest.permission.RECORD_AUDIO`, instancie `android.speech.SpeechRecognizer` (détection multi-langue API 33+ ou repli script/lexical), et bascule vers `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` si besoin. États : `IDLE`, `LISTENING`, `PROCESSING`, `RESULT`, `CANCELLED`, `ERROR_PERMISSION`, `ERROR`.
- **[`VoiceParser`](../../app/src/main/java/com/masrouf/controller/VoiceParser.java)** : parseur déterministe hors ligne supportant français, arabe, darija/arabizi (`3`→`ع`, `7`→`ح`, `5`→`خ`, `9`→`ق`) et anglais ; extrait `amountMilli`, `operationType` (`purchase` / `expense` / `child_allowance`), catégorie, quantité/unité, magasin et enfant sans jamais traduire le libellé.
- **[`VoiceFieldHelper`](../../app/src/main/java/com/masrouf/controller/VoiceFieldHelper.java)** : relie n'importe quel bouton micro inline `ImageButton` à un `EditText` cible (insertion au curseur ou extraction numérique si champ chiffré).
- **[`VoiceTransactionDialogFragment`](../../app/src/main/java/com/masrouf/controller/VoiceTransactionDialogFragment.java)** : modale flottante globale accessible depuis le FAB micro de `MainActivity`, avec aperçu entièrement éditable et bouton explicite de confirmation.
- **[`CustomCategoryDialogFragment`](../../app/src/main/java/com/masrouf/controller/CustomCategoryDialogFragment.java)** : ajout rapide d'une catégorie catalogue non active ou personnalisée avec palette de 8 couleurs botaniques et jauge `%`.
- **[`PhotoSourceBottomSheetDialog`](../../app/src/main/java/com/masrouf/controller/PhotoSourceBottomSheetDialog.java)** : feuille modale Android choisissant Caméra (`TakePicture` + `FileProvider`) ou Galerie (`GetContent("image/*")`), renvoyant le chemin local via `Listener.onPhotoSelected(String path)`.

---

## 4. Classes abstraites de base & Helper UI

- **[`DashboardSectionFragment`](../../app/src/main/java/com/example/masrouf1/DashboardSectionFragment.java)** : génère en Java la `ScrollView` crème + barre supérieure (`☰`, titre, cloche badge rouge, raccourci profil) pour `ProductsFragment`, `CalendarFragment`, `AnalyticsFragment`.
- **[`BaseFeatureActivity`](../../app/src/main/java/com/masrouf/controller/BaseFeatureActivity.java)** : enveloppe `activity_section.xml`, gère retour toolbar, titre et nettoyage `voiceFieldHelper` dans `onDestroy()`.
- **[`SectionActivity`](../../app/src/main/java/com/example/masrouf1/SectionActivity.java)** : applique le contrôle d'accès par rôle (`requiresParentRole()`, `disallowParentRole()`) sur les sous-écrans dédiés.
- **[`PasswordVisibilityHelper`](../../app/src/main/java/com/masrouf/controller/PasswordVisibilityHelper.java)** : synchronise les icônes `ic_eye_closed` / `ic_eye_open` sans perdre ni le texte ni la position du curseur.

---

## 5. Les 12 `RecyclerView.Adapter` et les Listeners dédiés

- **Actifs (9)** : `SlideAdapter` (interne `WelcomeActivity`), `CategorySelectionAdapter` (interne `OnboardingActivity`), `ManageCategoriesAdapter` (interne `ManageCategoriesActivity`), `ChildAdapter` (interne `ChildrenManagementActivity`), `PriceHistoryAdapter` (interne `ProductPriceHistoryActivity`), `ProductCatalogAdapter` (catalogue produits), `PurchaseHistoryAdapter` (historique achats fiche produit), `NotificationAdapter` (notifications Room + bouton lecture), `MoneyRequestAdapter` (liste demandes enfant).
- **Historiques non utilisés par l'UI actuelle (3)** : `HomeCategoryAdapter`, `CategoryAdapter`, `RecentTransactionsAdapter`.
- **Listeners & Watchers dédiés** : `LoginClickListener` (actif dans `LoginActivity`), `FormValidationTextWatcher` (formulaires), `FormPriceTextWatcher` (prix unitaire temps réel `AddProductActivity`), `GmailValidationWatcher` (contrôle Gmail en direct `LoginActivity` / `SettingsFragment`), ainsi que `SafeClickListener` et `OnCategorySelectedListener` (présents sans appelant actif).
