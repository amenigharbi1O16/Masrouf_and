// ============================================================================
// FICHIER : app/build.gradle.kts
// MISSION : Script de configuration Gradle du module applicatif Android (:app).
// IMPLÉMENTE : Plugin Android Application, SDK min/compile/target, ViewBinding, encodage UTF-8 et dépendances.
// EFFET À L'ÉCRAN : Aucun rendu direct ; produit l'APK Java 11 embarquant Material 3, Room, WorkManager et ViewPager2.
// COMMUNICATIONS : Consomme les versions et alias définis dans gradle/libs.versions.toml.
// ============================================================================
import org.gradle.api.tasks.compile.JavaCompile

plugins {
    // Active le plugin applicatif Android (AGP 9.4.1) via le catalogue de versions.
    alias(libs.plugins.android.application)
}

android {
    // Espace de noms Java générant la classe de ressources R et BuildConfig.
    namespace = "com.example.masrouf1"
    compileSdk {
        // Compile avec la plateforme Android SDK 37.
        version = release(37)
    }

    defaultConfig {
        // Identifiant unique de l'application sur le téléphone.
        applicationId = "com.example.masrouf1"
        // Niveau d'API minimum supporté (Android 7.0 Nougat).
        minSdk = 24
        // Niveau d'API cible pour les comportements runtime Android.
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // Lanceur JUnit utilisé pour app/src/androidTest (RoomSelectionInstrumentedTest, PasswordVisibilityInstrumentedTest).
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                // Désactive la minification/R8 en release pour simplifier le diagnostic académique.
                enable = false
            }
        }
    }
    buildFeatures {
        // Active la génération de classes de liaison typées XML (ex. ActivityLoginBinding dans LoginActivity).
        viewBinding = true
    }
    compileOptions {
        // Compile et cible Java 11 (aucun code source Kotlin dans ce module).
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Garantit l'encodage UTF-8 sur toutes les tâches javac afin de préserver accents français et caractères arabes.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

dependencies {
    // API Activity Result (permissions, photos, retour système) — dépendance directe appelée en Java.
    implementation(libs.activity.ktx)
    // Rétrocompatibilité AppCompatActivity, AlertDialog et locales dynamiques.
    implementation(libs.appcompat)
    // Conteneur ConstraintLayout utilisé notamment par activity_section.xml.
    implementation(libs.constraintlayout)
    // Composants visuels Material Design 3 et thèmes DayNight.
    implementation(libs.material)
    // Persistance SQLite locale via AndroidX Room 2.6.1.
    implementation(libs.room.runtime)
    // Processeur d'annotations générant MasroufDatabase_Impl et MasroufDao_Impl à la compilation.
    annotationProcessor(libs.room.compiler)
    // Ordonnanceur hors ligne WorkManager pour BudgetRolloverWorker.
    implementation(libs.work.runtime)
    // Utilitaires AndroidX Core (FileProvider, NotificationCompat, ContextCompat).
    implementation(libs.core)
    // Carrousel horizontal ViewPager2 utilisé dans WelcomeActivity.
    implementation(libs.viewpager2)
    // Listes recyclées performantes et ViewHolders pour les 12 adapters.
    implementation(libs.recyclerview)
    // Tests unitaires locaux exécutés sur JVM hôte.
    testImplementation(libs.junit)
    // Assertions UI et tests instrumentés sur émulateur/téléphone.
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
