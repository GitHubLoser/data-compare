package com.example.datacompare.controller;

import com.example.datacompare.model.CompareRequest;
import com.example.datacompare.model.CompareResult;
import com.example.datacompare.service.CompareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/compare")
@CrossOrigin(origins = "*") // 开发环境下允许跨域
public class CompareController {
    
    @Autowired
    private CompareService compareService;
    
    @PostMapping("/data")
    public CompareResult compareData(@RequestBody CompareRequest request) {
        return compareService.compareData(request);
    }
} 