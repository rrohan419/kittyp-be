package com.kittyp.common.service;

import java.util.List;
import org.springframework.stereotype.Service;

import com.kittyp.article.dao.ArticleDao;
import com.kittyp.article.enums.ArticleStatus;
import com.kittyp.clinic.repository.ClinicRepository;
import com.kittyp.common.model.AdminDashboardResponse;
import com.kittyp.doctor.enums.DoctorStatus;
import com.kittyp.doctor.repository.DoctorProfileRepository;
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
    private final DoctorProfileRepository doctorProfileRepository;
    private final ClinicRepository clinicRepository;
    
    @Override
    public AdminDashboardResponse getAdminDashboardData() {

        List<OrderStatus> orderStatuses = List.of(OrderStatus.SUCCESSFULL, OrderStatus.DELIVERED, OrderStatus.IN_TRANSIT, OrderStatus.PROCESSING);
        List<ArticleStatus> articleStatuses = List.of(ArticleStatus.PUBLISHED, ArticleStatus.DRAFT, ArticleStatus.SCHEDULED);
        List<DoctorStatus> pendingDoctorStatuses = List.of(DoctorStatus.DOCUMENTS_SUBMITTED, DoctorStatus.UNDER_REVIEW);
        
        Integer totalUsers = userDao.countActiveUsers();
        Integer totalProducts = productDao.productCount(true);
        Integer totalOrders = orderDao.countOfOrderByStatus(true, orderStatuses);
        Integer articleCount = articleDao.countByIsActiveAndStatusIn(true, articleStatuses);
        long pendingDoctorsCount = doctorProfileRepository.countByStatusIn(pendingDoctorStatuses);
        long clinicsCount = clinicRepository.countOrganizationClinics();
        return new AdminDashboardResponse(
                totalProducts, totalOrders, totalUsers, articleCount, pendingDoctorsCount, clinicsCount);

    }
    
}
