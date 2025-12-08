package com.example.BasicCRM_FWF.Service.User;

import com.example.BasicCRM_FWF.DTO.ChangePasswordRequest;
import com.example.BasicCRM_FWF.DTO.PageableResponse;
import com.example.BasicCRM_FWF.DTO.UserDTO;
import jakarta.validation.Valid;
import org.apache.coyote.BadRequestException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

@Service
public interface UserServiceInterface {
    void changePassword(ChangePasswordRequest request, Principal connectedUser);

    PageableResponse<UserDTO> getAllUsers(int pageNumber, int pageSize, String sortBy, String sortDir);

    UserDTO setUserRole(UserDTO userDTO, Integer userId, Principal connectedUser) throws BadCredentialsException;

    UserDTO banOrUbanUser(UserDTO userDTO, Integer userId, Principal connectedUser) throws BadCredentialsException;

    void deleteUser(Integer userId, Principal connectedUser) throws BadCredentialsException;
}
