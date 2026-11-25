package com.masrouf.controller;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.MasroufViewModel;
import com.example.masrouf1.MasroufViewModelFactory;
import com.example.masrouf1.R;
import com.example.masrouf1.db.MasroufEntities.ProductEntity;
import com.example.masrouf1.db.MasroufEntities.PurchaseItemEntity;
import com.example.masrouf1.db.PurchaseHistoryRow;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * ACTIVITÉ : ProductPriceHistoryActivity
 * RÔLE : Affiche l'historique chronologique des achats d'un produit donné,
 *         avec l'évolution des prix unitaires dans le temps.
 *
 * PARAMÈTRES ATTENDUS EN EXTRA :
 *   - "PRODUCT_ID" (long) : identifiant du produit
 *   - "PRODUCT_NAME" (String) : nom du produit à afficher dans l'en-tête
 *
 * CORRESPONDANCE REACT NATIVE : src/app/produit/historique.tsx
 */
public final class ProductPriceHistoryActivity extends AppCompatActivity {

    private MasroufViewModel viewModel;
    private RecyclerView recyclerView;
    private View loadingView;
    private View emptyView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_price_history);

        // Récupération des paramètres de navigation
        long productId = getIntent().getLongExtra("PRODUCT_ID", 0L);
        String productName = getIntent().getStringExtra("PRODUCT_NAME");
        if (productName == null) productName = getString(R.string.product_name);

        // Liaisons avec les vues
        TextView titleView = findViewById(R.id.price_history_product_name);
        TextView subtitleView = findViewById(R.id.price_history_subtitle);
        loadingView = findViewById(R.id.price_history_loading);
        emptyView = findViewById(R.id.price_history_empty);
        recyclerView = findViewById(R.id.price_history_list);

        titleView.setText(productName);
        subtitleView.setText(R.string.price_history_subtitle);

        // Flèche retour
        View backButton = findViewById(R.id.price_history_back);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Configuration du RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        PriceHistoryAdapter adapter = new PriceHistoryAdapter();
        recyclerView.setAdapter(adapter);

        // Initialisation du ViewModel et chargement des données
        viewModel = new androidx.lifecycle.ViewModelProvider(
                this, new MasroufViewModelFactory(this)
        ).get(MasroufViewModel.class);

        if (productId > 0) {
            loadHistory(productId, adapter);
        } else {
            showEmpty();
        }
    }

    /**
     * Chargement de l'historique des achats depuis la base Room.
     * Affiche : date, prix total, quantité, prix unitaire, magasin.
     */
    private void loadHistory(long productId, PriceHistoryAdapter adapter) {
        viewModel.<List<PurchaseHistoryRow>>request(
                repository -> repository.purchaseHistoryForProduct(productId, SessionStore.userId(this)),
                state -> {
                    loadingView.setVisibility(View.GONE);
                    if (state.failed || state.data == null || state.data.isEmpty()) {
                        showEmpty();
                        return;
                    }
                    recyclerView.setVisibility(View.VISIBLE);
                    adapter.setItems(state.data);
                }
        );
    }

    private void showEmpty() {
        loadingView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
    }

    // ─────────────────────────── Adapter interne ───────────────────────────

    /**
     * Adaptateur pour la liste d'historique des prix.
     * Chaque élément représente un achat passé du produit.
     */
    private final class PriceHistoryAdapter extends RecyclerView.Adapter<PriceHistoryAdapter.Holder> {

        private List<PurchaseHistoryRow> items = new ArrayList<>();

        void setItems(List<PurchaseHistoryRow> newItems) {
            items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // Création programmatique de la ligne d'historique
            LinearLayout card = new LinearLayout(ProductPriceHistoryActivity.this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackgroundResource(R.drawable.card_white_rounded);
            int dp8 = dp(8);
            int dp12 = dp(12);
            int dp16 = dp(16);
            card.setPadding(dp16, dp12, dp16, dp12);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.bottomMargin = dp8;
            card.setLayoutParams(cardParams);

            // Ligne supérieure : date + prix total
            LinearLayout topRow = new LinearLayout(ProductPriceHistoryActivity.this);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView dateLabel = new TextView(ProductPriceHistoryActivity.this);
            dateLabel.setTag("date");
            dateLabel.setTextColor(getColor(R.color.muted_green));
            dateLabel.setTextSize(13f);
            topRow.addView(dateLabel, new LinearLayout.LayoutParams(0, -2, 1));

            TextView priceLabel = new TextView(ProductPriceHistoryActivity.this);
            priceLabel.setTag("price");
            priceLabel.setTextColor(getColor(R.color.forest));
            priceLabel.setTextSize(16f);
            priceLabel.setTypeface(null, android.graphics.Typeface.BOLD);
            topRow.addView(priceLabel, new LinearLayout.LayoutParams(-2, -2));

            card.addView(topRow, new LinearLayout.LayoutParams(-1, -2));

            // Ligne inférieure : quantité + unité + magasin
            TextView detailLabel = new TextView(ProductPriceHistoryActivity.this);
            detailLabel.setTag("detail");
            detailLabel.setTextColor(getColor(R.color.muted_green));
            detailLabel.setTextSize(12f);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
            detailParams.topMargin = dp(4);
            card.addView(detailLabel, detailParams);

            // Prix unitaire mis en valeur
            TextView unitPriceLabel = new TextView(ProductPriceHistoryActivity.this);
            unitPriceLabel.setTag("unit_price");
            unitPriceLabel.setTextColor(getColor(R.color.emerald));
            unitPriceLabel.setTextSize(13f);
            LinearLayout.LayoutParams upParams = new LinearLayout.LayoutParams(-1, -2);
            upParams.topMargin = dp(2);
            card.addView(unitPriceLabel, upParams);

            return new Holder(card);
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            PurchaseHistoryRow row = items.get(position);
            DateFormat fmt = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());

            // Date
            TextView dateLabel = holder.itemView.findViewWithTag("date");
            if (dateLabel != null) {
                dateLabel.setText(fmt.format(new Date(row.occurredAt)));
            }

            // Prix total
            TextView priceLabel = holder.itemView.findViewWithTag("price");
            if (priceLabel != null) {
                priceLabel.setText(CurrencyFormatter.formatMilli(row.totalMilli));
            }

            // Détails : qté + magasin
            TextView detailLabel = holder.itemView.findViewWithTag("detail");
            if (detailLabel != null) {
                StringBuilder detail = new StringBuilder();
                if (row.quantityMilli > 0) {
                    detail.append(getString(R.string.quantity_label))
                          .append(" × ")
                          .append(String.format(Locale.getDefault(), "%.2f", row.quantityMilli / 1000.0));
                }
                if (row.storeName != null && !row.storeName.isEmpty()) {
                    if (detail.length() > 0) detail.append("  ·  ");
                    detail.append(row.storeName);
                }
                detailLabel.setText(detail.toString());
            }

            // Prix unitaire
            TextView unitPriceLabel = holder.itemView.findViewWithTag("unit_price");
            if (unitPriceLabel != null && row.unitPriceMilli > 0) {
                String unit = row.unit != null ? row.unit : "pièce";
                unitPriceLabel.setText(
                        CurrencyFormatter.formatMilli(row.unitPriceMilli) + " / " + unit
                );
                unitPriceLabel.setVisibility(View.VISIBLE);
            } else if (unitPriceLabel != null) {
                unitPriceLabel.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        final class Holder extends RecyclerView.ViewHolder {
            Holder(View view) { super(view); }
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }
}
