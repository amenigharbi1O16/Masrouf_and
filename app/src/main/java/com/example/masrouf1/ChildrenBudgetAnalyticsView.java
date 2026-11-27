/*
 * ============================================================================
 * FICHIER : ChildrenBudgetAnalyticsView.java
 * MISSION : Composant graphique personnalisé 2D (Canvas) affichant la consommation mensuelle en % des enfants.
 * IMPLÉMENTE : Sous-classe de android.view.View sans dépendance graphique externe (onMeasure + onDraw).
 * EFFET À L'ÉCRAN : Dessine des barres verticales arrondies colorées par seuils (< 80 % menthe, >= 80 % ambre,
 *                   > 100 % corail), avec étiquette en % au-dessus et mois ("MMM") en abscisse.
 * COMMUNICATIONS : Utilisé dans activity_family_budgets_analytics.xml et AnalyticsRenderer via setPoints(...).
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Graphique en barres verticales dessiné sur Canvas natif Android (aucune bibliothèque graphique tierce).
 */
public final class ChildrenBudgetAnalyticsView extends View {
    /** Point de données mensuel agrégé (mois, dépensé et alloué en millimes). */
    public static final class Point {
        public long monthStart,spentMilli,allocatedMilli;
        public Point(long month,long spent,long allocated) { monthStart=month; spentMilli=spent; allocatedMilli=allocated; }
    }
    // Pinceau anticrénelé partagé, série de points, format de mois localisé et couleurs du thème.
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<Point> points=new ArrayList<>();
    private final SimpleDateFormat monthLabel=new SimpleDateFormat("MMM",Locale.getDefault());
    private int forest,mint,emerald,amber,coral,muted,track; private float density;

    /** Constructeur programmatique Java. */
    public ChildrenBudgetAnalyticsView(Context context) { this(context,null); }

    /** Constructeur XML résolvant la densité écran et les couleurs botaniques du thème. */
    public ChildrenBudgetAnalyticsView(Context context,android.util.AttributeSet attrs) {
        super(context,attrs); density=getResources().getDisplayMetrics().density; forest=context.getColor(R.color.forest); mint=context.getColor(R.color.mint); emerald=context.getColor(R.color.emerald); amber=context.getColor(R.color.amber); coral=context.getColor(R.color.coral); muted=context.getColor(R.color.muted_green); track=context.getColor(R.color.card_border);
    }

    /** Met à jour les barres mensuelles affichées puis déclenche un redessin Canvas (invalidate). */
    public void setPoints(List<Point> values) { points.clear(); if(values!=null) points.addAll(values); invalidate(); }

    /** Fixe une hauteur recommandée de 210 dp afin de réserver l'espace du graphique dans un ScrollView. */
    @Override protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec) { setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),resolveSize((int)(210*density),heightMeasureSpec)); }

    /** Trace la ligne de base, les pistes grises, les barres colorées par pourcentage et leurs libellés. */
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Si aucune donnée historique n'est disponible, affiche un texte centré neutre.
        if(points.isEmpty()) { paint.setColor(muted); paint.setTextSize(13*density); canvas.drawText(getContext().getString(R.string.history_none),getPaddingLeft()+8*density,getHeight()/2f,paint); return; }
        // Calcule le rectangle intérieur de tracé et dessine la ligne horizontale de référence.
        float left=getPaddingLeft()+12*density,right=getWidth()-getPaddingRight()-12*density,top=getPaddingTop()+12*density,bottom=getHeight()-getPaddingBottom()-28*density;
        paint.setColor(track); paint.setStrokeWidth(1*density); canvas.drawLine(left,bottom,right,bottom,paint);
        // Détermine l'échelle verticale (au moins 100 %, plafonnée à 300 % pour préserver la lisibilité).
        int maxRatio=100; for(Point point:points) maxRatio=Math.max(maxRatio,Math.min(300,MoneyMath.percentOf(point.spentMilli,point.allocatedMilli)));
        float slot=(right-left)/points.size(),barWidth=Math.min(34*density,slot*.52f),chartHeight=bottom-top;
        // Boucle sur chaque mois pour tracer fond de piste, barre pleine, pourcentage et étiquette mois.
        for(int i=0;i<points.size();i++) {
            Point point=points.get(i); int percent=MoneyMath.percentOf(point.spentMilli,point.allocatedMilli); float height=chartHeight*(Math.min(maxRatio,Math.max(0,percent))/(float)maxRatio);
            int color=point.spentMilli>point.allocatedMilli?coral:MoneyMath.atLeastPercent(point.spentMilli,point.allocatedMilli,80)?amber:mint; float center=left+slot*(i+.5f);
            RectF bar=new RectF(center-barWidth/2,bottom-height,center+barWidth/2,bottom); paint.setColor(track); canvas.drawRoundRect(new RectF(center-barWidth/2,top,center+barWidth/2,bottom),barWidth/2,barWidth/2,paint); paint.setColor(color); if(height>0) canvas.drawRoundRect(bar,barWidth/2,barWidth/2,paint);
            paint.setColor(forest); paint.setTextSize(11*density); String value=String.format(Locale.ROOT,"%d%%",percent); canvas.drawText(value,center-paint.measureText(value)/2,bottom-height-5*density,paint);
            paint.setColor(muted); String label=monthLabel.format(point.monthStart); canvas.drawText(label,center-paint.measureText(label)/2,bottom+17*density,paint);
        }
    }
}
