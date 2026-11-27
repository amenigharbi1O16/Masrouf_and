/*
 * ============================================================================
 * FICHIER : CategoryProgressCardView.java
 * MISSION : Composant réutilisable construit 100 % en Java affichant la progression d'une enveloppe de catégorie.
 * IMPLÉMENTE : Sous-classe de MaterialCardView assemblant icône, nom, taux prévu (bps), alloué, dépensé et ProgressBar.
 * EFFET À L'ÉCRAN : Carte blanche à bordure fine passant automatiquement sur fond danger_surface + contour corail
 *                   avec détail chiffré du dépassement dès que spentMilli dépasse allocatedMilli (ou enveloppe à 0).
 * COMMUNICATIONS : Instanciée dynamiquement dans HomeFragment.renderCategoriesList() et HomeCategoryAdapter.
 * ============================================================================
 */
package com.example.masrouf1.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.R;
import com.google.android.material.card.MaterialCardView;

/**
 * Carte d'enveloppe par catégorie construite 100 % en Java (hérite de MaterialCardView).
 */
public final class CategoryProgressCardView extends MaterialCardView {
    // Sous-vues internes générées par code : icône catégorie, libellés et jauge horizontale.
    private final ImageView icon;
    private final TextView name,planned,allocated,spent,consumed;
    private final ProgressBar progress;

    public CategoryProgressCardView(Context context){this(context,null);}
    public CategoryProgressCardView(Context context,@Nullable AttributeSet attrs){this(context,attrs,com.google.android.material.R.attr.materialCardViewStyle);}

