package com.zhetangtong.controller;

import com.zhetangtong.common.Result;
import com.zhetangtong.controller.dto.VarietyRequest;
import com.zhetangtong.entity.Variety;
import com.zhetangtong.service.VarietyService;
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
 * 甘蔗品种接口：查询登录即可，维护需管理员（由 Security 统一控制）。
 */
@RestController
@RequestMapping("/api/varieties")
public class VarietyController {

    private final VarietyService varietyService;

    public VarietyController(VarietyService varietyService) {
        this.varietyService = varietyService;
    }

    /** 品种列表 */
    @GetMapping
    public Result<List<Variety>> list(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status) {
        return Result.ok(varietyService.list(keyword, status));
    }

    /** 新增品种（ADMIN） */
    @PostMapping
    public Result<Variety> create(@Valid @RequestBody VarietyRequest req) {
        return Result.ok(varietyService.create(req));
    }

    /** 修改品种（ADMIN） */
    @PutMapping("/{id}")
    public Result<Variety> update(@PathVariable Long id, @Valid @RequestBody VarietyRequest req) {
        return Result.ok(varietyService.update(id, req));
    }

    /** 删除品种（ADMIN） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        varietyService.delete(id);
        return Result.ok();
    }
}
