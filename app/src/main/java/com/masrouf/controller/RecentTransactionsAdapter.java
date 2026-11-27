/*
 * ============================================================================
 * FICHIER : RecentTransactionsAdapter.java
 * MISSION : Adaptateur RecyclerView réutilisable pour présenter une liste de TransactionRow récentes.
 * IMPLÉMENTE : RecyclerView.Adapter<Holder> sur le layout item_transaction.xml + interface Listener.
 * EFFET À L'ÉCRAN : Affiche titre, date localisée et montant négatif ("− X DT") sur chaque ligne cliquable.
 * COMMUNICATIONS : Audit — présent comme composant réutilisable ; HomeFragment génère actuellement ses 5 lignes
 *                  directement dans un LinearLayout (home_recent_transactions).
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
import com.example.masrouf1.db.TransactionRow;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Adaptateur RecyclerView réutilisable pour afficher des TransactionRow dans item_transaction.xml.
 */
final class RecentTransactionsAdapter extends RecyclerView.Adapter<RecentTransactionsAdapter.Holder> {
    /** Rappel explicite déclenché au clic sur une transaction de la liste. */
    interface Listener { void onSelected(TransactionRow row); }
    // Liste des lignes financières affichées et écouteur de sélection.
    private final List<TransactionRow> rows=new ArrayList<>(); private final Listener listener;

    /** Initialise l'adaptateur avec le listener cible. */
    RecentTransactionsAdapter(Listener listener) { this.listener=listener; }

    /** Met à jour la liste de transactions puis déclenche le rafraîchissement complet du RecyclerView. */
    void submit(List<TransactionRow> values) { rows.clear(); if(values!=null) rows.addAll(values); notifyDataSetChanged(); }

    /** Gonfle le layout XML item_transaction.xml pour chaque cellule visible. */
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type) { return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction,parent,false)); }

    /** Remplit titre, date abrégée et montant formaté puis branche l'écouteur de clic sur la carte. */
    @Override public void onBindViewHolder(@NonNull Holder holder,int position) {
        TransactionRow row=rows.get(position);
        holder.title.setText(row.title);
        holder.date.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(row.occurredAt)));
        holder.amount.setText("− "+CurrencyFormatter.formatMilli(row.amountMilli));
        holder.itemView.setOnClickListener(v->listener.onSelected(row));
    }

    /** Retourne l'effectif total des transactions à lier. */
    @Override public int getItemCount() { return rows.size(); }

    /** ViewHolder conservant les 3 TextView d'une carte item_transaction. */
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title,date,amount;
        Holder(View view) { super(view); title=view.findViewById(R.id.transaction_title); date=view.findViewById(R.id.transaction_date); amount=view.findViewById(R.id.transaction_amount); }
    }
}
