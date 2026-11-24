package com.example.masrouf1;

import android.content.res.ColorStateList;
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

final class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.Holder> {
    interface Listener { void onSelected(NotificationEntity row); }
    private final List<NotificationEntity> rows=new ArrayList<>();private final Listener listener;
    NotificationAdapter(Listener listener){this.listener=listener;}
    void submit(List<NotificationEntity> values){rows.clear();if(values!=null)rows.addAll(values);notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification,parent,false));}
    @Override public void onBindViewHolder(@NonNull Holder holder,int position){NotificationEntity row=rows.get(position);android.content.Context context=holder.itemView.getContext();holder.title.setText(row.title==null?context.getString(R.string.app_name):row.title);holder.message.setText(row.message==null?"":row.message);holder.date.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(row.createdAt)));holder.unread.setVisibility(row.read?View.GONE:View.VISIBLE);holder.card.setCardBackgroundColor(context.getColor(row.read?R.color.alabaster:R.color.mint_light));holder.itemView.setOnClickListener(v->{if(listener!=null)listener.onSelected(row);});}
    @Override public int getItemCount(){return rows.size();}
    static final class Holder extends RecyclerView.ViewHolder {final MaterialCardView card;final TextView title,message,date,unread;Holder(View root){super(root);card=(MaterialCardView)root;title=root.findViewById(R.id.notification_title);message=root.findViewById(R.id.notification_message);date=root.findViewById(R.id.notification_date);unread=root.findViewById(R.id.notification_unread_badge);}}
}
