package com.zhetangtong.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 客户新增/修改请求。
 */
@Data
public class CustomerRequest {

    /** 姓名 / 名称 */
    @NotBlank(message = "请填写客户名称")
    private String name;

    /** 类型：FARMER 蔗农 / BROKER 经纪人 / COOP 合作社 */
    @NotBlank(message = "请选择客户类型")
    private String type;

    /** 电话 */
    private String phone;

    /** 地址 */
    private String address;

    /** 备注 */
    private String remark;

    /** 1 启用 0 停用（编辑时可选） */
    private Integer status;
}
