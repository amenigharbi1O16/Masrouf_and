/*
 * ============================================================================
 * FICHIER : AppDrawer.java
 * MISSION : Construit et gère le tiroir latéral clair destiné aux profils
 *           Normal User (USER) et Enfant (CHILD). Affiche les 7 destinations
 *           avec icônes vectorielles vertes, surbrillance de l'onglet actif
 *           et badge dynamique de notifications non lues.
 * IMPLÉMENTE : Vues Java programmatiques (LinearLayout, ImageView, TextView,
 *              GradientDrawable) injectées dans personal_drawer_panel et requête
 *              asynchrone du compteur unreadNotificationCount via ViewModel.
 * EFFET À L'ÉCRAN : Menu latéral crème/blanc comportant Accueil, Produits,
 *                   Calendrier, Analyses, Récompenses, Notifications (+ pastille)
 *                   et Paramètres, avec fond menthe clair pour la page active.
 * COMMUNICATIONS :
 *   ← MainActivity (initializeDashboard, refreshDrawerBadges, refreshDrawerSelection)
 *   → MainActivity.onDrawerDestination (navigation vers l'onglet ou Activity cible)
 *   → MasroufViewModel / MasroufRepository.unreadNotificationCount (Room)
 *   ← SessionStore.userId (identifiant de l'utilisateur connecté)
 * ============================================================================
 */
package com.example.masrouf1;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.masrouf.controller.SessionStore;

/**
 * COMPOSANT : AppDrawer
 * RÔLE : Construit le tiroir latéral pour les comptes personnels et enfants.
 * Affiche les 7 destinations avec icônes vectorielles, badge de notifications non lues
 * et mise en évidence de la destination active.
 */
public final class AppDrawer {
    /** Clés de routage correspondant aux 7 entrées du menu latéral USER/CHILD. */
    public static final String[] DESTINATIONS = {"home", "products", "calendar", "analytics", "rewards", "notifications", "settings"};
    /** Ressources texte localisées pour chaque entrée du menu. */
    private static final int[] LABELS = {R.string.nav_home, R.string.nav_products, R.string.nav_calendar,
            R.string.nav_analytics, R.string.nav_rewards, R.string.nav_notifications, R.string.nav_settings};
    /** Ressources vectorielles des icônes de navigation. */
    private static final int[] ICONS = {R.drawable.ic_home, R.drawable.ic_bag, R.drawable.ic_calendar,
            R.drawable.ic_pie_chart, R.drawable.ic_trophy, R.drawable.ic_notifications, R.drawable.ic_settings};

    /** Empêche l'instanciation de cette classe utilitaire statique. */
    private AppDrawer() { }

    /** Point d'entrée par défaut peuplant le tiroir avec "home" actif. */
    public static void populate(MainActivity activity, LinearLayout menu) {
        populate(activity, menu, "home");
    }

    /** Peuple les 7 entrées du menu latéral USER/CHILD et met en surbrillance {@code activeDestination}. */
    public static void populate(MainActivity activity, LinearLayout menu, String activeDestination) {
        addItems(activity, menu, DESTINATIONS, LABELS, ICONS, activeDestination);
    }

    /** Surcharge interne utilisant "home" comme destination active par défaut. */
    static void addItems(MainActivity activity, LinearLayout menu, String[] destinations, int[] labels, int[] icons) {
        addItems(activity, menu, destinations, labels, icons, "home");
    }

