package com.xiaozhi.mall.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

/**
 * 订单接口
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderRepository orderRepository;
    private final UserClient userClient;

    public OrderController(OrderRepository orderRepository, UserClient userClient) {
        this.orderRepository = orderRepository;
        this.userClient = userClient;
    }

    @GetMapping
    public List<Order> list() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    public Order get(@PathVariable Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在: " + id));
    }

    /**
     * 下单：先远程调用 mall-user 校验用户是否存在，再保存订单
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Order create(@Valid @RequestBody CreateOrderRequest request) {
        UserClient.UserDTO user = userClient.getUser(request.userId());
        return orderRepository.save(new Order(user.id(), user.username(), request.productName(), request.amount()));
    }

    public record CreateOrderRequest(@NotNull Long userId,
                                     @NotBlank String productName,
                                     @NotNull @DecimalMin("0.01") BigDecimal amount) {
    }
}
