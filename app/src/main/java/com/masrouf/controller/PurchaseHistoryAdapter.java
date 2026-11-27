/*
 * ============================================================================
 * FICHIER : PurchaseHistoryAdapter.java
 * MISSION : Adaptateur RecyclerView affichant l'historique d'achats sur la fiche produit détaillée.
 * IMPLÉMENTE : RecyclerView.Adapter<Holder> sur le layout item_purchase_history.xml.
 * EFFET À L'ÉCRAN : Formate quantité exacte (sans zéros superflus via BigDecimal) + unité + magasin + date,
 *                   puis le montant total payé en dinars tunisiens.
 * COMMUNICATIONS : Piloté par ProductDetailsActivity avec la liste ProductDetailsData.purchases.
 * ============================================================================
 */
package com.masrouf.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.R;
import com.example.masrouf1.db.PurchaseHistoryRow;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Adaptateur RecyclerView en lecture seule utilisé dans ProductDetailsActivity.
 */
public final class PurchaseHistoryAdapter extends RecyclerView.Adapter<PurchaseHistoryAdapter.Holder> {
    // Liste des achats antérieurs du produit sélectionné.
    private final List<PurchaseHistoryRow> rows = new ArrayList<>();

    /** Gonfle le layout XML item_purchase_history.xml pour chaque ligne. */
    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase_history, parent, false));
    }

    /** Formate quantité, magasin, date et total payé dans les deux TextView de la ligne. */
    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        PurchaseHistoryRow row = rows.get(position);
        String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(row.occurredAt));
        // Convertit quantityMilli (ex. 1500) en décimal propre ("1.5 kg") sans zéros finaux inutiles.
        String quantity = BigDecimal.valueOf(row.quantityMilli, 3).stripTrailingZeros().toPlainString() + " "
                + (row.unit == null ? "" : row.unit);
        String store = row.storeName == null || row.storeName.trim().isEmpty()
                ? holder.itemView.getContext().getString(R.string.no_store_recorded)
                : row.storeName;
        holder.title
                .setText(holder.itemView.getContext().getString(R.string.purchase_history_item, quantity, store, date));
        holder.price.setText(CurrencyFormatter.formatMilli(row.totalMilli));
    }

    /** Retourne le nombre total d'achats enregistrés pour ce produit. */
    @Override
    public int getItemCount() {
        return rows.size();
    }

    /** Met à jour la liste d'achats et déclenche le rafraîchissement du RecyclerView. */
    public void submit(List<PurchaseHistoryRow> values) {
        rows.clear();
        if (values != null)
            rows.addAll(values);
        notifyDataSetChanged();
    }

    /** ViewHolder mémorisant titre et montant d'une ligne item_purchase_history.xml. */
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title, price;

        Holder(@NonNull View item) {
            super(item);
            title = item.findViewById(R.id.purchase_history_title);
            price = item.findViewById(R.id.purchase_history_amount);
        }
    }
}
