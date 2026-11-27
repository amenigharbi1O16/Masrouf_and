/*
 * ============================================================================
 * FICHIER : AnalyticsRenderer.java
 * MISSION : Générateur visuel 100 % programmatique (vues Java créées par code)
 *           rendant le tableau complet d'analyses budgétaires selon le profil :
 *           - USER / CHILD : carte héroïque, conclusion et recommandation,
 *             comparaison temporelle, jauge de consommation, graphique 6 mois
 *             PillBarChart, détail par catégorie, rythme quotidien, alerte
 *             catégorie et historique des achats/dépenses de la période.
 *           - PARENT : budget mensuel familial, dépassement d'enveloppes,
 *             graphique 6 mois familial, progression agrégée confidentielle des
 *             enfants (sans exposer leurs achats privés) et graphique enfant.
 * IMPLÉMENTE : Méthodes factory statiques construisant MaterialCardView,
 *              ProgressBar horizontales, PillBarChart, badges arrondis
 *              GradientDrawable, boutons et séparateurs sans fichier XML dédié.
 * EFFET À L'ÉCRAN : Ensemble des cartes, barres, icônes et graphiques affichés
 *                   dans AnalyticsFragment et FamilyBudgetsAnalyticsActivity.
 * COMMUNICATIONS :
 *   ← AnalyticsFragment (USER / CHILD / PARENT central tab)
 *   ← FamilyBudgetsAnalyticsActivity (écran Parent dédié depuis le tiroir)
 *   ← AnalyticsSummaryData (données Room agrégées)
 *   → PillBarChart, BudgetPeriodMath, CurrencyFormatter, MoneyMath,
 *     CategoryIconResources
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.masrouf1.db.MasroufEntities.BudgetEntity;
import com.example.masrouf1.db.TransactionRow;
import com.example.masrouf1.ui.CategoryIconResources;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Générateur visuel 100 % programmatique (vues Java : MaterialCardView, ProgressBar, PillBarChart, TextView)
 * rendant le tableau d'analyses pour Normal User, Child ou Parent (enveloppes, tendance 6 mois, enfants).
 */
/**
 * Shared programmatic renderer for role-specific Analytics screens (Normal User, Child, Parent),
 * faithfully matching React Native's `analyses.tsx` and `(admin)/budget-analyses.tsx`.
 */
public final class AnalyticsRenderer {
    public interface Actions {
        void onOpenCalendar();
        void onCreateOrEditBudget(BudgetEntity currentBudget);
        void onOpenTransaction(TransactionRow row);
    }

    private AnalyticsRenderer() { }

