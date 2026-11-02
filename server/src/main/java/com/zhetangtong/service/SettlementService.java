package com.zhetangtong.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhetangtong.common.BizException;
import com.zhetangtong.entity.Customer;
import com.zhetangtong.entity.Payment;
import com.zhetangtong.entity.PurchaseOrder;
import com.zhetangtong.mapper.CustomerMapper;
import com.zhetangtong.mapper.PaymentMapper;
import com.zhetangtong.mapper.PurchaseOrderMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 结算对账服务：按 客户 + 起止日期 输出期初/笔数/总吨/总额/已付/期末应付与逐笔明细。
 * 口径：只统计已确认单据；期末应付 = 期初应付 + 期间已确认金额 − 期间付款合计。
 */
@Service
public class SettlementService {

    private final CustomerMapper customerMapper;
    private final PurchaseOrderMapper orderMapper;
    private final PaymentMapper paymentMapper;
    private final DebtService debtService;

    public SettlementService(CustomerMapper customerMapper, PurchaseOrderMapper orderMapper,
                             PaymentMapper paymentMapper, DebtService debtService) {
        this.customerMapper = customerMapper;
        this.orderMapper = orderMapper;
        this.paymentMapper = paymentMapper;
        this.debtService = debtService;
    }

    public Map<String, Object> reconcile(Long customerId, LocalDate dateFrom, LocalDate dateTo) {
        if (customerId == null) {
            throw new BizException(400, "请选择客户");
        }
        Customer customer = customerMapper.selectById(customerId);
        if (customer == null) {
            throw new BizException(404, "客户不存在");
        }

        // 期初 = 起始日前的已确认金额 − 起始日前的付款
        BigDecimal opening = debtService.confirmedTotal(customerId, dateFrom)
                .subtract(debtService.paidTotal(customerId, dateFrom));

        // 期间明细
        List<PurchaseOrder> orders = orderMapper.selectList(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getStatus, "CONFIRMED")
                .eq(PurchaseOrder::getCustomerId, customerId)
                .ge(dateFrom != null, PurchaseOrder::getOrderDate, dateFrom)
                .le(dateTo != null, PurchaseOrder::getOrderDate, dateTo)
                .orderByAsc(PurchaseOrder::getOrderDate)
                .orderByAsc(PurchaseOrder::getId));
        List<Payment> payments = paymentMapper.selectList(new LambdaQueryWrapper<Payment>()
                .eq(Payment::getStatus, "REGISTERED")
                .eq(Payment::getCustomerId, customerId)
                .ge(dateFrom != null, Payment::getPayDate, dateFrom)
                .le(dateTo != null, Payment::getPayDate, dateTo)
                .orderByAsc(Payment::getPayDate)
                .orderByAsc(Payment::getId));

        BigDecimal periodAmount = BigDecimal.ZERO;
        BigDecimal periodWeight = BigDecimal.ZERO;
        for (PurchaseOrder o : orders) {
            periodAmount = periodAmount.add(nvl(o.getAmount()));
            periodWeight = periodWeight.add(nvl(o.getCalcWeight()));
        }
        BigDecimal periodPaid = BigDecimal.ZERO;
        for (Payment p : payments) {
            periodPaid = periodPaid.add(nvl(p.getAmount()));
        }
        BigDecimal closing = opening.add(periodAmount).subtract(periodPaid);

        // 期间内未纳入统计的待复核单据（显著提示）
        Long pendingCount = orderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getStatus, "PENDING")
                .eq(PurchaseOrder::getCustomerId, customerId)
                .ge(dateFrom != null, PurchaseOrder::getOrderDate, dateFrom)
                .le(dateTo != null, PurchaseOrder::getOrderDate, dateTo));

        List<Map<String, Object>> orderItems = new ArrayList<>();
        for (PurchaseOrder o : orders) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("orderDate", o.getOrderDate());
            m.put("varietyName", o.getVarietyName());
            m.put("plateNo", o.getPlateNo());
            m.put("calcWeight", o.getCalcWeight());
            m.put("amount", o.getAmount());
            m.put("createdByName", o.getCreatedByName());
            orderItems.add(m);
        }
        List<Map<String, Object>> paymentItems = new ArrayList<>();
        for (Payment p : payments) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("payDate", p.getPayDate());
            m.put("amount", p.getAmount());
            m.put("method", p.getMethod());
            m.put("handler", p.getHandler());
            paymentItems.add(m);
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("customerId", customer.getId());
        data.put("customerName", customer.getName());
        data.put("customerType", customer.getType());
        data.put("dateFrom", dateFrom);
        data.put("dateTo", dateTo);
        data.put("openingBalance", opening.setScale(2, RoundingMode.HALF_UP));
        data.put("orderCount", (long) orders.size());
        data.put("totalWeight", periodWeight.setScale(3, RoundingMode.HALF_UP));
        data.put("totalAmount", periodAmount.setScale(2, RoundingMode.HALF_UP));
        data.put("paymentTotal", periodPaid.setScale(2, RoundingMode.HALF_UP));
        data.put("closingBalance", closing.setScale(2, RoundingMode.HALF_UP));
        data.put("pendingCount", pendingCount);
        data.put("orders", orderItems);
        data.put("payments", paymentItems);
        return data;
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
