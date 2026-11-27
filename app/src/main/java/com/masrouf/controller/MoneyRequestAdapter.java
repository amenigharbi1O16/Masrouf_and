/*
 * ============================================================================
 * FICHIER : MoneyRequestAdapter.java
 * MISSION : Adaptateur RecyclerView affichant la liste des demandes d'argent envoyées par un profil CHILD.
 * IMPLÉMENTE : RecyclerView.Adapter<MoneyRequestAdapter.Holder> gonflant le layout item_money_request.xml.
 * EFFET À L'ÉCRAN : Affiche pour chaque demande : montant formaté, statut localisé (Acceptée / Refusée / En attente),
 *                   motif saisi et date/heure de soumission.
 * COMMUNICATIONS : Piloté par MoneyRequestActivity ; reçoit ses données via submit(List<MoneyRequestEntity>).
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
import com.example.masrouf1.db.MasroufEntities.MoneyRequestEntity;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Adaptateur RecyclerView de l'historique des demandes d'argent utilisé par MoneyRequestActivity (profil Enfant).
 */
final class MoneyRequestAdapter extends RecyclerView.Adapter<MoneyRequestAdapter.Holder> {
    // Source interne contenant les entités MoneyRequestEntity triées par date décroissante.
    private final List<MoneyRequestEntity> rows=new ArrayList<>();

    /** Remplace la liste affichée par les nouvelles demandes chargées depuis Room et rafraîchit le RecyclerView. */
    void submit(List<MoneyRequestEntity> values){
        // Vide la liste précédente avant d'insérer les lignes fraîches si non nulles.
        rows.clear();if(values!=null)rows.addAll(values);
        // Notifie RecyclerView de redessiner toutes les cartes visibles.
        notifyDataSetChanged();
    }

    /** Gonfle le layout XML d'une ligne item_money_request.xml et encapsule ses vues dans Holder. */
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_money_request,parent,false));}

    /** Lie les données métier d'une demande d'argent aux quatre TextView de la carte recyclée. */
    @Override public void onBindViewHolder(@NonNull Holder holder,int position){
        // Récupère l'entité à afficher à cet index.
        MoneyRequestEntity row=rows.get(position);
        // Formate le montant demandé en dinars tunisiens (DT).
        holder.amount.setText(CurrencyFormatter.formatMilli(row.amountMilli));
        // Associe le statut SQLite ("accepted", "rejected", "pending") à sa ressource traduite FR/AR/EN.
        int status="accepted".equalsIgnoreCase(row.status)?R.string.money_request_accepted:"rejected".equalsIgnoreCase(row.status)?R.string.money_request_rejected:R.string.money_request_pending;
        holder.status.setText(status);
        // Affiche le motif textuel (ou vide si null).
        holder.reason.setText(row.reason==null?"":row.reason);
        // Formate la date et l'heure selon les conventions locales du téléphone.
        holder.date.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(row.createdAt)));
    }

    /** Retourne le nombre total de demandes d'argent à afficher dans la liste. */
    @Override public int getItemCount(){return rows.size();}

    /** ViewHolder mémorisant les 4 TextView de item_money_request.xml pour éviter des findViewById répétés. */
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView amount,status,reason,date;
        Holder(View root){super(root);amount=root.findViewById(R.id.money_request_amount_value);status=root.findViewById(R.id.money_request_status_value);reason=root.findViewById(R.id.money_request_reason_value);date=root.findViewById(R.id.money_request_date_value);}
    }
}
