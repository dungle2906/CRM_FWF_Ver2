package com.example.BasicCRM_FWF.DTORealTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceItems {
    private String serviceName;
    private String serviceUsageAmount;
    private String serviceUsagePercentage;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceUsageAmount() {
        return serviceUsageAmount;
    }

    public void setServiceUsageAmount(String serviceUsageAmount) {
        this.serviceUsageAmount = serviceUsageAmount;
    }

    public String getServiceUsagePercentage() {
        return serviceUsagePercentage;
    }

    public void setServiceUsagePercentage(String serviceUsagePercentage) {
        this.serviceUsagePercentage = serviceUsagePercentage;
    }
}
