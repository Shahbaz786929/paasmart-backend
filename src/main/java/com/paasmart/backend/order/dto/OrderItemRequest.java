package com.paasmart.backend.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class OrderItemRequest {

    @NotNull(message = "Product ID zaroori hai")
    private Long productId;

    @NotNull(message = "Quantity zaroori hai")
    @Min(value = 1, message = "Quantity kam se kam 1 honi chahiye")
    private Integer quantity;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}