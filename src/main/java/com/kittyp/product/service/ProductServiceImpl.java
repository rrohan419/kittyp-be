/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.product.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.Mapper;
import com.kittyp.product.dao.ProductDao;
import com.kittyp.product.dto.ProductFilterDto;
import com.kittyp.product.dto.ProductSaveDto;
import com.kittyp.product.entity.Product;
import com.kittyp.product.model.ProductModel;
import com.kittyp.product.entity.StockReservation;
import com.kittyp.product.dao.StockReservationRepository;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

	private final ProductDao productDao;
	private final Environment env;
	private final Mapper mapper;
	private final StockReservationRepository stockReservationRepository;
	
	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public ProductModel saveProduct(ProductSaveDto productSaveDto) {
		if(productDao.productExistsByName(productSaveDto.getName())) {
			throw new CustomException("product exists with name : "+productSaveDto.getName(), HttpStatus.CONFLICT);
		}
		
		Product product = mapper.convert(productSaveDto, Product.class);
		product.setUuid(UUID.randomUUID().toString());
		product = productDao.saveProduct(product);
		
		return mapper.convert(product, ProductModel.class);
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public ProductModel productByUuid(String uuid) {
		Product product = productDao.productUuid(uuid);
		
		if(product == null) {
			throw new CustomException("product not found by uuid : "+uuid, HttpStatus.NOT_FOUND);
		}
		return mapper.convert(product, ProductModel.class);
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public PaginationModel<ProductModel> productsByFilter(ProductFilterDto productFilterDto, Integer pageNumber,
			Integer pageSize) {
		Pageable pageable = PageRequest.of(pageNumber-1, pageSize);
		
		Specification<Product> productSpecification = ProductSpecification.productByFilters(productFilterDto);
		
		Page<Product> productPage = productDao.productsByFilter(pageable, productSpecification);
		
		List<ProductModel> productListModel = productPage.getContent().stream().map(product -> mapper.convert(product, ProductModel.class)).toList();
	
		return productPageToModel(new PageImpl<>(productListModel, pageable, productPage.getTotalElements()));
	}
	
	private PaginationModel<ProductModel> productPageToModel(Page<ProductModel> productPage) {
		PaginationModel<ProductModel> projectModel = new PaginationModel<>();
		projectModel.setModels(productPage.getContent());
		projectModel.setIsFirst(productPage.isFirst());
		projectModel.setIsLast(productPage.isLast());
		projectModel.setTotalElements(productPage.getTotalElements());
		projectModel.setTotalPages(productPage.getTotalPages());
		return projectModel;
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public Integer productCount(Boolean isActive) {
		return productDao.productCount(isActive == null ? true : isActive);
	}

	@Override
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void updateProductStock(String productUuid, int quantityToReduce) {
		Product product = productDao.productUuid(productUuid);
		if (product == null) {
			throw new CustomException("Product not found: " + productUuid, HttpStatus.NOT_FOUND);
		}

		int newQuantity = product.getStockQuantity() - quantityToReduce;
		if (newQuantity < 0) {
			throw new CustomException("Insufficient stock for product: " + product.getName(), HttpStatus.BAD_REQUEST);
		}

		product.setStockQuantity(newQuantity);
		productDao.saveProduct(product);
	}

	@Override
	@Transactional(readOnly = true)
	public void validateProductStock(String productUuid, int requestedQuantity) {
		Product product = productDao.productUuid(productUuid);
		if (product == null) {
			throw new CustomException("Product not found: " + productUuid, HttpStatus.NOT_FOUND);
		}

		if (product.getStockQuantity() < requestedQuantity) {
			throw new CustomException("Insufficient stock for product: " + product.getName() + 
				". Available: " + product.getStockQuantity(), HttpStatus.BAD_REQUEST);
		}
	}

	@Override
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void reserveStock(String productUuid, int quantity, String orderNumber) {
		Product product = productDao.productUuid(productUuid);
		if (product == null) {
			throw new CustomException("Product not found: " + productUuid, HttpStatus.NOT_FOUND);
		}

		// Check available stock (considering existing reservations)
		int reservedQuantity = stockReservationRepository.findActiveReservationsQuantityForProduct(product.getId(), LocalDateTime.now());
		int availableStock = product.getStockQuantity() - reservedQuantity;

		if (availableStock < quantity) {
			throw new CustomException("Insufficient stock for product: " + product.getName(), HttpStatus.BAD_REQUEST);
		}

		// Create reservation
		StockReservation reservation = StockReservation.builder()
			.product(product)
			.quantity(quantity)
			.orderNumber(orderNumber)
			.expirationTime(LocalDateTime.now().plusMinutes(30)) // 30-minute reservation
			.confirmed(false)
			.build();

		stockReservationRepository.save(reservation);
	}

	@Override
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void confirmStockReservation(String orderNumber) {
		List<StockReservation> reservations = stockReservationRepository.findByOrderNumber(orderNumber);
		if (reservations.isEmpty()) {
			throw new CustomException("No reservations found for order: " + orderNumber, HttpStatus.NOT_FOUND);
		}

		for (StockReservation reservation : reservations) {
			if (reservation.isConfirmed()) {
				continue; // Skip if already confirmed
			}

			Product product = reservation.getProduct();
			int newQuantity = product.getStockQuantity() - reservation.getQuantity();
			
			if (newQuantity < 0) {
				throw new CustomException("Insufficient stock for product: " + product.getName(), HttpStatus.BAD_REQUEST);
			}

			product.setStockQuantity(newQuantity);
			productDao.saveProduct(product);

			reservation.setConfirmed(true);
			stockReservationRepository.save(reservation);
		}
	}

	@Override
	@Transactional(isolation = Isolation.REPEATABLE_READ)
	public void cancelStockReservation(String orderNumber) {
		List<StockReservation> reservations = stockReservationRepository.findByOrderNumber(orderNumber);
		if (!reservations.isEmpty()) {
			stockReservationRepository.deleteAll(reservations);
		}
	}

}
