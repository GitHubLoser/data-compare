package com.example.datacompare.model;

import lombok.Data;

@Data
public class DiffInfo {
    private Object sourceValue;
    private Object targetValue;
    private String targetField;
} 