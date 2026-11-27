# Vérification des Exigences Officielles de l'Enseignant — Android Natif

Ce document répond point par point aux exigences pédagogiques d'Android natif en fournissant exclusivement des preuves vérifiées dans le code source de `Masrouf1`.

---

## Tableau de synthèse

| # | Exigence du professeur | Statut réel | Preuve dans le code |
|---|---|---|---|
| 1 | **Au moins 2 types d'utilisateurs (dont un Admin et un autre utilisateur)** | **Partiel (3 rôles réels, Admin absent)** | `AppRole` (`USER`, `PARENT`, `CHILD`) — `PARENT` gère son foyer uniquement, aucun super-admin global ([AppRole.java](../app/src/main/java/com/example/masrouf1/AppRole.java)) |
| 2 | **Activities avec interfaces XML et vues créées en Java** | **Conforme** | 50 layouts XML + construction Java pure dans `DashboardSectionFragment`, `AnalyticsRenderer`, `AppDrawer`, `AdminDrawer`, `BudgetPeriodActivity` |
| 3 | **Écouteurs (Listeners) explicites et implicites** | **Conforme** | `View.OnClickListener`, `TextWatcher`, `OnItemSelectedListener`, `OnCheckedStateChangeListener`, `OnPageChangeCallback`, classes dédiées `LoginClickListener`, `FormPriceTextWatcher`, `GmailValidationWatcher` |
| 4 | **Intents explicites et implicites** | **Conforme** | Intents explicites avec extras typés (`USER_ID`, `EXTRA_CENTRAL_TAB`, `PRODUCT_ID`, `BUDGET_ID`, `TRANSACTION_ID`) + `SystemIntents` (`ACTION_DIAL`, `ACTION_SENDTO`, `ACTION_VIEW` + `FileProvider`, `ACTION_SEND`, `MediaStore.ACTION_IMAGE_CAPTURE`, `RecognizerIntent`) |
| 5 | **Ressources avec Qualifiers** | **Partiel ciblé** | `values/` (FR défaut), `values-en/`, `values-ar/` (RTL), `values-night/`, `values-sw{360,480,600}dp/`, `mipmap-{mdpi..xxxhdpi,anydpi-v26}/`, `drawable-{mdpi,hdpi,xhdpi,xxhdpi}/ic_masrouf_leaf.png` ; **absence** de `values-fr/` et de `values-land/` |
| 6 | **Notifications locales** | **Conforme** | `BudgetNotificationManager` (`NotificationChannel` `"budget_channel"`, `NotificationCompat`, `VISIBILITY_PRIVATE`, `PendingIntent`, `POST_NOTIFICATIONS` API 33+, table `notifications`) |
| 7 | **Services / tâches en arrière-plan** | **Conforme** | `BudgetEvaluationService` (Foreground `dataSync` via `AlarmManager`), `BudgetRolloverWorker` (WorkManager quotidien), `BudgetAllocationReceiver` (déclaré dans Manifeste, non émis côté Repository) |
| 8 | **Architecture propre / persistance SQLite** | **Conforme** | Architecture hybride MVC/MVVM + `MasroufRepository` + `MasroufDao` + Room SQLite `masrouf.db` v11 (18 tables) |

---

## Détail des preuves et localisation exacte

### 1. Plusieurs profils et mise au point `Admin`
Voir l'analyse complète et la matrice des permissions dans [PROFILES.md](PROFILES.md).  
Point clé devant le jury : ne jamais présenter `AdminDrawer.java` ou `admin.masrouf@gmail.com` comme un rôle Administrateur système — `AppRole.from("admin")` retourne `AppRole.PARENT`.

### 2. Interfaces XML vs Vues créées en Java
- **XML-based Views (50 layouts dans `app/src/main/res/layout/`)** :
  - Associées via `setContentView(R.layout.*)` (ex. `WelcomeActivity`, `OnboardingActivity`, `BudgetPeriodActivity`), `ActivityLoginBinding.inflate(...)` dans `LoginActivity`, ou `LayoutInflater.from(...).inflate(...)` dans `HomeFragment` et `BaseFeatureActivity`.
  - Styles centralisés dans [styles.xml](../app/src/main/res/values/styles.xml) et [themes.xml](../app/src/main/res/values/themes.xml) ; sélecteurs de couleur dans `res/color/` (`chip_background_color.xml`, `chip_stroke_color.xml`, `masrouf_check_tint.xml`, `text_input_label_color.xml`).
