package com.example.BasicCRM_FWF.Controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('STORE_LEADER', 'AREA_MANAGER', 'TEAM_LEAD', 'CEO', 'ADMIN')")
public class AdminController {

    @GetMapping
    @PreAuthorize("hasAuthority('admin:viewData')")
    public String get(){
        return "GET: admin controller";
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin:insertData')")
    public String post(){
        return "POST: admin controller";
    }

    @PutMapping
    @PreAuthorize("hasAuthority('admin:updateData')")
    public String put(){
        return "PUT: admin controller";
    }

    @DeleteMapping
    @PreAuthorize("hasAuthority('admin:deleteData')")
    public String delete(){
        return "DELETE: admin controller";
    }
}
