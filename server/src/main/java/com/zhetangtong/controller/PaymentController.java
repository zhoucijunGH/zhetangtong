package com.zhetangtong.controller;

import com.zhetangtong.common.PageResult;
import com.zhetangtong.common.Result;
import com.zhetangtong.controller.dto.PaymentRequest;
import com.zhetangtong.controller.dto.VoidRequest;
import com.zhetangtong.entity.Payment;
import com.zhetangtong.security.LoginUser;
import com.zhetangtong.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * 付款记录接口：登记/作废需财务或老板（由 Security 统一控制）。
 * 登记后不可修改，只可作废；作废后客户余额自动回滚。
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** 登记付款（FINANCE/ADMIN） */
    @PostMapping
    public Result<Payment> create(@Valid @RequestBody PaymentRequest req,
                                  @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(paymentService.create(req, loginUser));
    }

    /** 付款记录分页 */
    @GetMapping
    public Result<PageResult<Payment>> page(
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(paymentService.page(customerId, dateFrom, dateTo, status, page, size));
    }

    /** 付款记录详情 */
    @GetMapping("/{id}")
    public Result<Payment> get(@PathVariable Long id) {
        return Result.ok(paymentService.get(id));
    }

    /** 作废付款（FINANCE/ADMIN，必填原因） */
    @PostMapping("/{id}/void")
    public Result<Payment> voidPayment(@PathVariable Long id,
                                       @Valid @RequestBody VoidRequest req,
                                       @AuthenticationPrincipal LoginUser loginUser) {
        return Result.ok(paymentService.voidPayment(id, req.getReason(), loginUser));
    }
}
