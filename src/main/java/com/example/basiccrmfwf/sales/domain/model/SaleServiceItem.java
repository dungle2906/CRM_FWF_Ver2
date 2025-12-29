package com.example.basiccrmfwf.sales.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Domain model representing a service item in a sales transaction.
 * Pure domain model - no JPA annotations.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaleServiceItem {
    private Long id;
    private Integer quantity;
    private SalesTransaction salesTransaction;
    private ServiceType serviceType;
    
    /**
     * Domain model for ServiceType (simplified for now).
     * This will be properly modeled when we refactor service domain.
     */
    public static class ServiceType {
        private Long id;
        private String serviceName;
        private String serviceCode;
        private String category;
        
        // Getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getServiceName() { return serviceName; }
        public void setServiceName(String serviceName) { this.serviceName = serviceName; }
        public String getServiceCode() { return serviceCode; }
        public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
    }
}
