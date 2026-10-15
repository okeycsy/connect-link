package com.connectlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 연결고리 서버. 화면(frontend 빌드 결과)과 API를 한 프로세스에서 제공한다. */
@SpringBootApplication
public class ConnectLinkApplication {

  public static void main(String[] args) {
    SpringApplication.run(ConnectLinkApplication.class, args);
  }
}
