/*
 * ============================================================================
 * FICHIER : PriceChartView.java
 * MISSION : Graphique 2D Canvas natif traçant l'évolution chronologique des prix unitaires d'un produit.
 * IMPLÉMENTE : Sous-classe de android.view.View avec calcul exact BigInteger et courbe quadratique lissée.
 * EFFET À L'ÉCRAN : Dessine grille 4 niveaux, libellés min/moyenne/max en haut, ligne pointillée ambre (moyenne),
 *                   courbe quadratique vert forêt et points menthe cerclés ; affiche état vide si aucun relevé.
 * COMMUNICATIONS : Embarqué dans activity_product_details.xml -> alimenté par ProductDetailsActivity.setHistory().
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

import com.example.masrouf1.db.MasroufEntities.PriceHistoryEntity;

import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Courbe chronologique d'évolution des prix tracée directement sur Canvas Android natif.
 */
public final class PriceChartView extends View {
    // Pinceaux graphiques et texte, série chronologique price_history et palette fixe.
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private List<PriceHistoryEntity> history=new ArrayList<>();
    private final int forest=0xff112b28,mint=0xffd9eedd,grid=0xffe2e7df,muted=0xff61746d;

    public PriceChartView(Context context){super(context);init();}
    public PriceChartView(Context context,AttributeSet attrs){super(context,attrs);init();}
    public PriceChartView(Context context,AttributeSet attrs,int style){super(context,attrs,style);init();}

    /** Configure le rendu logiciel nécessaire pour DashPathEffect, fond blanc et taille de texte axe. */
    private void init(){setBackgroundColor(0xffffffff);setLayerType(View.LAYER_TYPE_SOFTWARE,null);text.setTextSize(sp(11));text.setColor(muted);}

    /** Remplace la série affichée par les relevés price_history fournis et déclenche le redessin Canvas. */
    public void setHistory(List<PriceHistoryEntity> values){history=values==null?new ArrayList<>():new ArrayList<>(values);invalidate();}

