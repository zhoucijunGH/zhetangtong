package com.zhetangtong.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhetangtong.common.BizException;
import com.zhetangtong.controller.dto.CustomerRequest;
import com.zhetangtong.entity.Customer;
import com.zhetangtong.mapper.CustomerMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 客户档案服务：查询 / 维护（维护仅管理员）。
 */
@Service
public class CustomerService {

    private final CustomerMapper customerMapper;

    public CustomerService(CustomerMapper customerMapper) {
        this.customerMapper = customerMapper;
    }

    /** 客户列表：名称/电话模糊 + 状态过滤 */
    public List<Customer> list(String keyword, Integer status) {
        LambdaQueryWrapper<Customer> wrapper = new LambdaQueryWrapper<Customer>()
                .eq(status != null, Customer::getStatus, status)
                .and(StringUtils.hasText(keyword), w -> w
                        .like(Customer::getName, keyword)
                        .or().like(Customer::getPhone, keyword))
                .orderByDesc(Customer::getId);
        return customerMapper.selectList(wrapper);
    }

    /** 新增客户 */
    public Customer create(CustomerRequest req) {
        Customer customer = new Customer();
        apply(customer, req);
        customer.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        customerMapper.insert(customer);
        return customer;
    }

    /** 修改客户 */
    public Customer update(Long id, CustomerRequest req) {
        Customer customer = get(id);
        apply(customer, req);
        if (req.getStatus() != null) {
            customer.setStatus(req.getStatus());
        }
        customerMapper.updateById(customer);
        return customer;
    }

    /** 删除客户（档案仅被单据冗余引用，物理删除可接受） */
    public void delete(Long id) {
        get(id);
        customerMapper.deleteById(id);
    }

    public Customer get(Long id) {
        Customer customer = customerMapper.selectById(id);
        if (customer == null) {
            throw new BizException(404, "客户不存在");
        }
        return customer;
    }

    private void apply(Customer customer, CustomerRequest req) {
        customer.setName(req.getName().trim());
        customer.setType(req.getType());
        customer.setPhone(req.getPhone());
        customer.setAddress(req.getAddress());
        customer.setRemark(req.getRemark());
    }
}
