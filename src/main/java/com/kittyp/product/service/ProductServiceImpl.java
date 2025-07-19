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
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.Mapper;
import com.kittyp.product.dao.ProductDao;
import com.kittyp.product.dao.StockReservationRepository;
import com.kittyp.product.dto.ProductFilterDto;
import com.kittyp.product.dto.ProductSaveDto;
import com.kittyp.product.entity.Product;
import com.kittyp.product.entity.StockReservation;
import com.kittyp.product.enums.ProductStatus;
import com.kittyp.product.model.ProductModel;

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
		Product product = productDao.productByUuid(uuid);
		
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
		Sort sort = Sort.by(Direction.DESC, KeyConstant.CREATED_AT);
				Pageable pageable = PageRequest.of(pageNumber-1, pageSize, sort);
		
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
		Product product = productDao.productByUuid(productUuid);
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
		Product product = productDao.productByUuid(productUuid);
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
		Product product = productDao.productByUuid(productUuid);
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

			if( newQuantity == 0) {
				product.setStatus(ProductStatus.OUT_OF_STOCK);
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

	@Override
	public ProductModel updateProduct(String productUuid, ProductSaveDto productSaveDto) {
		Product product = productDao.productByUuid(productUuid);

		Product updatedProduct = mapper.convert(productSaveDto, Product.class);
		updatedProduct.setUuid(product.getUuid());
		updatedProduct.setId(product.getId());
		updatedProduct.setCreatedAt(product.getCreatedAt());
		updatedProduct.setUpdatedAt(LocalDateTime.now());
		updatedProduct.setIsActive(product.getIsActive());

		if(updatedProduct.getStockQuantity() == 0 && updatedProduct.getStatus() != ProductStatus.OUT_OF_STOCK) {
			updatedProduct.setStatus(ProductStatus.OUT_OF_STOCK);
		}

		if(updatedProduct.getStockQuantity() > 0 && productSaveDto.getStatus().equals(ProductStatus.OUT_OF_STOCK)){
			updatedProduct.setStatus(ProductStatus.ACTIVE);
		}

		product = productDao.saveProduct(updatedProduct);
		return mapper.convert(product, ProductModel.class);
	}

	@Override
	public void deleteProduct(String productUuid) {
		Product product = productDao.productByUuid(productUuid);
		product.setIsActive(false);
		productDao.saveProduct(product);
	}

}
