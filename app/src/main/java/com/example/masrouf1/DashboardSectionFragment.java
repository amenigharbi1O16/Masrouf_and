/*
 * ============================================================================
 * FICHIER : DashboardSectionFragment.java
 * MISSION : Classe de base abstraite des 4 onglets centraux secondaires
 *           (Produits, Calendrier, Analyses, Paramètres). Construit une coque
 *           visuelle unifiée 100 % en Java (ScrollView crème + barre supérieure
 *           avec bouton menu ☰, titre, cloche à badge rouge et icône profil).
 * IMPLÉMENTE : Template Method Pattern avec les hooks pageTitle(),
 *              createContent(LinearLayout) et observeContent(), ainsi que la
 *              mise à jour du badge de notifications non lues à l'ouverture
 *              et au retour (onViewCreated / onResume).
 * EFFET À L'ÉCRAN : En-tête supérieur cohérent sur Produits, Calendrier,
 *                   Analyses et Paramètres avec pastille rouge « 99+ » sur la
 *                   cloche lorsque des alertes ne sont pas encore lues.
 * COMMUNICATIONS :
 *   ← ProductsFragment, CalendarFragment, AnalyticsFragment, SettingsFragment
 *   → MainActivity.openMenu / onDrawerDestination("settings")
 *   → NotificationsActivity (clic sur la cloche)
 *   → MasroufViewModel / MasroufRepository.unreadNotificationCount
 *   ← SessionStore.userId
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.masrouf.controller.SessionStore;

/**
 * Fragment de base abstrait construisant intégralement en Java la coque d'en-tête (bouton menu ☰, titre,
 * cloche de notifications avec badge rouge et raccourci Paramètres) des onglets centraux secondaires.
 */
/** Shared programmatic shell for the non-home central tabs with bell badge & profile navigation. */
public abstract class DashboardSectionFragment extends Fragment {
    protected LinearLayout page;
    protected MasroufViewModel viewModel;
    private TextView notificationBadge;

    /** Convertit une dimension en dp vers pixels réels d'écran pour les vues créées en Java. */
    protected int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    /** Retourne l'identifiant de ressource string du titre affiché dans l'en-tête du onglet. */
    protected abstract int pageTitle();

    /** Construit le contenu métier de l'onglet en l'ajoutant au conteneur vertical {@code content}. */
    protected abstract void createContent(LinearLayout content);

    /** Hook appelé après création de la vue pour attacher les observateurs LiveData / ViewModel. */
    protected void observeContent() { }

    @Override
    public void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        viewModel = new androidx.lifecycle.ViewModelProvider(
                this, new MasroufViewModelFactory(requireContext())).get(MasroufViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle state) {
        super.onViewCreated(view, state);
        observeContent();
        refreshHeaderBadge();
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshHeaderBadge();
    }

