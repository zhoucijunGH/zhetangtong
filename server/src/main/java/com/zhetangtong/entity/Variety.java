package com.zhetangtong.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 甘蔗品种档案。
 */
@Data
@TableName("variety")
public class Variety {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 品种名，如 桂糖42号 */
    private String name;

    /** 备注 */
    private String remark;

    /** 1 启用 0 停用 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
