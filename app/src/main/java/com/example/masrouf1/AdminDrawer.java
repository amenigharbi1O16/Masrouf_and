/*
 * ============================================================================
 * FICHIER : AdminDrawer.java
 * MISSION : Construit et gère le tiroir latéral sombre (fond forest) réservé
 *           au profil PARENT (ex-appellation historique « Admin » conservée dans
 *           le nom de classe uniquement). Affiche 7 destinations familiales
 *           avec badges dynamiques (nombre d'enfants, notifications non lues).
 * IMPLÉMENTE : Vues Java programmatiques (LinearLayout, ImageView, TextView,
 *              GradientDrawable) ajoutées au menu XML, surbrillance de l'onglet
 *              actif et requête asynchrone des badges via MasroufViewModel.
 * EFFET À L'ÉCRAN : Menu latéral sombre comportant Accueil, Enfants (+ badge),
 *                   Produits, Calendrier, Analyses, Notifications (+ badge)
 *                   et Paramètres, avec bordure menthe autour de la ligne active.
 * COMMUNICATIONS :
 *   ← MainActivity (initializeDashboard, refreshDrawerBadges, refreshDrawerSelection)
 *   → MainActivity.onDrawerDestination (routage au clic sur une entrée)
 *   → MasroufViewModel / MasroufRepository.parentDrawerBadges (compteurs SQLite)
 *   ← SessionStore.userId (identifiant du parent connecté)
 * ============================================================================
 */
package com.example.masrouf1;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.masrouf.controller.SessionStore;

/**
 * Tiroir latéral sombre (fond forest) dédié exclusivement au profil PARENT (ex-"admin" dans le nom de classe historique).
 * Affiche 7 destinations familiales et interroge Room pour afficher les badges Enfants et Notifications non lues.
 */
public final class AdminDrawer {
    /** Clés de routage correspondant aux 7 entrées visibles du tiroir Parent. */
    public static final String[] DESTINATIONS = {"home", "children", "products", "calendar", "analytics", "notifications", "settings"};
    /** Ressources texte localisées associées dans le même ordre aux 7 destinations. */
    private static final int[] LABELS = {R.string.nav_home, R.string.nav_children, R.string.nav_products, R.string.nav_calendar_history, R.string.nav_budget_analytics, R.string.nav_notifications, R.string.nav_settings};
    /** Ressources vectorielles ic_* associées aux 7 destinations du tiroir Parent. */
    private static final int[] ICONS = {R.drawable.ic_home, R.drawable.ic_people, R.drawable.ic_bag, R.drawable.ic_calendar, R.drawable.ic_pie_chart, R.drawable.ic_notifications, R.drawable.ic_settings};

    /** Classe utilitaire statique non instanciable. */
    private AdminDrawer() { }

    /** Peuple le tiroir familial sombre (profil PARENT) avec "home" sélectionné par défaut. */
    public static void populate(MainActivity activity, LinearLayout menu) {
        populate(activity, menu, "home");
    }

    /**
     * Construit par programmation les 7 lignes du menu Parent (icône menthe, libellé albâtre,
     * pastille badge blanche) puis applique la surbrillance active et charge les compteurs SQLite.
     */
    public static void populate(MainActivity activity, LinearLayout menu, String activeDestination) {
        menu.removeAllViews(); // Nettoie d'éventuelles lignes précédentes avant reconstruction.
        TextView childrenBadge = null, notificationsBadge = null;
        for (int i = 0; i < DESTINATIONS.length; i++) {
            String destination = DESTINATIONS[i];
            // Conteneur horizontal de la ligne avec tag de destination pour updateSelection / findBadge.
            LinearLayout row = new LinearLayout(activity);
            row.setTag(destination);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPaddingRelative(dp(activity, 13), 0, dp(activity, 10), 0);
            row.setMinimumHeight(dp(activity, 50));

            // Icône vectorielle teintée en vert menthe clair (R.color.mint).
            ImageView icon = new ImageView(activity);
            icon.setImageResource(ICONS[i]);
            icon.setColorFilter(activity.getColor(R.color.mint));
            row.addView(icon, new LinearLayout.LayoutParams(dp(activity, 23), dp(activity, 23)));

            // Libellé occupant tout l'espace horizontal disponible (weight = 1).
            TextView label = new TextView(activity);
            label.setText(LABELS[i]);
            label.setTextColor(activity.getColor(R.color.alabaster));
            label.setTextSize(14.5f);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
            labelParams.setMarginStart(dp(activity, 13));
            row.addView(label, labelParams);

            // Pastille compteur initialement masquée (affichée uniquement si > 0).
            TextView badge = new TextView(activity);
            badge.setGravity(Gravity.CENTER);
            badge.setTextColor(activity.getColor(R.color.forest));
            badge.setTextSize(10);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setPadding(dp(activity, 7), dp(activity, 3), dp(activity, 7), dp(activity, 3));
            badge.setBackgroundResource(R.drawable.badge_background);
            badge.setVisibility(View.GONE);
            row.addView(badge, new LinearLayout.LayoutParams(-2, -2));

            if ("children".equals(destination)) childrenBadge = badge;
            if ("notifications".equals(destination)) notificationsBadge = badge;

            // Listener de clic déléguant la navigation à MainActivity.
            row.setOnClickListener(v -> activity.onDrawerDestination(destination));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(activity, 3);
            menu.addView(row, rowParams);
        }

        updateSelection(menu, activeDestination);
        refreshBadges(activity, menu);
    }

