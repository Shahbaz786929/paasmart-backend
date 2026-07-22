package com.paasmart.backend.returns.dto;

import java.math.BigDecimal;

public class ResolveReturnDto {

    private boolean approved;
    private String rejectionReason;      // agar approved=false
    private BigDecimal refundAmount;     // agar approved=true

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public void setRefundAmount(BigDecimal refundAmount) { this.refundAmount = refundAmount; }
}