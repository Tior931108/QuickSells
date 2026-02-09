package com.example.quicksells.domain.payment.service;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.enums.PointTransactionType;
import com.example.quicksells.common.exception.CustomException;
import com.example.quicksells.domain.payment.entity.PointWallet;
import com.example.quicksells.domain.payment.model.TransactionReference;
import com.example.quicksells.domain.payment.model.response.AdminPointGrantResponse;
import com.example.quicksells.domain.payment.model.response.PointTransactionGetResponse;
import com.example.quicksells.domain.payment.model.response.WalletGetResponse;
import com.example.quicksells.domain.payment.repository.PointTransactionRepository;
import com.example.quicksells.domain.payment.repository.PointWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지갑 조회/생성 공통 서비스
 *
 * 정책
 * - 지갑이 없으면 예외로 끊지 않고 생성한다.
 * - 생성 사실은 로그로 남겨 운영/추적 가능하게.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointWalletService {

    private final PointWalletRepository pointWalletRepository;
    private final PointLedgerService pointLedgerService;
    private final PointTransactionRepository pointTransactionRepository;

    /**
     * 지갑이 없으면 생성한다.
     */
    @Transactional
    public PointWallet getOrCreate(Long userId) {
        return pointWalletRepository.findById(userId)
                .orElseGet(() -> {
                    log.info("[Wallet] 포인트 지갑이 없어 생성하겠습니다. userId={}", userId);
                    return pointWalletRepository.save(new PointWallet(userId));
                });
    }

    /**
     * 내 지갑 조회 응답용
     */
    @Transactional
    public WalletGetResponse getMyWalletResponse(Long userId) {

        PointWallet wallet = getOrCreate(userId);

        return WalletGetResponse.from(wallet);
    }

    /**
     * 내 거래내역 조회 응답용
     */
    @Transactional(readOnly = true)
    public Page<PointTransactionGetResponse> getMyTransactionsResponse(Long userId, int page, int size) {

        return pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(PointTransactionGetResponse::from);
    }

    /**
     * 관리자 포인트 지급 응답용
     */
    @Transactional
    public AdminPointGrantResponse grantPointResponse(Long targetUserId, Long amount) {

        PointWallet wallet = grantPoint(targetUserId, amount);

        return AdminPointGrantResponse.from(targetUserId, amount, wallet);
    }

    /**
     * 관리자: 특정 유저 지갑 조회 응답용
     */
    @Transactional
    public WalletGetResponse getUserWalletResponse(Long userId) {

        PointWallet wallet = getOrCreate(userId);

        return WalletGetResponse.from(wallet);
    }

    /**
     * 관리자: 특정 유저 거래내역 조회 응답용
     */
    @Transactional(readOnly = true)
    public Page<PointTransactionGetResponse> getUserTransactionsResponse(Long userId, int page, int size) {

        return pointTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                .map(PointTransactionGetResponse::from);
    }


    /**
     * 관리자 포인트 지급(내부 로직)
     */
    @Transactional
    public PointWallet grantPoint(Long targetUserId, Long amount) {

        if (amount == null || amount <= 0) {
            throw new CustomException(ExceptionCode.INVALID_CHARGE_AMOUNT);
        }

        return pointLedgerService.credit(
                targetUserId,
                amount,
                PointTransactionType.ADMIN_GRANT,
                TransactionReference.none()
        );
    }
}
