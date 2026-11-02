package com.zhetangtong.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhetangtong.common.BizException;
import com.zhetangtong.controller.dto.VarietyRequest;
import com.zhetangtong.entity.Variety;
import com.zhetangtong.mapper.VarietyMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 甘蔗品种档案服务：查询 / 维护（维护仅管理员）。
 */
@Service
public class VarietyService {

    private final VarietyMapper varietyMapper;

    public VarietyService(VarietyMapper varietyMapper) {
        this.varietyMapper = varietyMapper;
    }

    /** 品种列表：名称模糊 + 状态过滤 */
    public List<Variety> list(String keyword, Integer status) {
        LambdaQueryWrapper<Variety> wrapper = new LambdaQueryWrapper<Variety>()
                .eq(status != null, Variety::getStatus, status)
                .like(StringUtils.hasText(keyword), Variety::getName, keyword)
                .orderByDesc(Variety::getId);
        return varietyMapper.selectList(wrapper);
    }

    public Variety create(VarietyRequest req) {
        Variety variety = new Variety();
        apply(variety, req);
        variety.setStatus(req.getStatus() == null ? 1 : req.getStatus());
        varietyMapper.insert(variety);
        return variety;
    }

    public Variety update(Long id, VarietyRequest req) {
        Variety variety = get(id);
        apply(variety, req);
        if (req.getStatus() != null) {
            variety.setStatus(req.getStatus());
        }
        varietyMapper.updateById(variety);
        return variety;
    }

    public void delete(Long id) {
        get(id);
        varietyMapper.deleteById(id);
    }

    public Variety get(Long id) {
        Variety variety = varietyMapper.selectById(id);
        if (variety == null) {
            throw new BizException(404, "品种不存在");
        }
        return variety;
    }

    private void apply(Variety variety, VarietyRequest req) {
        variety.setName(req.getName().trim());
        variety.setRemark(req.getRemark());
    }
}
