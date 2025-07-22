package com.kittyp.user.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminStatsModel {
    private long totalUsers;
    private Long totalOrders;
    private Long totalProducts;
    private Long totalArticles;
}
