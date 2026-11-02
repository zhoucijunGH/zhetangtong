package com.zhetangtong.controller.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 价格政策新增/修改请求。
 */
@Data
public class PricePolicyRequest {

    /** 品种 id */
    @NotNull(message = "请选择品种")
    private Long varietyId;

    /** 单价（元/吨） */
    @NotNull(message = "请填写单价")
    @DecimalMin(value = "0", message = "单价不能为负数")
    private BigDecimal price;

    /** 生效日期（含） */
    @NotNull(message = "请选择生效日期")
    private LocalDate startDate;

    /** 失效日期（含），为空表示长期有效 */
    private LocalDate endDate;

    /** 备注 */
    private String remark;
}
