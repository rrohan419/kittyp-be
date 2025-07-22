package com.kittyp.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminDashboardResponse {
    
    private long productCount;
    private long totalOrders;
    private long usersCount;
    private long articleCount;
}
