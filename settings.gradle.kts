// ============================================================================
// FICHIER : settings.gradle.kts
// MISSION : Paramétrage global de l'espace Gradle : dépôts Maven/Google autorisés et liste des modules du projet.
// COMMUNICATIONS : Résout les plugins Gradle et artefacts Android/Material avant la lecture des fichiers build.gradle.kts.
// ============================================================================
pluginManagement {
    repositories {
        // Restreint les résolutions Android/Google/AndroidX au dépôt Google officiel.
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // Dépôt central pour JUnit et bibliothèques Java tierces/transitives.
        mavenCentral()
        // Portail officiel des plugins Gradle.
        gradlePluginPortal()
    }
}
plugins {
    // Résolveur de chaînes d'outils JDK Java (Java 11).
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    // Impose la déclaration centralisée des dépôts uniquement dans settings.gradle.kts.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

// Nom racine du projet Gradle et inclusion du module Android :app.
rootProject.name = "Masrouf1"
include(":app")
 