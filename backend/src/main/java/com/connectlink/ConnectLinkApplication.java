package com.connectlink;

import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 연결이 서버. 화면(frontend 빌드 결과)과 API를 한 프로세스에서 제공한다. */
@SpringBootApplication
public class ConnectLinkApplication {

  static {
    // 서버 시간대는 Asia/Seoul 이다(CLAUDE.md §6). 실행 호스트가 UTC 여도 LocalDateTime.now(), DB 의
    // CURRENT_TIMESTAMP,
    // Hibernate 의 jdbc.time_zone 이 같은 시간대를 쓰도록 JVM 기본값을 여기서 고정한다. 테스트도 이 클래스를 읽으므로 같이 적용된다.
    TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
  }

  public static void main(String[] args) {
    SpringApplication.run(ConnectLinkApplication.class, args);
  }
}
