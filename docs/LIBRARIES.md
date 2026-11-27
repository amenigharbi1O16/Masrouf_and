# Bibliothèques et Technologies Utilisées — Masrouf Android Natif

> **Référence absolue** : Dépendances vérifiées directement dans [gradle/libs.versions.toml](../gradle/libs.versions.toml), [app/build.gradle.kts](../app/build.gradle.kts) et la résolution Gradle réelle (`:app:dependencies --configuration debugRuntimeClasspath --offline`).  
> Aucune bibliothèque non utilisée dans le code source n'est présentée comme active.

---

## 1. Distinction : Dépendances Directes vs Dépendances Indirectes (Transitives) vs SDK Android

### 1.1 Dépendances directes déclarées dans `app/build.gradle.kts`

| Bibliothèque | Version déclarée (→ résolue) | Configuration Gradle | Mission principale dans Masrouf |
|---|---|---|---|
| **Android Gradle Plugin (`com.android.application`)** | `9.4.1` | `plugins { alias(libs.plugins.android.application) }` | Compilation Java 11, packaging APK, ViewBinding (`9.4.1`), AAPT2 sur ressources et qualifiers (`compileSdk = 37`, `targetSdk = 37`, `minSdk = 24`). |
| **Material Components (`com.google.android.material:material`)** | `1.14.0` | `implementation(libs.material)` | Composants Material 3 (`MaterialCardView`, `MaterialButton`, `TextInputLayout`, `TextInputEditText`, `FloatingActionButton`, `Chip`, `ChipGroup`, `BottomSheetDialogFragment`, `MaterialToolbar`) et thème `Theme.Material3.DayNight.NoActionBar`. |
| **AndroidX AppCompat (`androidx.appcompat:appcompat`)** | `1.8.0` | `implementation(libs.appcompat)` | Rétrocompatibilité `AppCompatActivity`, fenêtres `AlertDialog` et changement de langue à chaud (`AppCompatDelegate.setApplicationLocales`). |
| **AndroidX Activity (`androidx.activity:activity-ktx`)** | `1.13.0` | `implementation(libs.activity.ktx)` | Gestion moderne des permissions runtime (`RequestPermission`), contrats photo/résultats (`StartActivityForResult`, `TakePicture`, `GetContent`) et interception retour (`OnBackPressedCallback`). *(API appelées en Java uniquement).* |
| **AndroidX ConstraintLayout (`androidx.constraintlayout:constraintlayout`)** | `2.2.2` | `implementation(libs.constraintlayout)` | Disposition par contraintes plates de l'enveloppe réutilisable `activity_section.xml` (utilisée par `BaseFeatureActivity` et `SectionActivity`). |
| **AndroidX Room Runtime & Compiler (`androidx.room:room-runtime`, `room-compiler`)** | `2.6.1` | `implementation(libs.room.runtime)` + `annotationProcessor(libs.room.compiler)` | Persistance SQLite locale (`masrouf.db`, 18 `@Entity`, `version = 11`, migrations `1 -> 11`), génération de `MasroufDatabase_Impl` / `MasroufDao_Impl`, `@Transaction` et `LiveData`. |
| **AndroidX WorkManager (`androidx.work:work-runtime`)** | `2.9.0` | `implementation(libs.work.runtime)` | Planification quotidienne fiable et hors ligne de `BudgetRolloverWorker` (`PeriodicWorkRequest`, 1 jour) depuis `MasroufApplication`. |
| **AndroidX Core (`androidx.core:core`)** | `1.15.0` → résolue `1.18.0` | `implementation(libs.core)` | Notifications compat (`NotificationCompat`, `NotificationManagerCompat`), `ContextCompat.checkSelfPermission`, `FileProvider` (manuel PDF & photos), `LocaleListCompat`. |
| **AndroidX ViewPager2 (`androidx.viewpager2:viewpager2`)** | `1.1.0` | `implementation(libs.viewpager2)` | Carrousel horizontal 3 slides (compatible RTL) dans `WelcomeActivity` via `SlideAdapter` et `OnPageChangeCallback`. |
| **AndroidX RecyclerView (`androidx.recyclerview:recyclerview`)** | `1.4.0` | `implementation(libs.recyclerview)` | Listes recyclées performantes : 12 implémentations d'adaptateurs (9 actives + 3 historiques). |
| **ViewBinding (`androidx.databinding:viewbinding`)** | `9.4.1` | `buildFeatures { viewBinding = true }` | Génération de classes de liaison typées (ex. `ActivityLoginBinding` utilisée dans `LoginActivity`). |
| **JUnit (`junit:junit`)** | `4.13.2` | `testImplementation(libs.junit)` | Tests unitaires JVM locaux (`MoneyUtilsTest`, `BudgetPeriodMathTest`, `InputValidationTest`, `VoiceParserTest`, `ExampleUnitTest`). |
| **AndroidX Test Ext JUnit & Espresso (`ext:junit`, `espresso-core`)** | `1.3.0` / `3.7.0` | `androidTestImplementation(...)` | Tests instrumentés sur émulateur/téléphone (`RoomSelectionInstrumentedTest`, `PasswordVisibilityInstrumentedTest`, `ExampleInstrumentedTest`). |

