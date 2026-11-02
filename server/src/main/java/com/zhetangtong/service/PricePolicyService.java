package com.zhetangtong.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhetangtong.common.BizException;
import com.zhetangtong.controller.dto.PricePolicyRequest;
import com.zhetangtong.entity.PricePolicy;
import com.zhetangtong.entity.Variety;
import com.zhetangtong.mapper.PricePolicyMapper;
import com.zhetangtong.mapper.VarietyMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 价格政策服务：状态按日期自动流转；同品种生效区间不允许重叠。
 */
@Service
public class PricePolicyService {

    private final PricePolicyMapper policyMapper;
    private final VarietyMapper varietyMapper;

    public PricePolicyService(PricePolicyMapper policyMapper, VarietyMapper varietyMapper) {
        this.policyMapper = policyMapper;
        this.varietyMapper = varietyMapper;
    }

    /** 政策列表（附带计算后的状态） */
    public List<PricePolicy> list(Long varietyId) {
        List<PricePolicy> list = policyMapper.selectList(new LambdaQueryWrapper<PricePolicy>()
                .eq(varietyId != null, PricePolicy::getVarietyId, varietyId)
                .orderByDesc(PricePolicy::getStartDate)
                .orderByDesc(PricePolicy::getId));
        LocalDate today = LocalDate.now();
        list.forEach(p -> p.setStatus(computeStatus(p, today)));
        return list;
    }

    /** 某品种在指定日期生效中的政策（开单带价用），没有则返回 null */
    public PricePolicy effective(Long varietyId, LocalDate date) {
        if (varietyId == null || date == null) {
            return null;
        }
        List<PricePolicy> list = policyMapper.selectList(new LambdaQueryWrapper<PricePolicy>()
                .eq(PricePolicy::getVarietyId, varietyId)
                .le(PricePolicy::getStartDate, date)
                .and(w -> w.isNull(PricePolicy::getEndDate).or().ge(PricePolicy::getEndDate, date)));
        return list.isEmpty() ? null : list.get(0);
    }

    public PricePolicy create(PricePolicyRequest req) {
        Variety variety = requireVariety(req.getVarietyId());
        validateRange(req, null);
        PricePolicy policy = new PricePolicy();
        policy.setVarietyId(variety.getId());
        policy.setVarietyName(variety.getName());
        apply(policy, req);
        policyMapper.insert(policy);
        policy.setStatus(computeStatus(policy, LocalDate.now()));
        return policy;
    }

    public PricePolicy update(Long id, PricePolicyRequest req) {
        PricePolicy policy = get(id);
        Variety variety = requireVariety(req.getVarietyId());
        validateRange(req, id);
        policy.setVarietyId(variety.getId());
        policy.setVarietyName(variety.getName());
        apply(policy, req);
        policyMapper.updateById(policy);
        policy.setStatus(computeStatus(policy, LocalDate.now()));
        return policy;
    }

    public void delete(Long id) {
        get(id);
        policyMapper.deleteById(id);
    }

    public PricePolicy get(Long id) {
        PricePolicy policy = policyMapper.selectById(id);
        if (policy == null) {
            throw new BizException(404, "价格政策不存在");
        }
        return policy;
    }

    private Variety requireVariety(Long varietyId) {
        Variety variety = varietyMapper.selectById(varietyId);
        if (variety == null) {
            throw new BizException(404, "品种不存在");
        }
        return variety;
    }

    /** 区间校验 + 同品种生效期重叠校验 */
    private void validateRange(PricePolicyRequest req, Long excludeId) {
        if (req.getEndDate() != null && req.getEndDate().isBefore(req.getStartDate())) {
            throw new BizException(400, "失效日期不能早于生效日期");
        }
        List<PricePolicy> sameVariety = policyMapper.selectList(new LambdaQueryWrapper<PricePolicy>()
                .eq(PricePolicy::getVarietyId, req.getVarietyId())
                .ne(excludeId != null, PricePolicy::getId, excludeId));
        LocalDate newStart = req.getStartDate();
        LocalDate newEnd = req.getEndDate() == null ? LocalDate.of(9999, 12, 31) : req.getEndDate();
        for (PricePolicy exist : sameVariety) {
            LocalDate existStart = exist.getStartDate();
            LocalDate existEnd = exist.getEndDate() == null ? LocalDate.of(9999, 12, 31) : exist.getEndDate();
            // 区间相交（含端点）即视为重叠
            if (!newStart.isAfter(existEnd) && !newEnd.isBefore(existStart)) {
                throw new BizException(400, "与该品种已有价格政策的生效期重叠（"
                        + existStart + " ~ " + (exist.getEndDate() == null ? "长期" : exist.getEndDate()) + "）");
            }
        }
    }

    /** 状态按日期自动流转：未生效 / 生效中 / 已失效 */
    public String computeStatus(PricePolicy policy, LocalDate today) {
        if (today.isBefore(policy.getStartDate())) {
            return "NOT_STARTED";
        }
        if (policy.getEndDate() != null && today.isAfter(policy.getEndDate())) {
            return "EXPIRED";
        }
        return "ACTIVE";
    }

    private void apply(PricePolicy policy, PricePolicyRequest req) {
        policy.setPrice(req.getPrice());
        policy.setStartDate(req.getStartDate());
        policy.setEndDate(req.getEndDate());
        policy.setRemark(req.getRemark());
    }
}
