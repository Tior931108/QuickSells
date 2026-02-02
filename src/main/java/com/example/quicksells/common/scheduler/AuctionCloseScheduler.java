package com.example.quicksells.common.scheduler;

import com.example.quicksells.common.enums.AuctionStatusType;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.auction.repository.AuctionRepository;
import com.example.quicksells.domain.auction.service.AuctionSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionCloseScheduler {

    private final AuctionRepository auctionRepository;
    private final AuctionSettlementService auctionSettlementService;

    // 스케쥴러를 활용한 분기마다 종료시간 체크
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void auctionIsCloseSchedule() {

        // 마감 시간 기준 오름차순
        Pageable pageable = PageRequest.of(0, 100, Sort.by("endTime").ascending());

        while (true) {

            // 마감시간이 현재 시간보다 이 전일떄 진행중인 경매 조회
            Slice<Auction> foundAuction = auctionRepository.findAllByStatusAndEndTimeBefore(pageable, AuctionStatusType.AUCTIONING, LocalDateTime.now(Clock.systemDefaultZone()));

            log.info("마감 처리할 경매 - {}", foundAuction);

            // 슬라이스가 존재하지 않으면 종료
            if (!foundAuction.hasContent()) {break;}

            // 슬라이스 내용마다 마감시간 체크 + 낙찰이면 정산
            for (Auction auction : foundAuction.getContent()) {

                // 1) 경매 종료 상태 반영 (AUCTIONING -> SUCCESSFUL_BID / UNSUCCESSFUL_BID)
                auction.auctionEndTimeCheck();

                // 2) 낙찰이면 정산(포인트 이동 + Deal SOLD 처리)
                if (auction.getStatus() == AuctionStatusType.SUCCESSFUL_BID) {
                    auctionSettlementService.settleSuccessfulAuction(auction);
                }
            }
        }
    }
}
