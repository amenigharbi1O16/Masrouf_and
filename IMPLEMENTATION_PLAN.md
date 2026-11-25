# PLAN D'IMPLÉMENTATION — MASROUF ANDROID
*Généré le 24 novembre 2026*

---

## ORDRE DE PRIORITÉ

### GROUPE A — Thème et couleurs (Impact global, sans risque)

**A1. Correction du thème Material3 pour éliminer tout violet/mauve**
- Fichier : `app/src/main/res/values/themes.xml`
- Fichier : `app/src/main/res/values-night/themes.xml`
- Ajout des attributs manquants : `colorControlActivated`, `colorControlHighlight`, `textColorPrimary`, `android:textColorHint`, `colorOnSurface`, `colorSurfaceVariant`
- Résultat attendu : inputs, curseurs, labels, checkboxes utilisent les couleurs forest/emerald/mint

---

### GROUPE B — Écrans manquants (Impact fonctionnel)

**B1. Écran Changer mot de passe**
- Créer : `app/src/main/java/com/masrouf/controller/ChangePasswordActivity.java`
- Créer : `app/src/main/res/layout/activity_change_password.xml`
- Enregistrer dans : `AndroidManifest.xml`
- Brancher depuis : `SettingsActivity` (bouton "Modifier le mot de passe")
- Brancher depuis : `SettingsFragment` (via bouton dans AppDrawer)
- Fonctionnalité : saisie ancien mot de passe + nouveau + confirmation, validation, mise à jour DB

**B2. Écran Historique prix produit**
- Créer : `app/src/main/java/com/masrouf/controller/ProductPriceHistoryActivity.java`
- Créer : `app/src/main/res/layout/activity_product_price_history.xml`
- Enregistrer dans : `AndroidManifest.xml`
- Brancher depuis : `ProductDetailsActivity` (bouton "Consulter l'historique")
- Fonctionnalité : liste chronologique de tous les achats du produit avec prix unitaire, date, magasin

**B3. Écran Félicitations**
- Créer : `app/src/main/java/com/masrouf/controller/CongratulationsActivity.java`
- Créer : `app/src/main/res/layout/activity_congratulations.xml`
- Enregistrer dans : `AndroidManifest.xml`
- Brancher depuis : `ConfirmationActivity` ou après un succès de budget

---

### GROUPE C — Corrections des tiroirs (Impact UX)

**C1. Ajout d'icônes dans AppDrawer**
- Modifier : `app/src/main/java/com/example/masrouf1/AppDrawer.java`
- Ajouter tableau d'icônes correspondant à chaque destination
- Utiliser les mêmes icônes que AdminDrawer (ic_home, ic_bag, ic_calendar, etc.)
- Ajouter ic_trophy pour rewards, ic_notifications existant

**C2. Ajout du sélecteur de langue dans AdminDrawer**
- Modifier : `app/src/main/res/layout/drawer_admin.xml`
- Ajouter section langue avec boutons FR/AR/EN similaire au drawer personnel
- Modifier : `app/src/main/java/com/example/masrouf1/AdminDrawer.java`
- Brancher sur `LocaleController.apply(tag)`

---

### GROUPE D — Amélioration des fragments existants (Impact qualité)

**D1. Amélioration SettingsFragment**
- Modifier : `app/src/main/java/com/example/masrouf1/SettingsFragment.java`
- Afficher : nom, email, langue actuelle, bouton Changer mot de passe, bouton Réglages complets
- Design : carte blanche avec informations lisibles au lieu du simple texte

**D2. Amélioration AnalyticsFragment**
- Modifier : `app/src/main/java/com/example/masrouf1/AnalyticsFragment.java`
- Ajouter : graphique par catégorie plus riche avec barres de progression
- Ajouter : sélecteur de période (mois en cours / mois précédent)
- Ajouter : état vide illustré

---

### GROUPE E — Corrections de navigation (Impact fonctionnel)

**E1. Connexion Changer mot de passe depuis SettingsActivity**
- Modifier : `app/src/main/java/com/example/masrouf1/SettingsActivity.java`
- Ajouter bouton "Modifier le mot de passe" → lance `ChangePasswordActivity`

**E2. Connexion Historique prix depuis ProductDetailsActivity**
- Modifier : `app/src/main/java/com/masrouf/controller/ProductDetailsActivity.java`
- Vérifier/corriger le bouton "Historique" → lance `ProductPriceHistoryActivity`

---

### GROUPE F — Corrections de formulaires (Impact qualité)

**F1. Remplacement EditText standalone par TextInputLayout dans LoginActivity**
- Modifier : `app/src/main/res/layout/activity_login.xml`
- Email field : wrapper dans `TextInputLayout` pour cohérence avec password field

**F2. Vérification des placeholders**
- Vérifier tous les `hint` dans les layouts pour s'assurer qu'ils sont neutres et professionnels
- `hint_email` = `nom@gmail.com` ✅ déjà fait (vérifier strings.xml)

---

### GROUPE G — Documentation (Impact maintenance)

**G1. Création EXPLICATIONS.md**
- Créer : `Masrouf1/EXPLICATIONS.md`
- Documenter : architecture, navigation, rôles, DB, composants

---

## DÉPENDANCES ENTRE TÂCHES

```
A1 (thème) → Aucune dépendance
B1 (changer MDP) → A1, E1
B2 (historique prix) → E2
B3 (félicitations) → Aucune
C1 (AppDrawer icônes) → Aucune
C2 (AdminDrawer langue) → Aucune
D1 (Settings fragment) → B1
D2 (Analytics) → Aucune
E1 → B1
E2 → B2
F1, F2 → A1
G1 → Toutes les précédentes
```

---

## ESTIMATION ET ORDRE D'EXÉCUTION

| Phase | Groupe | Effort | Risque |
|---|---|---|---|
| 1 | A1 — Thème | Faible | Faible |
| 2 | C1, C2 — Drawers | Moyen | Faible |
| 3 | B1 — Changer MDP | Moyen | Faible |
| 4 | B2 — Historique prix | Moyen | Faible |
| 5 | B3 — Félicitations | Faible | Faible |
| 6 | D1, D2 — Fragments | Moyen | Faible |
| 7 | E1, E2 — Navigation | Faible | Faible |
| 8 | F1, F2 — Formulaires | Faible | Faible |
| 9 | G1 — Documentation | Faible | Nul |

**Total estimé** : 2 à 3 heures de développement ciblé.

---

## FICHIERS À NE PAS MODIFIER

- `MasroufDatabase.java` — migrations actives, risque de perte de données
- `MasroufEntities.java` — sauf ajout d'entités non destructif
- `MasroufDao.java` — sauf ajout de requêtes
- `SessionStore.java` — logique d'authentification critique
- `BudgetEvaluationService.java` — service en arrière-plan actif
- Tout le projet `react-native-project/masrouf` — référence uniquement
