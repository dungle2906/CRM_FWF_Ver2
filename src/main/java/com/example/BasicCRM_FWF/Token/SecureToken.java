package com.example.BasicCRM_FWF.Token;

import com.example.BasicCRM_FWF.Model.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "secureTokens")
public class SecureToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String token;

    @Column(updatable = false)
    @Basic(optional = false)
    private LocalDateTime expiredAt;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    public boolean isExpired() {
        return getExpiredAt().isBefore(LocalDateTime.now());
    }
}
