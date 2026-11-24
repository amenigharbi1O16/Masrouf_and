package com.masrouf.controller;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import com.example.masrouf1.CurrencyFormatter;
import com.example.masrouf1.MoneyMath;

/** Explicit listener that updates the per-unit price as form values change. */
public final class FormPriceTextWatcher implements TextWatcher {
    private final EditText totalPrice;
    private final EditText quantity;
    private final TextView result;

    public FormPriceTextWatcher(EditText totalPrice, EditText quantity, TextView result) {
        this.totalPrice=totalPrice; this.quantity=quantity; this.result=result;
    }
    @Override public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
    @Override public void onTextChanged(CharSequence s,int start,int before,int count) { }
    @Override public void afterTextChanged(Editable editable) {
        try {
            long price=MoneyInput.toMilli(totalPrice.getText().toString());
            long quantityMilli=MoneyInput.toMilli(quantity.getText().toString());
            if(price>0&&quantityMilli>0) result.setText(result.getContext().getString(
                    com.example.masrouf1.R.string.unit_price_result,
                    CurrencyFormatter.formatMilli(MoneyMath.pricePerUnitMilli(price,quantityMilli))));
            else result.setText(com.example.masrouf1.R.string.unit_price_not_available);
        } catch(RuntimeException ignored) {
            result.setText(com.example.masrouf1.R.string.unit_price_not_available);
        }
    }
}
