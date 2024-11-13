package com.example.datacompare.model;

import lombok.Data;

@Data
public class FieldMapping {
    private String sourcePath;  // 源字段路径
    private String targetPath;  // 目标字段路径
} 