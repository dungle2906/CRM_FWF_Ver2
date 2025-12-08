package com.example.BasicCRM_FWF.Service.AppUsageRecord;

import org.springframework.web.multipart.MultipartFile;

public interface AppUsageRecordInterface {
    void importFromExcel(MultipartFile file);
}
