package com.connectlink.attachment;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * 첨부파일 설정 ({@code app.attachment.*}).
 *
 * <ul>
 *   <li>{@code dir}: 임시 디렉터리 저장소가 파일을 두는 곳. 기본은 OS 임시 디렉터리 아래 {@code connect-link-files}.
 *   <li>{@code max-size}: 파일 하나의 최대 크기. {@code spring.servlet.multipart.max-file-size} 와 맞춘다.
 *   <li>{@code inspection.simulated-delay}: 프로토타입 반입 보안 검사({@link SimulatedInboundFileInspector})가
 *       통과로 바꾸기까지 걸리는 시간. 0 이면 올리자마자 통과다.
 *   <li>{@code inspection.poll-interval}: 검사 중인 파일의 결과를 확인하는 간격(밀리초).
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.attachment")
public class AttachmentProperties {

  private String dir = System.getProperty("java.io.tmpdir") + "/connect-link-files";
  private DataSize maxSize = DataSize.ofMegabytes(20);
  private final Inspection inspection = new Inspection();

  public String getDir() {
    return dir;
  }

  public void setDir(String dir) {
    this.dir = dir;
  }

  public DataSize getMaxSize() {
    return maxSize;
  }

  public void setMaxSize(DataSize maxSize) {
    this.maxSize = maxSize;
  }

  public Inspection getInspection() {
    return inspection;
  }

  /** 반입 보안 검사 설정. */
  public static class Inspection {

    private Duration simulatedDelay = Duration.ofSeconds(3);
    private long pollInterval = 1000;

    public Duration getSimulatedDelay() {
      return simulatedDelay;
    }

    public void setSimulatedDelay(Duration simulatedDelay) {
      this.simulatedDelay = simulatedDelay;
    }

    public long getPollInterval() {
      return pollInterval;
    }

    public void setPollInterval(long pollInterval) {
      this.pollInterval = pollInterval;
    }
  }
}
