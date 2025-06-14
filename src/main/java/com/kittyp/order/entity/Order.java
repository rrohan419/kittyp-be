package com.kittyp.order.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.order.emus.CurrencyType;
import com.kittyp.order.emus.OrderStatus;
import com.kittyp.user.entity.User;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author rrohan419@gmail.com
 */
@Entity
@Table(name = "orders")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

	private static final long serialVersionUID = 1L;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_uuid", referencedColumnName = "uuid", nullable = false)
	private User user;

	@Column(name = "order_number", unique = true, nullable = false, length = 20)
	private String orderNumber;
	
	@Column
	private String aggregatorOrderNumber;

	@Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
	private BigDecimal totalAmount;
	
	@Column(name = "sub_total", nullable = false, precision = 10, scale = 2)
	private BigDecimal subTotal;
	
	@Column(name = "taxes", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
	private Taxes taxes;
	
	@Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, columnDefinition = "VARCHAR(10) DEFAULT 'INR'")
	@Builder.Default
	private CurrencyType currency = CurrencyType.INR;

	@Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;
	
	@Column(name = "shipping_address", columnDefinition = "json")
    @JdbcTypeCode(SqlTypes.JSON)
   private  Address shippingAddress;

   @Column(name = "billing_address", columnDefinition = "json")
   @JdbcTypeCode(SqlTypes.JSON)
   private Address billingAddress;

   @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
   private List<OrderItem> orderItems;
   
   public void removeOrderItems(OrderItem orderItem) {
	    orderItems.remove(orderItem);
	    orderItem.setOrder(null); // This is critical for proper orphan removal
	}
   
   public void addOrderItem(OrderItem item) {
	   if(orderItems == null) {
		   orderItems = new ArrayList<>();
	   }
	    orderItems.add(item);
	    item.setOrder(this);
	}

	public Integer getQuantity() {
		return orderItems.stream()
			.map(OrderItem::getQuantity)
			.reduce(0, Integer::sum);
	}


}
