/*
 * ============================================================================
 * FICHIER : TransactionDetailsActivity.java
 * MISSION : Écran de consultation détaillée (lecture seule) d'une écriture financière Room.
 * IMPLÉMENTE : Hérite de BaseFeatureActivity et utilise activity_transaction_details.xml.
 * EFFET À L'ÉCRAN : Affiche titre/libellé, montant formaté en DT, date/heure exacte, libellé localisé du type et note éventuelle (ou message vide).
 * COMMUNICATIONS : Ouvert via Intent explicite (extra "TRANSACTION_ID") depuis CalendarFragment et AnalyticsFragment ;
 *                  interroge MasroufViewModel -> MasroufRepository.transactionForUser(owner, id).
 * ============================================================================
 */
package com.masrouf.controller;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufEntities.TransactionEntity;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Écran de consultation détaillée en lecture seule d'une transaction financière Room.
 */
public final class TransactionDetailsActivity extends BaseFeatureActivity {
    /**
     * Initialise la coque d'écran, gonfle activity_transaction_details et charge la transaction demandée.
     * @param state état sauvegardé par Android lors d'un changement de configuration.
     */
    @Override protected void onCreate(Bundle state){
        // Initialise BaseFeatureActivity (toolbar, bouton retour, conteneur ScrollView).
        super.onCreate(state);showPage(R.string.transaction_details);
        // Injecte le layout XML de fiche transaction directement dans le conteneur parent.
        getLayoutInflater().inflate(R.layout.activity_transaction_details,content,true);
        // Récupère les références des TextView de titre, montant, date, type, note et état vide.
        TextView title=findViewById(R.id.transaction_detail_title),amount=findViewById(R.id.transaction_detail_amount),date=findViewById(R.id.transaction_detail_date),kind=findViewById(R.id.transaction_detail_kind),note=findViewById(R.id.transaction_detail_note),empty=findViewById(R.id.transaction_detail_empty);
        // Lit l'identifiant du propriétaire connecté (sécurité) et l'ID de transaction passé dans l'Intent.
        long owner=SessionStore.userId(this),id=getIntent().getLongExtra("TRANSACTION_ID",0);
        // Obtient le ViewModel central relié à MasroufRepository.
        MasroufViewModel model=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        // Lance la lecture asynchrone hors thread UI et vérifie que la transaction appartient bien à owner.
        model.<TransactionEntity>request(repository->repository.transactionForUser(owner,id),result->{
            // Si la transaction est introuvable ou appartient à un autre compte, affiche le message vide.
            if(result.data==null){empty.setVisibility(View.VISIBLE);return;}
            TransactionEntity row=result.data;
            // Affiche le libellé saisi (ou un nom générique localisé s'il est vide).
            title.setText(row.label==null||row.label.trim().isEmpty()?getString(R.string.transaction_name):row.label);
            // Formate le montant entier (millimes) en dinars tunisiens ("12,500 DT").
            amount.setText(CurrencyFormatter.formatMilli(row.amountMilli));
            // Formate le timestamp epoch en date et heure localisées selon la langue active (FR/AR/EN).
            date.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT,Locale.getDefault()).format(new Date(row.occurredAt)));
            // Traduit le code technique kind en libellé compréhensible pour l'utilisateur.
            kind.setText(kindLabel(row.kind));
            // Masque la zone note si aucune remarque n'est présente, sinon l'affiche à l'écran.
            if(row.note==null||row.note.trim().isEmpty())note.setVisibility(View.GONE);else note.setText(row.note);
        });
    }

    /**
     * Traduit le type technique stocké dans SQLite (money_transactions.kind) vers une ressource texte localisée.
     * @param value valeur brute en base ("purchase", "donation", "allowance_received", "expense").
     * @return libellé affiché à l'écran dans la langue de l'utilisateur.
     */
    private String kindLabel(String value){
        // Cas d'un achat produit enregistré via AddProductActivity ou saisie vocale.
        if("purchase".equalsIgnoreCase(value))return getString(R.string.add_purchase_product);
        // Cas d'un don ou œuvre caritative.
        if("donation".equalsIgnoreCase(value))return getString(R.string.donation);
        // Cas d'une dotation ou argent de poche reçu par un profil CHILD.
        if("allowance_received".equalsIgnoreCase(value))return getString(R.string.notification_gift_received);
        // Par défaut : dépense directe standard.
        return getString(R.string.add_expense);
    }
}
