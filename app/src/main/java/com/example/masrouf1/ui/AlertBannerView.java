/*
 * ============================================================================
 * FICHIER : AlertBannerView.java
 * MISSION : Composant UI réutilisable affichant une bannière d'alerte ou de validation construite 100 % en Java.
 * IMPLÉMENTE : Sous-classe de MaterialCardView (Tone : NONE, SUCCESS, WARNING, DANGER).
 * EFFET À L'ÉCRAN : Dessine une carte arrondie (20 dp) avec icône « ✓ » / « ! », titre gras et sous-texte coloré
 *                   selon le ton (vert doux, jaune ambre ou rouge doux) ; se masque via GONE sous Tone.NONE.
 * COMMUNICATIONS : Injecté dans les XML d'écrans formulaires, modales et dashboard ; piloté par setAlert(...).
 * ============================================================================
 */
package com.example.masrouf1.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.masrouf1.R;
import com.google.android.material.card.MaterialCardView;

/**
 * Bannière réutilisable construite 100 % en Java (hérite de MaterialCardView).
 */
public final class AlertBannerView extends MaterialCardView {
    /**
     * Niveaux visuels supportés : masqué (NONE), succès (SUCCESS), avertissement
     * (WARNING), erreur (DANGER).
     */
    public enum Tone {
        NONE, SUCCESS, WARNING, DANGER
    }

    // Vues internes générées en Java : picto gauche ("✓" / "!"), titre gras et
    // détail optionnel.
    private final TextView icon;
    private final TextView titleView;
    private final TextView detailView;

    /** Constructeur programmatique Java. */
    public AlertBannerView(Context context) {
        this(context, null);
    }

    /** Constructeur XML sans style explicite. */
    public AlertBannerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.materialCardViewStyle);
    }

    /**
     * Construit l'arborescence Java complète de la bannière (LinearLayout
     * horizontal + colonne texte).
     */
    public AlertBannerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setRadius(dp(20));
        setCardElevation(0);
        setStrokeWidth(0);
        setUseCompatPadding(false);
        // Conteneur principal respectant la direction RTL/LTR locale avec marges
        // internes confortables.
        LinearLayout row = new LinearLayout(context);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        // Symbole d'état à gauche/droite selon la langue.
        icon = new TextView(context);
        icon.setGravity(Gravity.CENTER);
        icon.setTextSize(17);
        icon.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(icon, new LinearLayout.LayoutParams(dp(30), dp(30)));
        // Colonne verticale hébergeant le titre et le message détaillé.
        LinearLayout copy = new LinearLayout(context);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setPaddingRelative(dp(10), 0, 0, 0);
        titleView = new TextView(context);
        titleView.setTextSize(14);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        copy.addView(titleView);
        detailView = new TextView(context);
        detailView.setTextSize(13);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
        detailParams.topMargin = dp(2);
        copy.addView(detailView, detailParams);
        // Par défaut, initialise la bannière à l'état masqué (Tone.NONE).
        row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1));
        addView(row, new LayoutParams(-1, -2));
        setAlert(Tone.NONE, "", "");
    }

    /**
     * Met à jour le ton visuel et le texte de la bannière (NONE la masque avec
     * View.GONE).
     * 
     * @param tone  niveau sémantique (SUCCESS, WARNING, DANGER ou NONE).
     * @param title titre principal en gras ; @param detail précision optionnelle
     *              sous le titre.
     */
    public void setAlert(Tone tone, String title, String detail) {
        // Cache la vue si aucun message actif ne doit être affiché.
        if (tone == null || tone == Tone.NONE) {
            setVisibility(GONE);
            return;
        }
        setVisibility(VISIBLE);
        int background, text, mark;
        // Résout fond sémantique, couleur de texte et icône selon le ton demandé.
        switch (tone) {
            case WARNING:
                background = R.color.warning_surface;
                text = R.color.warning_text;
                mark = R.color.amber;
                icon.setText("!");
                break;
            case DANGER:
                background = R.color.danger_surface;
                text = R.color.danger_text;
                mark = R.color.coral;
                icon.setText("!");
                break;
            default:
                background = R.color.success_surface;
                text = R.color.emerald;
                mark = R.color.emerald;
                icon.setText("✓");
                break;
        }
        int bg = getContext().getColor(background), fg = getContext().getColor(text),
                accent = getContext().getColor(mark);
        setCardBackgroundColor(bg);
        icon.setTextColor(accent);
        titleView.setTextColor(fg);
        detailView.setTextColor(fg);
        titleView.setText(title == null ? "" : title);
        detailView.setText(detail == null ? "" : detail);
        detailView.setVisibility(detail == null || detail.trim().isEmpty() ? GONE : VISIBLE);
        setContentDescription(detail == null || detail.isEmpty() ? title : title + ". " + detail);
    }

    /** Convertit une dimension en dp vers pixels écran réels. */
    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
