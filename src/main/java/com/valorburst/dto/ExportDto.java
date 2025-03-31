package com.valorburst.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.*;

@Data
@Builder
public class ExportDto {

    @ExcelProperty("ID")
    private Integer id;

    @ExcelProperty("账号ID")
    private String agentAccount;

    @ExcelProperty("账号")
    private String agentName;

    @ExcelProperty("业务ID")
    private String myBusinessId;

    @ExcelProperty("关键词")
    private String guanjianziName;

    @ExcelProperty("平台")
    private String pingtai;

    @ExcelProperty("作品链接")
    private String worksUrl;

    @ExcelProperty("发布日期")
    private String fabuDate;

    @ExcelProperty("播放量")
    private Long playCount;

    @ExcelProperty("更新日期")
    private String updateDate;
}