    public static void render(Context context, LinearLayout container, AnalyticsSummaryData data, Actions actions) {
        container.removeAllViews();
        if (data == null) return;
        float density = context.getResources().getDisplayMetrics().density;
        if (data.role == AppRole.PARENT) {
            renderParent(context, container, data, actions, density);
        } else {
            renderUserOrChild(context, container, data, actions, density);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // NORMAL USER & CHILD DASHBOARD (7 Sections + Breakdown & Purchase History)
    // ════════════════════════════════════════════════════════════════════════
    private static void renderUserOrChild(
            Context ctx,
            LinearLayout root,
            AnalyticsSummaryData data,
            Actions actions,
            float d) {
        Locale locale = Locale.getDefault();
        BudgetEntity budget = data.budget;

        if (budget == null) {
            // Empty state matching screenshot (Aucun budget défini pour cette période + Créer un budget)
            MaterialCardView emptyCard = card(ctx, d, R.color.alabaster, R.color.card_border, 1, 20, 22);
            LinearLayout body = vertical(ctx);
            body.setGravity(Gravity.CENTER_HORIZONTAL);

            ImageView icon = new ImageView(ctx);
            icon.setImageResource(R.drawable.ic_pie_chart);
            icon.setColorFilter(ctx.getColor(R.color.emerald));
            LinearLayout iconCircle = new LinearLayout(ctx);
            iconCircle.setGravity(Gravity.CENTER);
            GradientDrawable circleBg = new GradientDrawable();
            circleBg.setShape(GradientDrawable.OVAL);
            circleBg.setColor(ctx.getColor(R.color.mint_light));
            iconCircle.setBackground(circleBg);
            iconCircle.addView(icon, new LinearLayout.LayoutParams(dp(d, 26), dp(d, 26)));
            body.addView(iconCircle, new LinearLayout.LayoutParams(dp(d, 56), dp(d, 56)));

            TextView title = boldText(ctx, ctx.getString(R.string.analytics_empty_budget_title), 17, R.color.forest);
            title.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2);
            tp.topMargin = dp(d, 14);
            body.addView(title, tp);

            TextView desc = mutedText(ctx, ctx.getString(R.string.analytics_empty_budget_desc), 13);
            desc.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams dpParams = new LinearLayout.LayoutParams(-1, -2);
            dpParams.topMargin = dp(d, 6);
            body.addView(desc, dpParams);

            MaterialButton cta = primaryButton(ctx, ctx.getString(R.string.analytics_create_budget_cta), R.color.emerald, R.color.alabaster, d);
            cta.setOnClickListener(v -> actions.onCreateOrEditBudget(null));
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, dp(d, 50));
            bp.topMargin = dp(d, 16);
            body.addView(cta, bp);

            emptyCard.addView(body);
            root.addView(emptyCard, marginBottom(d, 14));

            // Still render 6-month historical pill chart & any recorded period transactions if user already logged items
            renderPillChartSection(ctx, root, data, d);
            renderPurchasesAndHistoryCard(ctx, root, data, actions, d);
            return;
        }

        // Clickable Period Selector
        String rangeLabel = BudgetPeriodMath.formatRange(budget.periodStart, budget.periodEnd, locale);
        MaterialCardView periodCard = card(ctx, d, R.color.alabaster, R.color.card_border, 1, 16, 14);
        LinearLayout periodRow = horizontal(ctx);
        ImageView calIcon = new ImageView(ctx);
        calIcon.setImageResource(R.drawable.ic_calendar);
        calIcon.setColorFilter(ctx.getColor(R.color.emerald));
        periodRow.addView(calIcon, new LinearLayout.LayoutParams(dp(d, 20), dp(d, 20)));
        LinearLayout periodCopy = vertical(ctx);
        periodCopy.setPaddingRelative(dp(d, 12), 0, dp(d, 8), 0);
        periodCopy.addView(boldText(ctx, rangeLabel, 14, R.color.forest));
        periodCopy.addView(mutedText(ctx, ctx.getString(R.string.analytics_select_period), 12));
        periodRow.addView(periodCopy, new LinearLayout.LayoutParams(0, -2, 1));
        TextView chev = mutedText(ctx, "›", 20);
        chev.setTypeface(null, Typeface.BOLD);
        periodRow.addView(chev);
        periodCard.addView(periodRow);
        periodCard.setOnClickListener(v -> actions.onOpenCalendar());
        root.addView(periodCard, marginBottom(d, 14));

        // 1. Hero Dark Forest Budget Synthesis Card
        long curSpent = data.currentSpentMilli;
        long totalBudget = budget.monthlyLimitMilli;
        boolean isExceeded = totalBudget > 0L && curSpent > totalBudget;
        long remainingBudget = Math.max(0L, MoneyMath.subtract(totalBudget, curSpent));
        long exceededAmount = Math.max(0L, MoneyMath.subtract(curSpent, totalBudget));
        double consumptionPct = totalBudget > 0L ? (curSpent * 100.0) / totalBudget : 0.0;

        MaterialCardView heroCard = card(ctx, d, R.color.forest, R.color.forest, 0, 20, 20);
        LinearLayout heroBody = vertical(ctx);
        LinearLayout heroTop = horizontal(ctx);
        TextView eyebrow = labelText(ctx, ctx.getString(R.string.analytics_hero_eyebrow), 11, R.color.mint_accent, true);
        heroTop.addView(eyebrow, new LinearLayout.LayoutParams(0, -2, 1));
        String statusBadgeText = isExceeded
                ? ctx.getString(R.string.analytics_budget_exceeded).toUpperCase(locale)
                : String.format(Locale.US, "%.1f%%", consumptionPct);
        heroTop.addView(badgePill(ctx, d, statusBadgeText,
                isExceeded ? Color.argb(68, 220, 38, 38) : Color.argb(45, 255, 255, 255),
                isExceeded ? Color.parseColor("#FF8A80") : ctx.getColor(R.color.mint)));
        heroBody.addView(heroTop);

        TextView heroAmount = labelText(ctx, CurrencyFormatter.formatMilli(curSpent), 30,
                R.color.alabaster, true);
        if (isExceeded) heroAmount.setTextColor(Color.parseColor("#FF8A80"));
        LinearLayout.LayoutParams hap = new LinearLayout.LayoutParams(-1, -2);
        hap.topMargin = dp(d, 10);
        heroBody.addView(heroAmount, hap);

        TextView heroSub = labelText(ctx, ctx.getString(R.string.analytics_amount_spent_sub, rangeLabel), 12,
                R.color.mint_accent, false);
        heroSub.setAlpha(0.9f);
        heroBody.addView(heroSub);

        LinearLayout heroGrid = horizontal(ctx);
        heroGrid.setPadding(0, dp(d, 14), 0, 0);
        heroGrid.addView(heroMetricCol(ctx, ctx.getString(R.string.analytics_budget_total),
                CurrencyFormatter.formatMilli(totalBudget), R.color.alabaster, false),
                new LinearLayout.LayoutParams(0, -2, 1));
        heroGrid.addView(heroMetricCol(ctx,
                isExceeded ? ctx.getString(R.string.analytics_budget_exceeded) : ctx.getString(R.string.analytics_budget_remaining),
                isExceeded ? "+" + CurrencyFormatter.formatMilli(exceededAmount) : CurrencyFormatter.formatMilli(remainingBudget),
                R.color.alabaster, isExceeded),
                new LinearLayout.LayoutParams(0, -2, 1));
        heroGrid.addView(heroMetricCol(ctx, ctx.getString(R.string.analytics_percent_consumed),
                String.format(Locale.US, "%.1f%%", consumptionPct),
                R.color.alabaster, isExceeded),
                new LinearLayout.LayoutParams(0, -2, 1));
        heroBody.addView(heroGrid);
        heroCard.addView(heroBody);
        root.addView(heroCard, marginBottom(d, 14));

        // 2. Conclusion financière & Recommandation personnalisée
        long prevSpent = data.previousSpentMilli;
        long diff = curSpent - prevSpent;
        boolean isSaving = diff < 0L;
        boolean isStable = Math.abs(diff) < 1L;
        String[] insights = computeInsights(ctx, data, totalBudget, curSpent, remainingBudget, exceededAmount, consumptionPct, isExceeded, prevSpent, isSaving);

        MaterialCardView insightCard = card(ctx, d, R.color.alabaster, R.color.card_border, 1, 18, 16);
        LinearLayout insightBody = vertical(ctx);
        insightBody.addView(boldText(ctx, "✦ " + ctx.getString(R.string.analytics_conclusion_title), 14, R.color.forest));
        TextView conclusionView = mutedText(ctx, insights[0], 13);
        conclusionView.setTextColor(ctx.getColor(R.color.ink));
        conclusionView.setPadding(0, dp(d, 6), 0, dp(d, 10));
        insightBody.addView(conclusionView);
        insightBody.addView(divider(ctx, d));
        TextView recTitle = boldText(ctx, "◉ " + ctx.getString(R.string.analytics_recommendation_title), 14, R.color.emerald);
        recTitle.setPadding(0, dp(d, 10), 0, dp(d, 4));
        insightBody.addView(recTitle);
        TextView recView = mutedText(ctx, insights[1], 13);
        recView.setTextColor(ctx.getColor(R.color.ink));
        insightBody.addView(recView);
        insightCard.addView(insightBody);
        root.addView(insightCard, marginBottom(d, 16));

        // 3. Comparaison temporelle
        root.addView(sectionHeader(ctx, d, ctx.getString(R.string.analytics_temporal_title)));
        MaterialCardView compCard = card(ctx, d, R.color.alabaster, R.color.card_border, 1, 16, 16);
        LinearLayout compBody = vertical(ctx);
        LinearLayout compRow = horizontal(ctx);
        LinearLayout leftCol = vertical(ctx);
        leftCol.addView(labelText(ctx, ctx.getString(R.string.analytics_this_period_upper), 11, R.color.muted_green, true));
        leftCol.addView(mutedText(ctx, ctx.getString(R.string.analytics_date_range_sep,
                BudgetPeriodMath.formatIsoDate(budget.periodStart), BudgetPeriodMath.formatIsoDate(budget.periodEnd)), 11));
        TextView curAmtV = boldText(ctx, CurrencyFormatter.formatMilli(curSpent), 16, R.color.forest);
        curAmtV.setPadding(0, dp(d, 4), 0, 0);
        leftCol.addView(curAmtV);
        compRow.addView(leftCol, new LinearLayout.LayoutParams(0, -2, 1));

        View vDiv = new View(ctx);
        vDiv.setBackgroundColor(ctx.getColor(R.color.card_border));
        LinearLayout.LayoutParams vdp = new LinearLayout.LayoutParams(dp(d, 1), -1);
        vdp.setMargins(dp(d, 10), 0, dp(d, 10), 0);
        compRow.addView(vDiv, vdp);

        LinearLayout rightCol = vertical(ctx);
        rightCol.addView(labelText(ctx, ctx.getString(R.string.analytics_prev_period_upper), 11, R.color.muted_green, true));
        rightCol.addView(mutedText(ctx, ctx.getString(R.string.analytics_date_range_sep,
                BudgetPeriodMath.formatIsoDate(data.prevWindowStartMillis), BudgetPeriodMath.formatIsoDate(data.prevWindowEndMillis)), 11));
        TextView prevAmtV = boldText(ctx, CurrencyFormatter.formatMilli(prevSpent), 16, R.color.forest);
        prevAmtV.setPadding(0, dp(d, 4), 0, 0);
        rightCol.addView(prevAmtV);
        compRow.addView(rightCol, new LinearLayout.LayoutParams(0, -2, 1));
        compBody.addView(compRow);

        String trendMsg;
        int trendBg;
        int trendFg;
        if (prevSpent == 0L) {
            trendMsg = ctx.getString(R.string.analytics_no_prev_spend);
            trendBg = ctx.getColor(R.color.mint_light);
            trendFg = ctx.getColor(R.color.forest);
        } else if (isSaving) {
            double pDiff = Math.abs(diff) * 100.0 / prevSpent;
            trendMsg = ctx.getString(R.string.analytics_trend_down, String.format(Locale.US, "%.1f", pDiff), CurrencyFormatter.formatMilli(Math.abs(diff)));
            trendBg = ctx.getColor(R.color.success_surface);
            trendFg = ctx.getColor(R.color.emerald);
        } else if (isStable) {
            trendMsg = ctx.getString(R.string.analytics_trend_flat);
            trendBg = ctx.getColor(R.color.cream);
            trendFg = ctx.getColor(R.color.muted_green);
        } else {
            double pDiff = Math.abs(diff) * 100.0 / prevSpent;
            trendMsg = ctx.getString(R.string.analytics_trend_up, String.format(Locale.US, "%.1f", pDiff), CurrencyFormatter.formatMilli(Math.abs(diff)));
            trendBg = ctx.getColor(R.color.danger_surface);
            trendFg = ctx.getColor(R.color.danger_text);
        }
        TextView trendPill = new TextView(ctx);
        trendPill.setText(trendMsg);
        trendPill.setTextSize(12);
        trendPill.setTextColor(trendFg);
        trendPill.setPadding(dp(d, 12), dp(d, 9), dp(d, 12), dp(d, 9));
        GradientDrawable tBg = new GradientDrawable();
        tBg.setColor(trendBg);
        tBg.setCornerRadius(dp(d, 10));
        trendPill.setBackground(tBg);
        LinearLayout.LayoutParams tpp = new LinearLayout.LayoutParams(-1, -2);
        tpp.topMargin = dp(d, 12);
        compBody.addView(trendPill, tpp);
        compCard.addView(compBody);
        root.addView(compCard, marginBottom(d, 16));

        // 4. Consommation du budget gauge
        root.addView(sectionHeader(ctx, d, ctx.getString(R.string.analytics_gauge_title)));
        MaterialCardView gaugeCard = card(ctx, d, R.color.alabaster, R.color.card_border, 1, 16, 16);
        LinearLayout gaugeBody = vertical(ctx);
        LinearLayout gaugeTop = horizontal(ctx);
        LinearLayout gLeft = vertical(ctx);
        gLeft.addView(mutedText(ctx, ctx.getString(R.string.analytics_gauge_spent_label), 12));
        gLeft.addView(boldText(ctx,
                CurrencyFormatter.formatMilli(curSpent) + " (" + String.format(Locale.US, "%.1f%%", consumptionPct) + ")",
                14, isExceeded ? R.color.coral : R.color.forest));
        gaugeTop.addView(gLeft, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout gRight = vertical(ctx);
        gRight.setGravity(Gravity.END);
        gRight.addView(mutedText(ctx, ctx.getString(R.string.analytics_gauge_ceiling_label), 12));
        gRight.addView(boldText(ctx, CurrencyFormatter.formatMilli(totalBudget), 14, R.color.forest));
        gaugeTop.addView(gRight);
        gaugeBody.addView(gaugeTop);

        int barColor = isExceeded ? R.color.coral : (consumptionPct >= 80.0 ? R.color.amber : R.color.emerald);
        gaugeBody.addView(progressTrack(ctx, d, (int) Math.min(100L, Math.round(consumptionPct)), barColor, 10, 10));

        LinearLayout gaugeFoot = horizontal(ctx);
        gaugeFoot.setPadding(0, dp(d, 8), 0, 0);
        TextView gfLeft = mutedText(ctx,
                isExceeded ? ctx.getString(R.string.analytics_gauge_over_footer, CurrencyFormatter.formatMilli(exceededAmount))
                        : ctx.getString(R.string.analytics_gauge_avail_footer, CurrencyFormatter.formatMilli(remainingBudget)),
                12);
        if (isExceeded) gfLeft.setTextColor(ctx.getColor(R.color.coral));
        gaugeFoot.addView(gfLeft, new LinearLayout.LayoutParams(0, -2, 1));
        gaugeFoot.addView(boldText(ctx, ctx.getString(R.string.analytics_days_remaining, data.daysRemaining), 12, R.color.forest));
        gaugeBody.addView(gaugeFoot);
        gaugeCard.addView(gaugeBody);
        root.addView(gaugeCard, marginBottom(d, 14));

        // 4b. 6-month Pill Bar Chart
        renderPillChartSection(ctx, root, data, d);

        // 5. Consommation par catégorie (Croissante)
        root.addView(sectionHeader(ctx, d, ctx.getString(R.string.analytics_cat_section_asc)));
        if (data.categoryStats.isEmpty()) {
            MaterialCardView noCat = card(ctx, d, R.color.alabaster, R.color.card_border, 1, 16, 16);
            TextView tv = mutedText(ctx, ctx.getString(R.string.analytics_cat_empty), 13);
            tv.setGravity(Gravity.CENTER);
            noCat.addView(tv);
            root.addView(noCat, marginBottom(d, 16));
        } else {
            for (AnalyticsSummaryData.CategoryStat cat : data.categoryStats) {
                boolean isOverCat = cat.hasBudget && cat.consumptionPct > 100.0;
                int catColorRes = cat.hasBudget
                        ? (cat.consumptionPct >= 100.0 ? R.color.coral : (cat.consumptionPct >= 70.0 ? R.color.amber : R.color.emerald))
                        : (cat.shareOfTotal >= 35.0 ? R.color.amber : R.color.emerald);
                MaterialCardView cCard = card(ctx, d,
                        isOverCat ? R.color.danger_surface : R.color.alabaster,
                        isOverCat ? R.color.coral : R.color.card_border,
                        1, 16, 14);
                LinearLayout cBody = vertical(ctx);

                LinearLayout cHeader = horizontal(ctx);
                ImageView cIcon = new ImageView(ctx);
                cIcon.setImageResource(CategoryIconResources.resolve(cat.iconName, "expense"));
                cIcon.setColorFilter(ctx.getColor(catColorRes));
                cHeader.addView(cIcon, new LinearLayout.LayoutParams(dp(d, 18), dp(d, 18)));
                TextView cName = boldText(ctx, cat.name, 14, isOverCat ? R.color.danger_text : R.color.forest);
                cName.setPaddingRelative(dp(d, 8), 0, dp(d, 6), 0);
                cHeader.addView(cName, new LinearLayout.LayoutParams(0, -2, 1));
                if (isOverCat) {
                    cHeader.addView(badgePill(ctx, d, ctx.getString(R.string.parent_cat_exceeded_badge).toUpperCase(locale),
                            ctx.getColor(R.color.danger_surface), ctx.getColor(R.color.danger_text)));
                }
                TextView cAmt = boldText(ctx, CurrencyFormatter.formatMilli(cat.spentMilli), 14,
                        isOverCat ? R.color.danger_text : R.color.forest);
                cAmt.setPaddingRelative(dp(d, 8), 0, 0, 0);
                cHeader.addView(cAmt);
                cBody.addView(cHeader);

                int ratio = (int) Math.min(100L, Math.round(cat.hasBudget ? cat.consumptionPct : cat.shareOfTotal));
                cBody.addView(progressTrack(ctx, d, ratio, catColorRes, 7, 8));

                LinearLayout cFooter = horizontal(ctx);
                cFooter.setPadding(0, dp(d, 7), 0, 0);
                if (cat.hasBudget) {
                    String leftStr = isOverCat
                            ? "+" + CurrencyFormatter.formatMilli(MoneyMath.subtract(cat.spentMilli, cat.allocatedMilli))
                                + " (" + String.format(Locale.US, "%.1f%%", cat.consumptionPct) + ")"
                            : ctx.getString(R.string.analytics_cat_pct_of_env, String.format(Locale.US, "%.1f", cat.consumptionPct));
                    TextView fl = labelText(ctx, leftStr, 12, catColorRes, true);
                    cFooter.addView(fl, new LinearLayout.LayoutParams(0, -2, 1));
                    cFooter.addView(mutedText(ctx, ctx.getString(R.string.analytics_cat_envelope,
                            CurrencyFormatter.formatMilli(cat.allocatedMilli)), 12));
                } else {
                    cFooter.addView(mutedText(ctx, ctx.getString(R.string.analytics_cat_share_of_total,
                            String.format(Locale.US, "%.1f", cat.shareOfTotal)), 12),
                            new LinearLayout.LayoutParams(0, -2, 1));
                    TextView unalloc = mutedText(ctx, "(" + ctx.getString(R.string.analytics_cat_no_budget) + ")", 12);
                    unalloc.setTypeface(null, Typeface.ITALIC);
                    cFooter.addView(unalloc);
                }
                cBody.addView(cFooter);
                cCard.addView(cBody);
                root.addView(cCard, marginBottom(d, 10));
            }
            Spacer(ctx, root, d, 6);
        }

        // 6. Rythme de dépenses quotidien
        root.addView(sectionHeader(ctx, d, ctx.getString(R.string.analytics_pace_section)));
        MaterialCardView paceCard = card(ctx, d, R.color.alabaster, R.color.card_border, 1, 16, 16);
        LinearLayout paceBody = vertical(ctx);
        LinearLayout paceGrid = horizontal(ctx);
        paceGrid.addView(heroMetricCol(ctx, ctx.getString(R.string.analytics_pace_avg),
                CurrencyFormatter.formatMilli(data.avgDailySpendMilli), R.color.forest, false),
                new LinearLayout.LayoutParams(0, -2, 1));
        paceGrid.addView(heroMetricCol(ctx, ctx.getString(R.string.analytics_pace_elapsed),
                ctx.getString(R.string.analytics_pace_days_ratio, data.daysElapsed, data.totalDays), R.color.forest, false),
                new LinearLayout.LayoutParams(0, -2, 1));
        paceGrid.addView(heroMetricCol(ctx, ctx.getString(R.string.analytics_pace_remaining),
                ctx.getString(R.string.analytics_pace_days_short, data.daysRemaining), R.color.forest, false),
                new LinearLayout.LayoutParams(0, -2, 1));
        paceBody.addView(paceGrid);
        paceBody.addView(spaceDivider(ctx, d));

        LinearLayout projRow = horizontal(ctx);
        LinearLayout projLeft = vertical(ctx);
        projLeft.addView(mutedText(ctx, ctx.getString(R.string.analytics_pace_est_label), 12));
        projLeft.addView(boldText(ctx, CurrencyFormatter.formatMilli(data.projectedTotalMilli), 17,
                data.paceSustainable ? R.color.forest : R.color.coral));
        projRow.addView(projLeft, new LinearLayout.LayoutParams(0, -2, 1));
        projRow.addView(badgePill(ctx, d,
                ctx.getString(data.paceSustainable ? R.string.analytics_pace_sustainable : R.string.analytics_pace_risk),
                ctx.getColor(data.paceSustainable ? R.color.success_surface : R.color.danger_surface),
                ctx.getColor(data.paceSustainable ? R.color.emerald : R.color.danger_text)));
        paceBody.addView(projRow);

        TextView paceMsg = mutedText(ctx, ctx.getString(
                data.paceSustainable ? R.string.analytics_pace_ok_msg : R.string.analytics_pace_risk_msg,
                CurrencyFormatter.formatMilli(data.avgDailySpendMilli),
                CurrencyFormatter.formatMilli(data.projectedDiffMilli)), 12);
        paceMsg.setTextColor(ctx.getColor(data.paceSustainable ? R.color.muted_green : R.color.danger_text));
        paceMsg.setPadding(0, dp(d, 10), 0, 0);
        paceBody.addView(paceMsg);
        paceCard.addView(paceBody);
        root.addView(paceCard, marginBottom(d, 16));

        // 7. Catégorie requérant attention
        root.addView(sectionHeader(ctx, d, ctx.getString(R.string.analytics_attention_section)));
        AnalyticsSummaryData.CategoryStat attn = data.attentionCategory;
        if (attn != null) {
            MaterialCardView attnCard = card(ctx, d, R.color.danger_surface, R.color.coral, 1, 16, 16);
            LinearLayout attnBody = vertical(ctx);
            LinearLayout attnTop = horizontal(ctx);
            TextView attnName = boldText(ctx, attn.name, 15, R.color.danger_text);
            attnTop.addView(attnName, new LinearLayout.LayoutParams(0, -2, 1));
            attnTop.addView(badgePill(ctx, d,
                    attn.hasBudget
                            ? ctx.getString(R.string.analytics_attention_pct_consumed, (int) Math.round(attn.consumptionPct))
                            : ctx.getString(R.string.analytics_attention_pct_total, (int) Math.round(attn.shareOfTotal)),
                    Color.argb(35, 220, 38, 38), ctx.getColor(R.color.danger_text)));
            attnBody.addView(attnTop);

            TextView reason = mutedText(ctx, attn.hasBudget
                    ? ctx.getString(R.string.analytics_attention_reason_budget,
                            String.format(Locale.US, "%.1f", attn.consumptionPct),
                            CurrencyFormatter.formatMilli(attn.spentMilli),
                            CurrencyFormatter.formatMilli(attn.allocatedMilli))
                    : ctx.getString(R.string.analytics_attention_reason_unalloc,
                            String.format(Locale.US, "%.1f", attn.shareOfTotal),
                            CurrencyFormatter.formatMilli(attn.spentMilli)), 13);
            reason.setTextColor(ctx.getColor(R.color.ink));
            reason.setPadding(0, dp(d, 8), 0, dp(d, 10));
            attnBody.addView(reason);

            MaterialCardView sugBox = card(ctx, d, R.color.alabaster, R.color.coral, 1, 12, 12);
            LinearLayout sugBody = vertical(ctx);
            sugBody.addView(boldText(ctx, ctx.getString(R.string.analytics_suggestion_title), 13, R.color.forest));
            TextView sugText = mutedText(ctx, attn.hasBudget
                    ? ctx.getString(R.string.analytics_suggestion_budget)
                    : ctx.getString(R.string.analytics_suggestion_unalloc, attn.name), 12);
            sugText.setTextColor(ctx.getColor(R.color.ink));
            sugText.setPadding(0, dp(d, 4), 0, 0);
            sugBody.addView(sugText);
            sugBox.addView(sugBody);
            attnBody.addView(sugBox);
            attnCard.addView(attnBody);
            root.addView(attnCard, marginBottom(d, 16));
        } else {
            MaterialCardView okCard = card(ctx, d, R.color.success_surface, R.color.mint_accent, 1, 16, 16);
            LinearLayout okBody = vertical(ctx);
            okBody.addView(boldText(ctx, "✓ " + ctx.getString(R.string.analytics_healthy_title), 15, R.color.emerald));
            TextView okSub = mutedText(ctx, ctx.getString(R.string.analytics_healthy_body), 13);
            okSub.setTextColor(ctx.getColor(R.color.ink));
            okSub.setPadding(0, dp(d, 6), 0, 0);
            okBody.addView(okSub);
            okCard.addView(okBody);
            root.addView(okCard, marginBottom(d, 16));
        }

        // 8. Purchases vs Expenses Breakdown & Period History (User & Child strictly isolated!)
        renderPurchasesAndHistoryCard(ctx, root, data, actions, d);
    }

    // ════════════════════════════════════════════════════════════════════════
    // PARENT DASHBOARD (Household Month Budget, Categories, 6m Chart, Children)
    // ════════════════════════════════════════════════════════════════════════
    private static void renderParent(
            Context ctx,
            LinearLayout root,
            AnalyticsSummaryData data,
            Actions actions,
            float d) {
        Locale locale = Locale.getDefault();
        BudgetEntity budget = data.budget;
        long totalBudgeted = budget == null ? 0L : budget.monthlyLimitMilli;
        long totalSpent = data.currentSpentMilli;
        int percentUsed = totalBudgeted > 0L ? Math.min(100, Math.max(0, MoneyMath.percentOf(totalSpent, totalBudgeted))) : 0;
        long remaining = totalBudgeted > 0L ? Math.max(0L, MoneyMath.subtract(totalBudgeted, totalSpent)) : 0L;
        boolean isOverBudget = totalBudgeted > 0L && totalSpent > totalBudgeted;

        String monthKey;
        if (budget != null) {
            monthKey = BudgetPeriodMath.formatIsoDate(budget.periodStart).substring(0, 7);
        } else {
            monthKey = BudgetPeriodMath.formatIsoDate(System.currentTimeMillis()).substring(0, 7);
        }

        TextView ey = labelText(ctx, ctx.getString(R.string.parent_analytics_eyebrow), 11, R.color.muted_green, true);
        ey.setPadding(0, 0, 0, dp(d, 8));
        root.addView(ey);

        // Household Hero Card
        MaterialCardView hero = card(ctx, d, R.color.forest, R.color.forest, 0, 20, 18);
        LinearLayout hBody = vertical(ctx);
        LinearLayout hTop = horizontal(ctx);
        LinearLayout hLeft = vertical(ctx);
        hLeft.addView(labelText(ctx, ctx.getString(R.string.parent_month_budget_label, monthKey), 11, R.color.mint_accent, true));
        TextView totalTv = labelText(ctx, CurrencyFormatter.formatMilli(totalBudgeted), 26, R.color.alabaster, true);
        totalTv.setPadding(0, dp(d, 4), 0, 0);
        hLeft.addView(totalTv);
        hTop.addView(hLeft, new LinearLayout.LayoutParams(0, -2, 1));

        MaterialButton adjustBtn = new MaterialButton(ctx);
        adjustBtn.setText(ctx.getString(R.string.parent_adjust_cta));
        adjustBtn.setAllCaps(false);
        adjustBtn.setTextColor(ctx.getColor(R.color.mint));
        adjustBtn.setBackgroundTintList(ColorStateList.valueOf(ctx.getColor(R.color.emerald)));
        adjustBtn.setCornerRadius(dp(d, 14));
        adjustBtn.setMinHeight(dp(d, 38));
        adjustBtn.setOnClickListener(v -> actions.onCreateOrEditBudget(budget));
        hTop.addView(adjustBtn);
        hBody.addView(hTop);

        hBody.addView(progressTrack(ctx, d, percentUsed, isOverBudget ? R.color.coral : R.color.emerald, 10, 12));

        LinearLayout legend = horizontal(ctx);
        legend.setPadding(0, dp(d, 8), 0, dp(d, 12));
        TextView legLeft = labelText(ctx,
                ctx.getString(R.string.parent_legend_spent, CurrencyFormatter.formatMilli(totalSpent), percentUsed),
                12, R.color.alabaster, true);
        legend.addView(legLeft, new LinearLayout.LayoutParams(0, -2, 1));
        TextView legRight = labelText(ctx,
                isOverBudget
                        ? ctx.getString(R.string.parent_legend_over, CurrencyFormatter.formatMilli(MoneyMath.subtract(totalSpent, totalBudgeted)))
                        : ctx.getString(R.string.parent_legend_remaining, CurrencyFormatter.formatMilli(remaining)),
                12, isOverBudget ? R.color.coral : R.color.alabaster, true);
        legend.addView(legRight);
        hBody.addView(legend);

        AnalyticsSummaryData.CategoryStat overCat = null;
        if (data.categoryStats != null) {
            for (AnalyticsSummaryData.CategoryStat cs : data.categoryStats) {
                boolean csOver = (cs.hasBudget && cs.spentMilli > cs.allocatedMilli)
                        || (!cs.hasBudget && "children".equals(cs.catalogKey) && cs.spentMilli > 0L);
                if (csOver) {
                    overCat = cs;
                    if ("children".equals(cs.catalogKey)) break;
                }
            }
        }
        boolean catExceededWarning = !isOverBudget && overCat != null;
        TextView statusBadge = new TextView(ctx);
        if (isOverBudget) {
            statusBadge.setText(ctx.getString(R.string.parent_badge_over));
        } else if (catExceededWarning) {
            long overAmt = overCat.hasBudget
                    ? MoneyMath.subtract(overCat.spentMilli, overCat.allocatedMilli)
                    : overCat.spentMilli;
            statusBadge.setText(ctx.getString(R.string.parent_badge_cat_over,
                    overCat.name, CurrencyFormatter.formatMilli(overAmt)));
        } else {
            statusBadge.setText(ctx.getString(R.string.parent_badge_ok));
        }
        boolean warnOrDanger = isOverBudget || catExceededWarning;
        statusBadge.setTextColor(ctx.getColor(isOverBudget ? R.color.danger_text : (catExceededWarning ? R.color.warning_text : R.color.forest)));
        statusBadge.setTextSize(12);
        statusBadge.setTypeface(null, Typeface.BOLD);
        statusBadge.setPadding(dp(d, 12), dp(d, 9), dp(d, 12), dp(d, 9));
        GradientDrawable sbBg = new GradientDrawable();
        sbBg.setColor(ctx.getColor(isOverBudget ? R.color.danger_surface : (warnOrDanger ? R.color.warning_surface : R.color.success_surface)));
        sbBg.setCornerRadius(dp(d, 10));
        statusBadge.setBackground(sbBg);
        hBody.addView(statusBadge);
        hero.addView(hBody);
        root.addView(hero, marginBottom(d, 14));

        // Empty Budget CTA Card when totalBudgeted == 0
        if (totalBudgeted <= 0L) {
            MaterialCardView emptyBudget = card(ctx, d, R.color.alabaster, R.color.card_border, 1, 18, 18);
            LinearLayout ebBody = vertical(ctx);
            ebBody.setGravity(Gravity.CENTER_HORIZONTAL);
            ImageView walletIc = new ImageView(ctx);
            walletIc.setImageResource(R.drawable.ic_wallet);
            walletIc.setColorFilter(ctx.getColor(R.color.forest));
            ebBody.addView(walletIc, new LinearLayout.LayoutParams(dp(d, 32), dp(d, 32)));
            TextView ebTitle = boldText(ctx, ctx.getString(R.string.parent_no_budget_title), 15, R.color.forest);
            ebTitle.setGravity(Gravity.CENTER);
            ebTitle.setPadding(0, dp(d, 8), 0, dp(d, 4));
            ebBody.addView(ebTitle);
            TextView ebDesc = mutedText(ctx, ctx.getString(R.string.parent_no_budget_desc), 12);
            ebDesc.setGravity(Gravity.CENTER);
            ebBody.addView(ebDesc);
            MaterialButton defBtn = primaryButton(ctx, ctx.getString(R.string.parent_define_month_cta), R.color.emerald, R.color.alabaster, d);
            defBtn.setOnClickListener(v -> actions.onCreateOrEditBudget(budget));
            LinearLayout.LayoutParams dbp = new LinearLayout.LayoutParams(-1, dp(d, 48));
            dbp.topMargin = dp(d, 12);
            ebBody.addView(defBtn, dbp);
            emptyBudget.addView(ebBody);
            root.addView(emptyBudget, marginBottom(d, 16));
        }

        // Dépenses par catégorie
        root.addView(sectionHeader(ctx, d, ctx.getString(R.string.parent_cat_section)));
        if (data.categoryStats.isEmpty()) {
            MaterialCardView emptyCat = card(ctx, d, R.color.alabaster, R.color.card_border, 1, 16, 16);
            TextView tv = mutedText(ctx, ctx.getString(R.string.parent_cat_empty), 13);
            tv.setGravity(Gravity.CENTER);
            emptyCat.addView(tv);
            root.addView(emptyCat, marginBottom(d, 16));
        } else {
            for (AnalyticsSummaryData.CategoryStat cat : data.categoryStats) {
                boolean isUnallocChildrenOver = !cat.hasBudget && "children".equals(cat.catalogKey) && cat.spentMilli > 0L;
                boolean isOver = (cat.hasBudget && cat.spentMilli > cat.allocatedMilli) || isUnallocChildrenOver;
                int cColor = isOver ? R.color.coral : (cat.consumptionPct >= 80.0 ? R.color.amber : R.color.forest);
                MaterialCardView cCard = card(ctx, d,
                        isOver ? R.color.danger_surface : R.color.alabaster,
                        isOver ? R.color.coral : R.color.card_border,
                        1, 16, 14);
                LinearLayout cBody = vertical(ctx);
                LinearLayout cTop = horizontal(ctx);
                TextView cName = boldText(ctx, cat.name, 14, isOver ? R.color.danger_text : R.color.forest);
                cTop.addView(cName, new LinearLayout.LayoutParams(0, -2, 1));
                if (isOver) {
                    cTop.addView(badgePill(ctx, d, ctx.getString(R.string.parent_cat_exceeded_badge),
                            ctx.getColor(R.color.danger_surface), ctx.getColor(R.color.danger_text)));
                }
                TextView cAmt = boldText(ctx, CurrencyFormatter.formatMilli(cat.spentMilli), 14,
                        isOver ? R.color.danger_text : R.color.forest);
                cAmt.setPaddingRelative(dp(d, 8), 0, 0, 0);
                cTop.addView(cAmt);
                cBody.addView(cTop);

                int ratio = isUnallocChildrenOver ? 100
                        : (int) Math.min(100L, Math.round(cat.hasBudget ? cat.consumptionPct : cat.shareOfTotal));
                cBody.addView(progressTrack(ctx, d, ratio, cColor, 7, 8));

                LinearLayout cBot = horizontal(ctx);
                cBot.setPadding(0, dp(d, 6), 0, 0);
                if (isUnallocChildrenOver) {
                    cBot.addView(labelText(ctx,
                            ctx.getString(R.string.category_unalloc_overrun_detail, CurrencyFormatter.formatMilli(cat.spentMilli)),
                            11, R.color.danger_text, true), new LinearLayout.LayoutParams(0, -2, 1));
                } else if (isOver) {
                    long overAmt = MoneyMath.subtract(cat.spentMilli, cat.allocatedMilli);
                    cBot.addView(labelText(ctx,
                            ctx.getString(R.string.parent_cat_over_detail, CurrencyFormatter.formatMilli(overAmt), (int) Math.round(cat.consumptionPct)),
                            11, R.color.danger_text, true), new LinearLayout.LayoutParams(0, -2, 1));
                } else if (cat.hasBudget) {
                    long leftCat = Math.max(0L, MoneyMath.subtract(cat.allocatedMilli, cat.spentMilli));
                    cBot.addView(labelText(ctx,
                            ctx.getString(R.string.parent_cat_consumed_detail, (int) Math.round(cat.consumptionPct), CurrencyFormatter.formatMilli(leftCat)),
                            11, R.color.forest, true), new LinearLayout.LayoutParams(0, -2, 1));
                } else {
                    cBot.addView(mutedText(ctx, ctx.getString(R.string.parent_cat_out_of_budget), 11),
                            new LinearLayout.LayoutParams(0, -2, 1));
                }
                if (cat.hasBudget) {
                    cBot.addView(mutedText(ctx, ctx.getString(R.string.parent_cat_planned_detail,
                            cat.plannedPercent, CurrencyFormatter.formatMilli(cat.allocatedMilli)), 11));
                } else if (cat.shareOfTotal > 0.0) {
                    cBot.addView(mutedText(ctx, ctx.getString(R.string.parent_cat_share_of_spend,
                            (int) Math.round(cat.shareOfTotal)), 11));
                }
                cBody.addView(cBot);
                cCard.addView(cBody);
                root.addView(cCard, marginBottom(d, 10));
            }
            Spacer(ctx, root, d, 6);
        }

        // Parent & Household 6-Month Pill Bar Chart
        root.addView(sectionHeader(ctx, d, ctx.getString(R.string.parent_history_section)));
        TextView pHistSub = mutedText(ctx, ctx.getString(R.string.parent_history_desc), 12);
        pHistSub.setPadding(0, 0, 0, dp(d, 8));
        root.addView(pHistSub);
        renderPillChartSection(ctx, root, data, d);

        // Children Progress Section (aggregate & privacy-safe)
        root.addView(sectionHeader(ctx, d, ctx.getString(R.string.parent_children_progress_title)));
        TextView privNote = labelText(ctx, "🔒 " + ctx.getString(R.string.parent_children_privacy), 11, R.color.forest, false);
        privNote.setPadding(0, 0, 0, dp(d, 10));
        root.addView(privNote);

        List<AnalyticsSummaryData.ChildAnalyticsItem> children = data.childrenProgress;
        if (children.isEmpty()) {
            MaterialCardView noChildCard = card(ctx, d, R.color.alabaster, R.color.card_border, 1, 16, 20);
            LinearLayout ncBody = vertical(ctx);
            ncBody.setGravity(Gravity.CENTER_HORIZONTAL);
            ImageView ppl = new ImageView(ctx);
            ppl.setImageResource(R.drawable.ic_people);
            ppl.setColorFilter(ctx.getColor(R.color.muted_green));
            ncBody.addView(ppl, new LinearLayout.LayoutParams(dp(d, 28), dp(d, 28)));
            TextView tv = mutedText(ctx, ctx.getString(R.string.parent_children_empty), 13);
            tv.setGravity(Gravity.CENTER);
            tv.setPadding(0, dp(d, 8), 0, 0);
            ncBody.addView(tv);
            noChildCard.addView(ncBody);
            root.addView(noChildCard, marginBottom(d, 16));
            return;
        }

        for (AnalyticsSummaryData.ChildAnalyticsItem ch : children) {
            boolean chOver = ch.percent > 100;
            MaterialCardView chCard = card(ctx, d,
                    chOver ? R.color.danger_surface : R.color.alabaster,
                    chOver ? R.color.coral : R.color.card_border,
                    1, 16, 14);
            LinearLayout chBody = vertical(ctx);
            LinearLayout chHeader = horizontal(ctx);
            LinearLayout avatarBox = new LinearLayout(ctx);
            avatarBox.setGravity(Gravity.CENTER);
            GradientDrawable avBg = new GradientDrawable();
            avBg.setShape(GradientDrawable.OVAL);
            avBg.setColor(ctx.getColor(R.color.mint_light));
            avatarBox.setBackground(avBg);
            ImageView avIcon = new ImageView(ctx);
            avIcon.setImageResource(R.drawable.ic_people);
            avIcon.setColorFilter(ctx.getColor(R.color.forest));
            avatarBox.addView(avIcon, new LinearLayout.LayoutParams(dp(d, 18), dp(d, 18)));
            chHeader.addView(avatarBox, new LinearLayout.LayoutParams(dp(d, 36), dp(d, 36)));

            LinearLayout chInfo = vertical(ctx);
            chInfo.setPaddingRelative(dp(d, 10), 0, dp(d, 6), 0);
            chInfo.addView(boldText(ctx, ch.nickname, 14, R.color.forest));
            chInfo.addView(mutedText(ctx, ch.email, 11));
            chHeader.addView(chInfo, new LinearLayout.LayoutParams(0, -2, 1));

            chHeader.addView(badgePill(ctx, d,
                    chOver ? ctx.getString(R.string.parent_child_pct_over, ch.percent) : ch.percent + "%",
                    ctx.getColor(chOver ? R.color.danger_surface : R.color.mint_light),
                    ctx.getColor(chOver ? R.color.danger_text : R.color.forest)));
            chBody.addView(chHeader);

            int chColor = chOver ? R.color.coral : (ch.percent >= 80 ? R.color.amber : R.color.mint_accent);
            chBody.addView(progressTrack(ctx, d, Math.min(100, ch.percent), chColor, 7, 10));

            LinearLayout chMetrics = horizontal(ctx);
            chMetrics.setPadding(0, dp(d, 10), 0, 0);
            chMetrics.addView(heroMetricCol(ctx, ctx.getString(R.string.parent_child_allocated_lbl),
                    CurrencyFormatter.formatMilli(ch.allocatedMilli), R.color.forest, false),
                    new LinearLayout.LayoutParams(0, -2, 1));
            chMetrics.addView(heroMetricCol(ctx, ctx.getString(R.string.parent_child_spent_lbl),
                    CurrencyFormatter.formatMilli(ch.spentMilli), chOver ? R.color.danger_text : R.color.forest, false),
                    new LinearLayout.LayoutParams(0, -2, 1));
            chMetrics.addView(heroMetricCol(ctx, ctx.getString(R.string.parent_child_remaining_lbl),
                    CurrencyFormatter.formatMilli(ch.remainingMilli),
                    ch.remainingMilli == 0L && ch.allocatedMilli > 0L ? R.color.coral : R.color.forest, false),
                    new LinearLayout.LayoutParams(0, -2, 1));
            chBody.addView(chMetrics);
            chCard.addView(chBody);
            root.addView(chCard, marginBottom(d, 12));
        }

        // Child 6-Month Historical Comparison Pill Chart with Interactive Child Selector Tabs
        root.addView(sectionHeader(ctx, d, ctx.getString(R.string.parent_children_hist_title)));
        TextView histDesc = mutedText(ctx, ctx.getString(R.string.parent_children_hist_desc), 12);
        histDesc.setPadding(0, 0, 0, dp(d, 8));
        root.addView(histDesc);

        final MaterialCardView childChartCard = card(ctx, d, R.color.alabaster, R.color.card_border, 1, 18, 16);
        final LinearLayout childChartBody = vertical(ctx);
        childChartCard.addView(childChartBody);

        final int[] activeChildIdx = {0};
        if (children.size() > 1) {
            LinearLayout tabsRow = horizontal(ctx);
            tabsRow.setPadding(0, 0, 0, dp(d, 10));
            final MaterialButton[] tabBtns = new MaterialButton[children.size()];
            for (int i = 0; i < children.size(); i++) {
                final int idx = i;
                MaterialButton tab = new MaterialButton(ctx);
                tab.setText(children.get(i).nickname);
                tab.setAllCaps(false);
                tab.setTextSize(12);
                tab.setCornerRadius(dp(d, 14));
                tab.setMinHeight(dp(d, 38));
                LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, -2, 1);
                if (i > 0) tp.setMarginStart(dp(d, 6));
                tab.setOnClickListener(v -> {
                    activeChildIdx[0] = idx;
                    updateChildTabs(ctx, tabBtns, activeChildIdx[0]);
                    bindChildChartCard(ctx, childChartBody, children.get(idx), children.size() > 1, d);
                });
                tabBtns[i] = tab;
                tabsRow.addView(tab, tp);
            }
            updateChildTabs(ctx, tabBtns, 0);
            root.addView(tabsRow);
        }

        bindChildChartCard(ctx, childChartBody, children.get(0), children.size() > 1, d);
        root.addView(childChartCard, marginBottom(d, 18));
    }

