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

final class RecentTransactionsAdapter extends RecyclerView.Adapter<RecentTransactionsAdapter.Holder> {
    interface Listener { void onSelected(TransactionRow row); }
    private final List<TransactionRow> rows=new ArrayList<>(); private final Listener listener;
    RecentTransactionsAdapter(Listener listener) { this.listener=listener; }
    void submit(List<TransactionRow> values) { rows.clear(); if(values!=null) rows.addAll(values); notifyDataSetChanged(); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int type) { return new Holder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction,parent,false)); }
    @Override public void onBindViewHolder(@NonNull Holder holder,int position) {
        TransactionRow row=rows.get(position); holder.title.setText(row.title); holder.date.setText(DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(row.occurredAt)));
        holder.amount.setText("− "+CurrencyFormatter.formatMilli(row.amountMilli)); holder.itemView.setOnClickListener(v->listener.onSelected(row));
    }
    @Override public int getItemCount() { return rows.size(); }
    static final class Holder extends RecyclerView.ViewHolder {
        final TextView title,date,amount;
        Holder(View view) { super(view); title=view.findViewById(R.id.transaction_title); date=view.findViewById(R.id.transaction_date); amount=view.findViewById(R.id.transaction_amount); }
    }
}
