package com.paasmart.backend.order.dto;

import jakarta.validation.constraints.NotBlank;

public class OrderStatusUpdateRequest {

    @NotBlank(message = "Status zaroori hai")
    private String status;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}