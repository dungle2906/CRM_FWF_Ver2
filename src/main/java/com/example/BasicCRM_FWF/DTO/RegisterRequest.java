package com.example.BasicCRM_FWF.DTO;

import com.example.BasicCRM_FWF.RoleAndPermission.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    private String username;
    private String emailOrPhoneNumber;
    private String password;
}
