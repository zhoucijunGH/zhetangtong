package com.zhetangtong.controller;

import com.zhetangtong.common.Result;
import com.zhetangtong.service.SettlementService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Map;

/**
 * 结算对账接口（FINANCE/ADMIN，由 Security 统一控制）。
 */
@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    /** 客户 + 起止日期的对账单：期初/笔数/总吨/总额/已付/期末应付 + 逐笔明细 */
    @GetMapping("/reconcile")
    public Result<Map<String, Object>> reconcile(
            @RequestParam Long customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        return Result.ok(settlementService.reconcile(customerId, dateFrom, dateTo));
    }
}
