package com.example.quicksells.domain.payment.service;

import com.example.quicksells.domain.payment.entity.PointWallet;
import com.example.quicksells.domain.payment.repository.PointWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지갑 조회/생성 공통 서비스
 *
 *  정책
 * - 지갑이 없으면 예외로 끊지 않고 생성한다.
 * - 생성 사실은 로그로 남겨 운영/추적 가능하게.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointWalletService {

    private final PointWalletRepository pointWalletRepository;

    @Transactional
    public PointWallet getOrCreate(Long userId) {
        return pointWalletRepository.findById(userId)
                .orElseGet(() -> {
                    log.info("[Wallet] 포인트 지갑이 없어 생성하겠습니다. userId={}", userId);
                    return pointWalletRepository.save(new PointWallet(userId));
                });
    }
}
