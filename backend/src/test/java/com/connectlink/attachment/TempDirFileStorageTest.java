package com.connectlink.attachment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

class TempDirFileStorageTest {

  @TempDir Path dir;

  @Test
  void 저장한_내용을_키로_다시_열고_지운다() throws Exception {
    TempDirFileStorage storage = new TempDirFileStorage(properties(dir));
    String key =
        storage.store(new ByteArrayInputStream("안녕".getBytes(StandardCharsets.UTF_8)), "공문.PDF");

    assertThat(key).matches("\\d{6}/[0-9a-f-]{36}\\.pdf");
    Resource opened = storage.open(key);
    assertThat(opened.exists()).isTrue();
    assertThat(opened.getInputStream().readAllBytes())
        .isEqualTo("안녕".getBytes(StandardCharsets.UTF_8));

    storage.delete(key);
    assertThat(storage.open(key).exists()).isFalse();
    storage.delete(key); // 이미 없어도 조용히 지나간다.
  }

  @Test
  void 확장자가_이상하면_붙이지_않고_저장소_밖_키는_열지_않는다() throws Exception {
    TempDirFileStorage storage = new TempDirFileStorage(properties(dir));
    String key = storage.store(new ByteArrayInputStream(new byte[] {1}), "이름만.있는 파일");
    assertThat(key).doesNotContain(".");

    assertThatThrownBy(() -> storage.open("../../etc/passwd"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static AttachmentProperties properties(Path dir) {
    AttachmentProperties properties = new AttachmentProperties();
    properties.setDir(dir.toString());
    return properties;
  }
}
