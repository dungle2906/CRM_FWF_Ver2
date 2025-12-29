package com.example.BasicCRM_FWF.Controller;

import com.example.BasicCRM_FWF.DTO.ChangePasswordRequest;
import com.example.BasicCRM_FWF.DTO.PageableResponse;
import com.example.BasicCRM_FWF.DTO.UserDTO;
import com.example.BasicCRM_FWF.Model.User;
import com.example.BasicCRM_FWF.Service.User.UserService;
import com.example.BasicCRM_FWF.Service.User.UserServiceInterface;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceInterface userService;

    @PatchMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            Principal connectedUser
    ){
        userService.changePassword(request, connectedUser);
        return ResponseEntity.ok().build(); 
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/get-all-users")
    public PageableResponse<UserDTO> getAllUsers(int pageNumber, int pageSize, String sortBy, String sortDir) {
        return userService.getAllUsers(pageNumber, pageSize, sortBy, sortDir);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/setUserRole/{userId}")
    public ResponseEntity<?> setUserRole(
            @PathVariable Integer userId,
            @RequestBody UserDTO userDTO,
            Principal connectedUser
    ) throws BadCredentialsException {
        UserDTO userDto = userService.setUserRole(userDTO, userId, connectedUser);
        return new ResponseEntity<>(userDto, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/banUser/{userId}")
    public ResponseEntity<?> banUser(
            @PathVariable Integer userId,
            @Valid @RequestBody UserDTO userDTO,
            Principal connectedUser
    ) throws BadCredentialsException {
        UserDTO userDto = userService.banOrUbanUser(userDTO, userId, connectedUser);
        return new ResponseEntity<>(userDto, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/delete-user/{userId}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Integer userId,
            Principal connectedUser
    ) throws BadCredentialsException {
        userService.deleteUser(userId, connectedUser);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
