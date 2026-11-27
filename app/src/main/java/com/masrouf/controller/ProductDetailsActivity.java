/*
 * ============================================================================
 * FICHIER : ProductDetailsActivity.java
 * MISSION : Fiche détaillée d'un produit présentant ses caractéristiques, sa courbe de prix et son historique d'achats.
 * IMPLÉMENTE : Hérite de BaseFeatureActivity et gonfle activity_product_details.xml.
 * EFFET À L'ÉCRAN : Affiche nom/marque/prix unitaire actuel, photo locale (BitmapFactory), statistiques min/moy/max,
 *                   courbe 2D PriceChartView, liste PurchaseHistoryAdapter et bouton vers ProductPriceHistoryActivity.
 * COMMUNICATIONS : Ouvert depuis ProductsFragment avec l'Extra "PRODUCT_ID" ; interroge MasroufRepository.productDetails()
 *                  via MasroufViewModel puis ouvre ProductPriceHistoryActivity au clic sur « Voir tout l'historique ».
 * ============================================================================
 */
package com.masrouf.controller;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.ProductDetailsData;
import com.example.masrouf1.PriceChartView;
import com.example.masrouf1.R;

import java.io.File;

/**
 * Fiche détaillée d'un produit (hérite de BaseFeatureActivity + activity_product_details.xml).
 */
public final class ProductDetailsActivity extends BaseFeatureActivity {
    /**
     * Monte le formulaire de détail, charge la fiche produit depuis Room et configure graphique + historique.
     * @param state état sauvegardé Android éventuel.
     */
    @Override protected void onCreate(Bundle state){
        // Initialise la coque d'écran avec barre retour et gonfle activity_product_details dans le conteneur.
        super.onCreate(state);showPage(R.string.product_details);getLayoutInflater().inflate(R.layout.activity_product_details,content,true);
        // Récupère les vues image, textes de titre/statistiques/état vide et bouton historique complet.
        ImageView photo=findViewById(R.id.product_detail_photo);TextView title=findViewById(R.id.product_detail_title),stats=findViewById(R.id.product_detail_stats),empty=findViewById(R.id.product_purchase_empty);
        com.google.android.material.button.MaterialButton fullHistoryButton=findViewById(R.id.product_view_full_history);
        // Configure la courbe personnalisée PriceChartView et le RecyclerView d'achats avec PurchaseHistoryAdapter.
        PriceChartView chart=findViewById(R.id.product_price_chart);RecyclerView purchases=findViewById(R.id.product_purchase_history);purchases.setLayoutManager(new LinearLayoutManager(this));PurchaseHistoryAdapter adapter=new PurchaseHistoryAdapter();purchases.setAdapter(adapter);
        // Extrait l'ID produit passé par ProductsFragment et l'ID du compte propriétaire pour isoler la requête.
        long id=getIntent().getLongExtra("PRODUCT_ID",0),userId=SessionStore.userId(this);MasroufViewModel vm=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        // Charge ProductDetailsData hors UI thread via MasroufViewModel.
        vm.<ProductDetailsData>request(repository->repository.productDetails(userId,id),result->{
            // Si le produit est introuvable ou n'appartient pas au compte, vide les widgets et affiche l'état indisponible.
            if(result.failed||result.data==null||result.data.product==null){title.setText(R.string.product_details_unavailable);stats.setText("");chart.setHistory(java.util.Collections.emptyList());adapter.submit(java.util.Collections.emptyList());empty.setVisibility(android.view.View.VISIBLE);if(fullHistoryButton!=null)fullHistoryButton.setVisibility(android.view.View.GONE);return;}
            ProductDetailsData data=result.data;
            // Affiche nom + marque éventuelle + dernier prix unitaire et unité sur deux lignes.
            title.setText(data.product.name+(data.product.brand==null||data.product.brand.isEmpty()?"":" · "+data.product.brand)+"\n"+CurrencyFormatter.formatMilli(data.product.currentPriceMilli)+" / "+data.product.unit);
            // Décode la photo enregistrée dans le cache local si le fichier existe encore sur disque.
            if(data.product.photoPath!=null&&new File(data.product.photoPath).exists())photo.setImageBitmap(BitmapFactory.decodeFile(data.product.photoPath));
            // Affiche les bornes min/moyenne/max ou un message d'absence d'historique.
            if(data.minPriceMilli==null)stats.setText(R.string.price_history_empty);else stats.setText(getString(R.string.product_price_stats,CurrencyFormatter.formatMilli(data.minPriceMilli),CurrencyFormatter.formatMilli(data.averagePriceMilli),CurrencyFormatter.formatMilli(data.maxPriceMilli)));
            // Transmet les points à la courbe Canvas et la liste d'achats à l'adaptateur.
            chart.setHistory(data.history);adapter.submit(data.purchases);empty.setVisibility(data.purchases.isEmpty()?android.view.View.VISIBLE:android.view.View.GONE);
            // Affiche et câble le bouton d'accès à ProductPriceHistoryActivity si au moins un achat existe.
            if(fullHistoryButton!=null){
                fullHistoryButton.setVisibility(data.purchases.isEmpty()?android.view.View.GONE:android.view.View.VISIBLE);
                fullHistoryButton.setOnClickListener(v->startActivity(new android.content.Intent(this,ProductPriceHistoryActivity.class)
                        .putExtra("PRODUCT_ID",data.product.id)
                        .putExtra("PRODUCT_NAME",data.product.name)));
            }
        });
    }
}
