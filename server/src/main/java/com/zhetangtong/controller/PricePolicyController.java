package com.zhetangtong.controller;

import com.zhetangtong.common.Result;
import com.zhetangtong.controller.dto.PricePolicyRequest;
import com.zhetangtong.entity.PricePolicy;
import com.zhetangtong.service.PricePolicyService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * 价格政策接口：查询登录即可（开单带价），维护需管理员（由 Security 统一控制）。
 */
@RestController
@RequestMapping("/api/price-policies")
public class PricePolicyController {

    private final PricePolicyService policyService;

    public PricePolicyController(PricePolicyService policyService) {
        this.policyService = policyService;
    }

    /** 政策列表（附带自动流转后的状态） */
    @GetMapping
    public Result<List<PricePolicy>> list(@RequestParam(required = false) Long varietyId) {
        return Result.ok(policyService.list(varietyId));
    }

    /** 某品种指定日期生效中的政策（开单带价），无则返回 null */
    @GetMapping("/effective")
    public Result<PricePolicy> effective(
            @RequestParam(required = false) Long varietyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.ok(policyService.effective(varietyId, date == null ? LocalDate.now() : date));
    }

    /** 新增政策（ADMIN，同品种生效期不可重叠） */
    @PostMapping
    public Result<PricePolicy> create(@Valid @RequestBody PricePolicyRequest req) {
        return Result.ok(policyService.create(req));
    }

    /** 修改政策（ADMIN） */
    @PutMapping("/{id}")
    public Result<PricePolicy> update(@PathVariable Long id, @Valid @RequestBody PricePolicyRequest req) {
        return Result.ok(policyService.update(id, req));
    }

    /** 删除政策（ADMIN） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        policyService.delete(id);
        return Result.ok();
    }
}
