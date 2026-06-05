package com.back.shop.service.impl;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.shop.model.dto.request.PlaceOrderRequest;
import com.back.shop.model.dto.response.OrderItemResponse;
import com.back.shop.model.dto.response.OrderResponse;
import com.back.shop.model.entity.*;
import com.back.shop.model.enums.*;
import com.back.shop.repo.*;
import com.back.shop.service.IOrderService;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductMediaRepository productMediaRepository;
    private final ShopRepository shopRepository;
    private final IUserRepo userRepo;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName().equals("anonymousUser")) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        String email;
        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
            email = oauthToken.getPrincipal().getAttribute("email");
        } else {
            email = authentication.getName();
        }
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        User buyer = getCurrentUser();
        List<CartItem> cartItems = cartItemRepository.findAllById(request.getCartItemIds());

        if (cartItems.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        // Validate items belong to buyer's cart
        Cart cart = cartRepository.findByUserId(buyer.getId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        for (CartItem item : cartItems) {
            if (!item.getCartId().equals(cart.getId())) {
                throw new AppException(ErrorCode.FORBIDDEN);
            }
        }

        // Group cart items by product shop
        Map<Long, List<CartItem>> itemsByShop = cartItems.stream()
                .collect(Collectors.groupingBy(item -> {
                    Product p = productRepository.findById(item.getProductId())
                            .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
                    return p.getShopId();
                }));

        // For simplicity, we process/create orders sequentially. In a split-order scenario, we return the first order or primary order detail.
        Order primaryOrder = null;

        for (var entry : itemsByShop.entrySet()) {
            Long shopId = entry.getKey();
            List<CartItem> shopCartItems = entry.getValue();

            Shop shop = shopRepository.findById(shopId)
                    .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

            BigDecimal subtotal = BigDecimal.ZERO;
            List<OrderItem> orderItemsToSave = new ArrayList<>();

            // Verify stock and calculate subtotal
            for (CartItem ci : shopCartItems) {
                Product product = productRepository.findById(ci.getProductId()).orElseThrow();
                ProductVariant variant = null;
                if (ci.getVariantId() != null) {
                    variant = productVariantRepository.findById(ci.getVariantId()).orElseThrow();
                }

                int availableStock = (variant != null) ? variant.getStockQuantity() : product.getStockQuantity();
                if (availableStock < ci.getQuantity()) {
                    throw new AppException(ErrorCode.BAD_REQUEST); // Stock check failed
                }

                // Deduct stock
                if (variant != null) {
                    variant.setStockQuantity(availableStock - ci.getQuantity());
                    productVariantRepository.save(variant);
                } else {
                    product.setStockQuantity(availableStock - ci.getQuantity());
                }
                product.setSoldCount(product.getSoldCount() + ci.getQuantity());
                productRepository.save(product);

                BigDecimal unitPrice = (variant != null) ? variant.getPrice() : product.getBasePrice();
                BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(ci.getQuantity()));
                subtotal = subtotal.add(itemTotal);

                String mediaUrl = null;
                List<ProductMedia> mediaList = productMediaRepository.findAllByProductIdOrderBySortOrderAsc(product.getId());
                if (!mediaList.isEmpty()) {
                    mediaUrl = mediaList.get(0).getUrl();
                }

                orderItemsToSave.add(OrderItem.builder()
                        .productId(product.getId())
                        .variantId(ci.getVariantId())
                        .productTitle(product.getTitle())
                        .variantName(variant != null ? variant.getName() : null)
                        .productImageUrl(mediaUrl)
                        .unitPrice(unitPrice)
                        .quantity(ci.getQuantity())
                        .totalPrice(itemTotal)
                        .build());
            }

            BigDecimal shippingFee = BigDecimal.valueOf(30000L); // 30,000 VND flat per shop order
            BigDecimal total = subtotal.add(shippingFee);

            String orderCode = "TT" + System.currentTimeMillis() + "" + (new Random().nextInt(900) + 100);

            Order order = Order.builder()
                    .orderCode(orderCode)
                    .buyerId(buyer.getId())
                    .shopId(shop.getId())
                    .subtotalAmount(subtotal)
                    .shippingFee(shippingFee)
                    .discountAmount(BigDecimal.ZERO)
                    .totalAmount(total)
                    .currency("VND")
                    .status("COD".equalsIgnoreCase(request.getPaymentProvider()) ? OrderStatus.SELLER_CONFIRMING : OrderStatus.PENDING_PAYMENT)
                    .paymentStatus("COD".equalsIgnoreCase(request.getPaymentProvider()) ? PaymentStatus.UNPAID : PaymentStatus.UNPAID)
                    .shippingStatus(ShippingStatus.NOT_SHIPPED)
                    .receiverName(request.getReceiverName())
                    .receiverPhone(request.getReceiverPhone())
                    .receiverAddress(request.getReceiverAddress())
                    .note(request.getNote())
                    .build();

            Order savedOrder = orderRepository.save(order);

            for (OrderItem oi : orderItemsToSave) {
                oi.setOrderId(savedOrder.getId());
                orderItemRepository.save(oi);
            }

            // Create initial payment record
            Payment payment = Payment.builder()
                    .orderId(savedOrder.getId())
                    .provider(request.getPaymentProvider())
                    .amount(total)
                    .currency("VND")
                    .status(PaymentStatus.UNPAID)
                    .build();
            paymentRepository.save(payment);

            // Remove items from cart
            cartItemRepository.deleteAll(shopCartItems);

            if (primaryOrder == null) {
                primaryOrder = savedOrder;
            }
        }

        if (primaryOrder == null) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        return getOrderById(primaryOrder.getId());
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        User buyer = getCurrentUser();

        // Check permission (must be buyer or shop owner or admin)
        boolean allowed = order.getBuyerId().equals(buyer.getId());
        if (!allowed) {
            Optional<Shop> shopOpt = shopRepository.findByOwnerId(buyer.getId());
            if (shopOpt.isPresent() && shopOpt.get().getId().equals(order.getShopId())) {
                allowed = true;
            }
        }

        if (!allowed) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        List<OrderItem> items = orderItemRepository.findAllByOrderId(order.getId());
        Shop shop = shopRepository.findById(order.getShopId()).orElseThrow();

        return mapToResponse(order, items, shop.getName());
    }

    @Override
    public Page<OrderResponse> getMyOrders(Pageable pageable) {
        User user = getCurrentUser();
        return orderRepository.findAllByBuyerId(user.getId(), pageable)
                .map(o -> {
                    List<OrderItem> items = orderItemRepository.findAllByOrderId(o.getId());
                    Shop shop = shopRepository.findById(o.getShopId()).orElse(null);
                    return mapToResponse(o, items, shop != null ? shop.getName() : "Unknown Shop");
                });
    }

    @Override
    public Page<OrderResponse> getShopOrders(Pageable pageable) {
        User user = getCurrentUser();
        Shop shop = shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN));

        return orderRepository.findAllByShopId(shop.getId(), pageable)
                .map(o -> {
                    List<OrderItem> items = orderItemRepository.findAllByOrderId(o.getId());
                    return mapToResponse(o, items, shop.getName());
                });
    }

    @Override
    public Page<OrderResponse> getOrdersForAdmin(Pageable pageable) {
        return orderRepository.findAll(pageable)
                .map(o -> {
                    List<OrderItem> items = orderItemRepository.findAllByOrderId(o.getId());
                    Shop shop = shopRepository.findById(o.getShopId()).orElse(null);
                    return mapToResponse(o, items, shop != null ? shop.getName() : "Unknown Shop");
                });
    }

    @Override
    public OrderResponse getOrderForAdmin(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        List<OrderItem> items = orderItemRepository.findAllByOrderId(order.getId());
        Shop shop = shopRepository.findById(order.getShopId()).orElse(null);
        return mapToResponse(order, items, shop != null ? shop.getName() : "Unknown Shop");
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String status) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        // Only seller can update order flow
        Shop shop = shopRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN));

        if (!order.getShopId().equals(shop.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        OrderStatus newStatus = OrderStatus.valueOf(status);

        // Simple validation state transition
        if (newStatus == OrderStatus.PACKING) {
            order.setShippingStatus(ShippingStatus.PREPARING);
        } else if (newStatus == OrderStatus.SHIPPING) {
            order.setShippingStatus(ShippingStatus.SHIPPED);
        } else if (newStatus == OrderStatus.DELIVERED) {
            order.setShippingStatus(ShippingStatus.DELIVERED);
        } else if (newStatus == OrderStatus.COMPLETED) {
            order.setShippingStatus(ShippingStatus.DELIVERED);
            if (order.getPaymentStatus() == PaymentStatus.UNPAID) {
                order.setPaymentStatus(PaymentStatus.PAID);
                Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
                if (payment != null) {
                    payment.setStatus(PaymentStatus.PAID);
                    payment.setPaidAt(LocalDateTime.now());
                    paymentRepository.save(payment);
                }
            }
        }

        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findAllByOrderId(saved.getId());
        return mapToResponse(saved, items, shop.getName());
    }

    @Override
    @Transactional
    public OrderResponse payOrder(Long orderId, String paymentProvider, String transactionId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        payment.setProvider(paymentProvider);
        payment.setProviderTransactionId(transactionId);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.PAID);
        order.setStatus(OrderStatus.SELLER_CONFIRMING);
        Order saved = orderRepository.save(order);

        List<OrderItem> items = orderItemRepository.findAllByOrderId(saved.getId());
        Shop shop = shopRepository.findById(saved.getShopId()).orElseThrow();
        return mapToResponse(saved, items, shop.getName());
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        boolean isBuyer = order.getBuyerId().equals(user.getId());
        boolean isSeller = false;

        Optional<Shop> shopOpt = shopRepository.findByOwnerId(user.getId());
        if (shopOpt.isPresent() && shopOpt.get().getId().equals(order.getShopId())) {
            isSeller = true;
        }

        if (!isBuyer && !isSeller) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }

        // Cancel conditions
        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.SHIPPING || order.getStatus() == OrderStatus.DELIVERED) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        order.setStatus(OrderStatus.CANCELLED);

        // Restore stock
        List<OrderItem> items = orderItemRepository.findAllByOrderId(order.getId());
        for (OrderItem oi : items) {
            Product product = productRepository.findById(oi.getProductId()).orElse(null);
            if (product != null) {
                if (oi.getVariantId() != null) {
                    ProductVariant variant = productVariantRepository.findById(oi.getVariantId()).orElse(null);
                    if (variant != null) {
                        variant.setStockQuantity(variant.getStockQuantity() + oi.getQuantity());
                        productVariantRepository.save(variant);
                    }
                } else {
                    product.setStockQuantity(product.getStockQuantity() + oi.getQuantity());
                }
                product.setSoldCount(Math.max(0, product.getSoldCount() - oi.getQuantity()));
                productRepository.save(product);
            }
        }

        Order saved = orderRepository.save(order);
        Shop shop = shopRepository.findById(saved.getShopId()).orElseThrow();
        return mapToResponse(saved, items, shop.getName());
    }

    private OrderResponse mapToResponse(Order order, List<OrderItem> items, String shopName) {
        List<OrderItemResponse> itemResponses = items.stream()
                .map(oi -> OrderItemResponse.builder()
                        .id(oi.getId())
                        .productId(oi.getProductId())
                        .variantId(oi.getVariantId())
                        .productTitle(oi.getProductTitle())
                        .variantName(oi.getVariantName())
                        .productImageUrl(oi.getProductImageUrl())
                        .unitPrice(oi.getUnitPrice())
                        .quantity(oi.getQuantity())
                        .totalPrice(oi.getTotalPrice())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .buyerId(order.getBuyerId())
                .shopId(order.getShopId())
                .shopName(shopName)
                .subtotalAmount(order.getSubtotalAmount())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .status(order.getStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .shippingStatus(order.getShippingStatus().name())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .receiverAddress(order.getReceiverAddress())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
