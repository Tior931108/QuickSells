package com.example.quicksells.domain.auction.service;

import com.example.quicksells.common.enums.AuctionStatusType;
import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.auction.entity.Auction;
import com.example.quicksells.domain.auction.repository.AuctionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionCloseService {

    private final AuctionRepository auctionRepository;

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

            // 슬라이스 내용마다 마감시간 체크
            foundAuction.getContent().forEach(Auction::auctionEndTimeCheck);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void auctionIsCloseCheckResult(Long auctionId) {

        // 마감시간이 현재 시간보다 이 후일떄 진행중인 경매 조회
        Auction foundAuction = auctionRepository.findByIdAndStatusAndEndTimeAfter(auctionId, AuctionStatusType.AUCTIONING, LocalDateTime.now(Clock.systemDefaultZone()))
                .orElseThrow(() -> new CustomException(ExceptionCode.NOT_FOUND_AUCTION));

        // 마감 시간 체크
        foundAuction.auctionEndTimeCheck();
    }
}
