package com.connectlink.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 정기 작업(@Scheduled)을 켠다. 예: 일시중단 완료 처리, 전 영업일 취합 전달. demo 프로필에는 시연용 즉시 실행 API 를 함께 둔다. 테스트는 작업이
 * 데이터를 바꾸지 않도록 {@code app.scheduling.enabled=false} 로 끈다.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {}
