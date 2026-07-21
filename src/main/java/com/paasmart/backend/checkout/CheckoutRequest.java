package com.paasmart.backend.checkout;

public class CheckoutRequest {

    private Long addressId;
    private String paymentMethod;
    private String couponCode;
    private Boolean useWallet = false;

    public CheckoutRequest() {
    }

    public Long getAddressId() {
        return addressId;
    }

    public void setAddressId(Long addressId) {
        this.addressId = addressId;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public Boolean getUseWallet() { return useWallet; }
    public void setUseWallet(Boolean useWallet) { this.useWallet = useWallet; }
}