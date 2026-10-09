package com.kittyp.common.model;

import java.util.List;

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
    /** All doctor profiles, same set as the admin doctor list with no status filter. */
    private long doctorsCount;
    /** Accounts created since the start of the current month. */
    private long usersJoinedThisMonth;
    /** Doctors in Verified or Published status. */
    private long verifiedDoctorsCount;
    /** Organization clinics with status VERIFIED. */
    private long verifiedClinicsCount;
    /** Organization clinics with status PENDING. */
    private long pendingClinicsCount;
    /** Signups per day from the 1st of this month through today. */
    private List<Long> userSignupsByDay;
    /** Orders per day from the 1st of this month through today. */
    private List<Long> ordersByDay;
}
