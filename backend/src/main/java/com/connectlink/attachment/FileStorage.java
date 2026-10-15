package com.connectlink.attachment;

import java.io.IOException;
import java.io.InputStream;
import org.springframework.core.io.Resource;

/**
 * 파일 내용 저장소. 업무 코드는 이 인터페이스로만 파일을 다룬다. 프로토타입 구현은 {@link TempDirFileStorage}(임시 디렉터리)이고, 운영에서는 공유
 * 스토리지 구현으로 바꾼다.
 */
public interface FileStorage {

  /** 내용을 저장하고 나중에 찾을 키를 돌려준다. 키는 원래 파일 이름을 담지 않는다. */
  String store(InputStream content, String originalName) throws IOException;

  /** 키에 해당하는 내용. 없으면 {@code exists()} 가 false 인 Resource. */
  Resource open(String key);

  /** 키에 해당하는 내용을 지운다. 이미 없으면 아무 일도 하지 않는다. */
  void delete(String key) throws IOException;
}
