package com.example.quicksells.domain.payment.entity;

import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.exception.CustomException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "point_wallets")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointWallet {

    /**
     * 사용자 식별자 = 포인트 지갑 식별자
     *
     * - 사용자 1명당 포인트 지갑은 정확히 1개만 존재해야 하므로,
     *   포인트 지갑의 기본키를 사용자 식별자와 동일하게 설정하여 1:1을 강제한다.
     * - 별도 기본키를 만들면 지갑이 사용자 없이 존재하는 형태가 되어 도메인 의미가 흐려진다.
     */
    @Id
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 현재 사용 가능한 포인트 잔액
     *
     * - 충전 시 증가
     * - 낙찰 성공 시 감소
     * - 입찰 시점에는 차감하지 않고 "예약(홀드)" 개념을 두지 않는다 (v3 정책)
     */
    @Column(name = "available_balance", nullable = false)
    private Long availableBalance;

    /**
     * 낙관적 락(Optimistic Lock)을 위한 버전 값
     *
     * - 동시에 여러 요청이 포인트를 변경하는 상황(여러 경매 입찰/낙찰 처리 등)에서
     *   데이터 경합이 발생하면 마지막 업데이트가 이전 버전 조건을 만족하지 못해 예외가 발생한다.
     * - 이를 통해 포인트 음수, 이중 차감 같은 치명적 오류를 방지한다.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public PointWallet(Long userId) {
        this.userId = userId;
        this.availableBalance = 0L;
    }

    /* ===== 비즈니스 메서드 ===== */

    /**
     * 포인트 충전 (잔액 증가)
     */
    public void increaseBalance(long amount) {
        if (amount <= 0) {
            throw new CustomException(ExceptionCode.INVALID_CHARGE_AMOUNT);
        }
        this.availableBalance += amount;
    }

    /**
     * 낙찰 성공 등으로 포인트 차감 (잔액 감소)
     */
    public void decreaseBalance(long amount) {
        if (amount <= 0) {
            throw new CustomException(ExceptionCode.INVALID_PAYMENT_AMOUNT);
        }
        if (this.availableBalance < amount) {
            // 정책: 잔액 부족은 비즈니스 예외로 통일
            throw new CustomException(ExceptionCode.INSUFFICIENT_BALANCE);
        }
        this.availableBalance -= amount;
    }
}
