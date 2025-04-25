/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.core.env.Environment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.kittyp.common.constants.ExceptionConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.Mapper;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.dto.OrderDto;
import com.kittyp.order.dto.OrderFilterDto;
import com.kittyp.order.dto.OrderItemDto;
import com.kittyp.order.dto.OrderStatusUpdateDto;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.entity.Order;
import com.kittyp.order.entity.OrderItem;
import com.kittyp.order.model.OrderModel;
import com.kittyp.order.util.OrderNumberGenerator;
import com.kittyp.product.dao.ProductDao;
import com.kittyp.product.entity.Product;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private final OrderDao orderDao;
	private final Mapper mapper;
	private final Environment env;
	private final ProductDao productDao;
	private final OrderNumberGenerator orderNumberGenerator;
	private final UserDao userDao;

	/**
	 * @author rrohan419@gmail.com
	 */
	@Transactional
	@Override
	public OrderModel createUpdateOrder(OrderDto orderDto) {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		User user = userDao.userByEmail(email);

		// 1. Get the latest cart or create a new one
		Order order = getLatestCreatedCart(user, orderDto);
		order.setTotalAmount(orderDto.getTotalAmount());
		order.setBillingAddress(orderDto.getBillingAddress());
		order.setShippingAddress(orderDto.getShippingAddress());
		order.setCurrency(orderDto.getCurrency());

		// 2. Extract incoming order items and their UUIDs
		List<OrderItemDto> orderItemDtos = orderDto.getOrderItems();
		Map<String, OrderItemDto> dtoItemMap = orderItemDtos.stream()
				.collect(Collectors.toMap(OrderItemDto::getProductUuid, Function.identity()));

		Set<String> incomingProductUuids = dtoItemMap.keySet();
		List<OrderItem> existingItems = order.getOrderItems() != null ? order.getOrderItems() : new ArrayList<>();

		// 3. Track existing product UUIDs
		Set<String> existingProductUuids = existingItems.stream().map(item -> item.getProduct().getUuid())
				.collect(Collectors.toSet());
		
//		Set<String> existingProductUuids = existingItems.stream()
//		        .filter(item -> item.getProduct() != null)
//		        .map(item -> item.getProduct().getUuid())
//		        .collect(Collectors.toSet());


		// 4. Remove items not in DTO
		List<OrderItem> toRemove = existingItems.stream()
				.filter(item -> !incomingProductUuids.contains(item.getProduct().getUuid())).toList();
		toRemove.forEach(order::removeOrderItems);

		// 5. Update existing items
		existingItems.forEach(item -> {
			String uuid = item.getProduct().getUuid();
			if (dtoItemMap.containsKey(uuid)) {
				OrderItemDto dto = dtoItemMap.get(uuid);
				item.setQuantity(dto.getQuantity());
				item.setPrice(dto.getPrice());
				item.setItemDetails(dto.getItemDetails());
			}
		});

		// 6. Add new items
		List<String> newUuids = incomingProductUuids.stream().filter(uuid -> !existingProductUuids.contains(uuid))
				.toList();

		for (String uuid : newUuids) {
			Product product = productDao.productUuid(uuid);
			if (product == null) {
				throw new CustomException(String.format(env.getProperty(ExceptionConstant.PRODUCT_NOT_FOUND), uuid),
						HttpStatus.NOT_FOUND);
			}

			OrderItemDto dto = dtoItemMap.get(uuid);

			OrderItem newItem = OrderItem.builder().order(order).product(product).price(dto.getPrice())
					.quantity(dto.getQuantity()).itemDetails(dto.getItemDetails()).build();

			order.addOrderItem(newItem);
		}

//		 7. Recalculate total
//		BigDecimal total = order.getOrderItems().stream()
//				.map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
//				.reduce(BigDecimal.ZERO, BigDecimal::add);
//		order.setTotalAmount(total);
		
		// 7. Recalculate subtotal and total amount
//		BigDecimal subTotal = order.getOrderItems().stream()
//		        .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
//		        .reduce(BigDecimal.ZERO, BigDecimal::add);
		
		List<OrderItem> items = order.getOrderItems();
		if (items == null) items = Collections.emptyList();

		BigDecimal subTotal = items.stream()
		    .filter(i -> i != null && i.getPrice() != null)
		    .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
		    .reduce(BigDecimal.ZERO, BigDecimal::add);

		order.setSubTotal(subTotal);

		BigDecimal serviceCharge = order.getTaxes() != null && order.getTaxes().getServiceCharge() != null
		        ? order.getTaxes().getServiceCharge()
		        : BigDecimal.ZERO;

		BigDecimal shippingCharges = order.getTaxes() != null && order.getTaxes().getShippingCharges() != null
		        ? order.getTaxes().getShippingCharges()
		        : BigDecimal.ZERO;

		BigDecimal totalAmount = subTotal.add(serviceCharge).add(shippingCharges);
		order.setTotalAmount(totalAmount);


		// 8. Save order and return model
		Order saved = orderDao.saveOrder(order);
		OrderModel orderModel = mapper.convert(saved, OrderModel.class);
		orderModel.setBillingAddress(saved.getBillingAddress());
		orderModel.setShippingAddress(saved.getShippingAddress());

		return orderModel;
	}

	private Order getLatestCreatedCart(User user, OrderDto orderDto) {
		Order order = orderDao.getLastCreatedOrder(user.getUuid());
		if (order == null) {
			order = new Order();
			order.setOrderItems(null);
			order.setUser(user);
			order.setStatus(OrderStatus.CREATED);
			order.setOrderNumber(orderNumberGenerator.generateInvoiceNumber());
			order = orderDao.saveOrder(order);
		}

		return order;
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public OrderModel orderDetailsByOrderNumber(String orderNumber) {
		Order order = orderDao.orderByOrderNumber(orderNumber);

		return mapper.convert(order, OrderModel.class);
	}

	@Transactional
	@Override
	public OrderModel updateOrderStatus(OrderStatusUpdateDto orderStatusUpdateDto) {
		Order order = orderDao.orderByOrderNumber(orderStatusUpdateDto.getOrderNumber());
		order.setStatus(orderStatusUpdateDto.getOrderStatus());
		order = orderDao.saveOrder(order);
		return mapper.convert(order, OrderModel.class);
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public OrderModel latestCreatedCartByUser(String userUuid) {
		User user = userDao.userByUuid(userUuid);
		Order order = orderDao.getLastCreatedOrder(user.getUuid());
		if (order == null) {
			order = new Order();
			order.setOrderItems(List.of());
			order.setUser(user);
			order.setTotalAmount(BigDecimal.ZERO);
			order.setStatus(OrderStatus.CREATED);
			order.setSubTotal(BigDecimal.ZERO);
			order.setOrderNumber(orderNumberGenerator.generateInvoiceNumber());
			order = orderDao.saveOrder(order);
		}
		return mapper.convert(order, OrderModel.class);
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	public PaginationModel<OrderModel> allOrderByFilter(OrderFilterDto orderFilterDto, Integer pageNumber,
			Integer pageSize) {
		Sort sort = Sort.by(Direction.DESC, "updatedAt");
		Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, sort);

		Specification<Order> orderSpecification = OrderSpecification.articlesByFilters(orderFilterDto);
		Page<Order> orderPage = orderDao.findAllOrders(pageable, orderSpecification);

		List<OrderModel> orderListModel = orderPage.getContent().stream()
				.map(order -> mapper.convert(order, OrderModel.class)).toList();

		return orderPageToModel(new PageImpl<>(orderListModel, pageable, orderPage.getTotalElements()));
	}

	private PaginationModel<OrderModel> orderPageToModel(Page<OrderModel> orderPage) {
		PaginationModel<OrderModel> orderModel = new PaginationModel<>();
		orderModel.setModels(orderPage.getContent());
		orderModel.setIsFirst(orderPage.isFirst());
		orderModel.setIsLast(orderPage.isLast());
		orderModel.setTotalElements(orderPage.getTotalElements());
		orderModel.setTotalPages(orderPage.getTotalPages());
		return orderModel;
	}

}
