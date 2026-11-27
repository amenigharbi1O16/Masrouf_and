/*
 * ============================================================================
 * FICHIER : NotificationAdapter.java
 * MISSION : Adaptateur RecyclerView affichant le fil des notifications locales
 *           dans NotificationsActivity. Adapte dynamiquement la couleur de fond,
 *           la bordure et le badge de gravité selon le type d'alerte (dépassement
 *           rouge corail, alerte seuil 80 %/100 % ambre, information menthe/blanc).
 * IMPLÉMENTE : RecyclerView.Adapter<Holder> liant item_notification.xml avec
 *              formatage date/heure locale et listener de clic vers l'Activity.
 * EFFET À L'ÉCRAN : Cartes empilées indiquant titre, message détaillé, date,
 *                   pastille « Non lu » et pilule de gravité « Dépassement » / « Alerte ».
 * COMMUNICATIONS :
 *   ← NotificationsActivity (submit() et interface Listener.onSelected)
 *   ← NotificationEntity (données Room)
 * ============================================================================
 */
package com.example.masrouf1;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.masrouf1.db.MasroufEntities.NotificationEntity;
import com.google.android.material.card.MaterialCardView;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Adaptateur RecyclerView utilisé par NotificationsActivity (layout item_notification.xml).
 * Mission : lier chaque NotificationEntity à une carte stylée dynamiquement selon sa gravité :
 * rouge corail (budget_over / category_over / child_budget_over), ambre (seuils 80%/100% / grosses dépenses),
 * ou menthe/blanc (budget_closed, dotations, demandes). Remonte le clic vers NotificationAdapter.Listener.
 */
final class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.Holder> {
    interface Listener { void onSelected(NotificationEntity row); }

    private final List<NotificationEntity> rows = new ArrayList<>();
    private final Listener listener;

    NotificationAdapter(Listener listener) {
        this.listener = listener;
    }

    void submit(List<NotificationEntity> values) {
        rows.clear();
        if (values != null) rows.addAll(values);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        NotificationEntity row = rows.get(position);
        Context context = holder.itemView.getContext();
        String kind = row.kind == null ? "" : row.kind;
        boolean isOverrun = "budget_over".equals(kind) || "category_over".equals(kind) || "child_budget_over".equals(kind);
        boolean isAlert = !isOverrun && ("budget_caution".equals(kind) || "budget_100".equals(kind)
                || "category_caution".equals(kind) || "child_budget_caution".equals(kind)
                || "child_budget_100".equals(kind) || "important_purchase".equals(kind)
                || "important_expense".equals(kind) || "child_important_tx".equals(kind));

        holder.title.setText(row.title == null ? context.getString(R.string.app_name) : row.title);
        holder.message.setText(row.message == null ? "" : row.message);
        holder.date.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(new Date(row.createdAt)));
        holder.unread.setVisibility(row.read ? View.GONE : View.VISIBLE);

        float d = context.getResources().getDisplayMetrics().density;
        if (isOverrun) {
            holder.card.setCardBackgroundColor(context.getColor(row.read ? R.color.alabaster : R.color.danger_surface));
            holder.card.setStrokeColor(context.getColor(R.color.coral));
            holder.card.setStrokeWidth((int) (1.5f * d + 0.5f));
            holder.title.setTextColor(context.getColor(R.color.danger_text));
            holder.unread.setTextColor(context.getColor(R.color.danger_text));
            bindPill(holder.severity, context.getString(R.string.notif_badge_overrun),
                    context.getColor(R.color.danger_surface), context.getColor(R.color.danger_text), d);
        } else if (isAlert) {
            holder.card.setCardBackgroundColor(context.getColor(row.read ? R.color.alabaster : R.color.warning_surface));
            holder.card.setStrokeColor(context.getColor(R.color.amber));
            holder.card.setStrokeWidth((int) (1f * d + 0.5f));
            holder.title.setTextColor(context.getColor(R.color.warning_text));
            holder.unread.setTextColor(context.getColor(R.color.warning_text));
            bindPill(holder.severity, context.getString(R.string.notif_badge_alert),
                    context.getColor(R.color.warning_surface), context.getColor(R.color.warning_text), d);
        } else {
            holder.card.setCardBackgroundColor(context.getColor(row.read ? R.color.alabaster : R.color.mint_light));
            holder.card.setStrokeColor(context.getColor(R.color.card_border));
            holder.card.setStrokeWidth((int) (1f * d + 0.5f));
            holder.title.setTextColor(context.getColor(R.color.forest));
            holder.unread.setTextColor(context.getColor(R.color.forest));
            holder.severity.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onSelected(row);
        });
    }

    private static void bindPill(TextView badge, String text, int bgColor, int fgColor, float d) {
        badge.setVisibility(View.VISIBLE);
        badge.setText(text);
        badge.setTextColor(fgColor);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setStroke((int) (1f * d + 0.5f), fgColor);
        bg.setCornerRadius(999f);
        badge.setBackground(bg);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView title, message, date, unread, severity;

        Holder(View root) {
            super(root);
            card = (MaterialCardView) root;
            title = root.findViewById(R.id.notification_title);
            message = root.findViewById(R.id.notification_message);
            date = root.findViewById(R.id.notification_date);
            unread = root.findViewById(R.id.notification_unread_badge);
            severity = root.findViewById(R.id.notification_severity_badge);
        }
    }
}
