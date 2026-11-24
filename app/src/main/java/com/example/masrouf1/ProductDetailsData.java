package com.example.masrouf1;

import com.example.masrouf1.db.MasroufEntities.PriceHistoryEntity;
import com.example.masrouf1.db.MasroufEntities.ProductEntity;
import java.util.List;

/** Product details and price history loaded with the authenticated owner's scope. */
public final class ProductDetailsData {
    public final ProductEntity product;
    public final List<PriceHistoryEntity> history;
    public ProductDetailsData(ProductEntity product,List<PriceHistoryEntity> history){this.product=product;this.history=history;}
}
