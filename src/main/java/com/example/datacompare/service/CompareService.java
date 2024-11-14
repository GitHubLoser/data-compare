package com.example.datacompare.service;

import com.example.datacompare.model.CompareRequest;
import com.example.datacompare.model.CompareResult;
import com.example.datacompare.model.DiffInfo;
import com.example.datacompare.model.FieldMapping;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Arrays;
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

            System.out.println("Source Data: " + sourceRoot);
            System.out.println("Target Data: " + targetRoot);
            System.out.println("Mappings: " + request.getMappings());

            if (request.getMappings() != null && !request.getMappings().isEmpty()) {
                if (sourceRoot.isArray() && targetRoot.isArray()) {
                    for (int i = 0; i < Math.max(sourceRoot.size(), targetRoot.size()); i++) {
                        if (i < sourceRoot.size() && i < targetRoot.size()) {
                            JsonNode sourceElement = sourceRoot.get(i);
                            JsonNode targetElement = targetRoot.get(i);
                            
                            for (FieldMapping mapping : request.getMappings()) {
                                System.out.println("\nProcessing mapping for array element " + i + ": " + mapping.getSourcePath());
                                String processedPath = convertPathToArrayFormat(mapping.getSourcePath());
                                processMapping(sourceElement, targetElement, processedPath, "[" + i + "]", differences);
                            }
                        }
                    }
                } else {
                    for (FieldMapping mapping : request.getMappings()) {
                        System.out.println("\nProcessing mapping: " + mapping.getSourcePath());
                        String processedPath = convertPathToArrayFormat(mapping.getSourcePath());
                        processMapping(sourceRoot, targetRoot, processedPath, "", differences);
                    }
                }
            }

            result.setDifferences(differences);
            result.setSuccess(true);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("比对过程出错: " + e.getMessage());
            e.printStackTrace();
        }
        return result;
    }

    private String convertPathToArrayFormat(String path) {
        String[] parts = path.split("\\.");
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                result.append(".");
            }
            
            if (parts[i].equals("sales_shipment_detail_info") || 
                parts[i].equals("sales_shipment_multi_detail_info")) {
                result.append(parts[i]).append("[]");
            } else {
                result.append(parts[i]);
            }
        }
        
        return result.toString();
    }

    private void processMapping(JsonNode sourceNode, JsonNode targetNode, String path, String parentPath, Map<String, DiffInfo> differences) {
        System.out.println("Processing path with array format: " + path);
        
        if (path.contains("[]")) {
            String[] parts = path.split("\\.");
            processArrayPath(sourceNode, targetNode, parts, 0, parentPath, differences);
        } else {
            JsonNode sourceValue = getValueByPath(sourceNode, path);
            JsonNode targetValue = getValueByPath(targetNode, path);
            if (!areNodesEqual(sourceValue, targetValue)) {
                String fullPath = parentPath.isEmpty() ? path : parentPath + "." + path;
                recordDifference(fullPath, sourceValue, targetValue, differences);
            }
        }
    }

    private void processArrayPath(JsonNode sourceNode, JsonNode targetNode, String[] pathParts, int currentIndex, 
                                String currentPath, Map<String, DiffInfo> differences) {
        if (currentIndex >= pathParts.length) {
            return;
        }

        String currentPart = pathParts[currentIndex];
        System.out.println("Processing array path part: " + currentPart + " at index: " + currentIndex);

        if (currentPart.contains("[]")) {
            String arrayField = currentPart.replace("[]", "");
            JsonNode sourceArray = sourceNode.get(arrayField);
            JsonNode targetArray = targetNode.get(arrayField);

            System.out.println("Array field: " + arrayField);
            System.out.println("Source array: " + sourceArray);
            System.out.println("Target array: " + targetArray);

            if (sourceArray != null && targetArray != null && sourceArray.isArray() && targetArray.isArray()) {
                for (int i = 0; i < Math.max(sourceArray.size(), targetArray.size()); i++) {
                    if (i < sourceArray.size() && i < targetArray.size()) {
                        JsonNode sourceElement = sourceArray.get(i);
                        JsonNode targetElement = targetArray.get(i);
                        
                        String newPath = currentPath.isEmpty() ? 
                            arrayField + "[" + i + "]" : 
                            currentPath + "." + arrayField + "[" + i + "]";

                        System.out.println("Processing array element at index " + i + " with path: " + newPath);

                        if (currentIndex == pathParts.length - 1) {
                            if (!areNodesEqual(sourceElement, targetElement)) {
                                recordDifference(newPath, sourceElement, targetElement, differences);
                            }
                        } else {
                            processArrayPath(
                                sourceElement,
                                targetElement,
                                pathParts,
                                currentIndex + 1,
                                newPath,
                                differences
                            );
                        }
                    }
                }
            }
        } else {
            JsonNode nextSourceNode = sourceNode.get(currentPart);
            JsonNode nextTargetNode = targetNode.get(currentPart);
            
            String newPath = currentPath.isEmpty() ? currentPart : currentPath + "." + currentPart;
            
            if (nextSourceNode != null && nextTargetNode != null) {
                if (currentIndex == pathParts.length - 1) {
                    if (!areNodesEqual(nextSourceNode, nextTargetNode)) {
                        recordDifference(newPath, nextSourceNode, nextTargetNode, differences);
                    }
                } else {
                    processArrayPath(nextSourceNode, nextTargetNode, pathParts, currentIndex + 1, newPath, differences);
                }
            }
        }
    }

    private JsonNode getValueByPath(JsonNode node, String path) {
        if (node == null || path == null || path.isEmpty()) {
            return node;
        }

        String[] parts = path.split("\\.");
        JsonNode current = node;

        for (String part : parts) {
            if (current == null) {
                return null;
            }
            current = current.get(part);
        }

        return current;
    }

    private boolean areNodesEqual(JsonNode node1, JsonNode node2) {
        if (node1 == null && node2 == null) return true;
        if (node1 == null || node2 == null) {
            System.out.println("One of the nodes is null");
            return false;
        }
        
        if (node1.isNumber() && node2.isNumber()) {
            double value1 = node1.asDouble();
            double value2 = node2.asDouble();
            boolean equal = Math.abs(value1 - value2) < 0.000001;
            System.out.println("Comparing numbers: " + value1 + " vs " + value2 + " = " + equal);
            return equal;
        }
        
        boolean equal = node1.equals(node2);
        System.out.println("Comparing nodes: " + node1 + " vs " + node2 + " = " + equal);
        return equal;
    }

    private void recordDifference(String path, JsonNode sourceValue, JsonNode targetValue, Map<String, DiffInfo> differences) {
        DiffInfo diffInfo = new DiffInfo();
        diffInfo.setSourceValue(sourceValue);
        diffInfo.setTargetValue(targetValue);
        diffInfo.setTargetField(path);
        differences.put(path, diffInfo);
        
        System.out.println("Recording difference:");
        System.out.println("Path: " + path);
        System.out.println("Source value: " + sourceValue);
        System.out.println("Target value: " + targetValue);
    }
} 