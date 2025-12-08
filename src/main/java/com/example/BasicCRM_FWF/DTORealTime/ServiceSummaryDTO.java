package com.example.BasicCRM_FWF.DTORealTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceSummaryDTO {
    private String totalServices;
    private String totalServicesServing;
    private String totalServiceDone;
    private List<ServiceItems> items;

    public String getTotalServices() {
        return totalServices;
    }

    public void setTotalServices(String totalServices) {
        this.totalServices = totalServices;
    }

    public String getTotalServicesServing() {
        return totalServicesServing;
    }

    public void setTotalServicesServing(String totalServicesServing) {
        this.totalServicesServing = totalServicesServing;
    }

    public String getTotalServiceDone() {
        return totalServiceDone;
    }

    public void setTotalServiceDone(String totalServiceDone) {
        this.totalServiceDone = totalServiceDone;
    }

    public List<ServiceItems> getItems() {
        return items;
    }

    public void setItems(List<ServiceItems> items) {
        this.items = items;
    }
}
