package com.zhetangtong.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhetangtong.common.BizException;
import com.zhetangtong.common.PageResult;
import com.zhetangtong.controller.dto.PaymentRequest;
import com.zhetangtong.entity.Customer;
import com.zhetangtong.entity.Payment;
import com.zhetangtong.entity.SysUser;
import com.zhetangtong.mapper.CustomerMapper;
import com.zhetangtong.mapper.PaymentMapper;
import com.zhetangtong.mapper.SysUserMapper;
import com.zhetangtong.security.LoginUser;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 付款记录服务：登记后不可修改，只可作废（作废后余额自动回滚——余额按有效付款实时汇总）。
 */
@Service
public class PaymentService {

    public static final String STATUS_REGISTERED = "REGISTERED";
    public static final String STATUS_VOIDED = "VOIDED";

    private final PaymentMapper paymentMapper;
    private final CustomerMapper customerMapper;
    private final SysUserMapper userMapper;

    public PaymentService(PaymentMapper paymentMapper, CustomerMapper customerMapper, SysUserMapper userMapper) {
        this.paymentMapper = paymentMapper;
        this.customerMapper = customerMapper;
        this.userMapper = userMapper;
    }

    /** 登记付款 */
    public Payment create(PaymentRequest req, LoginUser loginUser) {
        Customer customer = customerMapper.selectById(req.getCustomerId());
        if (customer == null) {
            throw new BizException(404, "客户不存在");
        }
        Payment payment = new Payment();
        payment.setCustomerId(customer.getId());
        payment.setCustomerName(customer.getName());
        payment.setAmount(req.getAmount());
        payment.setMethod(req.getMethod());
        payment.setHandler(StringUtils.hasText(req.getHandler())
                ? req.getHandler() : currentName(loginUser));
        payment.setPayDate(req.getPayDate() == null ? LocalDate.now() : req.getPayDate());
        payment.setVoucherUrl(req.getVoucherUrl());
        payment.setStatus(STATUS_REGISTERED);
        payment.setCreatedBy(loginUser.getUserId());
        payment.setCreatedByName(currentName(loginUser));
        paymentMapper.insert(payment);
        return payment;
    }

    /** 付款记录分页：客户 + 日期区间 + 状态 */
    public PageResult<Payment> page(Long customerId, LocalDate dateFrom, LocalDate dateTo,
                                    String status, int page, int size) {
        LambdaQueryWrapper<Payment> wrapper = new LambdaQueryWrapper<Payment>()
                .eq(customerId != null, Payment::getCustomerId, customerId)
                .ge(dateFrom != null, Payment::getPayDate, dateFrom)
                .le(dateTo != null, Payment::getPayDate, dateTo)
                .eq(StringUtils.hasText(status), Payment::getStatus, status)
                .orderByDesc(Payment::getPayDate)
                .orderByDesc(Payment::getId);
        IPage<Payment> result = paymentMapper.selectPage(new Page<>(page, size), wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    public Payment get(Long id) {
        Payment payment = paymentMapper.selectById(id);
        if (payment == null) {
            throw new BizException(404, "付款记录不存在");
        }
        return payment;
    }

    /** 作废付款：只可作废不可改，作废后余额回滚 */
    public Payment voidPayment(Long id, String reason, LoginUser loginUser) {
        Payment payment = get(id);
        if (STATUS_VOIDED.equals(payment.getStatus())) {
            throw new BizException(400, "该付款记录已作废");
        }
        payment.setStatus(STATUS_VOIDED);
        payment.setVoidReason(reason);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.updateById(payment);
        return payment;
    }

    private String currentName(LoginUser loginUser) {
        SysUser user = userMapper.selectById(loginUser.getUserId());
        return user != null && StringUtils.hasText(user.getNickname())
                ? user.getNickname() : loginUser.getUsername();
    }
}
