package com.paasmart.backend.tryon;

import java.util.Map;

public interface TryOnProvider {

    // Try-on job start karo, ek jobId return karega
    String startJob(String userPhotoUrl, String clothImageUrl, String category);

    // jobId se status check karo — Map me "status" (PROCESSING/COMPLETED/FAILED) aur "resultUrl" hoga
    Map<String, Object> checkJobStatus(String jobId);
}