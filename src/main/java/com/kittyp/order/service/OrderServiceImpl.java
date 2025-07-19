/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.service;

import java.math.BigDecimal;
import java.util.List;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kittyp.cart.dto.CartCheckoutRequest;
import com.kittyp.cart.entity.Cart;
import com.kittyp.cart.entity.CartItem;
import com.kittyp.cart.service.CartService;
import com.kittyp.common.constants.KeyConstant;
import com.kittyp.common.exception.CustomException;
import com.kittyp.common.model.PaginationModel;
import com.kittyp.common.util.Mapper;
import com.kittyp.order.dao.OrderDao;
import com.kittyp.order.dto.OrderFilterDto;
import com.kittyp.order.dto.OrderStatusUpdateDto;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.order.emus.ShippingTypes;
import com.kittyp.order.entity.Order;
import com.kittyp.order.entity.OrderItem;
import com.kittyp.order.entity.Taxes;
import com.kittyp.order.model.OrderModel;
import com.kittyp.order.util.OrderNumberGenerator;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;
import com.kittyp.product.service.ProductService;

import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

	private final OrderDao orderDao;
	private final Mapper mapper;
	private final OrderNumberGenerator orderNumberGenerator;
	private final UserDao userDao;
	private final CartService cartService;
	private final ProductService productService;

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	@Transactional(readOnly = true)
	public OrderModel orderDetailsByOrderNumber(String orderNumber) {
		Order order = orderDao.orderByOrderNumber(orderNumber);
		return mapper.convert(order, OrderModel.class);
	}

	/**
	 * @author rrohan419@gmail.com
	 */
	@Override
	@Transactional(readOnly = true)
	public PaginationModel<OrderModel> allOrderByFilter(OrderFilterDto orderFilterDto, Integer pageNumber,
			Integer pageSize) {
		Sort sort = Sort.by(Direction.DESC, KeyConstant.CREATED_AT);
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

	@Override
	@Transactional(isolation = Isolation.REPEATABLE_READ, propagation = Propagation.REQUIRED)
	public OrderModel createOrderFromCart(String userUuid, CartCheckoutRequest request) {
		User user = userDao.userByUuid(userUuid);
		Cart cart = cartService.getOrCreateCart(user);

		if (cart.getCartItems().isEmpty()) {
			throw new CustomException("Cart is empty", HttpStatus.BAD_REQUEST);
		}

		String orderNumber = orderNumberGenerator.generateInvoiceNumber();

		// Validate and reserve stock for all items first
		for (CartItem cartItem : cart.getCartItems()) {
			productService.validateProductStock(cartItem.getProduct().getUuid(), cartItem.getQuantity());
		}

		// Create order
		Order order = new Order();
		order.setUser(user);
		order.setOrderNumber(orderNumber);
		order.setStatus(OrderStatus.CREATED);

		// Set addresses
		order.setBillingAddress(request.getBillingAddress());
		order.setShippingAddress(request.getShippingAddress());

		// Convert cart items to order items
		BigDecimal subTotal = BigDecimal.ZERO;
		for (CartItem cartItem : cart.getCartItems()) {
			OrderItem orderItem = OrderItem.builder()
				.order(order)
				.product(cartItem.getProduct())
				.quantity(cartItem.getQuantity())
				.price(cartItem.getPrice())
				.build();
			order.addOrderItem(orderItem);
			subTotal = subTotal.add(cartItem.getTotal());
		}

		// Calculate totals
		order.setSubTotal(subTotal);

		// Add shipping cost based on method
		BigDecimal shippingCost = calculateShippingCost(request.getShippingMethod());

		// Calculate tax
		BigDecimal tax = calculateTax(subTotal);

		// Calculate Service charge
		BigDecimal serviceCharge = calculateServiceCharge(subTotal);

		Taxes taxes = new Taxes();
		taxes.setOtherTax(tax);
		taxes.setServiceCharge(serviceCharge);
		taxes.setShippingCharges(shippingCost);

		BigDecimal totalAmount = subTotal.add(shippingCost).add(tax).add(serviceCharge);

		// Set total amount
		order.setTotalAmount(totalAmount);
		order.setTaxes(taxes);

		// Save order
		Order savedOrder = orderDao.saveOrder(order);

		// Clear the cart
		cartService.clearCart(userUuid);

		return mapper.convert(savedOrder, OrderModel.class);
	}

	public BigDecimal calculateShippingCost(ShippingTypes shippingMethod) {
	    return shippingMethod.getCost();
	}


	private BigDecimal calculateTax(BigDecimal amount) {
		return amount.multiply(new BigDecimal("0.18")); // 18% GST
	}

	private BigDecimal calculateServiceCharge(BigDecimal amount) {
		return amount.multiply(new BigDecimal("0.05")); // 5%
	}

	@Override
	public OrderModel updateOrderStatus(OrderStatusUpdateDto orderQuantityUpdateDto) {
		Order order = orderDao.orderByOrderNumber(orderQuantityUpdateDto.getOrderNumber());
		order.setStatus(orderQuantityUpdateDto.getOrderStatus());
		return mapper.convert(orderDao.saveOrder(order), OrderModel.class);
	}
}
