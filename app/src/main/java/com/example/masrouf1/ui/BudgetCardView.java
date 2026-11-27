/*
 * ============================================================================
 * FICHIER : BudgetCardView.java
 * MISSION : Carte principale sombre (« Hero Card ») du tableau de bord Accueil construite en Java pur.
 * IMPLÉMENTE : Sous-classe personnalisée de MaterialCardView assemblant texte et ProgressBar par programmation.
 * EFFET À L'ÉCRAN : Affiche sur fond vert forêt la période, le « Reste à vivre » en 32sp gras, la barre
 *                   horizontale colorée dynamiquement (menthe <80 %, ambre >=80 %, corail >=100 %), le dépensé/total
 *                   et le bouton « Répartir mon budget par catégorie » en mode Enfant.
 * COMMUNICATIONS : Déclarée dans fragment_home.xml -> alimentée par HomeFragment.renderBudget().
 * ============================================================================
 */
package com.example.masrouf1.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.MoneyMath;
import com.example.masrouf1.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

/**
 * Carte principale « Hero » du tableau de bord construite intégralement par
 * programmation en Java.
 */
public final class BudgetCardView extends MaterialCardView {
    // Vues internes construites en Java pour la période, le reste à vivre, le total
    // consommé/alloué, la jauge et le bouton enfant.
    private final TextView period, remaining, spent, total;
    private final ProgressBar progress;
    private final MaterialButton allocationAction;

    /** Constructeur programmatique. */
    public BudgetCardView(Context context) {
        this(context, null);
    }

