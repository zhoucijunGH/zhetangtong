package com.zhetangtong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 价格政策（品种 × 单价 × 生效区间）。
 * 同一生效期内同一品种不允许重叠；状态按日期自动流转（不落库）。
 */
@Data
@TableName("price_policy")
public class PricePolicy {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 品种 id */
    private Long varietyId;

    /** 品种名（冗余快照） */
    private String varietyName;

    /** 单价（元/吨） */
    private BigDecimal price;

    /** 生效日期（含） */
    private LocalDate startDate;

    /** 失效日期（含），为空表示长期有效 */
    private LocalDate endDate;

    /** 备注 */
    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** 状态：NOT_STARTED 未生效 / ACTIVE 生效中 / EXPIRED 已失效（按日期自动计算，不落库） */
    @TableField(exist = false)
    private String status;
}
