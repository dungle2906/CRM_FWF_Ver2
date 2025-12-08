package com.example.BasicCRM_FWF.Repository;

import com.example.BasicCRM_FWF.Model.OTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface OTPRepository extends JpaRepository<OTP, Long> {

    Optional<OTP> findByEmailAndOtp(String email, String otp);

    @Modifying
    @Query("DELETE FROM OTP o WHERE o.expiredAt <= :now")
    void deleteAllExpiredSince(LocalDateTime now);

    @Modifying
    @Query("DELETE FROM OTP o WHERE o.email = :email")
    void deleteByEmail(String email);
}