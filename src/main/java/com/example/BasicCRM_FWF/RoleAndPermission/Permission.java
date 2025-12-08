package com.example.BasicCRM_FWF.RoleAndPermission;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Permission {

    // 👤 USER (Nhân viên cửa hàng - chỉ thao tác cơ bản)
    USER_VIEW("user:viewData"),
    USER_INSERT("user:insertData"),
    USER_UPDATE("user:updateData"),
    USER_DELETE("user:deleteData"),

    // 🏪 STORE LEADER (Cửa hàng trưởng - quản lý 1 cửa hàng)
    STORE_LEADER_VIEW("store_leader:viewData"),
    STORE_LEADER_INSERT("store_leader:insertData"),
    STORE_LEADER_UPDATE("store_leader:updateData"),
    STORE_LEADER_DELETE("store_leader:deleteData"),

    // 🏬 AREA MANAGER (Quản lý khu vực - giám sát nhiều cửa hàng)
    AREA_MANAGER_VIEW("area_manager:viewData"),
    AREA_MANAGER_INSERT("area_manager:insertData"),
    AREA_MANAGER_UPDATE("area_manager:updateData"),
    AREA_MANAGER_DELETE("area_manager:deleteData"),

    // 🏢 TEAM LEAD (Trưởng phòng - quản lý nhiều khu vực)
    TEAM_LEAD_VIEW("team_lead:viewData"),
    TEAM_LEAD_INSERT("team_lead:insertData"),
    TEAM_LEAD_UPDATE("team_lead:updateData"),
    TEAM_LEAD_DELETE("team_lead:deleteData"),

    // 🌍 CEO (Giám đốc điều hành - quản lý toàn vùng lớn)
    CEO_VIEW("ceo:viewData"),
    CEO_INSERT("ceo:insertData"),
    CEO_UPDATE("ceo:updateData"),
    CEO_DELETE("ceo:deleteData"),

    // 🧑‍💼 ADMIN (Sếp tổng / Admin toàn hệ thống - full quyền)
    ADMIN_VIEW("admin:viewData"),
    ADMIN_INSERT("admin:insertData"),
    ADMIN_UPDATE("admin:updateData"),
    ADMIN_DELETE("admin:deleteData");

    private final String permissions;
}
