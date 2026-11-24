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

/** Read-only, owner-scoped history of persisted purchases for a product. */
public final class PurchaseHistoryAdapter extends RecyclerView.Adapter<PurchaseHistoryAdapter.Holder> {
    private final List<PurchaseHistoryRow> rows=new ArrayList<>();
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase_history,parent,false));}
    @Override public void onBindViewHolder(@NonNull Holder holder,int position){
        PurchaseHistoryRow row=rows.get(position);String date=new SimpleDateFormat("dd/MM/yyyy",Locale.getDefault()).format(new Date(row.occurredAt));
        String quantity=BigDecimal.valueOf(row.quantityMilli,3).stripTrailingZeros().toPlainString()+" "+(row.unit==null?"":row.unit);
        String store=row.storeName==null||row.storeName.trim().isEmpty()?holder.itemView.getContext().getString(R.string.no_store_recorded):row.storeName;
        holder.title.setText(holder.itemView.getContext().getString(R.string.purchase_history_item,quantity,store,date));
        holder.price.setText(CurrencyFormatter.formatMilli(row.totalMilli));
    }
    @Override public int getItemCount(){return rows.size();}
    public void submit(List<PurchaseHistoryRow> values){rows.clear();if(values!=null)rows.addAll(values);notifyDataSetChanged();}
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title,price;
        Holder(@NonNull View item){super(item);title=item.findViewById(R.id.purchase_history_title);price=item.findViewById(R.id.purchase_history_amount);}
    }
}
