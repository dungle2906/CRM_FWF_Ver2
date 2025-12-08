package com.example.BasicCRM_FWF.RoleAndPermission;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.BasicCRM_FWF.RoleAndPermission.Permission.*;

@Getter
@RequiredArgsConstructor
public enum Role {

    // 👤 Nhân viên cửa hàng — chỉ xem và thao tác cơ bản
    USER(
            Set.of(
                    USER_VIEW, USER_INSERT, USER_UPDATE, USER_DELETE
            )
    ),

    // 🏪 Cửa hàng trưởng — quản lý 1 cửa hàng
    STORE_LEADER(
            Set.of(
                    STORE_LEADER_VIEW, STORE_LEADER_INSERT, STORE_LEADER_UPDATE, STORE_LEADER_DELETE,
                    USER_VIEW, USER_INSERT, USER_UPDATE, USER_DELETE
            )
    ),

    // 🏬 Quản lý cửa hàng khu vực (giám sát nhiều cửa hàng)
    AREA_MANAGER(
            Set.of(
                    AREA_MANAGER_VIEW, AREA_MANAGER_INSERT, AREA_MANAGER_UPDATE, AREA_MANAGER_DELETE,
                    STORE_LEADER_VIEW, STORE_LEADER_INSERT, STORE_LEADER_UPDATE, STORE_LEADER_DELETE,
                    USER_VIEW, USER_INSERT, USER_UPDATE, USER_DELETE
            )
    ),

    // 🏢 Trưởng phòng (quản lý nhiều khu vực)
    TEAM_LEAD(
            Set.of(
                    TEAM_LEAD_VIEW, TEAM_LEAD_INSERT, TEAM_LEAD_UPDATE, TEAM_LEAD_DELETE,
                    AREA_MANAGER_VIEW, AREA_MANAGER_INSERT, AREA_MANAGER_UPDATE, AREA_MANAGER_DELETE,
                    STORE_LEADER_VIEW, STORE_LEADER_INSERT, STORE_LEADER_UPDATE, STORE_LEADER_DELETE,
                    USER_VIEW, USER_INSERT, USER_UPDATE, USER_DELETE
            )
    ),

    // 🌍 Sếp (quản lý các phòng ban hoặc vùng lớn)
    CEO(
            Set.of(
                    CEO_VIEW, CEO_INSERT, CEO_UPDATE, CEO_DELETE,
                    TEAM_LEAD_VIEW, TEAM_LEAD_INSERT, TEAM_LEAD_UPDATE, TEAM_LEAD_DELETE,
                    AREA_MANAGER_VIEW, AREA_MANAGER_INSERT, AREA_MANAGER_UPDATE, AREA_MANAGER_DELETE,
                    STORE_LEADER_VIEW, STORE_LEADER_INSERT, STORE_LEADER_UPDATE, STORE_LEADER_DELETE,
                    USER_VIEW, USER_INSERT, USER_UPDATE, USER_DELETE
            )
    ),

    // 🧑‍💼 Sếp tổng / Admin toàn hệ thống
    ADMIN(
            Set.of(
                    ADMIN_VIEW, ADMIN_INSERT, ADMIN_UPDATE, ADMIN_DELETE,
                    CEO_VIEW, CEO_INSERT, CEO_UPDATE, CEO_DELETE,
                    TEAM_LEAD_VIEW, TEAM_LEAD_INSERT, TEAM_LEAD_UPDATE, TEAM_LEAD_DELETE,
                    AREA_MANAGER_VIEW, AREA_MANAGER_INSERT, AREA_MANAGER_UPDATE, AREA_MANAGER_DELETE,
                    STORE_LEADER_VIEW, STORE_LEADER_INSERT, STORE_LEADER_UPDATE, STORE_LEADER_DELETE,
                    USER_VIEW, USER_INSERT, USER_UPDATE, USER_DELETE
            )
    );

    private final Set<Permission> permissions;

    public List<SimpleGrantedAuthority> getAuthorities() {
        var authorities = getPermissions()
                .stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getPermissions()))
                .collect(Collectors.toList());
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        return authorities;
    };
}
