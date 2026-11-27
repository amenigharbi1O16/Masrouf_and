/*
 * ============================================================================
 * FICHIER : GiftEnvelopeData.java
 * MISSION : Évaluer avant validation si un don d'argent Parent -> Enfant respecte l'enveloppe « Enfants » et le plafond mensuel.
 * IMPLÉMENTE : DTO immuable exposant les méthodes métier willExceedCategory() et willExceedMonthly().
 * EFFET À L'ÉCRAN : Déclenche l'affichage de bannières d'avertissement ambre/corail dans dialog_give_money
 *                   et dialog_add_child au sein de ChildrenManagementActivity.
 * COMMUNICATIONS : Calculé par MasroufRepository -> consommé par ChildrenManagementActivity.
 * ============================================================================
 */
package com.example.masrouf1;

import com.example.masrouf1.db.MasroufEntities.BudgetEntity;

/**
 * État budgétaire calculé avant un don d'argent Parent -> Enfant dans ChildrenManagementActivity.
 */
public final class GiftEnvelopeData {
    // Budget actif du parent, reste sur l'enveloppe Enfants et aptitude au transfert.
    public final BudgetEntity budget;
    public final long remainingMilli;
    public final boolean ready;
    // Plafond alloué à la catégorie Enfants, déjà consommé, reste global mensuel et présence explicite d'enveloppe.
    public final long categoryAllocatedMilli;
    public final long categorySpentMilli;
    public final long monthlyRemainingMilli;
    public final boolean hasEnfantEnvelope;

    /** Constructeur simplifié déduisant la marge mensuelle depuis le budget actif. */
    public GiftEnvelopeData(BudgetEntity budget, long remainingMilli, boolean ready) {
        this(budget, remainingMilli, ready, Math.max(0L, remainingMilli), 0L,
                budget == null ? 0L : Math.max(0L, budget.monthlyLimitMilli - budget.spentMilli), remainingMilli > 0L);
    }

    /** Constructeur complet initialisant les plafonds catégorie Enfants et mensuel. */
    public GiftEnvelopeData(BudgetEntity budget,
                            long remainingMilli,
                            boolean ready,
                            long categoryAllocatedMilli,
                            long categorySpentMilli,
                            long monthlyRemainingMilli,
                            boolean hasEnfantEnvelope) {
        this.budget = budget;
        this.remainingMilli = remainingMilli;
        this.ready = ready;
        this.categoryAllocatedMilli = Math.max(0L, categoryAllocatedMilli);
        this.categorySpentMilli = Math.max(0L, categorySpentMilli);
        this.monthlyRemainingMilli = monthlyRemainingMilli;
        this.hasEnfantEnvelope = hasEnfantEnvelope;
    }

    /** @return true si le don saisi dépasse l'enveloppe « Enfants » (déclenche une bannière d'avertissement). */
    public boolean willExceedCategory(long amountMilli) {
        return amountMilli > 0L && (!hasEnfantEnvelope || amountMilli > remainingMilli);
    }

    /** @return true si le don dépasse également le reste global du budget mensuel du parent. */
    public boolean willExceedMonthly(long amountMilli) {
        return budget != null && amountMilli > 0L && amountMilli > monthlyRemainingMilli;
    }
}
