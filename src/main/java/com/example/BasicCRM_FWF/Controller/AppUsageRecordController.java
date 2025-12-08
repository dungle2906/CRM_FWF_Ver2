package com.example.BasicCRM_FWF.Controller;

import com.example.BasicCRM_FWF.Service.AppUsageRecord.AppUsageRecordInterface;
import com.example.BasicCRM_FWF.Service.AppUsageRecord.AppUsageRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// BÁO CÁO KHÁCH SỬ DỤNG APP
@RestController
@RequestMapping("/api/app-usage")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
// khach hang su dung app
public class AppUsageRecordController {

    private final AppUsageRecordInterface service;

    @PostMapping("/upload")
    @PreAuthorize("hasAuthority('admin:insertData')")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) {
        service.importFromExcel(file);
        return ResponseEntity.ok("Upload completed. Check logs for details." + file.getOriginalFilename());
    }

    @GetMapping("/test")
    public String test() {
        return "Hello from test controller";
    }

    @GetMapping("/helloTest")
    public String helloTest() {
        return "Hello from test controller";
    }
}