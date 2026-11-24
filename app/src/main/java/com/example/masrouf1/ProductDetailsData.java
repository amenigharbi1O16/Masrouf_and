package com.example.masrouf1;

import com.example.masrouf1.db.MasroufEntities.PriceHistoryEntity;
import com.example.masrouf1.db.MasroufEntities.ProductEntity;
import com.example.masrouf1.db.PurchaseHistoryRow;
import java.math.BigInteger;
import java.util.List;

/** Product details and price history loaded with the authenticated owner's scope. */
public final class ProductDetailsData {
    public final ProductEntity product;
    public final List<PriceHistoryEntity> history;
    public final List<PurchaseHistoryRow> purchases;
    public final Long minPriceMilli,averagePriceMilli,maxPriceMilli;
    public ProductDetailsData(ProductEntity product,List<PriceHistoryEntity> history,List<PurchaseHistoryRow> purchases){
        this.product=product;this.history=history;this.purchases=purchases;
        if(history==null||history.isEmpty()){minPriceMilli=null;averagePriceMilli=null;maxPriceMilli=null;}
        else {long min=Long.MAX_VALUE,max=Long.MIN_VALUE;BigInteger total=BigInteger.ZERO;for(PriceHistoryEntity row:history){min=Math.min(min,row.priceMilli);max=Math.max(max,row.priceMilli);total=total.add(BigInteger.valueOf(row.priceMilli));}minPriceMilli=min;maxPriceMilli=max;averagePriceMilli=total.divide(BigInteger.valueOf(history.size())).longValueExact();}
    }
}
