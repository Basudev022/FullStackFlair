package com.fsf.habitup.Controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fsf.habitup.Service.FileStorageService;

@RestController
@RequestMapping("/habitup/files")
public class FileStorageController {
    private final FileStorageService fileStorageService;

    public FileStorageController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "others") String subFolder) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Please upload a non-empty file.");
        }

        try {
            String storedPath = fileStorageService.storeFile(file, subFolder);
            return ResponseEntity.ok("File uploaded successfully: " + storedPath);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("File upload failed: " + e.getMessage());
        }
    }
}