    /** Dessine la grille, les axes, la courbe de Bézier, les points de prix et la ligne de moyenne. */
    @Override protected void onDraw(Canvas canvas){
        super.onDraw(canvas); float left=dp(58),right=getWidth()-dp(12),top=dp(25),bottom=getHeight()-dp(34);
        if(right<=left||bottom<=top)return;
        // Affiche un texte centré si aucun relevé d'achat n'existe pour ce produit.
        if(history.isEmpty()){text.setTextAlign(Paint.Align.CENTER);canvas.drawText(getResources().getString(R.string.price_history_empty),getWidth()/2f,(top+bottom)/2f,text);return;}
        // Calcule min, max, moyenne exacte BigInteger et marges hautes/basses (+-15 %) pour aérer la courbe.
        long min=Long.MAX_VALUE,max=Long.MIN_VALUE; BigInteger sum=BigInteger.ZERO;
        for(PriceHistoryEntity point:history){min=Math.min(min,point.priceMilli);max=Math.max(max,point.priceMilli);sum=sum.add(BigInteger.valueOf(point.priceMilli));}
        BigInteger average=sum.divide(BigInteger.valueOf(history.size()));
        BigInteger minimum=BigInteger.valueOf(min),maximum=BigInteger.valueOf(max),span=maximum.subtract(minimum);
        BigInteger minimumSpan=maximum.abs().multiply(BigInteger.valueOf(18L)).divide(BigInteger.valueOf(100L)).max(BigInteger.valueOf(1_000L));
        if(span.compareTo(minimumSpan)<0)span=minimumSpan;
        BigInteger low=minimum.subtract(span.multiply(BigInteger.valueOf(15L)).divide(BigInteger.valueOf(100L))).max(BigInteger.ZERO);
        BigInteger high=maximum.add(span.multiply(BigInteger.valueOf(15L)).divide(BigInteger.valueOf(100L))).min(BigInteger.valueOf(Long.MAX_VALUE));
        if(high.compareTo(low)<=0)high=low.add(BigInteger.ONE);
        BigInteger plotSpan=high.subtract(low);

        // Dessine l'en-tête statistique et les 4 repères horizontaux Y.
        text.setTextAlign(Paint.Align.LEFT);text.setTextSize(sp(10));
        canvas.drawText(getResources().getString(R.string.chart_price_stats,CurrencyFormatter.formatMilli(min),CurrencyFormatter.formatMilli(average.longValueExact()),CurrencyFormatter.formatMilli(max)),left,dp(13),text);
        text.setTextSize(sp(10));paint.setColor(grid);paint.setStrokeWidth(dp(1));paint.setStyle(Paint.Style.STROKE);
        for(int i=0;i<4;i++){
            float fraction=i/3f,y=top+(bottom-top)*fraction;canvas.drawLine(left,y,right,y,paint);
            BigInteger value=high.subtract(plotSpan.multiply(BigInteger.valueOf(i)).divide(BigInteger.valueOf(3L)));
            text.setTextAlign(Paint.Align.RIGHT);String label=CurrencyFormatter.formatMilli(value.longValueExact()).replace(" DT","");canvas.drawText(label,left-dp(5),y+dp(4),text);
        }
        // Trace les axes gauche/bas puis la courbe quadratique lissée reliant chaque point chronologique.
        canvas.drawLine(left,top,left,bottom,paint);canvas.drawLine(left,bottom,right,bottom,paint);
        float step=history.size()==1?0:(right-left)/(history.size()-1f);Path line=new Path();float[] xs=new float[history.size()],ys=new float[history.size()];
        for(int i=0;i<history.size();i++){
            xs[i]=left+step*i;ys[i]=bottom-fraction(BigInteger.valueOf(history.get(i).priceMilli).subtract(low),plotSpan)*(bottom-top);
            if(i==0)line.moveTo(xs[i],ys[i]);else{float mid=(xs[i-1]+xs[i])/2f;line.quadTo(mid,ys[i-1],xs[i],(ys[i-1]+ys[i])/2f);if(i==history.size()-1)line.quadTo(xs[i],ys[i],xs[i],ys[i]);}
        }
        paint.setColor(forest);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(3));paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);canvas.drawPath(line,paint);
        // Dessine les points circulaires (remplissage menthe + bordure forêt) puis la ligne pointillée ambre de moyenne.
        for(int i=0;i<history.size();i++){paint.setStyle(Paint.Style.FILL);paint.setColor(mint);canvas.drawCircle(xs[i],ys[i],dp(5),paint);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(dp(1.5f));paint.setColor(forest);canvas.drawCircle(xs[i],ys[i],dp(5),paint);}
        float averageY=bottom-fraction(average.subtract(low),plotSpan)*(bottom-top);paint.setColor(0xffe9b66b);paint.setStrokeWidth(dp(1));paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{dp(4),dp(4)},0));canvas.drawLine(left,averageY,right,averageY,paint);paint.setPathEffect(null);paint.setStyle(Paint.Style.FILL);
        // Affiche première et dernière date (dd/MM) sous l'axe horizontal.
        text.setTextSize(sp(11));text.setTextAlign(Paint.Align.LEFT);canvas.drawText(new SimpleDateFormat("dd/MM",Locale.getDefault()).format(new Date(history.get(0).observedAt)),left,bottom+dp(22),text);text.setTextAlign(Paint.Align.RIGHT);canvas.drawText(new SimpleDateFormat("dd/MM",Locale.getDefault()).format(new Date(history.get(history.size()-1).observedAt)),right,bottom+dp(22),text);
    }

    /** Convertit un ratio BigInteger en fraction normalisée [0..1] sans risque d'overflow. */
    private float fraction(BigInteger numerator,BigInteger denominator){if(denominator.signum()<=0)return 0;return Math.max(0f,Math.min(1f,numerator.floatValue()/denominator.floatValue()));}
    private float dp(float value){return value*getResources().getDisplayMetrics().density;} private float sp(float value){return value*getResources().getDisplayMetrics().scaledDensity;}
}
