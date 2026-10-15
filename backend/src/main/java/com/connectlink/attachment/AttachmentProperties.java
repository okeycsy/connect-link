package com.connectlink.attachment;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

/**
 * 첨부파일 설정 ({@code app.attachment.*}).
 *
 * <ul>
 *   <li>{@code dir}: 임시 디렉터리 저장소가 파일을 두는 곳. 기본은 OS 임시 디렉터리 아래 {@code connect-link-files}.
 *   <li>{@code max-size}: 파일 하나의 최대 크기. {@code spring.servlet.multipart.max-file-size} 와 맞춘다.
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "app.attachment")
public class AttachmentProperties {

  private String dir = System.getProperty("java.io.tmpdir") + "/connect-link-files";
  private DataSize maxSize = DataSize.ofMegabytes(20);

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
}
