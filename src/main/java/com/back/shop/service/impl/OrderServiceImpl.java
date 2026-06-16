package com.back.shop.service.impl;

import com.back.config.FrontendProperties;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.shop.model.dto.request.PlaceOrderRequest;
import com.back.shop.model.dto.response.OrderItemResponse;
import com.back.shop.model.dto.response.OrderResponse;
import com.back.shop.model.dto.response.PaymentResponse;
import com.back.shop.model.entity.*;
import com.back.shop.model.enums.*;
import com.back.shop.repo.*;
import com.back.shop.service.IOrderService;
import com.back.user.model.entity.User;
import com.back.user.repo.IUserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final FrontendProperties frontendProperties;
    private final RestTemplate restTemplate;

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.currency:vnd}")
    private String stripeCurrency;

    @Value("${paypal.client-id:}")
    private String paypalClientId;

    @Value("${paypal.client-secret:}")
    private String paypalClientSecret;

    @Value("${paypal.base-url:https://api-m.sandbox.paypal.com}")
    private String paypalBaseUrl;

    @Value("${paypal.currency:USD}")
    private String paypalCurrency;

    @Value("${paypal.vnd-to-usd-rate:25000}")
    private BigDecimal paypalVndToUsdRate;

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
            ensureShopIsNotOwnedByBuyer(shop, buyer);

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
                    mediaUrl = mediaList.getFirst().getUrl();
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

            String orderCode = "TT" + System.currentTimeMillis() + (new Random().nextInt(900) + 100);

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
                    .paymentStatus(PaymentStatus.UNPAID)
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

        switch (newStatus) {
            case PACKING -> {
                order.setShippingStatus(ShippingStatus.PREPARING);
            }
            case SHIPPING -> {
                order.setShippingStatus(ShippingStatus.SHIPPED);
            }
            case DELIVERED -> {
                order.setShippingStatus(ShippingStatus.DELIVERED);
            }
            case COMPLETED -> {
                order.setShippingStatus(ShippingStatus.DELIVERED);
                if (order.getPaymentStatus() == PaymentStatus.UNPAID) {
                    order.setPaymentStatus(PaymentStatus.PAID);

                    Payment payment = paymentRepository.findByOrderId(order.getId())
                            .orElse(null);

                    if (payment != null) {
                        payment.setStatus(PaymentStatus.PAID);
                        payment.setPaidAt(LocalDateTime.now());
                        paymentRepository.save(payment);
                    }
                }
            }
            default -> {}
        }
        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        List<OrderItem> items = orderItemRepository.findAllByOrderId(saved.getId());
        return mapToResponse(saved, items, shop.getName());
    }

    @Override
    @Transactional
    public PaymentResponse payOrder(Long orderId, String paymentProvider) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        User buyer = getCurrentUser();
        if (!order.getBuyerId().equals(buyer.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));

        payment.setProvider(paymentProvider);
        if ("COD".equalsIgnoreCase(paymentProvider)) {
            payment.setStatus(PaymentStatus.UNPAID);
            paymentRepository.save(payment);

            order.setPaymentStatus(PaymentStatus.UNPAID);
            order.setStatus(OrderStatus.SELLER_CONFIRMING);
            orderRepository.save(order);

            return mapPaymentResponse(payment, null);
        }

        String redirectUrl;
        if ("PAYPAL".equalsIgnoreCase(paymentProvider)) {
            redirectUrl = createPaypalCheckoutUrl(order);
        } else if ("STRIPE".equalsIgnoreCase(paymentProvider)) {
            redirectUrl = createStripeCheckoutUrl(order);
        } else {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaidAt(null);
        paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        orderRepository.save(order);

        return mapPaymentResponse(payment, redirectUrl);
    }

    private String createStripeCheckoutUrl(Order order) {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        String orderUrl = orderDetailUrl(order.getId());
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(stripeSecretKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("mode", "payment");
        body.add("success_url", orderUrl + "?payment=stripe-success&session_id={CHECKOUT_SESSION_ID}");
        body.add("cancel_url", orderUrl + "?payment=stripe-cancel");
        body.add("line_items[0][quantity]", "1");
        body.add("line_items[0][price_data][currency]", stripeCurrency.toLowerCase(Locale.ROOT));
        body.add("line_items[0][price_data][product_data][name]", "TopTop order " + order.getOrderCode());
        body.add("line_items[0][price_data][unit_amount]", stripeUnitAmount(order.getTotalAmount(), stripeCurrency));
        body.add("metadata[orderId]", order.getId().toString());
        body.add("metadata[orderCode]", order.getOrderCode());

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.stripe.com/v1/checkout/sessions",
                new HttpEntity<>(body, headers),
                Map.class
        );

        Map<?, ?> responseBody = response.getBody();
        Object id = responseBody != null ? responseBody.get("id") : null;
        if (id != null) {
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            if (payment != null) {
                payment.setProviderTransactionId(id.toString());
                paymentRepository.save(payment);
            }
        }

        Object url = responseBody != null ? responseBody.get("url") : null;
        if (url == null || url.toString().isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return url.toString();
    }

    @Override
    @Transactional
    public OrderResponse completePayment(Long orderId, String paymentProvider, String providerReference) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        User buyer = getCurrentUser();
        if (!order.getBuyerId().equals(buyer.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return getOrderById(order.getId());
        }
        if (providerReference == null || providerReference.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        Payment payment = paymentRepository.findByOrderId(order.getId())
                .orElseThrow(() -> new AppException(ErrorCode.BAD_REQUEST));
        boolean paid;
        if ("PAYPAL".equalsIgnoreCase(paymentProvider)) {
            paid = capturePaypalPayment(providerReference, order);
        } else if ("STRIPE".equalsIgnoreCase(paymentProvider)) {
            paid = verifyStripePayment(providerReference, order);
        } else {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        if (!paid) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            order.setPaymentStatus(PaymentStatus.FAILED);
            orderRepository.save(order);
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        payment.setProvider(paymentProvider);
        payment.setProviderTransactionId(providerReference);
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

    private boolean verifyStripePayment(String sessionId, Order order) {
        if (stripeSecretKey == null || stripeSecretKey.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(stripeSecretKey);
        ResponseEntity<Map> response = restTemplate.exchange(
                "https://api.stripe.com/v1/checkout/sessions/" + sessionId,
                HttpMethod.GET,
                new HttpEntity<>(headers),
                Map.class
        );

        Map<?, ?> responseBody = response.getBody();
        if (responseBody == null) {
            return false;
        }
        Object paymentStatus = responseBody.get("payment_status");
        Object amountTotal = responseBody.get("amount_total");
        String expectedAmount = stripeUnitAmount(order.getTotalAmount(), stripeCurrency);
        return "paid".equals(paymentStatus)
                && amountTotal != null
                && expectedAmount.equals(amountTotal.toString());
    }

    private boolean capturePaypalPayment(String paypalOrderId, Order order) {
        if (paypalClientId == null || paypalClientId.isBlank() || paypalClientSecret == null || paypalClientSecret.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        String accessToken = requestPaypalAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                paypalBaseUrl + "/v2/checkout/orders/" + paypalOrderId + "/capture",
                new HttpEntity<>(Map.of(), headers),
                Map.class
        );

        Map<?, ?> responseBody = response.getBody();
        if (responseBody == null || !"COMPLETED".equals(responseBody.get("status"))) {
            return false;
        }

        Object purchaseUnits = responseBody.get("purchase_units");
        if (purchaseUnits instanceof List<?> units && !units.isEmpty() && units.get(0) instanceof Map<?, ?> unit) {
            Object referenceId = unit.get("reference_id");
            return order.getId().toString().equals(String.valueOf(referenceId));
        }
        return false;
    }

    private String createPaypalCheckoutUrl(Order order) {
        if (paypalClientId == null || paypalClientId.isBlank() || paypalClientSecret == null || paypalClientSecret.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }

        String accessToken = requestPaypalAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "intent", "CAPTURE",
                "purchase_units", List.of(Map.of(
                        "reference_id", order.getId().toString(),
                        "description", "TopTop order " + order.getOrderCode(),
                        "amount", Map.of(
                                "currency_code", paypalCurrency.toUpperCase(Locale.ROOT),
                                "value", paypalAmount(order.getTotalAmount(), order.getCurrency())
                        )
                )),
                "application_context", Map.of(
                        "return_url", orderDetailUrl(order.getId()) + "?payment=paypal-success",
                        "cancel_url", orderDetailUrl(order.getId()) + "?payment=paypal-cancel"
                )
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                paypalBaseUrl + "/v2/checkout/orders",
                new HttpEntity<>(body, headers),
                Map.class
        );

        Map<?, ?> responseBody = response.getBody();
        if (responseBody != null) {
            Object id = responseBody.get("id");
            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
            if (payment != null && id != null) {
                payment.setProviderTransactionId(id.toString());
                paymentRepository.save(payment);
            }

            Object links = responseBody.get("links");
            if (links instanceof List<?> linkList) {
                for (Object link : linkList) {
                    if (link instanceof Map<?, ?> linkMap && "approve".equals(linkMap.get("rel"))) {
                        Object href = linkMap.get("href");
                        if (href != null) {
                            return href.toString();
                        }
                    }
                }
            }
        }
        throw new AppException(ErrorCode.BAD_REQUEST);
    }

    private String requestPaypalAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(paypalClientId, paypalClientSecret);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        ResponseEntity<Map> response = restTemplate.postForEntity(
                paypalBaseUrl + "/v1/oauth2/token",
                new HttpEntity<>(body, headers),
                Map.class
        );

        Object token = response.getBody() != null ? response.getBody().get("access_token") : null;
        if (token == null || token.toString().isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST);
        }
        return token.toString();
    }

    private String stripeUnitAmount(BigDecimal amount, String currency) {
        boolean zeroDecimal = Set.of("bif", "clp", "djf", "gnf", "jpy", "kmf", "krw", "mga", "pyg", "rwf", "ugx", "vnd", "vuv", "xaf", "xof", "xpf")
                .contains(currency.toLowerCase(Locale.ROOT));
        BigDecimal unitAmount = zeroDecimal ? amount : amount.multiply(BigDecimal.valueOf(100));
        return unitAmount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private String paypalAmount(BigDecimal amount, String orderCurrency) {
        BigDecimal converted = amount;
        if ("VND".equalsIgnoreCase(orderCurrency) && "USD".equalsIgnoreCase(paypalCurrency)) {
            converted = amount.divide(paypalVndToUsdRate, 2, RoundingMode.HALF_UP);
        }
        return converted.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String orderDetailUrl(Long orderId) {
        return UriComponentsBuilder.fromUriString(frontendProperties.getPrimaryUrl())
                .path("/orders/{orderId}")
                .buildAndExpand(orderId)
                .toUriString();
    }

    private void ensureShopIsNotOwnedByBuyer(Shop shop, User buyer) {
        if (shop.getOwnerId().equals(buyer.getId())) {
            throw new AppException(ErrorCode.CANNOT_BUY_OWN_SHOP_PRODUCT);
        }
    }

    private PaymentResponse mapPaymentResponse(Payment payment, String redirectUrl) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrderId())
                .provider(payment.getProvider())
                .providerTransactionId(payment.getProviderTransactionId())
                .redirectUrl(redirectUrl)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus().name())
                .paidAt(payment.getPaidAt())
                .build();
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
        ShopPayout payout = calculateShopPayout(order);

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
                .commissionBaseAmount(payout.commissionBaseAmount())
                .shopPayoutRate(payout.shopPayoutRate())
                .shopPayoutAmount(payout.shopPayoutAmount())
                .platformFeeAmount(payout.platformFeeAmount())
                .commissionTier(payout.commissionTier())
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

    private ShopPayout calculateShopPayout(Order order) {
        BigDecimal commissionBase = order.getSubtotalAmount()
                .subtract(order.getDiscountAmount() != null ? order.getDiscountAmount() : BigDecimal.ZERO)
                .max(BigDecimal.ZERO);

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            return new ShopPayout(
                    commissionBase,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    commissionBase,
                    "UNPAID"
            );
        }

        List<PaymentStatus> paidStatuses = List.of(PaymentStatus.PAID);
        long paidOrderCount = orderRepository.countByShopIdAndPaymentStatusIn(order.getShopId(), paidStatuses);
        BigDecimal paidGross = orderRepository.sumPaidTotalByShopId(
                order.getShopId(),
                paidStatuses.stream().map(Enum::name).toList()
        );

        BigDecimal shopPayoutRate;
        String tier;
        if (paidOrderCount >= 100 || paidGross.compareTo(BigDecimal.valueOf(50_000_000L)) >= 0) {
            shopPayoutRate = BigDecimal.valueOf(0.40);
            tier = "LARGE";
        } else if (paidOrderCount >= 30 || paidGross.compareTo(BigDecimal.valueOf(10_000_000L)) >= 0) {
            shopPayoutRate = BigDecimal.valueOf(0.35);
            tier = "MEDIUM";
        } else {
            shopPayoutRate = BigDecimal.valueOf(0.30);
            tier = "SMALL";
        }

        BigDecimal shopPayout = commissionBase.multiply(shopPayoutRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal platformFee = commissionBase.subtract(shopPayout).setScale(2, RoundingMode.HALF_UP);
        return new ShopPayout(commissionBase, shopPayoutRate, shopPayout, platformFee, tier);
    }

    private record ShopPayout(
            BigDecimal commissionBaseAmount,
            BigDecimal shopPayoutRate,
            BigDecimal shopPayoutAmount,
            BigDecimal platformFeeAmount,
            String commissionTier
    ) {
    }
}
