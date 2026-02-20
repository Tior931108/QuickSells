package com.example.quicksells.domain.burkdata.service;

import com.example.quicksells.common.enums.AppraiseStatus;
import com.example.quicksells.common.enums.AuctionStatusType;
import com.example.quicksells.common.enums.StatusType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

import static com.example.quicksells.common.enums.AppraiseStatus.*;
import static com.example.quicksells.common.enums.AuctionStatusType.*;
import static com.example.quicksells.common.enums.StatusType.ON_SALE;
import static com.example.quicksells.common.enums.StatusType.SOLD;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkDataInsertService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    private static final int BATCH_SIZE = 1000;
    private static final Random RANDOM = new Random();

    /**
     * 사용자 데이터 삽입 (소셜 로그인 필드 추가)
     * - 모두 일반 회원가입 사용자로 생성 (provider_id = null)
     * - 일반 사용자: role=USER, status=ACTIVE
     * - 관리자: role=ADMIN, status=ACTIVE
     */
    @Transactional
    public void insertBulkUsers(int count) {
        log.info("========================================");
        log.info("{}명의 사용자 데이터 삽입 시작", count);
        log.info("========================================");

        long startTime = System.currentTimeMillis();

        String sql = """
        INSERT INTO users (email, password, name, phone, address, birth, role, 
                           status, provider_id, password_reset_required, is_deleted, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        int totalBatches = (int) Math.ceil((double) count / BATCH_SIZE);

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            final int currentBatch = batchIndex;
            final int startIdx = batchIndex * BATCH_SIZE;
            final int endIdx = Math.min(startIdx + BATCH_SIZE, count);
            final int batchCount = endIdx - startIdx;

            long batchStartTime = System.currentTimeMillis();

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    int actualIndex = startIdx + i;

                    ps.setString(1, "user" + actualIndex + "@quicksell.com"); // email
                    ps.setString(2, passwordEncoder.encode("password123!")); // password
                    ps.setString(3, "사용자" + actualIndex); // name
                    ps.setString(4, "010" + String.format("%08d", actualIndex % 100000000)); // phone
                    ps.setString(5, "서울시 " + getRandomDistrict() + " " + (actualIndex % 500 + 1) + "번지"); // address
                    ps.setString(6, generateRandomBirth()); // birth
                    ps.setString(7, actualIndex % 10 == 0 ? "ADMIN" : "USER"); // role (10%는 ADMIN)

                    // 소셜 로그인 관련 필드
                    ps.setString(8, "ACTIVE"); // status (모두 ACTIVE - 일반 회원가입)
                    ps.setNull(9, Types.VARCHAR); // provider_id = null (일반 회원가입)
                    ps.setBoolean(10, false); // password_reset_required

                    ps.setBoolean(11, false); // is_deleted
                    ps.setTimestamp(12, Timestamp.valueOf(LocalDateTime.now().minusDays(actualIndex % 365))); // created_at
                    ps.setTimestamp(13, Timestamp.valueOf(LocalDateTime.now().minusDays(actualIndex % 365))); // updated_at
                }

                @Override
                public int getBatchSize() {
                    return batchCount;
                }
            });

            long batchEndTime = System.currentTimeMillis();
            long batchDuration = batchEndTime - batchStartTime;

            int progress = (int) (((double) endIdx / count) * 100);
            log.info("[Users] 배치 {}/{} 완료 | 진행률: {}% | {}건 삽입 | 소요시간: {}ms",
                    currentBatch + 1, totalBatches, progress, batchCount, batchDuration);
        }

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;

        log.info("사용자 데이터 {} 건 삽입 완료 | 총 소요시간: {}초",
                count, totalDuration / 1000.0);
        log.info("평균 처리 속도: {}건/초", (count * 1000.0) / totalDuration);
        log.info("일반 사용자: {}명 (USER, ACTIVE), 관리자: {}명 (ADMIN, ACTIVE)", (int)(count * 0.9), (int)(count * 0.1));

    }

    /**
     * 상품 데이터 삽입
     */
    @Transactional
    public void insertBulkItems(int count) {

        log.info("{}개의 상품 데이터 삽입 시작", count);


        long startTime = System.currentTimeMillis();

        String sql = """
            INSERT INTO items (seller_id, name, hope_price, description, image, 
                             is_deleted, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        List<Long> userIds = getUserIds();
        if (userIds.isEmpty()) {
            throw new IllegalStateException("사용자 데이터가 없습니다. 먼저 사용자를 생성해주세요.");
        }

        log.info("사용 가능한 사용자 수: {} 명", userIds.size());

        int totalBatches = (int) Math.ceil((double) count / BATCH_SIZE);

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            final int currentBatch = batchIndex;
            final int startIdx = batchIndex * BATCH_SIZE;
            final int endIdx = Math.min(startIdx + BATCH_SIZE, count);
            final int batchCount = endIdx - startIdx;

            long batchStartTime = System.currentTimeMillis();

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    int actualIndex = startIdx + i;
                    ps.setLong(1, userIds.get(actualIndex % userIds.size()));
                    ps.setString(2, generateItemName(actualIndex));
                    ps.setLong(3, randomLong(100000, 1000000));
                    ps.setString(4, generateItemDescription(actualIndex));
                    ps.setString(5, "item_image_" + actualIndex + ".jpg");
                    ps.setBoolean(6, Math.random() > 0.3); // 70% ON_SALE
                    ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now().minusDays(actualIndex % 90)));
                    ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now().minusDays(actualIndex % 90)));
                }

                @Override
                public int getBatchSize() {
                    return batchCount;
                }
            });

            long batchEndTime = System.currentTimeMillis();
            long batchDuration = batchEndTime - batchStartTime;

            int progress = (int) (((double) endIdx / count) * 100);
            log.info("[Items] 배치 {}/{} 완료 | 진행률: {}% | {}건 삽입 | 소요시간: {}ms",
                    currentBatch + 1, totalBatches, progress, batchCount, batchDuration);
        }

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;


        log.info("상품 데이터 {} 건 삽입 완료 | 총 소요시간: {}초",
                count, totalDuration / 1000.0);
        log.info("평균 처리 속도: {}건/초", (count * 1000.0) / totalDuration);

    }

    /**
     * 감정 데이터 삽입 (수정됨)
     * - PENDING: 30% (판매자가 감정 선택 전)
     * - IMMEDIATE_SELL: 30% (즉시 판매 선택)
     * - AUCTION: 40% (경매 진행 선택)
     */
    @Transactional
    public void insertBulkAppraises(int count) {

        log.info("{}개의 감정 데이터 삽입 시작", count);


        long startTime = System.currentTimeMillis();

        String sql = """
            INSERT INTO appraises (admin_id, item_id, bid_price, appraise_status, 
                                  is_selected, is_deleted, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;

        List<Long> adminIds = getAdminIds();
        List<Long> itemIds = getItemIds();

        if (adminIds.isEmpty()) {
            throw new IllegalStateException("관리자(감정사) 데이터가 없습니다.");
        }
        if (itemIds.isEmpty()) {
            throw new IllegalStateException("상품 데이터가 없습니다.");
        }

        log.info("사용 가능한 관리자 수: {} 명, 상품 수: {} 개", adminIds.size(), itemIds.size());

        int totalBatches = (int) Math.ceil((double) count / BATCH_SIZE);

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            final int currentBatch = batchIndex;
            final int startIdx = batchIndex * BATCH_SIZE;
            final int endIdx = Math.min(startIdx + BATCH_SIZE, count);
            final int batchCount = endIdx - startIdx;

            long batchStartTime = System.currentTimeMillis();

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    int actualIndex = startIdx + i;

                    ps.setLong(1, adminIds.get(actualIndex % adminIds.size())); // admin_id
                    ps.setLong(2, itemIds.get(actualIndex % itemIds.size()));   // item_id
                    ps.setLong(3, randomLong(200000, 1000000)); // appraise_price

                    // 감정 상태 결정
                    int rand = actualIndex % 10;
                    AppraiseStatus appraiseStatus;
                    boolean isSelected;

                    if (rand < 3) {
                        // 30% PENDING - 판매자가 아직 선택 안 함
                        appraiseStatus = PENDING;
                        isSelected = false;
                    } else if (rand < 6) {
                        // 30% IMMEDIATE_SELL - 즉시 판매 선택
                        appraiseStatus = IMMEDIATE_SELL;
                        isSelected = true;
                    } else {
                        // 40% AUCTION - 경매 진행 선택
                        appraiseStatus = AUCTION;
                        isSelected = true;
                    }

                    ps.setString(4, appraiseStatus.toString()); // appraise_status
                    ps.setBoolean(5, isSelected); // is_seleted
                    ps.setBoolean(6, false); // is_deleted
                    ps.setTimestamp(7, Timestamp.valueOf(LocalDateTime.now().minusDays(actualIndex % 45)));
                }

                @Override
                public int getBatchSize() {
                    return batchCount;
                }
            });

            long batchEndTime = System.currentTimeMillis();
            long batchDuration = batchEndTime - batchStartTime;

            int progress = (int) (((double) endIdx / count) * 100);
            log.info("[Appraises] 배치 {}/{} 완료 | 진행률: {}% | {}건 삽입 | 소요시간: {}ms",
                    currentBatch + 1, totalBatches, progress, batchCount, batchDuration);
        }

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;


        log.info("감정 데이터 {} 건 삽입 완료 | 총 소요시간: {}초",
                count, totalDuration / 1000.0);
        log.info("평균 처리 속도: {}건/초", (count * 1000.0) / totalDuration);
        log.info("예상 분포: PENDING 30%, IMMEDIATE_SELL 30%, AUCTION 40%");

    }

    /**
     * 경매 데이터 삽입 (수정됨)
     * - appraise_status가 AUCTION인 것만 경매 생성
     * - 60%: SUCCESSFUL_BID (낙찰 성공)
     * - 20%: AUCTIONING (진행 중)
     * - 10%: UNSUCCESSFUL_BID (유찰)
     * - 10%: CANCELED (취소)
     */
    @Transactional
    public void insertBulkAuctions(int count) {
        log.info("========================================");
        log.info("{}개의 경매 데이터 삽입 시작", count);
        log.info("========================================");

        long startTime = System.currentTimeMillis();

        String sql = """
        INSERT INTO auctions (appraise_id, buyer_id, bid_price, status, 
                            end_time, is_deleted, settlement_status, created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        // appraise_status가 AUCTION인 것만 조회
        List<Long> auctionAppraiseIds = getAuctionAppraiseIds();
        List<Long> userIds = getUserIds();

        if (auctionAppraiseIds.isEmpty()) {
            throw new IllegalStateException("AUCTION 상태의 감정 데이터가 없습니다.");
        }
        if (userIds.isEmpty()) {
            throw new IllegalStateException("사용자 데이터가 없습니다.");
        }

        log.info("사용 가능한 AUCTION 감정 수: {} 개, 사용자 수: {} 명",
                auctionAppraiseIds.size(), userIds.size());

        // count가 AUCTION 감정 수보다 많으면 조정
        int actualCount = Math.min(count, auctionAppraiseIds.size());
        if (actualCount < count) {
            log.warn("요청한 경매 개수({})가 AUCTION 감정 수({})보다 많아 {}개만 생성합니다.",
                    count, auctionAppraiseIds.size(), actualCount);
        }

        int totalBatches = (int) Math.ceil((double) actualCount / BATCH_SIZE);

        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
            final int currentBatch = batchIndex;
            final int startIdx = batchIndex * BATCH_SIZE;
            final int endIdx = Math.min(startIdx + BATCH_SIZE, actualCount);
            final int batchCount = endIdx - startIdx;

            long batchStartTime = System.currentTimeMillis();

            jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    int actualIndex = startIdx + i;

                    ps.setLong(1, auctionAppraiseIds.get(actualIndex)); // appraise_id (AUCTION만)

                    // 경매 상태 결정
                    int rand = actualIndex % 10;
                    AuctionStatusType status;
                    boolean hasBuyer = false;

                    if (rand < 6) {
                        // 60% 낙찰 성공
                        status = SUCCESSFUL_BID;
                        hasBuyer = true;
                    } else if (rand < 8) {
                        // 20% 진행 중
                        status = AUCTIONING;
                    } else if (rand < 9) {
                        // 10% 유찰
                        status = UNSUCCESSFUL_BID;
                    } else {
                        // 10% 취소
                        status = CANCELED;
                    }

                    // buyer_id 설정
                    if (hasBuyer) {
                        ps.setLong(2, userIds.get(actualIndex % userIds.size())); // buyer_id
                    } else {
                        ps.setNull(2, Types.BIGINT); // buyer_id = NULL
                    }

                    ps.setLong(3, randomLong(100000, 600000)); // bid_price
                    ps.setString(4, status.toString()); // status

                    // end_time 설정
                    if (status == AUCTIONING) {
                        ps.setTimestamp(5, Timestamp.valueOf(
                                LocalDateTime.now().plusDays((actualIndex % 7) + 1)
                        ));
                    } else {
                        ps.setTimestamp(5, Timestamp.valueOf(
                                LocalDateTime.now().minusDays((actualIndex % 30) + 1)
                        ));
                    }

                    ps.setBoolean(6, false); // is_deleted

                    // settlement_status: 모두 PENDING
                    ps.setString(7, "PENDING"); // settlement_status

                    ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now().minusDays((actualIndex % 30) + 2)));
                    ps.setTimestamp(9, Timestamp.valueOf(LocalDateTime.now().minusDays((actualIndex % 30) + 1)));
                }

                @Override
                public int getBatchSize() {
                    return batchCount;
                }
            });

            long batchEndTime = System.currentTimeMillis();
            long batchDuration = batchEndTime - batchStartTime;

            int progress = (int) (((double) endIdx / actualCount) * 100);
            log.info("[Auctions] 배치 {}/{} 완료 | 진행률: {}% | {}건 삽입 | 소요시간: {}ms",
                    currentBatch + 1, totalBatches, progress, batchCount, batchDuration);
        }

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;


        log.info("경매 데이터 {} 건 삽입 완료 | 총 소요시간: {}초",
                actualCount, totalDuration / 1000.0);
        log.info("평균 처리 속도: {}건/초", (actualCount * 1000.0) / totalDuration);
        log.info("예상 분포: 낙찰 성공 60%, 진행 중 20%, 유찰 10%, 취소 10%");

    }

    /**
     * 거래 데이터 삽입 (auction_id 중복 제거)
     * - 경매 거래: 각 낙찰된 경매마다 1개의 거래만 생성
     * - 즉시 판매: IMMEDIATE_SELL 감정마다 거래 생성
     */
    @Transactional
    public void insertBulkDeals(int count) {

        log.info("{}개의 거래 데이터 삽입 시작", count);


        long startTime = System.currentTimeMillis();

        String sql = """
        INSERT INTO deals (auction_id, appraise_id, status, deal_price, created_at)
        VALUES (?, ?, ?, ?, ?)
        """;

        // 낙찰된 경매 (AUCTION 상태의 감정 + SUCCESSFUL_BID)
        List<Long> successfulAuctionIds = getSuccessfulAuctionIds();

        // IMMEDIATE_SELL 상태의 감정
        List<Long> immediateSellAppraiseIds = getImmediateSellAppraiseIds();

        if (successfulAuctionIds.isEmpty() && immediateSellAppraiseIds.isEmpty()) {
            throw new IllegalStateException("거래 가능한 데이터가 없습니다. (낙찰된 경매 또는 즉시 판매 감정)");
        }

        log.info("낙찰된 경매 수: {} 개, 즉시 판매 감정 수: {} 개",
                successfulAuctionIds.size(), immediateSellAppraiseIds.size());

        // 실제 생성 가능한 거래 개수 계산
        int maxAuctionDeals = successfulAuctionIds.size(); // 경매는 1:1 매핑
        int maxImmediateDeals = immediateSellAppraiseIds.size(); // 즉시 판매도 1:1 매핑
        int maxTotalDeals = maxAuctionDeals + maxImmediateDeals;

        int actualCount = Math.min(count, maxTotalDeals);

        if (actualCount < count) {
            log.warn("요청한 거래 개수({})가 생성 가능한 최대 거래 수({})보다 많아 {}개만 생성합니다.",
                    count, maxTotalDeals, actualCount);
        }

        // 70% 경매, 30% 즉시 판매 비율 계산
        int auctionDealCount = (int) (actualCount * 0.7);
        int immediateDealCount = actualCount - auctionDealCount;

        // 실제 가능한 개수로 조정
        if (auctionDealCount > maxAuctionDeals) {
            auctionDealCount = maxAuctionDeals;
            immediateDealCount = actualCount - auctionDealCount;
        }
        if (immediateDealCount > maxImmediateDeals) {
            immediateDealCount = maxImmediateDeals;
            auctionDealCount = actualCount - immediateDealCount;
        }

        log.info("생성할 거래: 경매 거래 {}개, 즉시 판매 {}개", auctionDealCount, immediateDealCount);

        int totalBatches = (int) Math.ceil((double) actualCount / BATCH_SIZE);
        int insertedCount = 0;

        // 1. 경매 거래 삽입
        if (auctionDealCount > 0) {
            log.info("경매 거래 삽입 시작 ({}개)", auctionDealCount);

            int auctionBatches = (int) Math.ceil((double) auctionDealCount / BATCH_SIZE);

            for (int batchIndex = 0; batchIndex < auctionBatches; batchIndex++) {
                final int startIdx = batchIndex * BATCH_SIZE;
                final int endIdx = Math.min(startIdx + BATCH_SIZE, auctionDealCount);
                final int batchCount = endIdx - startIdx;

                long batchStartTime = System.currentTimeMillis();

                jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        int actualIndex = startIdx + i;

                        // auction_id는 중복 없이 1:1 매핑
                        Long auctionId = successfulAuctionIds.get(actualIndex);

                        // auction_id로 appraise_id 조회
                        Long appraiseId = jdbcTemplate.queryForObject(
                                "SELECT appraise_id FROM auctions WHERE id = ?",
                                Long.class,
                                auctionId
                        );

                        ps.setLong(1, auctionId); // auction_id (unique, 중복 없음)
                        ps.setLong(2, appraiseId); // appraise_id
                        ps.setString(3, SOLD.toString()); // status
                        ps.setLong(4, randomLong(100000, 1000000)); // deal_price
                        ps.setTimestamp(5, Timestamp.valueOf(
                                LocalDateTime.now().minusDays(actualIndex % 30)
                        ));
                    }

                    @Override
                    public int getBatchSize() {
                        return batchCount;
                    }
                });

                long batchEndTime = System.currentTimeMillis();
                long batchDuration = batchEndTime - batchStartTime;

                insertedCount += batchCount;
                int progress = (int) (((double) insertedCount / actualCount) * 100);
                log.info("[Deals-Auction] 배치 {}/{} 완료 | 진행률: {}% | {}건 삽입 | 소요시간: {}ms",
                        batchIndex + 1, auctionBatches, progress, batchCount, batchDuration);
            }
        }

        // 2. 즉시 판매 거래 삽입
        if (immediateDealCount > 0) {
            log.info("즉시 판매 거래 삽입 시작 ({}개)", immediateDealCount);

            int immediateBatches = (int) Math.ceil((double) immediateDealCount / BATCH_SIZE);

            for (int batchIndex = 0; batchIndex < immediateBatches; batchIndex++) {
                final int startIdx = batchIndex * BATCH_SIZE;
                final int endIdx = Math.min(startIdx + BATCH_SIZE, immediateDealCount);
                final int batchCount = endIdx - startIdx;

                long batchStartTime = System.currentTimeMillis();

                jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        int actualIndex = startIdx + i;

                        // 즉시 판매 - appraise_id만 설정
                        ps.setNull(1, Types.BIGINT); // auction_id = NULL
                        ps.setLong(2, immediateSellAppraiseIds.get(actualIndex)); // appraise_id (unique, 중복 없음)
                        ps.setString(3, SOLD.toString()); // status
                        ps.setLong(4, randomLong(100000, 1000000)); // deal_price
                        ps.setTimestamp(5, Timestamp.valueOf(
                                LocalDateTime.now().minusDays(actualIndex % 30)
                        ));
                    }

                    @Override
                    public int getBatchSize() {
                        return batchCount;
                    }
                });

                long batchEndTime = System.currentTimeMillis();
                long batchDuration = batchEndTime - batchStartTime;

                insertedCount += batchCount;
                int progress = (int) (((double) insertedCount / actualCount) * 100);
                log.info("[Deals-Immediate] 배치 {}/{} 완료 | 진행률: {}% | {}건 삽입 | 소요시간: {}ms",
                        batchIndex + 1, immediateBatches, progress, batchCount, batchDuration);
            }
        }

        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;


        log.info("거래 데이터 {} 건 삽입 완료 | 총 소요시간: {}초",
                actualCount, totalDuration / 1000.0);
        log.info("평균 처리 속도: {}건/초", (actualCount * 1000.0) / totalDuration);
        log.info("경매 거래: {}건, 즉시 판매: {}건", auctionDealCount, immediateDealCount);

    }

    // ==================== 헬퍼 메소드 ====================

    private List<Long> getUserIds() {
        return jdbcTemplate.queryForList(
                "SELECT id FROM users WHERE is_deleted = false ORDER BY id LIMIT 50000",
                Long.class
        );
    }

    private List<Long> getAdminIds() {
        return jdbcTemplate.queryForList(
                "SELECT id FROM users WHERE role = 'ADMIN' AND is_deleted = false ORDER BY id LIMIT 10000",
                Long.class
        );
    }

    private List<Long> getItemIds() {
        return jdbcTemplate.queryForList(
                "SELECT id FROM items WHERE is_deleted = false ORDER BY id LIMIT 50000",
                Long.class
        );
    }

    /**
     * AUCTION 상태의 감정 ID 조회
     */
    private List<Long> getAuctionAppraiseIds() {
        return jdbcTemplate.queryForList(
                "SELECT id FROM appraises WHERE appraise_status = 'AUCTION' AND is_deleted = false ORDER BY id LIMIT 50000",
                Long.class
        );
    }

    /**
     * IMMEDIATE_SELL 상태의 감정 ID 조회
     */
    private List<Long> getImmediateSellAppraiseIds() {
        return jdbcTemplate.queryForList(
                "SELECT id FROM appraises WHERE appraise_status = 'IMMEDIATE_SELL' AND is_deleted = false ORDER BY id LIMIT 50000",
                Long.class
        );
    }

    /**
     * 낙찰 성공한 경매 ID 조회 (AUCTION 상태의 감정 + SUCCESSFUL_BID)
     */
    private List<Long> getSuccessfulAuctionIds() {
        return jdbcTemplate.queryForList(
                """
                SELECT a.id 
                FROM auctions a
                JOIN appraises ap ON a.appraise_id = ap.id
                WHERE a.status = 'SUCCESSFUL_BID' 
                  AND ap.appraise_status = 'AUCTION'
                  AND a.buyer_id IS NOT NULL 
                  AND a.is_deleted = false
                ORDER BY a.id 
                LIMIT 50000
                """,
                Long.class
        );
    }

    private <T> T getRandomElement(T[] array) {
        return array[RANDOM.nextInt(array.length)];
    }

    private String generateRandomBirth() {
        int year = 1970 + RANDOM.nextInt(40);
        int month = 1 + RANDOM.nextInt(12);
        int day = 1 + RANDOM.nextInt(28);
        return String.format("%d-%02d-%02d", year, month, day);
    }

    private String getRandomDistrict() {
        String[] districts = {
                "강남구", "서초구", "송파구", "강동구", "마포구",
                "용산구", "성동구", "광진구", "종로구", "중구"
        };
        return getRandomElement(districts);
    }

    private String generateItemName(int i) {
        String[] categories = {
                "맥북 프로", "아이폰", "갤럭시", "에어팟", "아이패드",
                "다이슨 청소기", "LG 그램", "삼성 갤럭시 워치", "애플 워치", "소니 카메라",
                "명품 가방", "롤렉스 시계", "나이키 신발", "아디다스 운동화", "구찌 벨트"
        };
        String category = categories[i % categories.length];
        int modelNumber = (i / categories.length) + 1;
        return category + " " + modelNumber + "세대";
    }

    private String generateItemDescription(int i) {
        String[] conditions = {
                "새상품 미개봉", "거의 새것", "사용감 적음",
                "중고 양호", "일부 사용감 있음"
        };
        String condition = conditions[i % conditions.length];
        return condition + " 상태입니다. 직거래 환영합니다. 상세 사진 추가 가능합니다.";
    }

    private long randomLong(long min, long max) {
        return min + (long) (RANDOM.nextDouble() * (max - min));
    }
}