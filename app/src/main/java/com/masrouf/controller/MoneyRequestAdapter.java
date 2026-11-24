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

final class MoneyRequestAdapter extends RecyclerView.Adapter<MoneyRequestAdapter.Holder> {
    private final List<MoneyRequestEntity> rows=new ArrayList<>();
    void submit(List<MoneyRequestEntity> values){rows.clear();if(values!=null)rows.addAll(values);notifyDataSetChanged();}
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_money_request,parent,false));}
    @Override public void onBindViewHolder(@NonNull Holder holder,int position){
        MoneyRequestEntity row=rows.get(position);holder.amount.setText(CurrencyFormatter.formatMilli(row.amountMilli));
        int status="accepted".equalsIgnoreCase(row.status)?R.string.money_request_accepted:"rejected".equalsIgnoreCase(row.status)?R.string.money_request_rejected:R.string.money_request_pending;
        holder.status.setText(status);holder.reason.setText(row.reason==null?"":row.reason);
        holder.date.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT).format(new Date(row.createdAt)));
    }
    @Override public int getItemCount(){return rows.size();}
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView amount,status,reason,date;
        Holder(View root){super(root);amount=root.findViewById(R.id.money_request_amount_value);status=root.findViewById(R.id.money_request_status_value);reason=root.findViewById(R.id.money_request_reason_value);date=root.findViewById(R.id.money_request_date_value);}
    }
}
