package com.paasmart.backend.search;

import com.paasmart.backend.product.Product;
import com.paasmart.backend.seller.dto.ShopSummaryResponse;

import java.util.List;

public class SearchResponse {

    private List<ShopSummaryResponse> shops;
    private List<Product> products;

    public SearchResponse(List<ShopSummaryResponse> shops, List<Product> products) {
        this.shops = shops;
        this.products = products;
    }

    public List<ShopSummaryResponse> getShops() { return shops; }
    public void setShops(List<ShopSummaryResponse> shops) { this.shops = shops; }
    public List<Product> getProducts() { return products; }
    public void setProducts(List<Product> products) { this.products = products; }
}