    private static void updateChildTabs(Context ctx, MaterialButton[] buttons, int activeIndex) {
        for (int i = 0; i < buttons.length; i++) {
            boolean active = i == activeIndex;
            buttons[i].setBackgroundTintList(ColorStateList.valueOf(
                    ctx.getColor(active ? R.color.forest : R.color.mint_light)));
            buttons[i].setTextColor(ctx.getColor(active ? R.color.alabaster : R.color.forest));
        }
    }

    private static void bindChildChartCard(
            Context ctx,
            LinearLayout target,
            AnalyticsSummaryData.ChildAnalyticsItem child,
            boolean showChildPrefix,
            float d) {
        target.removeAllViews();
        String title = showChildPrefix
                ? ctx.getString(R.string.parent_children_pill_title_named, child.nickname)
                : ctx.getString(R.string.parent_children_pill_title);
        target.addView(boldText(ctx, title, 14, R.color.forest));
        target.addView(mutedText(ctx, ctx.getString(R.string.parent_children_pill_sub), 11));
        PillBarChart chart = new PillBarChart(ctx);
        chart.setBarItems(child.sixMonthHistory);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(d, 182));
        cp.topMargin = dp(d, 6);
        target.addView(chart, cp);
        target.addView(legendLines(ctx, child.sixMonthHistory, d));
    }

    private static void renderPillChartSection(Context ctx, LinearLayout root, AnalyticsSummaryData data, float d) {
        String title;
        String subtitle;
        if (data.role == AppRole.PARENT) {
            title = ctx.getString(R.string.analytics_pill_parent_title);
            subtitle = ctx.getString(R.string.analytics_pill_parent_sub);
        } else if (data.role == AppRole.CHILD) {
            title = ctx.getString(R.string.analytics_pill_child_title);
            subtitle = ctx.getString(R.string.analytics_pill_child_sub);
        } else {
            title = ctx.getString(R.string.analytics_pill_user_title);
            subtitle = ctx.getString(R.string.analytics_pill_user_sub);
        }

        MaterialCardView pCard = card(ctx, d, R.color.alabaster, R.color.card_border, 1, 18, 16);
        LinearLayout pBody = vertical(ctx);
        pBody.addView(boldText(ctx, title, 14, R.color.forest));
        pBody.addView(mutedText(ctx, subtitle, 11));
        PillBarChart chart = new PillBarChart(ctx);
        chart.setBarItems(data.sixMonthHistory);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1, dp(d, 182));
        cp.topMargin = dp(d, 6);
        pBody.addView(chart, cp);
        pCard.addView(pBody);
        root.addView(pCard, marginBottom(d, 6));

        View legend = legendLines(ctx, data.sixMonthHistory, d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(d, 14);
        root.addView(legend, lp);
    }

    private static View legendLines(Context ctx, List<PillBarChart.BarItem> items, float d) {
        LinearLayout wrap = vertical(ctx);
        if (items == null || items.isEmpty()) return wrap;
        StringBuilder line1 = new StringBuilder();
        StringBuilder line2 = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            PillBarChart.BarItem item = items.get(i);
            String chunk = item.shortLabel + " " + BudgetPeriodMath.compactDinars(item.spentMilli);
            if (i == items.size() - 1) {
                chunk += " DT (" + ctx.getString(R.string.analytics_pill_to_date) + ")";
            }
            if (i < 4) {
                if (line1.length() > 0) line1.append(" · ");
                line1.append(chunk);
            } else {
                if (line2.length() > 0) line2.append(" · ");
                line2.append(chunk);
            }
        }
        TextView tv1 = mutedText(ctx, line1.toString(), 11);
        tv1.setPadding(dp(d, 4), dp(d, 2), dp(d, 4), 0);
        wrap.addView(tv1);
        if (line2.length() > 0) {
            TextView tv2 = mutedText(ctx, line2.toString(), 11);
            tv2.setPadding(dp(d, 4), dp(d, 2), dp(d, 4), 0);
            wrap.addView(tv2);
        }
        return wrap;
    }

    private static void renderPurchasesAndHistoryCard(
            Context ctx,
            LinearLayout root,
            AnalyticsSummaryData data,
            Actions actions,
            float d) {
        if (data.periodTransactions.isEmpty()) return;
        root.addView(sectionHeader(ctx, d, ctx.getString(R.string.analytics_breakdown_title)));
        MaterialCardView hCard = card(ctx, d, R.color.alabaster, R.color.card_border, 1, 16, 14);
        LinearLayout hBody = vertical(ctx);
        TextView splitTv = boldText(ctx, ctx.getString(R.string.analytics_split_totals,
                CurrencyFormatter.formatMilli(data.purchasesTotalMilli),
                CurrencyFormatter.formatMilli(data.expensesTotalMilli)), 12, R.color.emerald);
        splitTv.setPadding(0, 0, 0, dp(d, 8));
        hBody.addView(splitTv);

        int limit = Math.min(data.periodTransactions.size(), 10);
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
        for (int i = 0; i < limit; i++) {
            TransactionRow tx = data.periodTransactions.get(i);
            hBody.addView(divider(ctx, d));
            LinearLayout row = horizontal(ctx);
            row.setPadding(0, dp(d, 9), 0, dp(d, 9));
            row.setClickable(true);
            row.setFocusable(true);
            LinearLayout left = vertical(ctx);
            String title = tx.title == null || tx.title.trim().isEmpty()
                    ? ctx.getString(R.string.transaction_name)
                    : tx.title;
            left.addView(boldText(ctx, title, 13, R.color.ink));
            left.addView(mutedText(ctx, df.format(new Date(tx.occurredAt)), 11));
            row.addView(left, new LinearLayout.LayoutParams(0, -2, 1));
            row.addView(boldText(ctx, "− " + CurrencyFormatter.formatMilli(tx.amountMilli), 13, R.color.coral));
            row.setOnClickListener(v -> actions.onOpenTransaction(tx));
            hBody.addView(row);
        }
        hCard.addView(hBody);
        root.addView(hCard, marginBottom(d, 16));
    }

    private static String[] computeInsights(
            Context ctx,
            AnalyticsSummaryData data,
            long totalBudget,
            long curSpent,
            long remainingBudget,
            long exceededAmount,
            double consumptionPct,
            boolean isExceeded,
            long prevSpent,
            boolean isSaving) {
        String pctStr = String.format(Locale.US, "%.1f", consumptionPct);
        if (curSpent <= 0L) {
            return new String[]{
                    ctx.getString(R.string.analytics_conclusion_empty),
                    ctx.getString(R.string.analytics_rec_empty)
            };
        }
        if (isExceeded) {
            String rec = data.attentionCategory != null
                    ? ctx.getString(R.string.analytics_rec_exceeded_cat,
                            data.attentionCategory.name,
                            data.attentionCategory.hasBudget
                                    ? String.format(Locale.US, "%.0f%%", data.attentionCategory.consumptionPct)
                                    : String.format(Locale.US, "%.0f%%", data.attentionCategory.shareOfTotal))
                    : ctx.getString(R.string.analytics_rec_exceeded_generic);
            return new String[]{
                    ctx.getString(R.string.analytics_conclusion_exceeded,
                            CurrencyFormatter.formatMilli(curSpent),
                            CurrencyFormatter.formatMilli(exceededAmount),
                            pctStr),
                    rec
            };
        }
        if (consumptionPct >= 85.0) {
            String rec = data.daysRemaining > 0
                    ? ctx.getString(R.string.analytics_rec_near_days,
                            CurrencyFormatter.formatMilli(Math.round(remainingBudget / (double) data.daysRemaining)))
                    : ctx.getString(R.string.analytics_rec_near_done);
            return new String[]{
                    ctx.getString(R.string.analytics_conclusion_near, pctStr, data.daysRemaining,
                            CurrencyFormatter.formatMilli(remainingBudget)),
                    rec
            };
        }
        if (consumptionPct >= 65.0) {
            String rec = data.paceSustainable
                    ? ctx.getString(R.string.analytics_rec_mid_ok, CurrencyFormatter.formatMilli(data.avgDailySpendMilli))
                    : ctx.getString(R.string.analytics_rec_mid_warn, CurrencyFormatter.formatMilli(data.projectedDiffMilli));
            return new String[]{
                    ctx.getString(R.string.analytics_conclusion_mid, pctStr,
                            CurrencyFormatter.formatMilli(remainingBudget), data.daysRemaining),
                    rec
            };
        }
        String rec = prevSpent > 0L && isSaving
                ? ctx.getString(R.string.analytics_rec_healthy_saving,
                        CurrencyFormatter.formatMilli(curSpent), CurrencyFormatter.formatMilli(prevSpent))
                : ctx.getString(R.string.analytics_rec_healthy_pace, CurrencyFormatter.formatMilli(data.avgDailySpendMilli));
        return new String[]{
                ctx.getString(R.string.analytics_conclusion_healthy, pctStr,
                        CurrencyFormatter.formatMilli(curSpent),
                        CurrencyFormatter.formatMilli(totalBudget),
                        CurrencyFormatter.formatMilli(remainingBudget)),
                rec
        };
    }

    // ── Compact UI Building Helpers ──
    private static MaterialCardView card(Context ctx, float d, int bgColorRes, int strokeColorRes, int strokeDp, int radiusDp, int padDp) {
        MaterialCardView card = new MaterialCardView(ctx);
        card.setCardBackgroundColor(ctx.getColor(bgColorRes));
        card.setRadius(dp(d, radiusDp));
        card.setCardElevation(0);
        card.setStrokeWidth(dp(d, strokeDp));
        card.setStrokeColor(ctx.getColor(strokeColorRes));
        int p = dp(d, padDp);
        card.setContentPadding(p, p, p, p);
        return card;
    }

    private static LinearLayout vertical(Context ctx) {
        LinearLayout ll = new LinearLayout(ctx);
        ll.setOrientation(LinearLayout.VERTICAL);
        return ll;
    }

    private static LinearLayout horizontal(Context ctx) {
        LinearLayout ll = new LinearLayout(ctx);
        ll.setOrientation(LinearLayout.HORIZONTAL);
        ll.setGravity(Gravity.CENTER_VERTICAL);
        ll.setLayoutDirection(View.LAYOUT_DIRECTION_LOCALE);
        return ll;
    }

    private static TextView boldText(Context ctx, String text, float sizeSp, int colorRes) {
        return labelText(ctx, text, sizeSp, colorRes, true);
    }

    private static TextView mutedText(Context ctx, String text, float sizeSp) {
        return labelText(ctx, text, sizeSp, R.color.muted_green, false);
    }

    private static TextView labelText(Context ctx, String text, float sizeSp, int colorRes, boolean bold) {
        TextView tv = new TextView(ctx);
        tv.setText(text == null ? "" : text);
        tv.setTextSize(sizeSp);
        tv.setTextColor(ctx.getColor(colorRes));
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private static TextView sectionHeader(Context ctx, float d, String text) {
        TextView tv = boldText(ctx, text, 17, R.color.forest);
        tv.setPadding(0, dp(d, 4), 0, dp(d, 8));
        return tv;
    }

    private static TextView badgePill(Context ctx, float d, String text, int bgColor, int fgColor) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextSize(11);
        tv.setTextColor(fgColor);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setPadding(dp(d, 10), dp(d, 4), dp(d, 10), dp(d, 4));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(dp(d, 999));
        tv.setBackground(bg);
        return tv;
    }

    private static LinearLayout heroMetricCol(Context ctx, String label, String value, int valueColorRes, boolean dangerHighlight) {
        LinearLayout col = vertical(ctx);
        TextView lbl = labelText(ctx, label, 11, valueColorRes == R.color.alabaster ? R.color.mint_accent : R.color.muted_green, false);
        TextView val = boldText(ctx, value, 13, valueColorRes);
        if (dangerHighlight) val.setTextColor(Color.parseColor("#FF8A80"));
        col.addView(lbl);
        col.addView(val);
        return col;
    }

    private static ProgressBar progressTrack(Context ctx, float d, int progress, int tintColorRes, int heightDp, int topMarginDp) {
        ProgressBar pb = new ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal);
        pb.setMax(100);
        pb.setProgress(Math.max(0, Math.min(100, progress)));
        pb.setProgressTintList(ColorStateList.valueOf(ctx.getColor(tintColorRes)));
        pb.setProgressBackgroundTintList(ColorStateList.valueOf(ctx.getColor(R.color.mint_light)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(d, heightDp));
        lp.topMargin = dp(d, topMarginDp);
        pb.setLayoutParams(lp);
        return pb;
    }

    private static MaterialButton primaryButton(Context ctx, String label, int bgRes, int fgRes, float d) {
        MaterialButton btn = new MaterialButton(ctx);
        btn.setText(label);
        btn.setAllCaps(false);
        btn.setTypeface(null, Typeface.BOLD);
        btn.setTextColor(ctx.getColor(fgRes));
        btn.setBackgroundTintList(ColorStateList.valueOf(ctx.getColor(bgRes)));
        btn.setCornerRadius(dp(d, 14));
        return btn;
    }

    private static View divider(Context ctx, float d) {
        View v = new View(ctx);
        v.setBackgroundColor(ctx.getColor(R.color.card_border));
        v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(d, 1)));
        return v;
    }

    private static View spaceDivider(Context ctx, float d) {
        View v = divider(ctx, d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(d, 1));
        lp.setMargins(0, dp(d, 12), 0, dp(d, 12));
        v.setLayoutParams(lp);
        return v;
    }

    private static void Spacer(Context ctx, LinearLayout root, float d, int heightDp) {
        View v = new View(ctx);
        root.addView(v, new LinearLayout.LayoutParams(-1, dp(d, heightDp)));
    }

    private static LinearLayout.LayoutParams marginBottom(float d, int bottomDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(d, bottomDp);
        return lp;
    }

    private static int dp(float density, int value) {
        return (int) (value * density + 0.5f);
    }
}
