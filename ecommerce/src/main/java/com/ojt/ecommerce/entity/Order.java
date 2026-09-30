package com.ojt.ecommerce.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "order_id", nullable = false)
	private Long orderId;

	@Column(name = "order_no", nullable = false, length = 50)
	private String orderNo;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "customer_id", referencedColumnName = "customer_id", nullable = false, foreignKey = @ForeignKey(name = "fk_orders_customer_restrict"))
	private Customer customer;

	@Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal subtotalAmount;

	@Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal discountAmount;

	@Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal taxAmount;

	@Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
	private BigDecimal shippingFee;

	@Column(name = "order_status", nullable = false, columnDefinition = "ENUM('PENDING', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED')")
	private String orderStatus;

	@Column(name = "payment_status", nullable = false, columnDefinition = "ENUM('PENDING', 'SUCCESS', 'FAILED', 'PARTIALLY_REFUNDED', 'REFUNDED')")
	private String paymentStatus;

	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "created_by", referencedColumnName = "user_id", nullable = true, foreignKey = @ForeignKey(name = "fk_orders_created_by"))
	private User createdBy;

	@Column(name = "modified_at")
	private LocalDateTime modifiedAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = true)
	@JoinColumn(name = "modified_by", referencedColumnName = "user_id", nullable = true, foreignKey = @ForeignKey(name = "fk_orders_modified_by"))
	private User modifiedBy;

	@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
	private List<OrderItem> orderItems;

	@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
	private List<OrderStatusHistory> statusHistories;

	@OneToMany(mappedBy = "order", fetch = FetchType.LAZY)
	private List<OrderAddress> orderAddresses;

}
