/*
 * ============================================================================
 * FICHIER : PillBarChartView.java
 * MISSION : Graphique Canvas 2D horizontal interactif affichant l'avancement budgétaire agrégé de chaque enfant.
 * IMPLÉMENTE : Sous-classe de android.view.View avec GestureDetector pour détecter le clic sur la barre d'un enfant.
 * EFFET À L'ÉCRAN : Trace prénom en gras, barre arrondie grise/menthe/ambre/corail selon %, texte dépensé/alloué,
 *                   tout en préservant la confidentialité du détail des dépenses de l'enfant.
 * COMMUNICATIONS : Utilisé dans AnalyticsRenderer (mode PARENT) et activity_family_budgets_analytics.xml.
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import com.example.masrouf1.db.ChildSpendingSummary;
import com.example.masrouf1.MoneyMath;

import java.util.ArrayList;
import java.util.List;

/**
 * Graphique en barres horizontales « pilules » dessiné sur Canvas Android avec détection tactile GestureDetector.
 */
public final class PillBarChartView extends View {
    /** Rappel émis lorsque le Parent touche la barre horizontale d'un enfant donné. */
    public interface OnChildTapListener { void onChildTap(ChildSpendingSummary child); }
    // Pinceau anticrénelé, détecteur tactile de tap simple, données enfants et zones cliquables hitRows.
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final GestureDetector detector;
    private final List<ChildSpendingSummary> children=new ArrayList<>();
    private final List<RectF> hitRows=new ArrayList<>();
    private OnChildTapListener listener;
    private int pendingChildTap=-1;
    private int forest,ink,mint,emerald,amber,coral,muted,trackColor;
    private float density;

    public PillBarChartView(Context context) { this(context,null); }

    /** Initialise couleurs, densité écran et GestureDetector traduisant un appui en performClick() accessible. */
    public PillBarChartView(Context context,android.util.AttributeSet attrs) {
        super(context,attrs); density=getResources().getDisplayMetrics().density;
        forest=context.getColor(R.color.forest); ink=context.getColor(R.color.ink); mint=context.getColor(R.color.mint);
        emerald=context.getColor(R.color.emerald); amber=context.getColor(R.color.amber); coral=context.getColor(R.color.coral); muted=context.getColor(R.color.muted_green); trackColor=context.getColor(R.color.card_border);
        // Vérifie quelle zone RectF d'enfant contient les coordonnées tactiles du clic.
        detector=new GestureDetector(context,new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapUp(MotionEvent event) {
                for(int i=0;i<hitRows.size();i++) if(hitRows.get(i).contains(event.getX(),event.getY())) { pendingChildTap=i; performClick(); return true; }
                return false;
            }
        }); setClickable(true);
    }

    /** Met à jour la liste d'enfants et recalcule la hauteur du composant en conséquence. */
    public void setChildren(List<ChildSpendingSummary> rows) { children.clear(); if(rows!=null) children.addAll(rows); requestLayout(); invalidate(); }
    /** Branche le listener notifié quand une ligne enfant est cliquée. */
    public void setOnChildTapListener(OnChildTapListener listener) { this.listener=listener; }

    /** Calcule dynamiquement la hauteur (68 dp par enfant + marges, minimum 100 dp). */
    @Override protected void onMeasure(int widthMeasureSpec,int heightMeasureSpec) {
        int desired=(int)((children.size()*68+10)*density)+getPaddingTop()+getPaddingBottom();
        int width=MeasureSpec.getSize(widthMeasureSpec); int height=resolveSize(Math.max(desired,(int)(100*density)),heightMeasureSpec); setMeasuredDimension(width,height);
    }

    /** Dessine chaque enfant (prénom, piste, jauge pilule colorée, ratio chiffré) et mémorise son rectangle tactile. */
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas); hitRows.clear(); float left=getPaddingLeft(),right=getWidth()-getPaddingRight(); float y=getPaddingTop()+8*density;
        for(ChildSpendingSummary child:children) {
            float startY=y; paint.setColor(forest); paint.setTextSize(14*density); paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD));
            canvas.drawText(child.nickname==null?"":child.nickname,left,y+12*density,paint);
            float trackTop=y+23*density,trackHeight=13*density;
            RectF track=new RectF(left,trackTop,right,trackTop+trackHeight); paint.setColor(trackColor); canvas.drawRoundRect(track,trackHeight/2,trackHeight/2,paint);
            int percent=MoneyMath.percentOf(child.spentMilli,child.allocatedTotalMilli);
            float fraction=Math.max(0,Math.min(100,percent))/100f; RectF fill=new RectF(left,trackTop,left+Math.max(trackHeight,track.width()*fraction),trackTop+trackHeight);
            if(fraction==0) fill.right=left;
            paint.setColor(child.spentMilli>child.allocatedTotalMilli?coral:MoneyMath.atLeastPercent(child.spentMilli,child.allocatedTotalMilli,80)?amber:mint); if(fraction>0) canvas.drawRoundRect(fill,trackHeight/2,trackHeight/2,paint);
            paint.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.NORMAL)); paint.setTextSize(11*density); paint.setColor(muted);
            String ratioLabel=CurrencyFormatter.formatMilli(child.spentMilli)+" / "+CurrencyFormatter.formatMilli(child.allocatedTotalMilli);
            canvas.drawText(ratioLabel,left,trackTop+trackHeight+16*density,paint);
            hitRows.add(new RectF(0,startY,right,trackTop+trackHeight+21*density)); y+=68*density;
        }
    }

    /** Déclenche OnChildTapListener pour l'enfant identifié par GestureDetector. */
    @Override public boolean performClick() {
        super.performClick();
        if(pendingChildTap>=0&&pendingChildTap<children.size()&&listener!=null)listener.onChildTap(children.get(pendingChildTap));
        pendingChildTap=-1;
        return true;
    }

    /** Transmet les événements tactiles à GestureDetector tout en préservant la gestion standard View. */
    @Override public boolean onTouchEvent(MotionEvent event) { return detector.onTouchEvent(event)||super.onTouchEvent(event); }
}
