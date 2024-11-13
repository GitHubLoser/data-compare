package com.example.datacompare.model;

import java.util.List;
import lombok.Data;

@Data
public class CompareRequest {
    private String sourceData;      // 源数据 JSON 字符串
    private String targetData;      // 目标数据 JSON 字符串
    private List<FieldMapping> mappings;  // 字段映射关系
} 