package com.kittyp.common.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

import com.kittyp.article.dao.ArticleDao;
import com.kittyp.article.enums.ArticleStatus;
import com.kittyp.clinic.enums.ClinicStatus;
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
        List<DoctorStatus> verifiedDoctorStatuses = List.of(DoctorStatus.VERIFIED, DoctorStatus.PUBLISHED);
        
        LocalDate today = LocalDate.now();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime nextDay = today.plusDays(1).atStartOfDay();

        long totalUsers = userDao.countAllUsers();
        List<Long> userSignupsByDay = fillMonth(userDao.countCreatedByDay(monthStart, nextDay), today);
        long usersJoinedThisMonth = userSignupsByDay.stream().mapToLong(Long::longValue).sum();
        Integer totalProducts = productDao.productCount(true);
        Integer totalOrders = orderDao.countOfOrderByStatus(true, orderStatuses);
        List<Long> ordersByDay = fillMonth(orderDao.countCreatedByDay(monthStart, nextDay, orderStatuses), today);
        Integer articleCount = articleDao.countByIsActiveAndStatusIn(true, articleStatuses);
        long pendingDoctorsCount = doctorProfileRepository.countByStatusIn(pendingDoctorStatuses);
        long verifiedDoctorsCount = doctorProfileRepository.countByStatusIn(verifiedDoctorStatuses);
        long clinicsCount = clinicRepository.countOrganizationClinics();
        long verifiedClinicsCount = clinicRepository.countOrganizationClinicsByStatus(ClinicStatus.VERIFIED);
        long pendingClinicsCount = clinicRepository.countOrganizationClinicsByStatus(ClinicStatus.PENDING);
        long doctorsCount = doctorProfileRepository.count();
        return new AdminDashboardResponse(
                totalProducts, totalOrders, totalUsers, articleCount, pendingDoctorsCount, clinicsCount, doctorsCount,
                usersJoinedThisMonth, verifiedDoctorsCount, verifiedClinicsCount, pendingClinicsCount,
                userSignupsByDay, ordersByDay);

    }

    /** One count per day from the 1st through today. Missing days stay at zero. */
    private List<Long> fillMonth(List<Object[]> rows, LocalDate today) {
        int days = today.getDayOfMonth();
        List<Long> series = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            series.add(0L);
        }
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            LocalDate day = toLocalDate(row[0]);
            if (day.getYear() != today.getYear() || day.getMonthValue() != today.getMonthValue()) {
                continue;
            }
            int index = day.getDayOfMonth() - 1;
            if (index >= 0 && index < days) {
                series.set(index, ((Number) row[1]).longValue());
            }
        }
        return series;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate localDate) {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate();
        }
        return LocalDate.parse(value.toString());
    }
    
}
