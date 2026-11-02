package com.zhetangtong.controller;

import com.zhetangtong.common.Result;
import com.zhetangtong.controller.dto.CustomerRequest;
import com.zhetangtong.entity.Customer;
import com.zhetangtong.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 客户档案接口：查询登录即可，维护需管理员（由 Security 统一控制）。
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /** 客户列表：keyword 模糊名称/电话，status 过滤启停 */
    @GetMapping
    public Result<List<Customer>> list(@RequestParam(required = false) String keyword,
                                       @RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) Long id) {
        if (id != null) {
            return Result.ok(List.of(customerService.get(id)));
        }
        return Result.ok(customerService.list(keyword, status));
    }

    /** 新增客户（ADMIN） */
    @PostMapping
    public Result<Customer> create(@Valid @RequestBody CustomerRequest req) {
        return Result.ok(customerService.create(req));
    }

    /** 修改客户（ADMIN） */
    @PutMapping("/{id}")
    public Result<Customer> update(@PathVariable Long id, @Valid @RequestBody CustomerRequest req) {
        return Result.ok(customerService.update(id, req));
    }

    /** 删除客户（ADMIN） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return Result.ok();
    }
}
