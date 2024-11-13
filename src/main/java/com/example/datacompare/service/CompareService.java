package com.example.datacompare.service;

import com.example.datacompare.model.CompareRequest;
import com.example.datacompare.model.CompareResult;
import com.example.datacompare.model.DiffInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CompareService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public CompareResult compareData(CompareRequest request) {
        CompareResult result = new CompareResult();
        Map<String, DiffInfo> differences = new HashMap<>();

        try {
            JsonNode sourceRoot = objectMapper.readTree(request.getSourceData());
            JsonNode targetRoot = objectMapper.readTree(request.getTargetData());

            // 获取数组的所有元素
            for (int i = 0; i < sourceRoot.size(); i++) {
                JsonNode sourceObj = sourceRoot.get(i);
                JsonNode targetObj = targetRoot.get(i);

                request.getMappings().forEach(mapping -> {
                    JsonNode sourceValue = getValueByPath(sourceObj, mapping.getSourcePath());
                    JsonNode targetValue = getValueByPath(targetObj, mapping.getTargetPath());

                    if (sourceValue != null && targetValue != null &&
                            !sourceValue.equals(targetValue)) {
                        DiffInfo diffInfo = new DiffInfo();
                        diffInfo.setSourceValue(sourceValue);
                        diffInfo.setTargetValue(targetValue);
                        diffInfo.setTargetField(mapping.getTargetPath());
                        differences.put(mapping.getSourcePath(), diffInfo);
                    }
                });
            }

            result.setDifferences(differences);
            result.setSuccess(true);

        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("比对过程出错: " + e.getMessage());
        }
        return result;
    }

    private JsonNode getValueByPath(JsonNode root, String path) {
        try {
            JsonNode current = root;
            String[] parts = path.split("\\.");

            for (String part : parts) {
                if (current == null) {
                    return null;
                }

                if (current.isArray()) {
                    current = current.get(0);
                }

                current = current.get(part);
            }

            return current;

        } catch (Exception e) {
            return null;
        }
    }
} 