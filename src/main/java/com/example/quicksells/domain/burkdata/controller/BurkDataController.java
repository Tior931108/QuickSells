package com.example.quicksells.domain.burkdata.controller;

import com.example.quicksells.domain.burkdata.service.BulkDataInsertService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@Hidden // swagger API 문서에서 숨겨지는 어노테이션
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BurkDataController {

    private final BulkDataInsertService bulkDataInsertService;

    /**
     * V1 MVP 기준 5개 테이블 조회 성능 테스트 용도로 대용량 데이터 생성
     */
    @PostMapping("/burk-data/generate-all")
    public ResponseEntity<Map<String, String>> generateAllTestData(@RequestParam(defaultValue = "10000") int userCount, @RequestParam(defaultValue = "50000") int itemCount, @RequestParam(defaultValue = "100000") int appraiseCount, @RequestParam(defaultValue = "50000") int dealCount, @RequestParam(defaultValue = "50000") int auctionCount) {

        long startTime = System.currentTimeMillis();

        try {
            // 1. 사용자 데이터 삽입
            log.info("1단계: 사용자 데이터 생성");
            bulkDataInsertService.insertBulkUsers(userCount);

            // 2. 상품 데이터 삽입
            log.info("2단계: 상품 데이터 생성");
            bulkDataInsertService.insertBulkItems(itemCount);

            // 3. 감정 데이터 삽입
            log.info("3단계: 감정 데이터 생성");
            bulkDataInsertService.insertBulkAppraises(appraiseCount);

            // 4. 거래 데이터 삽입
            log.info("4단계: 거래 데이터 생성");
            bulkDataInsertService.insertBulkDeals(dealCount);

            // 5. 경매 데이터 삽입
            log.info("5단계: 경매 데이터 생성");
            bulkDataInsertService.insertBulkAuctions(auctionCount);

            long endTime = System.currentTimeMillis();
            long duration = (endTime - startTime) / 1000;

            Map<String, String> response = new HashMap<>();
            response.put("message", "테스트 데이터 생성 완료");
            response.put("duration", duration + "초");
            response.put("users", userCount + "명");
            response.put("items", itemCount + "개");
            response.put("appraises", appraiseCount + "개");
            response.put("deals", dealCount + "개");
            response.put("auctions", auctionCount + "개");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("테스트 데이터 생성 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 개별 테이블별 데이터 생성
     */
    @PostMapping("/burk-data/users")
    public ResponseEntity<String> generateUsers(@RequestParam(defaultValue = "20") int count) {

        bulkDataInsertService.insertBulkUsers(count);

        return ResponseEntity.status(HttpStatus.OK).body(count + "명의 사용자 데이터 생성 완료");
    }

    @PostMapping("/burk-data/items")
    public ResponseEntity<String> generateItems(@RequestParam(defaultValue = "30") int count) {

        bulkDataInsertService.insertBulkItems(count);

        return ResponseEntity.status(HttpStatus.OK).body(count + "개의 상품 데이터 생성 완료");
    }

    @PostMapping("/burk-data/appraises")
    public ResponseEntity<String> generateAppraises(@RequestParam(defaultValue = "60") int count) {

        bulkDataInsertService.insertBulkAppraises(count);

        return ResponseEntity.status(HttpStatus.OK).body(count + "개의 감정 데이터 생성 완료");
    }

    @PostMapping("/burk-data/deals")
    public ResponseEntity<String> generateDeals(@RequestParam(defaultValue = "40") int count) {

        bulkDataInsertService.insertBulkDeals(count);

        return ResponseEntity.status(HttpStatus.OK).body(count + "개의 거래 데이터 생성 완료");
    }

    @PostMapping("/burk-data/auctions")
    public ResponseEntity<String> generateAuctions(@RequestParam(defaultValue = "30") int count) {

        bulkDataInsertService.insertBulkAuctions(count);

        return ResponseEntity.status(HttpStatus.OK).body(count + "개의 경매 데이터 생성 완료");
    }
}
