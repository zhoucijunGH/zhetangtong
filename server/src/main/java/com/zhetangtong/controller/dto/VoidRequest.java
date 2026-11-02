package com.zhetangtong.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 作废请求（收购单 / 付款记录通用）。
 */
@Data
public class VoidRequest {

    /** 作废原因（必填，留痕） */
    @NotBlank(message = "请填写作废原因")
    private String reason;
}
