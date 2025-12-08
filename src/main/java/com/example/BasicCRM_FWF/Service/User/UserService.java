package com.example.BasicCRM_FWF.Service.User;

import com.example.BasicCRM_FWF.DTO.ChangePasswordRequest;
import com.example.BasicCRM_FWF.DTO.PageableResponse;
import com.example.BasicCRM_FWF.DTO.UserDTO;
import com.example.BasicCRM_FWF.Exception.ResourceNotFoundException;
import com.example.BasicCRM_FWF.Helper.Helper;
import com.example.BasicCRM_FWF.Model.User;
import com.example.BasicCRM_FWF.Repository.UserRepository;
import com.example.BasicCRM_FWF.RoleAndPermission.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserServiceInterface {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final ModelMapper mapper;

    @Override
    public void  changePassword(ChangePasswordRequest request, Principal connectedUser) {
        User userFind = getUserByPrincipal(connectedUser);

        if (request.getConfirmPassword().length() < 8 || request.getConfirmPassword().length() > 50) {
            throw new IllegalArgumentException("Password must be between 8 and 50 characters");
        }

        if (!passwordEncoder.matches(request.getCurrentPassword(), userFind.getPassword())) {
            throw new BadCredentialsException("Wrong password");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadCredentialsException("Passwords and Confirm Passwords do not match");
        }

        // ✅ Kiểm tra mật khẩu mới không được trùng với mật khẩu hiện tại
        if (passwordEncoder.matches(request.getNewPassword(), userFind.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        userFind.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(userFind);
    }

    private User getUserByPrincipal(Principal connectedUser) {
        int userId;

        if (connectedUser instanceof JwtAuthenticationToken jwtToken) {
            Object userIdClaim = jwtToken.getToken().getClaims().get("userId");

            if (userIdClaim instanceof Number number) {
                userId = number.intValue();
            } else {
                throw new IllegalArgumentException("Invalid userId claim in JWT");
            }
        } else {
            throw new IllegalArgumentException("Unsupported principal type: " + connectedUser.getClass().getName());
        }

        return userRepository.findUserById(userId);
    }

    @Override
    public PageableResponse<UserDTO> getAllUsers(
            int pageNumber, int pageSize, String sortBy, String sortDir) {
        Sort sort = (sortDir.equalsIgnoreCase("desc")) ? (Sort.by(sortBy).descending()) : (Sort.by(sortBy).ascending());
        Pageable pageable = PageRequest.of(pageNumber, pageSize, sort);
        Page<User> page = userRepository.findAll(pageable);
        return Helper.getPageableResponse(page, UserDTO.class);
    }

    @Override
    public UserDTO setUserRole(UserDTO userDTO, Integer userId, Principal connectedUser) throws BadCredentialsException {
        User userFind = getUserByPrincipal(connectedUser);

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found with given ID: " + userId));

        if(user.getUsername().equals("admin")) throw new BadCredentialsException("This privilege cannot be set role!");

        if(userFind.equals(user)) throw new BadCredentialsException("This account cannot set role for itself!");

        // ✅ Dùng giá trị role từ request (userDTO)
        String newRole = userDTO.getRole();
        if (newRole == null || newRole.isBlank()) {
            throw new IllegalArgumentException("Role must not be null or empty");
        }

        switch (newRole) {
            case "USER" -> user.setRole(Role.USER);
            case "STORE_LEADER" -> user.setRole(Role.STORE_LEADER);
            case "AREA_MANAGER" -> user.setRole(Role.AREA_MANAGER);
            case "TEAM_LEAD" -> user.setRole(Role.TEAM_LEAD);
            case "CEO" -> user.setRole(Role.CEO);
            case "ADMIN" -> user.setRole(Role.ADMIN);
            default -> throw new IllegalArgumentException("Invalid role: " + user.getRole());
        }

        userRepository.save(user);
        return mapper.map(user, UserDTO.class);
    }

    @Override
    public UserDTO banOrUbanUser(UserDTO userDTO, Integer userId, Principal connectedUser) throws BadCredentialsException {
        User userFind = getUserByPrincipal(connectedUser);

        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found along with given ID!"));

        if(user.getUsername().equals("admin")) throw new BadCredentialsException("This privilege cannot be banned!");

        if (userFind.equals(user)) throw new BadCredentialsException("This account cannot be ban by itself!");
        user.setIsActive(userDTO.isActive());
        user.setUpdatedBy(userFind.getRole().toString());
        user.setUpdatedAt(LocalDateTime.now());
        User userSaved = userRepository.save(user);
        return mapper.map(userSaved, UserDTO.class);
    }

    @Override
    public void deleteUser(Integer userId, Principal connectedUser) throws BadCredentialsException {
        User userFind = getUserByPrincipal(connectedUser);
        User user = userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User not found along with given ID!"));

        if(user.getUsername().equals("admin")) throw new BadCredentialsException("This privilege cannot be deleted!");

        if (userFind.equals(user)) throw new BadCredentialsException("This account cannot be deleted by itself!");

        userRepository.delete(user);
    }

}