    /** Crée programmatiquement chaque ligne du tiroir (ImageView + TextView + badge arrondi) et attache le routage. */
    static void addItems(MainActivity activity, LinearLayout menu, String[] destinations, int[] labels, int[] icons, String activeDestination) {
        menu.removeAllViews();
        TextView notificationsBadge = null;

        for (int i = 0; i < destinations.length; i++) {
            final String destination = destinations[i];

            // Construction Java pure de chaque ligne du menu latéral : icône + libellé + pastille badge.
            LinearLayout row = new LinearLayout(activity);
            row.setTag(destination);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPaddingRelative(dp(activity, 13), 0, dp(activity, 10), 0);
            row.setMinimumHeight(dp(activity, 50));

            // Icône teintée en vert forêt (ou émeraude si sélectionnée).
            ImageView icon = new ImageView(activity);
            icon.setImageResource(icons[i]);
            icon.setColorFilter(activity.getColor(R.color.forest));
            row.addView(icon, new LinearLayout.LayoutParams(dp(activity, 22), dp(activity, 22)));

            // Libellé prenant la largeur restante.
            TextView label = new TextView(activity);
            label.setText(labels[i]);
            label.setTextColor(activity.getColor(R.color.ink));
            label.setTextSize(14.5f);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, -2, 1);
            labelParams.setMarginStart(dp(activity, 13));
            row.addView(label, labelParams);

            // Pastille émeraude affichant le nombre de notifications non lues.
            TextView badge = new TextView(activity);
            badge.setGravity(Gravity.CENTER);
            badge.setTextColor(activity.getColor(R.color.alabaster));
            badge.setTextSize(10f);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setPadding(dp(activity, 7), dp(activity, 3), dp(activity, 7), dp(activity, 3));
            GradientDrawable badgeBg = new GradientDrawable();
            badgeBg.setColor(activity.getColor(R.color.emerald));
            badgeBg.setCornerRadius(dp(activity, 12));
            badge.setBackground(badgeBg);
            badge.setVisibility(View.GONE);
            row.addView(badge, new LinearLayout.LayoutParams(-2, -2));
            if ("notifications".equals(destination)) {
                notificationsBadge = badge;
            }

            // Écouteur de clic routant vers MainActivity.onDrawerDestination.
            row.setOnClickListener(v -> activity.onDrawerDestination(destination));
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            rowParams.bottomMargin = dp(activity, 3);
            menu.addView(row, rowParams);
        }

        updateSelection(menu, activeDestination);

        // Chargement asynchrone du compteur de notifications non lues (badge)
        refreshBadges(activity, menu);
    }

    /** Interroge SQLite Room en arrière-plan pour mettre à jour la pastille de notifications non lues. */
    public static void refreshBadges(MainActivity activity, LinearLayout menu) {
        if (activity == null || menu == null) return;
        TextView noticeBadgeView = findBadgeForDestination(menu, "notifications");
        long userId = SessionStore.userId(activity);
        if (noticeBadgeView != null && userId > 0L) {
            MasroufViewModel viewModel = new androidx.lifecycle.ViewModelProvider(
                    activity, new MasroufViewModelFactory(activity)).get(MasroufViewModel.class);
            viewModel.<Integer>request(repository -> repository.unreadNotificationCount(userId), state -> {
                if (activity.isFinishing() || activity.isDestroyed() || state.data == null) return;
                if (state.data > 0) {
                    noticeBadgeView.setText(String.valueOf(state.data));
                    noticeBadgeView.setVisibility(View.VISIBLE);
                } else {
                    noticeBadgeView.setVisibility(View.GONE);
                }
            });
        }
    }

    /** Recherche dans les enfants du menu la pastille badge associée à la destination donnée. */
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

    /** Met à jour visuellement la ligne correspondant à l'onglet actif (fond menthe, icône émeraude, gras). */
    public static void updateSelection(LinearLayout menu, String activeDestination) {
        if (menu == null) return;
        for (int i = 0; i < menu.getChildCount(); i++) {
            View child = menu.getChildAt(i);
            if (!(child instanceof LinearLayout)) continue;
            LinearLayout row = (LinearLayout) child;
            boolean active = activeDestination != null && activeDestination.equals(row.getTag());
            if (active) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(menu.getContext().getColor(R.color.mint));
                bg.setCornerRadius(dp(menu, 14));
                row.setBackground(bg);
            } else {
                row.setBackgroundResource(R.drawable.drawer_item_background);
            }
            if (row.getChildCount() >= 2 && row.getChildAt(0) instanceof ImageView && row.getChildAt(1) instanceof TextView) {
                ((ImageView) row.getChildAt(0)).setColorFilter(
                        menu.getContext().getColor(active ? R.color.emerald : R.color.forest));
                TextView label = (TextView) row.getChildAt(1);
                label.setTextColor(menu.getContext().getColor(active ? R.color.forest : R.color.ink));
                label.setTypeface(null, active ? Typeface.BOLD : Typeface.NORMAL);
            }
        }
    }

    /** Surcharge de compatibilité pour l'appel sans tableau d'icônes explicite. */
    static void addItems(MainActivity activity, LinearLayout menu, String[] destinations, int[] labels) {
        addItems(activity, menu, destinations, labels, ICONS, "home");
    }

    /** Convertit des dp en pixels à partir de MainActivity. */
    private static int dp(MainActivity activity, int value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + .5f);
    }

    /** Convertit des dp en pixels à partir d'une vue existante. */
    private static int dp(View view, int value) {
        return (int) (value * view.getResources().getDisplayMetrics().density + .5f);
    }
}
