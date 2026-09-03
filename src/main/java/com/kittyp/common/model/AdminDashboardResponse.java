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
    /** Doctors awaiting admin verification (documents submitted or under review). */
    private long pendingDoctorsCount;
    /** Total organization clinics (excludes personal doctor practices). */
    private long clinicsCount;
}
