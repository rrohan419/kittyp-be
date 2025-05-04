/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

		// 1. Get or create order
		Order order = getLatestCreatedCart(user, orderDto);
		order.setBillingAddress(orderDto.getBillingAddress());
		order.setShippingAddress(orderDto.getShippingAddress());
		order.setCurrency(orderDto.getCurrency());

		List<OrderItemDto> orderItemDtos = Optional.ofNullable(orderDto.getOrderItems())
				.orElse(Collections.emptyList());
		if (orderItemDtos.isEmpty()) {
			order.setOrderItems(new ArrayList<>());
			order.setSubTotal(BigDecimal.ZERO);
			order.setTotalAmount(BigDecimal.ZERO);
			return mapper.convert(orderDao.saveOrder(order), OrderModel.class);
		}

		Map<String, OrderItemDto> dtoItemMap = orderItemDtos.stream()
				.collect(Collectors.toMap(OrderItemDto::getProductUuid, Function.identity(), (a, b) -> b)); // Latest
																											// wins

		List<OrderItem> existingItems = Optional.ofNullable(order.getOrderItems()).orElse(new ArrayList<>());
		Map<String, OrderItem> existingItemMap = existingItems.stream()
				.filter(i -> i.getProduct() != null && i.getProduct().getUuid() != null)
				.collect(Collectors.toMap(i -> i.getProduct().getUuid(), Function.identity(), (a, b) -> a));

		Set<String> incomingUuids = dtoItemMap.keySet();

		// 2. Remove items not in DTO
		List<OrderItem> toRemove = existingItems.stream()
				.filter(item -> item.getProduct() == null || !incomingUuids.contains(item.getProduct().getUuid()))
				.toList();
		toRemove.forEach(order::removeOrderItems);

		// 3. Update or Add
		for (String uuid : incomingUuids) {
			OrderItemDto dto = dtoItemMap.get(uuid);
			OrderItem item = existingItemMap.get(uuid);

			if (item != null) {
				item.setQuantity(dto.getQuantity());
				item.setPrice(dto.getPrice());
				item.setItemDetails(dto.getItemDetails());
			} else {
				Product product = productDao.productUuid(uuid);
				if (product == null) {
					throw new CustomException(String.format(env.getProperty(ExceptionConstant.PRODUCT_NOT_FOUND), uuid),
							HttpStatus.NOT_FOUND);
				}
				item = OrderItem.builder().order(order).product(product).price(dto.getPrice())
						.quantity(dto.getQuantity()).itemDetails(dto.getItemDetails()).build();
				order.addOrderItem(item);
			}
		}

		// 4. Calculate amounts
		BigDecimal subTotal = Optional.ofNullable(order.getOrderItems())
			    .orElse(Collections.emptyList())
			    .stream()
			    .filter(i -> i != null && i.getPrice() != null)
			    .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
			    .reduce(BigDecimal.ZERO, BigDecimal::add);


		order.setSubTotal(subTotal);

//		BigDecimal serviceCharge = Optional.ofNullable(order.getTaxes())
//				.map(Taxes::getServiceCharge)
//				.orElse(BigDecimal.ZERO);
//
//		BigDecimal shippingCharge = Optional.ofNullable(order.getTaxes())
//				.map(Taxes::getShippingCharges)
//				.orElse(BigDecimal.ZERO);
//		
//		BigDecimal otherTaxes = Optional.ofNullable(order.getTaxes())
//				.map(Taxes::getOtherTax)
//				.orElse(BigDecimal.ZERO);
//
//		order.setTotalAmount(subTotal.add(serviceCharge).add(shippingCharge).add(otherTaxes));

		// 5. Save and return
		Order saved = orderDao.saveOrder(order);
		OrderModel model = mapper.convert(saved, OrderModel.class);
		model.setBillingAddress(saved.getBillingAddress());
		model.setShippingAddress(saved.getShippingAddress());
		return model;
	}

	private Order getLatestCreatedCart(User user, OrderDto orderDto) {
		Order order = orderDao.getLastCreatedOrder(user.getUuid());
		if (order == null) {
			order = new Order();
			order.setOrderItems(null);
			order.setUser(user);
			order.setStatus(OrderStatus.CREATED);
			order.setOrderNumber(orderNumberGenerator.generateInvoiceNumber());
			order.setSubTotal(BigDecimal.ZERO);
			order.setTotalAmount(BigDecimal.ZERO);
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
