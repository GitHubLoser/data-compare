package com.example.datacompare.model;

import java.util.Map;
import lombok.Data;

@Data
public class CompareResult {
    private Map<String, DiffInfo> differences;  // 差异信息
    private boolean success;
    private String message;
} 