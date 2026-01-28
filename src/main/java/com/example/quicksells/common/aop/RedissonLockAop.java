package com.example.quicksells.common.aop;

import com.example.quicksells.common.annotation.RedissonLock;
import com.example.quicksells.common.enums.ExceptionCode;
import com.example.quicksells.common.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class RedissonLockAop {

    private final RedissonClient redisson;

    @Around("@annotation(redissonLock)")
    public Object redissonLock(ProceedingJoinPoint joinPoint, RedissonLock redissonLock) throws Throwable {

        Object[] args = joinPoint.getArgs();

        // 0번째의 있는 고유 식별자 id
        Object id = args[0];

        // 락 키
        String locked = redissonLock.key() + id.toString();

        // 페어록 -> 대기순으로 락 획득
        RLock lock = redisson.getFairLock(locked);

        // 대기 시간
        long waitTime = redissonLock.waitTime();

        try {
            // 락 획득
            boolean res = lock.tryLock(waitTime, TimeUnit.SECONDS);

            // 락 획득 실패
            if (!res) {
                throw new CustomException(ExceptionCode.LOCK_ACQUISITION_FAILED);
            }

            return joinPoint.proceed(); // 비즈니스 로직 실행

        } catch (InterruptedException interruptedException) {
            // 인터럽트 상태 복구
            Thread.currentThread().interrupt();
            // 복구하는 과정에서 로직 수행을 막음
            throw new CustomException(ExceptionCode.LOCK_INTERRUPTED_ERROR);

        } finally {
            // 획득한 락의 주인 여부
            if (lock.isHeldByCurrentThread()) {
                // ttl 만료로 인한 동시성 이슈로 락이 헤재된 상태일때 에러발생 x
                lock.unlock();
            }
        }
    }
}
