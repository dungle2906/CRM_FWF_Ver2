package com.example.BasicCRM_FWF.Repository;

import com.example.BasicCRM_FWF.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    User findByEmail(String email);
    Optional<User> findByPhoneNumber(String phone);
    User findUserById(Integer id);
}