- **Java-based Views (création dynamique par code Java)** :
  - [`DashboardSectionFragment.onCreateView()`](../app/src/main/java/com/example/masrouf1/DashboardSectionFragment.java#L94-L176) : instancie en Java `ScrollView`, `LinearLayout`, bouton menu `☰` (`TextView`), titre, cloche + badge rouge (`FrameLayout` + `ImageView` + `TextView`), icône profil (`ImageView`).
  - [`AnalyticsRenderer`](../app/src/main/java/com/example/masrouf1/AnalyticsRenderer.java) : construit entièrement en Java les cartes `MaterialCardView`, `ProgressBar`, `PillBarChart` et listes d'enveloppes/achats (aucun layout XML d'écran Analytics dédié ; `PillBarChartView` et `ChildrenBudgetAnalyticsView` sont utilisés dans `FamilyBudgetsAnalyticsActivity`).
  - [`AppDrawer`](../app/src/main/java/com/example/masrouf1/AppDrawer.java) & [`AdminDrawer`](../app/src/main/java/com/example/masrouf1/AdminDrawer.java) : construisent chaque ligne de menu latéral (`LinearLayout` + `ImageView` + `TextView` + badge `GradientDrawable`).
  - [`ProductsFragment.createFilterChip()`](../app/src/main/java/com/example/masrouf1/ProductsFragment.java#L252-L265) : génère dynamiquement les puces `Chip` à partir des catégories Room.
  - [`BudgetPeriodActivity`](../app/src/main/java/com/masrouf/controller/BudgetPeriodActivity.java) : génère les lignes/cellules du sélecteur calendaire.
  - **Composants absents dans la création dynamique** : aucune `WebView`, aucun `GridLayout` natif, aucune `TableLayout`.

### 3. Listeners réels utilisés dans le code
| Type de listener | Rôle & déclencheur | Exemple exact vérifié |
|---|---|---|
| **Clic (`View.OnClickListener`)** | Navigation, validation formulaires, tiroir, onglets, bascules | Lambda sur `save`, `home_open_drawer`, `calendar_previous` ; classe dédiée [`LoginClickListener`](../app/src/main/java/com/masrouf/controller/LoginClickListener.java) |
| **Saisie (`android.text.TextWatcher`)** | Validation temps réel et calcul automatique du prix unitaire | [`FormValidationTextWatcher`](../app/src/main/java/com/masrouf/controller/FormValidationTextWatcher.java), [`FormPriceTextWatcher`](../app/src/main/java/com/masrouf/controller/FormPriceTextWatcher.java), [`GmailValidationWatcher`](../app/src/main/java/com/example/masrouf1/GmailValidationWatcher.java), watcher de recherche dans `ProductsFragment` |
| **Sélection Spinner (`AdapterView.OnItemSelectedListener`)** | Choix de catégorie et unité de mesure | [`AddExpenseActivity`](../app/src/main/java/com/masrouf/controller/AddExpenseActivity.java#L117-L131), `AddProductActivity` |
| **Puces & Boutons radio (`ChipGroup.OnCheckedStateChangeListener`, `RadioGroup.OnCheckedChangeListener`)** | Filtrage produits par catégorie, choix catalogue vs saisie manuelle | `ProductsFragment` (`filters`), `CustomCategoryDialogFragment` (`sourceGroup`) |
| **Pagination carrousel (`ViewPager2.OnPageChangeCallback`)** | Mise à jour des points indicateurs des 3 slides | `WelcomeActivity` |
| **Contrats résultat & Retour (`ActivityResultCallback`, `OnBackPressedCallback`)** | Permission micro/notifications, rafraîchissement post-gestion catégories, fermeture tiroir | `VoiceSpeechController`, `HomeFragment`, `MainActivity` |
| **Listeners non connectés (audit)** | Interface et classe utilitaire présentes sans appelant actif | [`SafeClickListener`](../app/src/main/java/com/example/masrouf1/SafeClickListener.java), [`OnCategorySelectedListener`](../app/src/main/java/com/example/masrouf1/OnCategorySelectedListener.java) |

### 4. Explicit Intents & Implicit Intents
Détaillés intégralement dans [NAVIGATION.md](NAVIGATION.md).

### 5. Ressources avec Qualifiers
Détaillées dans [DESIGN/CHARTE_ET_QUALIFIERS.md](DESIGN/CHARTE_ET_QUALIFIERS.md). Points notables vérifiés dans `app/src/main/res/` :
- **Langues** : Français par défaut dans `values/` (aucun dossier `values-fr/`), Anglais dans `values-en/`, Arabe dans `values-ar/` (activant automatiquement le mode RTL via `android:supportsRtl="true"` dans `AndroidManifest.xml` et `LocaleController.apply()`).
- **Thème sombre** : `values-night/themes.xml` et `values-night/colors.xml`.
- **Largeurs minimales** : `values/dimens.xml`, `values-sw360dp/dimens.xml`, `values-sw480dp/dimens.xml`, `values-sw600dp/dimens.xml`.
- **Densités raster** : `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/` et `drawable-{mdpi,hdpi,xhdpi,xxhdpi}/ic_masrouf_leaf.png` (les 71 XML de `drawable/` sont indépendants de la densité).

### 6. Notifications et Services / BroadcastReceiver
- **Canaux et émission** : `BudgetNotificationManager` crée `"budget_channel"` (`IMPORTANCE_HIGH`, vibration), vérifie `Manifest.permission.POST_NOTIFICATIONS` sur API 33+, insère dans la table Room `notifications` (clé unique `eventKey`) et poste via `NotificationCompat` avec `VISIBILITY_PRIVATE` et `PendingIntent` ouvrant `NotificationsActivity` ou `RewardsActivity`.
- **Service au premier plan** : `BudgetEvaluationService` (`foregroundServiceType="dataSync"`, `START_NOT_STICKY`), programmé le 1er de chaque mois à 00h10 par `AlarmManager.setAndAllowWhileIdle(...)`, crée le canal silencieux `"budget_evaluation_service"` (`IMPORTANCE_LOW`) et lance `MasroufDao.rolloverBudget(...)` sur un `ExecutorService` mono-thread.
- **WorkManager** : `BudgetRolloverWorker` planifié toutes les 24 h (`ExistingPeriodicWorkPolicy.KEEP`) dans `MasroufApplication.onCreate()`.
- **BroadcastReceiver** : `BudgetAllocationReceiver` déclaré dans `AndroidManifest.xml` (`exported="false"`) pour `com.masrouf.action.CHILD_ALLOCATION`, mais non déclenché activement via `sendBroadcast()` dans le code actuel.
