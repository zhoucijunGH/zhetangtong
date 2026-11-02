package com.zhetangtong.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 付款登记请求。
 */
@Data
public class PaymentRequest {

    /** 客户 id */
    @NotNull(message = "请选择客户")
    private Long customerId;

    /** 金额（元） */
    @NotNull(message = "请填写金额")
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    private BigDecimal amount;

    /** 方式：CASH 现金 / TRANSFER 转账 / OTHER 其他 */
    @NotBlank(message = "请选择付款方式")
    private String method;

    /** 经办人 */
    private String handler;

    /** 付款日期（缺省为今天） */
    private LocalDate payDate;

    /** 凭证照片（可选） */
    private String voucherUrl;
}
