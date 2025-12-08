
package com.example.BasicCRM_FWF.Model;
import com.example.BasicCRM_FWF.RoleAndPermission.Role;
import com.example.BasicCRM_FWF.Token.SecureToken;
import com.example.BasicCRM_FWF.Token.Token;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@SuperBuilder
@Builder
@Table(name = "user")
public class User extends BaseEntity implements UserDetails {

//    @Id
//    @GeneratedValue( generator = "uuid2" )
//    @UuidGenerator
//    @Column(columnDefinition = "VARCHAR(36)")
//    @JdbcTypeCode(SqlTypes.VARCHAR)
//    private UUID id;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false, unique = true)
    private Integer id;

    @Column(length = 50)
    private String firstname;

    @Column(length = 50)
    private String lastname;

    @Column(unique = true, length = 100)
    private String username;

    @Column(unique = true, length = 100)
    private String email;

    @Column(unique = true, length = 11)
    private String phoneNumber;

    @Column()
    private String password;

    @Column()
    private Date dob;

    @Column(columnDefinition = "bit default 1")
    private Boolean gender;

    @Column(length = 255)
    private String bio;

    @Column(nullable = true)
    private String avatar;

    @Column
    private String provider;

    @Column
    private Boolean isActive;

    @Column
    private Boolean isVerified;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<Token> tokens;

    @OneToMany(mappedBy = "user", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @ToString.Exclude
    private List<SecureToken> secureTokens;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role.getAuthorities();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
