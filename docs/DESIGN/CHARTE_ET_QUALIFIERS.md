# Charte Graphique & Qualifiers Android — Masrouf Android Natif

---

## 1. Palette botanique et surfaces chaudes ([colors.xml](../app/src/main/res/values/colors.xml))

Aucun composant n'utilise de violet Material par défaut ; toutes les teintes sont définies dans `app/src/main/res/values/colors.xml` :

| Ressource `@color/` | Code Hex | Usage principal |
|---|---|---|
| `forest` | `#112B28` | Titres principaux, boutons primaires sombres, fond du tiroir `PARENT`, barre de sélection langue active |
| `emerald` | `#2E7D43` | Actions positives (`Enregistrer`, `Confirmer`), jauges saines (< 80 %), coches et badges `AppDrawer` |
| `mint` / `mint_light` | `#D9EEDD` | Fond d'éléments sélectionnés, cercles icônes doux, pilules inactives |
| `mint_accent` | `#85B8A5` | Bordure active du tiroir Parent, accents secondaires |
| `cream` | `#F7F4EC` | Arrière-plan général des écrans (remplace le blanc froid) |
| `form_beige` | `#FAF6EC` | Fond des champs de saisie, Spinners et chips non sélectionnées |
| `form_surface` | `#FCFBF7` | Surface secondaire des dialogues modaux |
| `alabaster` | `#FFFFFF` | Surface des cartes `MaterialCardView` |
| `card_border` | `#DDD7C8` | Contour `1dp` des cartes |
| `input_border` | `#C7C0AE` | Contour `1dp` des champs de formulaire |
| `amber` | `#E9B66B` | Alerte modérée / attention |
| `coral` | `#DC7770` | Dépassement budgétaire (> 100 %), hausse de prix > 10 %, erreurs |
| `muted_green` | `#3B4D45` | Textes secondaires, sous-titres et libellés désactivés |
| `ink` / `ink_secondary` | `#FF000000` / `#4A554E` | Texte principal contrasté noir pur et texte descriptif |
| `success_surface` / `warning_surface` / `danger_surface` | `#EBF5EA` / `#FDF6E2` / `#FCEAE9` | Fonds sémantiques de `AlertBannerView` (associés à `warning_text` `#9C6A16`, `danger_text` `#C0392B`) |

---

## 2. Thèmes & Styles Material 3 ([themes.xml](../app/src/main/res/values/themes.xml), [styles.xml](../app/src/main/res/values/styles.xml))

- **`Base.Theme.Masrouf1`** hérite de `Theme.Material3.DayNight.NoActionBar` :
  - `colorPrimary = @color/forest`, `colorSecondary = @color/emerald`, `android:windowBackground = @color/cream`, `android:statusBarColor = @color/cream` (`windowLightStatusBar = true`).
  - `alertDialogTheme` pointe vers `ThemeOverlay.Masrouf.AlertDialog` (boutons positifs `@color/emerald`, fond beige).
- **`values-night/themes.xml`** : redéfinit le thème en mode nuit tout en préservant l'identité visuelle botanique (pas de fichier `values-night/colors.xml` séparé).
- **`styles.xml`** : définit les styles réutilisables des champs (`MasroufTextInput`), boutons (`MasroufPrimaryButton`, `MasroufEmeraldButton`, `MasroufOutlinedButton`) et cartes (`MasroufCard`).

---

## 3. Qualifiers de ressources réellement présents (`app/src/main/res/`)

| Type de qualifier | Dossiers présents | Rôle automatique Android | Vérifié ? |
|---|---|---|---|
| **Langue par défaut (Français)** | `values/` (`867` chaînes, `2` pluriels) | Chargée par défaut si la langue du système est le français ou non reconnue | **Oui** (**pas de dossier `values-fr/`**) |
| **Anglais** | `values-en/` (`867` chaînes, `2` pluriels) | Sélectionnée quand `"en"` est appliqué via `LocaleController.apply("en")` ou le système | **Oui** |
| **Arabe + RTL** | `values-ar/` (`867` chaînes, `2` pluriels) | Sélectionnée quand `"ar"` est actif ; active le miroir droite-à-gauche grâce à `android:supportsRtl="true"` et `android:layoutDirection="locale"` | **Oui** |
| **Config locales Android 13+** | `xml/locales_config.xml` | Déclare `fr`, `ar`, `en` dans le manifeste (`android:localeConfig`) | **Oui** |
| **Mode Nuit** | `values-night/themes.xml` | Appliqué quand le thème système sombre est actif | **Oui** |
| **Largeur d'écran minimale (`smallestWidth`)** | `values/dimens.xml`, `values-sw360dp/`, `values-sw480dp/`, `values-sw600dp/` | Adapte automatiquement les marges horizontales, tailles de police titres et rayons sur petits téléphones, grands smartphones et tablettes 7"+ | **Oui** |
| **Densités raster (logo feuille & lanceur)** | `drawable-{mdpi,hdpi,xhdpi,xxhdpi}/ic_masrouf_leaf.png` et `mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi,anydpi-v26}/` | Android sélectionne l'image bitmap ou icône adaptative correspondant au DPI de l'appareil (`1x`, `1.5x`, `2x`, `3x`, `4x`) | **Oui** |
| **Ressources vectorielles indépendantes DPI** | `drawable/` (**71** fichiers XML : 45 `ic_*.xml` + 26 shapes/ripples) | Redimensionnement net sans perte sur toute résolution | **Oui** |
| **Menus XML & Landscape** | `menu/` (`0` fichier), pas de `values-land/` | Les menus latéraux et barres sont générés en Java (`AppDrawer`, `AdminDrawer`) ou dans `activity_dashboard_host.xml` | **Constat d'audit** |