    /** Construit toute la hiérarchie visuelle Java (en-tête icône+titre, ligne alloué/dépensé, jauge et pourcentage). */
    public CategoryProgressCardView(Context context,@Nullable AttributeSet attrs,int defStyleAttr){
        super(context,attrs,defStyleAttr);setCardBackgroundColor(getContext().getColor(R.color.alabaster));setRadius(getResources().getDimension(R.dimen.radius_card));setCardElevation(0);setStrokeColor(getContext().getColor(R.color.card_border));setStrokeWidth(getResources().getDimensionPixelSize(R.dimen.stroke_card));
        LinearLayout body=new LinearLayout(context);body.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(14),dp(12),dp(14),dp(12));
        // Ligne d'en-tête : icône 30 dp + colonne [Nom catégorie / % prévu].
        LinearLayout heading=new LinearLayout(context);heading.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);heading.setGravity(Gravity.CENTER_VERTICAL);heading.setOrientation(LinearLayout.HORIZONTAL);
        icon=new ImageView(context);icon.setContentDescription(null);heading.addView(icon,new LinearLayout.LayoutParams(dp(30),dp(30)));
        LinearLayout textColumn=new LinearLayout(context);textColumn.setOrientation(LinearLayout.VERTICAL);textColumn.setPaddingRelative(dp(10),0,0,0);LinearLayout.LayoutParams textParams=new LinearLayout.LayoutParams(0,-2,1);heading.addView(textColumn,textParams);
        name=new TextView(context);name.setTextColor(getContext().getColor(R.color.forest));name.setTextSize(15);name.setTypeface(null,android.graphics.Typeface.BOLD);name.setMaxLines(2);textColumn.addView(name);
        planned=new TextView(context);planned.setTextColor(getContext().getColor(R.color.muted_green));planned.setTextSize(12);textColumn.addView(planned);
        body.addView(heading);
        // Ligne des montants : enveloppe allouée (début) et total dépensé (fin).
        LinearLayout amounts=new LinearLayout(context);amounts.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);amounts.setOrientation(LinearLayout.HORIZONTAL);amounts.setGravity(Gravity.CENTER_VERTICAL);LinearLayout.LayoutParams amountsParams=new LinearLayout.LayoutParams(-1,-2);amountsParams.topMargin=dp(10);
        allocated=new TextView(context);allocated.setTextColor(getContext().getColor(R.color.forest));allocated.setTextSize(12);allocated.setMaxLines(2);amounts.addView(allocated,new LinearLayout.LayoutParams(0,-2,1));
        spent=new TextView(context);spent.setTextColor(getContext().getColor(R.color.muted_green));spent.setTextSize(12);spent.setMaxLines(2);spent.setGravity(Gravity.END);amounts.addView(spent,new LinearLayout.LayoutParams(0,-2,1));body.addView(amounts,amountsParams);
        // Barre horizontale 7 dp et commentaire de consommation aligné à la fin.
        progress=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);LinearLayout.LayoutParams progressParams=new LinearLayout.LayoutParams(-1,dp(7));progressParams.topMargin=dp(9);body.addView(progress,progressParams);
        consumed=new TextView(context);consumed.setTextColor(getContext().getColor(R.color.muted_green));consumed.setTextSize(12);consumed.setGravity(Gravity.END);LinearLayout.LayoutParams consumedParams=new LinearLayout.LayoutParams(-1,-2);consumedParams.topMargin=dp(5);body.addView(consumed,consumedParams);
        addView(body,new LayoutParams(-1,-2));
    }

    /** Surcharge de compatibilité convertissant plannedPercent entier en points de base (bps). */
    public void setProgressData(String displayName,String iconName,String type,String colorHex,long allocatedMilli,long spentMilli,int plannedPercent,int consumedPercent){
        setProgressData(displayName,iconName,type,colorHex,allocatedMilli,spentMilli,plannedPercent,plannedPercent*100,consumedPercent);
    }

    /**
     * Met à jour tous les textes, l'icône, la jauge et la couleur de carte :
     * bascule automatiquement sur danger_surface + bordure corail en cas de dépassement.
     */
    public void setProgressData(String displayName,String iconName,String type,String colorHex,long allocatedMilli,long spentMilli,int plannedPercent,int plannedPercentBps,int consumedPercent){
        String resolvedName=displayName==null?"":displayName;name.setText(resolvedName);
        icon.setImageResource(CategoryIconResources.resolve(iconName,type));
        int iconColor=getContext().getColor(R.color.emerald);try{if(colorHex!=null)iconColor=Color.parseColor(colorHex);}catch(IllegalArgumentException ignored){}
        icon.setImageTintList(ColorStateList.valueOf(iconColor));
        int effectiveBps=plannedPercentBps>0?plannedPercentBps:Math.max(0,plannedPercent*100);
        planned.setText(getContext().getString(R.string.category_planned_percent_str,com.example.masrouf1.BudgetPeriodMath.formatPercentFromBps(effectiveBps)));
        allocated.setText(getContext().getString(R.string.category_allocated,CurrencyFormatter.formatMilli(allocatedMilli)));
        spent.setText(getContext().getString(R.string.category_spent,CurrencyFormatter.formatMilli(spentMilli)));
        // Détecte le dépassement sur enveloppe allouée ou toute dépense sans enveloppe dédiée.
        boolean isExceeded = (allocatedMilli > 0L && spentMilli > allocatedMilli) || (allocatedMilli <= 0L && spentMilli > 0L);
        setCardBackgroundColor(getContext().getColor(isExceeded ? R.color.danger_surface : R.color.alabaster));
        setStrokeColor(getContext().getColor(isExceeded ? R.color.coral : R.color.card_border));
        int tint=isExceeded?R.color.coral:consumedPercent>=100?R.color.coral:consumedPercent>=80?R.color.amber:R.color.mint_accent;
        progress.setProgress(isExceeded?100:Math.min(100,Math.max(0,consumedPercent)));
        progress.setProgressTintList(ColorStateList.valueOf(getContext().getColor(tint)));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(getContext().getColor(R.color.mint_light)));
        // Met à jour la ligne de bas de carte selon le dépassement éventuel.
        if (allocatedMilli > 0L && spentMilli > allocatedMilli) {
            long overMilli = com.example.masrouf1.MoneyMath.subtract(spentMilli, allocatedMilli);
            consumed.setText(getContext().getString(R.string.category_overrun_detail, CurrencyFormatter.formatMilli(overMilli), Math.max(101, consumedPercent)));
            consumed.setTextColor(getContext().getColor(R.color.danger_text));
        } else if (allocatedMilli <= 0L && spentMilli > 0L) {
            consumed.setText(getContext().getString(R.string.category_unalloc_overrun_detail, CurrencyFormatter.formatMilli(spentMilli)));
            consumed.setTextColor(getContext().getColor(R.color.danger_text));
        } else {
            consumed.setText(allocatedMilli<=0&&spentMilli<=0?getContext().getString(R.string.category_unallocated):getContext().getString(R.string.category_consumed_percent,consumedPercent));
            consumed.setTextColor(getContext().getColor(tint==R.color.mint_accent?R.color.muted_green:tint));
        }
    }
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
}
