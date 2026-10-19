package com.paasmart.backend.common;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/uploads")
public class FileUploadController {

    @Autowired
    private CloudinaryService cloudinaryService;

    @PostMapping("/image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        // CloudinaryService validates the file (not empty, allowed image type)
        // and uploads it — no more saving to local disk, which was ephemeral
        // and got wiped on every redeploy/restart on most cloud hosts.
        String url = cloudinaryService.uploadImage(file, "general");
        return ResponseEntity.ok(Map.of("url", url));
    }
}