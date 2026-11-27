# Design Détaillé — Authentification, Onboarding & Accueil

> **Convention** : Les composants réutilisables (`AppDrawer`, `AdminDrawer`, `BudgetCardView`, `CategoryProgressCardView`, `AlertBannerView`, `VoiceTransactionDialogFragment`, `CustomCategoryDialogFragment`) sont documentés dans [../COMPOSANTS/COMPOSANTS_REUTILISABLES.md](../COMPOSANTS/COMPOSANTS_REUTILISABLES.md) afin d'éviter toute redondance.

---

## 1. Parcours d'accueil & authentification

| Écran | Layout XML | Composants visibles & interactions clés |
|---|---|---|
| **`WelcomeActivity`** | [activity_welcome.xml](../../app/src/main/res/layout/activity_welcome.xml), [item_welcome_slide.xml](../../app/src/main/res/layout/item_welcome_slide.xml) | Décor cercles botaniques `bg_circle_forest` / `bg_circle_mint` + rangée langues (`FR`/`AR`/`EN`). **Phase 1** : logo `ic_masrouf_logo`, titre `Masrouf`, bouton `Commencer` -> passe en Phase 2 ; lien `Se connecter` -> `LoginActivity`. **Phase 2** : carrousel `ViewPager2` à 3 diapositives + points indicateurs `bg_dot_active`/`bg_dot_inactive` + bouton `Suivant`/`Continuer` -> `ChooseAccountTypeActivity`. |
| **`ChooseAccountTypeActivity`** | [activity_choose_account_type.xml](../../app/src/main/res/layout/activity_choose_account_type.xml) | Flèche retour, pilules langue construites en Java (`buildLanguageButtons()`), deux cartes cliquables **`personal_card`** (`USER`, `"personal"`) et **`family_card`** (`PARENT`, `"family"`) ouvrant `RegisterActivity` avec l'extra `"TYPE"`, et lien `login`. |
| **`RegisterActivity`** | [activity_register.xml](../../app/src/main/res/layout/activity_register.xml) | Sélecteur visuel 2 onglets (`tab_personal` / `tab_family`), bannière informative famille (`family_info_banner`), champs nom, Gmail normalisé (`InputValidation`), mot de passe + confirmation dotés de bascule œil (`PasswordVisibilityHelper`), panneau dynamique des 5 règles (`rule_length`, `rule_uppercase`, `rule_lowercase`, `rule_number`, `rule_symbol`), bouton `S'inscrire` actif uniquement si tout est valide. |
| **`LoginActivity`** | [activity_login.xml](../../app/src/main/res/layout/activity_login.xml) | Liaison par ViewBinding (`ActivityLoginBinding`), flèche retour, pilules langue (`Français` / `العربية` / `English`), email Gmail + `GmailValidationWatcher`, mot de passe avec icône œil fermée par défaut (`ic_eye_closed`), carte d'erreur rouge/corail, bouton `Se connecter` via `LoginClickListener`. |
| **`OnboardingActivity`** | [activity_onboarding.xml](../../app/src/main/res/layout/activity_onboarding.xml), [item_onboarding_category.xml](../../app/src/main/res/layout/item_onboarding_category.xml), [item_summary_category.xml](../../app/src/main/res/layout/item_summary_category.xml) | Parcours 3 étapes (`01/03` à `03/03`) : (1) présentation + exemples ; (2) `RecyclerView` de sélection de catégories isolées par compte (filtrées pour `CHILD` selon son Parent) avec compteur temps réel ; (3) récapitulatif et bouton final appelant `MasroufDao.finishOnboarding()` avant d'ouvrir `MainActivity`. |

---

## 2. Hôte Central (`MainActivity`) & Tableau de Bord Accueil (`HomeFragment`)

### `MainActivity` ([activity_dashboard_host.xml](../../app/src/main/res/layout/activity_dashboard_host.xml), [drawer_admin.xml](../../app/src/main/res/layout/drawer_admin.xml))
- `DrawerLayout` ouvrant depuis `GravityCompat.START` : **`AdminDrawer`** (fond sombre `forest`) si `PARENT`, **`AppDrawer`** (fond clair) si `USER` ou `CHILD`.
- Conteneur central `FragmentContainerView` hébergeant les 5 fragments conservés en mémoire (`add`/`hide`/`show`).
- Bouton d'action flottant micro **`voice_transaction_button`** en bas à droite ouvrant `VoiceTransactionDialogFragment`.

### `HomeFragment` ([fragment_home.xml](../../app/src/main/res/layout/fragment_home.xml))
1. **En-tête** : bouton hamburger `home_open_drawer` (`☰`), salutation personnalisée (`home_greeting`), pilule période active (`home_period`) + cloche notifications avec compteur non lues et lien Calendrier.
2. **Carte Budget (`BudgetCardView` — `home_budget_card`)** : affiche le plafond global, le dépensé, le reste disponible, la jauge colorée (vert / ambre ≥ 80 % / corail > 100 %) et s'adapte au rôle (`Parent` inclut l'enveloppe `Enfants` et les allocations).
3. **Bandeau de synthèse par profil (`home_summary` / carte Enfant)** :
   - **`USER`** : total dépensé du mois + nombre de transactions.
   - **`PARENT`** : budget utilisé foyer + reste foyer + nombre d'enfants + transactions famille + demandes d'argent en attente.
   - **`CHILD`** : carte `home_child_summary` (dotation reçue + dernier versement, barre et objectif d'épargne `home_child_goal` affichant `@string/child_goal_missing` si aucun enregistrement en table `goals`), et boutons `Demander de l'argent` / `Répartir mon budget`.
4. **Actions rapides** : `+ Ajouter un achat`, `+ Ajouter une dépense`, `+ Catégorie` (modale), `Gérer` (catégories), `Répartir` (`AllocateBudgetActivity`), plus `Mes Enfants` si `PARENT`.
5. **Enveloppes par catégorie (`CategoryProgressCardView`)** : liste repliable (`home_categories_toggle`) avec barres individuelles par catégorie ; carte d'invitation si vide.
6. **5 transactions récentes** : chaque carte ouvre `EditExpenseActivity` ou `EditPurchaseActivity` au clic (affichage lecture seule).
