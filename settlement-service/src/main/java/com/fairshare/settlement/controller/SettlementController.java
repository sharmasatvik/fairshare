package com.fairshare.settlement.controller;

import com.fairshare.settlement.domain.Settlement;
import com.fairshare.settlement.dto.CreateSettlementRequest;
import com.fairshare.settlement.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/groups/{groupId}/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @PostMapping
    public ResponseEntity<Settlement> recordSettlement(
            @PathVariable UUID groupId,
            @Valid @RequestBody CreateSettlementRequest request) {
        Settlement settlement = settlementService.recordSettlement(groupId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(settlement);
    }

    @GetMapping
    public List<Settlement> getSettlements(@PathVariable UUID groupId) {
        return settlementService.getSettlementsForGroup(groupId);
    }
}
