package com.example.quicksells.common.scheduler;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

@Configuration
@EnableScheduling
public class SchedulingConfiguration implements SchedulingConfigurer {

    private final static String THREAD_POOL_NAME = "auction-scheduled-thread-pool";

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

        // 스케쥴러
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();

        // 스케쥴 쓰레드 풀
        scheduler.setPoolSize(10);

        // 로그용 쓰레드 접두사
        scheduler.setThreadNamePrefix(THREAD_POOL_NAME);

        // 서버 종료 시 진행 중인 스케줄 작업이 완료될 때까지 대기
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);

        // 설정값들을 엔진에 주입하여 초기화
        scheduler.initialize();

        // 스프링 스케쥴러에 설정한 엔진 등록
        taskRegistrar.setTaskScheduler(scheduler);

    }
}
