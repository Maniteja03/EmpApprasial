package com.cvrce.apraisal.controller;

import com.cvrce.apraisal.dto.SummaryRowDTO;
import com.cvrce.apraisal.serviceImpl.SummaryViewServiceImpl;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class SummaryViewController {

	
    private final SummaryViewServiceImpl summaryViewService;

    @GetMapping("/rows")
    public ResponseEntity<List<SummaryRowDTO>> getSummaryRows() {
        return ResponseEntity.ok(summaryViewService.getSummaryRows());
    }
}