    /** Interroge Room via MasroufViewModel pour actualiser les pastilles Enfants et Notifications non lues. */
    public static void refreshBadges(MainActivity activity, LinearLayout menu) {
        if (activity == null || menu == null) return;
        TextView childCount = findBadgeForDestination(menu, "children");
        TextView noticeCount = findBadgeForDestination(menu, "notifications");
        long userId = SessionStore.userId(activity);
        if (userId <= 0L) return;
        MasroufViewModel viewModel = new androidx.lifecycle.ViewModelProvider(activity, new MasroufViewModelFactory(activity)).get(MasroufViewModel.class);
        viewModel.<DrawerBadgeData>request(repository -> repository.parentDrawerBadges(userId), state -> {
            if (activity.isFinishing() || activity.isDestroyed() || state.data == null) return;
            // Met à jour la pastille du nombre de profils enfants gérés par le parent.
            if (childCount != null) {
                if (state.data.children > 0) {
                    childCount.setText(String.valueOf(state.data.children));
                    childCount.setVisibility(View.VISIBLE);
                } else {
                    childCount.setVisibility(View.GONE);
                }
            }
            // Met à jour la pastille des notifications non lues du parent.
            if (noticeCount != null) {
                if (state.data.unreadNotifications > 0) {
                    noticeCount.setText(String.valueOf(state.data.unreadNotifications));
                    noticeCount.setVisibility(View.VISIBLE);
                } else {
                    noticeCount.setVisibility(View.GONE);
                }
            }
        });
    }

    /** Retrouve le TextView badge (3e enfant d'une ligne LinearLayout) correspondant à une clé de destination. */
    private static TextView findBadgeForDestination(LinearLayout menu, String destination) {
        for (int i = 0; i < menu.getChildCount(); i++) {
            View child = menu.getChildAt(i);
            if (!(child instanceof LinearLayout) || !destination.equals(child.getTag())) continue;
            LinearLayout row = (LinearLayout) child;
            if (row.getChildCount() >= 3 && row.getChildAt(2) instanceof TextView) {
                return (TextView) row.getChildAt(2);
            }
        }
        return null;
    }

    /** Met à jour visuellement la ligne correspondant à l'onglet actif dans le tiroir parent. */
    public static void updateSelection(LinearLayout menu, String activeDestination) {
        if (menu == null) return;
        for (int i = 0; i < menu.getChildCount(); i++) {
            View child = menu.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            LinearLayout row = (LinearLayout) child;
            boolean active = activeDestination != null && activeDestination.equals(row.getTag());
            // Fond translucide menthe + bordure si actif, sinon fond normal XML.
            if (active) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(Color.argb(48, 217, 238, 221));
                bg.setCornerRadius(dp(menu, 14));
                bg.setStroke(dp(menu, 1), menu.getContext().getColor(R.color.mint_accent));
                row.setBackground(bg);
            } else {
                row.setBackgroundResource(R.drawable.drawer_item_background);
            }
            // Met le texte en gras et teinte menthe lorsque la ligne correspond à la vue affichée.
            if (row.getChildCount() >= 2 && row.getChildAt(1) instanceof TextView) {
                TextView label = (TextView) row.getChildAt(1);
                label.setTextColor(menu.getContext().getColor(active ? R.color.mint : R.color.alabaster));
                label.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
            }
        }
    }

    /** Convertit des dp en pixels à partir du contexte de MainActivity. */
    private static int dp(MainActivity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + .5f);
    }

    /** Convertit des dp en pixels à partir d'une vue existante. */
    private static int dp(View view, int value) {
        return (int) (value * view.getResources().getDisplayMetrics().density + .5f);
    }
}
