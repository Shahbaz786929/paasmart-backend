package com.paasmart.backend.tryon.dto;

public class TryOnResponse {

    private Long tryOnId;
    private String status;
    private String resultImageUrl;

    public TryOnResponse(Long tryOnId, String status, String resultImageUrl) {
        this.tryOnId = tryOnId;
        this.status = status;
        this.resultImageUrl = resultImageUrl;
    }

    public Long getTryOnId() { return tryOnId; }
    public void setTryOnId(Long tryOnId) { this.tryOnId = tryOnId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResultImageUrl() { return resultImageUrl; }
    public void setResultImageUrl(String resultImageUrl) { this.resultImageUrl = resultImageUrl; }
}