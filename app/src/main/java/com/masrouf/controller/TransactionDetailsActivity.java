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

/** Read-only details for a transaction owned by the signed-in account. */
public final class TransactionDetailsActivity extends BaseFeatureActivity {
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);showPage(R.string.transaction_details);getLayoutInflater().inflate(R.layout.activity_transaction_details,content,true);
        TextView title=findViewById(R.id.transaction_detail_title),amount=findViewById(R.id.transaction_detail_amount),date=findViewById(R.id.transaction_detail_date),kind=findViewById(R.id.transaction_detail_kind),note=findViewById(R.id.transaction_detail_note),empty=findViewById(R.id.transaction_detail_empty);
        long owner=SessionStore.userId(this),id=getIntent().getLongExtra("TRANSACTION_ID",0);MasroufViewModel model=new androidx.lifecycle.ViewModelProvider(this,new MasroufViewModelFactory(this)).get(MasroufViewModel.class);
        model.<TransactionEntity>request(repository->repository.transactionForUser(owner,id),result->{if(result.data==null){empty.setVisibility(View.VISIBLE);return;}TransactionEntity row=result.data;
            title.setText(row.label==null||row.label.trim().isEmpty()?getString(R.string.transaction_name):row.label);amount.setText(CurrencyFormatter.formatMilli(row.amountMilli));date.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT,Locale.getDefault()).format(new Date(row.occurredAt)));
            kind.setText(kindLabel(row.kind));if(row.note==null||row.note.trim().isEmpty())note.setVisibility(View.GONE);else note.setText(row.note);
        });
    }
    private String kindLabel(String value){if("purchase".equalsIgnoreCase(value))return getString(R.string.add_purchase_product);if("donation".equalsIgnoreCase(value))return getString(R.string.donation);if("allowance_received".equalsIgnoreCase(value))return getString(R.string.notification_gift_received);return getString(R.string.add_expense);}
}
