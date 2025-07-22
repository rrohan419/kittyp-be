package com.kittyp.common.service;

import java.util.List;
import org.springframework.stereotype.Service;

import com.kittyp.article.dao.ArticleDao;
import com.kittyp.article.enums.ArticleStatus;
import com.kittyp.common.model.AdminDashboardResponse;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.product.dao.ProductDao;
import com.kittyp.user.dao.UserDao;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService{
    
    private final UserDao userDao;
    private final ProductDao productDao;
    private final OrderDao orderDao;
    private final ArticleDao articleDao;
    
    @Override
    public AdminDashboardResponse getAdminDashboardData() {

        List<OrderStatus> orderStatuses = List.of(OrderStatus.SUCCESSFULL, OrderStatus.DELIVERED, OrderStatus.IN_TRANSIT, OrderStatus.PROCESSING);
        List<ArticleStatus> articleStatuses = List.of(ArticleStatus.PUBLISHED, ArticleStatus.DRAFT);
        
        Integer totalUsers = userDao.countActiveUsers();
        Integer totalProducts = productDao.productCount(true);
        Integer totalOrders = orderDao.countOfOrderByStatus(true, orderStatuses);
        Integer articleCount = articleDao.countByIsActiveAndStatusIn(true, articleStatuses);
        return new AdminDashboardResponse(totalProducts, totalOrders, totalUsers, articleCount);

    }
    
}
