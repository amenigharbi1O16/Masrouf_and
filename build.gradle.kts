// ============================================================================
// FICHIER : build.gradle.kts (racine)
// MISSION : Déclaration globale des plugins Gradle partagés à l'échelle du projet multi-modules.
// IMPLÉMENTE : Enregistrement différé (apply false) du plugin Android Application issu du Version Catalog.
// COMMUNICATIONS : Charge les versions depuis gradle/libs.versions.toml avant d'être appliqué dans app/build.gradle.kts.
// ============================================================================
plugins {
    // Rend le plugin AGP disponible pour les sous-modules sans l'appliquer directement à la racine.
    alias(libs.plugins.android.application) apply false
}