### 1.2 Dépendances indirectes (transitives) résolues par Gradle

Vérifiées via `./gradlew :app:dependencies --configuration debugRuntimeClasspath --offline` :

| Module transitif | Version résolue | Apporté par | Utilisation réelle dans Masrouf |
|---|---|---|---|
| `androidx.fragment:fragment` | `1.5.4` | `appcompat`, `viewpager2` | `Fragment`, `FragmentManager` (`add`/`hide`/`show`), `DialogFragment` (`VoiceTransactionDialogFragment`, `CustomCategoryDialogFragment`), `FragmentContainerView`. |
| `androidx.lifecycle:lifecycle-*` (`viewmodel`, `livedata`, `livedata-core`, `runtime`, `process`, `service`, `common`, `viewmodel-savedstate`) | `2.6.2` | `appcompat`, `activity-ktx`, `room-runtime`, `work-runtime` | Architecture MVVM : `MasroufViewModel`, `MediatorLiveData`, `MutableLiveData`, `ViewModelProvider`, observation réactive dans les écrans. |
| `androidx.drawerlayout:drawerlayout` | `1.1.1` | `material`, `appcompat` | `DrawerLayout` dans `activity_dashboard_host.xml` pour `AppDrawer` (`USER`/`CHILD`) et `AdminDrawer` (`PARENT`). |
| `androidx.coordinatorlayout:coordinatorlayout` | `1.1.0` | `material` | Conteneur Material parent des barres/feuilles modales (`BottomSheetDialogFragment`). |
| `androidx.sqlite:sqlite` & `sqlite-framework` | `2.4.0` | `room-runtime` | Moteur SQLite bas niveau sous Room (`SupportSQLiteDatabase`). |
| `org.jetbrains.kotlin:kotlin-stdlib` | `2.2.10` | `activity-ktx` / AndroidX / AGP | Présent uniquement en dépendance transitive runtime : **0 fichier `.kt` écrit dans le projet**. |

### 1.3 API natives intégrées au SDK Android & Java 11 (sans dépendance externe)

- **Graphiques 2D natifs (`android.graphics.Canvas`, `Paint`, `Path`, `RectF`)** : dessinent entièrement en Java `PriceChartView`, `PillBarChart`, `PillBarChartView` et `ChildrenBudgetAnalyticsView`.
- **Reconnaissance vocale (`android.speech.SpeechRecognizer`, `RecognizerIntent`)** : saisie vocale FR/AR/EN/Darija dans `VoiceSpeechController`, `VoiceFieldHelper`, `VoiceTransactionDialogFragment`.
- **Planification alarme & Service (`AlarmManager`, `Service.startForeground`)** : clôture mensuelle dans `BudgetEvaluationService` (`dataSync`).
- **Média & Images (`android.media.MediaPlayer`, `android.graphics.BitmapFactory`)** : lecture de `res/raw/achievement.mp3` (`AchievementPlayer`) et affichage des photos locales produits.
- **Sécurité Java (`javax.crypto.SecretKeyFactory` / `PBEKeySpec`, `java.security.SecureRandom`)** : hachage PBKDF2-HMAC-SHA256/SHA1 à **120 000 itérations** dans `PasswordHasher`.
- **Arithmétique exacte (`java.math.BigDecimal`, `BigInteger`)** : calculs en millimes et pourcentages sans flottants (`MoneyMath`, `MoneyUtils`, `BudgetPeriodMath`).