    /**
     * Construit entièrement par programmation (Java-based Views) l'ossature ScrollView + barre d'en-tête
     * (bouton hamburger ☰, titre, cloche + badge FrameLayout rouge, icône profil) puis appelle {@link #createContent}.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull android.view.LayoutInflater inflater, @Nullable ViewGroup parent, @Nullable Bundle state) {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(requireContext().getColor(R.color.cream));

        page = new LinearLayout(requireContext());
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(18), dp(20), dp(96));
        scroll.addView(page, new ScrollView.LayoutParams(-1, -2));

        LinearLayout header = new LinearLayout(requireContext());
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(-1, -2);
        headerParams.bottomMargin = dp(8);

        TextView menu = new TextView(requireContext());
        menu.setText("☰");
        menu.setTextSize(22);
        menu.setTextColor(requireContext().getColor(R.color.forest));
        menu.setGravity(Gravity.CENTER);
        menu.setContentDescription(getString(R.string.open_menu));
        header.addView(menu, new LinearLayout.LayoutParams(dp(44), dp(48)));
        menu.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).openMenu();
            }
        });

        TextView title = new TextView(requireContext());
        title.setText(pageTitle());
        title.setTextColor(requireContext().getColor(R.color.forest));
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1f);
        titleParams.setMarginStart(dp(6));
        header.addView(title, titleParams);

        // Notification bell + live unread badge
        FrameLayout bellWrap = new FrameLayout(requireContext());
        bellWrap.setContentDescription(getString(R.string.nav_notifications));
        bellWrap.setClickable(true);
        bellWrap.setFocusable(true);
        ImageView bellIcon = new ImageView(requireContext());
        bellIcon.setImageResource(R.drawable.ic_notifications);
        bellIcon.setColorFilter(requireContext().getColor(R.color.forest));
        FrameLayout.LayoutParams bellIconLp = new FrameLayout.LayoutParams(dp(23), dp(23));
        bellIconLp.gravity = Gravity.CENTER;
        bellWrap.addView(bellIcon, bellIconLp);

        notificationBadge = new TextView(requireContext());
        notificationBadge.setBackgroundResource(R.drawable.bg_badge_red);
        notificationBadge.setTextColor(Color.WHITE);
        notificationBadge.setTextSize(10f);
        notificationBadge.setTypeface(null, Typeface.BOLD);
        notificationBadge.setGravity(Gravity.CENTER);
        notificationBadge.setVisibility(View.GONE);
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(dp(18), dp(18));
        badgeLp.gravity = Gravity.TOP | Gravity.END;
        badgeLp.topMargin = dp(5);
        badgeLp.setMarginEnd(dp(4));
        bellWrap.addView(notificationBadge, badgeLp);
        bellWrap.setOnClickListener(v -> startActivity(new Intent(requireContext(), NotificationsActivity.class)));
        header.addView(bellWrap, new LinearLayout.LayoutParams(dp(44), dp(48)));

        // Profile icon -> Settings
        ImageView profileBtn = new ImageView(requireContext());
        profileBtn.setImageResource(R.drawable.ic_person_outline);
        profileBtn.setContentDescription(getString(R.string.nav_settings));
        profileBtn.setPadding(dp(10), dp(12), dp(10), dp(12));
        profileBtn.setClickable(true);
        profileBtn.setFocusable(true);
        profileBtn.setOnClickListener(v -> {
            if (requireActivity() instanceof MainActivity) {
                ((MainActivity) requireActivity()).onDrawerDestination("settings");
            }
        });
        header.addView(profileBtn, new LinearLayout.LayoutParams(dp(44), dp(48)));

        page.addView(header, headerParams);
        createContent(page);
        return scroll;
    }

    /** Interroge Room via MasroufViewModel pour mettre à jour la pastille rouge de notifications non lues (jusqu'à "99+"). */
    protected void refreshHeaderBadge() {
        if (notificationBadge == null || viewModel == null || !isAdded()) return;
        long uid = signedInUserId();
        if (uid <= 0L) return;
        viewModel.<Integer>request(repo -> repo.unreadNotificationCount(uid), state -> {
            if (!isAdded() || notificationBadge == null || state.data == null) return;
            if (state.data > 0) {
                notificationBadge.setText(state.data > 99 ? "99+" : String.valueOf(state.data));
                notificationBadge.setVisibility(View.VISIBLE);
            } else {
                notificationBadge.setVisibility(View.GONE);
            }
        });
    }

    /** Crée un TextView standard stylé en couleur ink_secondary pour les fragments enfants. */
    protected TextView text(String value) {
        TextView view = new TextView(requireContext());
        view.setText(value);
        view.setTextColor(requireContext().getColor(R.color.ink_secondary));
        view.setTextSize(15);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    /** Retourne l'identifiant du compte actuellement authentifié depuis SessionStore. */
    protected long signedInUserId() {
        return SessionStore.userId(requireContext());
    }

    /** Exécute une mise à jour visuelle sur le thread UI si le Fragment est toujours attaché. */
    protected void ui(Runnable action) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                if (isAdded() && getView() != null) action.run();
            });
        }
    }
}
