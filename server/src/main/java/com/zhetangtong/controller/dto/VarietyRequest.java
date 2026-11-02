package com.zhetangtong.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 甘蔗品种新增/修改请求。
 */
@Data
public class VarietyRequest {

    /** 品种名 */
    @NotBlank(message = "请填写品种名称")
    private String name;

    /** 备注 */
    private String remark;

    /** 1 启用 0 停用（编辑时可选） */
    private Integer status;
}