---

## 2. Fonctionnement et communication entre bibliothèques

- **UI ↔ Données réactives** : `Material Components` + `RecyclerView` + `ViewPager2` capturent les événements utilisateur → les contrôleurs appellent `MasroufViewModel` (`Lifecycle`) → `Executors.newSingleThreadExecutor()` exécute `MasroufRepository` → `Room` (`MasroufDao` sur `SQLite`) retourne des `LiveData` ou DTO encapsulés dans `DataState<T>` → `Handler(Looper.getMainLooper())` remet à jour les vues sur le thread UI.
- **Tâches différées & Notifications** : `MasroufApplication` enregistre le job `WorkManager` (`BudgetRolloverWorker`) et l'alarme `AlarmManager` (`BudgetEvaluationService`) ; les deux appellent directement `MasroufDatabase.getInstance().masroufDao().rolloverBudget(...)` et émettent des alertes via `NotificationCompat` (`AndroidX Core`).

---

## 3. Exemples de code réels et commentés

### Exemple 1 — Room + LiveData + Exécuteur ([MasroufViewModel.java#L108](../app/src/main/java/com/example/masrouf1/MasroufViewModel.java#L108))
```java
// Observe la table SQLite products en temps réel via Room LiveData encapsulée dans MediatorLiveData :
public void loadProducts(long userId) {
    products.setValue(DataState.loading());
    if (productSource != null) products.removeSource(productSource);
    productSource = repository.observeProducts(userId);
    products.addSource(productSource, rows -> products.setValue(DataState.success(rows)));
}
```

### Exemple 2 — MaterialButton + AndroidX AppCompat LocaleController ([SettingsFragment.java#L555-L565](../app/src/main/java/com/example/masrouf1/SettingsFragment.java#L555-L565))
```java
MaterialButton btn = new MaterialButton(requireContext());
btn.setCornerRadius(dp(14));
btn.setBackgroundTintList(ColorStateList.valueOf(
        requireContext().getColor(selected ? R.color.forest : R.color.mint_light)));
// Délègue à AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
btn.setOnClickListener(v -> LocaleController.apply(tag));
```

### Exemple 3 — FileProvider + Implicit Intent (`AndroidX Core`, [SystemIntents.java#L74-L76](../app/src/main/java/com/example/masrouf1/SystemIntents.java#L74-L76))
```java
Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", target);
Intent view = new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf");
view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
launch(activity, Intent.createChooser(view, activity.getString(R.string.open_manual)));
```

### Exemple 4 — WorkManager (`WorkRequest`, [MasroufApplication.java#L49-L50](../app/src/main/java/com/example/masrouf1/MasroufApplication.java#L49-L50))
```java
PeriodicWorkRequest rollover = new PeriodicWorkRequest.Builder(BudgetRolloverWorker.class, 1, TimeUnit.DAYS).build();
WorkManager.getInstance(this).enqueueUniquePeriodicWork("masrouf-budget-rollover", ExistingPeriodicWorkPolicy.KEEP, rollover);
```

---

## 4. Bibliothèques explicitement absentes

Pour une transparence totale : le projet **n'utilise pas** Kotlin source (0 fichier `.kt`), Jetpack Navigation, `MPAndroidChart`, `Glide`/`Picasso`/`Coil`, `Lottie`, `Retrofit`/`OkHttp`/`Gson`, `Hilt`/`Dagger`, ni Firebase.