    /** Constructeur XML par défaut. */
    public BudgetCardView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.materialCardViewStyle);
    }

    /**
     * Initialise l'arborescence graphique complète en Java (couleurs forêt/menthe,
     * textes et jauge).
     */
    public BudgetCardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setCardBackgroundColor(getContext().getColor(R.color.forest));
        setRadius(getResources().getDimension(R.dimen.radius_budget_hero));
        setCardElevation(0);
        setStrokeWidth(0);
        setClickable(true);
        setFocusable(true);
        // Conteneur vertical principal avec gestion RTL et marges intérieures 20/18 dp.
        LinearLayout content = new LinearLayout(context);
        content.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(18), dp(20), dp(18));
        // Libellé de période (placé en haut de carte).
        period = new TextView(context);
        period.setId(R.id.home_budget_period);
        period.setTextColor(getContext().getColor(R.color.mint_accent));
        period.setTextSize(12);
        period.setMaxLines(2);
        content.addView(period);
        // Sous-titre fixe « Reste à vivre ».
        TextView label = new TextView(context);
        label.setText(R.string.remaining_to_live);
        label.setTextColor(getContext().getColor(R.color.mint_accent));
        label.setTextSize(13);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(-1, -2);
        labelParams.topMargin = dp(10);
        content.addView(label, labelParams);
        // Grand chiffre principal du reste disponible (32sp gras blanc).
        remaining = new TextView(context);
        remaining.setId(R.id.home_remaining);
        remaining.setTextColor(getContext().getColor(R.color.alabaster));
        remaining.setTextSize(32);
        remaining.setTypeface(null, android.graphics.Typeface.BOLD);
        remaining.setSingleLine(true);
        content.addView(remaining);
        // Jauge horizontale 8 dp initialement verte menthe.
        progress = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
        progress.setId(R.id.home_budget_progress);
        progress.setMax(100);
        progress.setProgress(0);
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(getContext().getColor(R.color.muted_green)));
        progress.setProgressTintList(ColorStateList.valueOf(getContext().getColor(R.color.mint_accent)));
        LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(-1, dp(8));
        progressParams.topMargin = dp(14);
        content.addView(progress, progressParams);
        // Ligne inférieure gauche/droite affichant dépensé et plafond/dotation.
        LinearLayout values = new LinearLayout(context);
        values.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        values.setGravity(Gravity.CENTER_VERTICAL);
        values.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams valuesParams = new LinearLayout.LayoutParams(-1, -2);
        valuesParams.topMargin = dp(12);
        spent = new TextView(context);
        spent.setId(R.id.home_spent);
        spent.setTextColor(getContext().getColor(R.color.alabaster));
        spent.setTextSize(12);
        values.addView(spent, new LinearLayout.LayoutParams(0, -2, 1));
        total = new TextView(context);
        total.setId(R.id.home_total);
        total.setTextColor(getContext().getColor(R.color.mint_accent));
        total.setTextSize(12);
        total.setGravity(Gravity.END);
        values.addView(total);
        content.addView(values, valuesParams);
        // Bouton tonal spécifique au profil CHILD permettant de répartir sa dotation
        // (masqué par défaut).
        allocationAction = new MaterialButton(context, null,
                com.google.android.material.R.attr.materialButtonTonalStyle);
        allocationAction.setId(R.id.home_partition_child_budget);
        allocationAction.setText(R.string.partition_child_budget);
        allocationAction.setTextColor(getContext().getColor(R.color.forest));
        allocationAction.setBackgroundTintList(ColorStateList.valueOf(getContext().getColor(R.color.mint_light)));
        allocationAction.setCornerRadius(dp(16));
        allocationAction.setVisibility(GONE);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(-1, dp(48));
        actionParams.topMargin = dp(12);
        content.addView(allocationAction, actionParams);
        addView(content, new LayoutParams(-1, -2));
        setBudgetUnavailable(getContext().getString(R.string.limit_label));
    }

    /**
     * Affiche l'état neutre lorsqu'aucun budget actif n'est configuré sur le
     * compte.
     */
    public void setBudgetUnavailable(String totalLabel) {
        period.setText(null);
        period.setVisibility(GONE);
        remaining.setText(R.string.budget_value_unavailable);
        spent.setText(R.string.spent_value_unavailable);
        total.setText(getContext().getString(R.string.budget_total_unavailable, totalLabel));
        progress.setProgress(0);
    }

    /**
     * Injecte les montants Room sur la carte Hero et adapte la couleur de la barre
     * horizontale
     * selon les seuils settings (warningFrom = 80 % ambre, dangerFrom = 100 %
     * corail).
     */
    public void setBudgetData(String periodLabel, long totalMilli, long spentMilli, String totalLabel, int warningFrom,
            int dangerFrom) {
        long remainingMilli = MoneyMath.subtract(totalMilli, spentMilli);
        int percent = totalMilli <= 0 ? (spentMilli > 0 ? 100 : 0)
                : Math.max(0, MoneyMath.percentOf(spentMilli, totalMilli));
        period.setText(periodLabel == null ? "" : periodLabel);
        period.setVisibility(periodLabel == null || periodLabel.trim().isEmpty() ? GONE : VISIBLE);
        remaining.setText(CurrencyFormatter.formatMilli(remainingMilli));
        spent.setText(getContext().getString(R.string.spent_label) + "  " + CurrencyFormatter.formatMilli(spentMilli));
        total.setText((totalLabel == null ? getContext().getString(R.string.limit_label) : totalLabel) + "  "
                + CurrencyFormatter.formatMilli(totalMilli));
        progress.setProgress(Math.min(100, percent));
        int tint = percent >= dangerFrom ? R.color.coral : percent >= warningFrom ? R.color.amber : R.color.mint_accent;
        progress.setProgressTintList(ColorStateList.valueOf(getContext().getColor(tint)));
    }

    /**
     * Affiche ou masque le bouton « Répartir mon budget par catégorie » (visible
     * uniquement en mode CHILD).
     */
    public void setAllocationActionVisible(boolean visible) {
        allocationAction.setVisibility(visible ? VISIBLE : GONE);
    }

    /** Branche le listener de clic sur le bouton de répartition Enfant. */
    public void setAllocationActionClickListener(OnClickListener listener) {
        allocationAction.setOnClickListener(listener);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
