package com.example.BasicCRM_FWF.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class UserDTO {
    private Integer id;

//    @NotBlank(message = "First name cannot be empty")
//    @Size(min = 3, max = 50, message = "First name must be between 3 and 50 characters")
    private String firstname;

//    @NotBlank(message = "Last name cannot be empty")
//    @Size(min = 3, max = 50, message = "Last name must be between 3 and 50 characters")
    private String lastname;

//    @NotBlank(message = "Username cannot be empty")
//    @Size(min = 6, max = 30, message = "Username must be between 6 and 30 characters")
    private String username;

//    @NotBlank(message = "Email cannot be empty")
//    @Email(message = "Invalid email format")
    private String email;

//    @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone number must be between 10 and 15 digits")
    private String phoneNumber;

//    @Past(message = "Date of birth must be in the past")
    private Date dob;

    private Boolean gender;

//    @Size(max = 255, message = "Bio cannot exceed 255 characters")
//    @Pattern(regexp = "^[a-zA-Z0-9 .,!?]+$", message = "Bio contains invalid characters")
    private String bio;

//    @Pattern(regexp = "^(http|https)://.*$", message = "Avatar must be a valid URL")
    private String avatar;

    private boolean isActive;

    private boolean isDeleted;

    private String role;
}
