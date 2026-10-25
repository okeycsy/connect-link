package com.connectlink.attachment.sample;

import com.connectlink.attachment.Attachment;
import com.connectlink.attachment.AttachmentRepository;
import com.connectlink.attachment.FileStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 샘플 시드의 첨부 내용을 저장소에 넣는다. 시드 SQL 은 첨부 행만 넣고 저장소 키 자리에 {@code seed:파일이름} 을 적는다. 서버가 뜰 때 {@code
 * db/seed/files/파일이름} 을 {@link FileStorage} 에 저장하고 행의 키와 크기를 실제 값으로 바꾼다. 메모리 DB 라 시드는 뜰 때마다 새로
 * 들어간다.
 */
@Component
@Profile({"local", "demo", "test"})
public class SampleAttachmentLoader implements ApplicationRunner {

  static final String KEY_PREFIX = "seed:";
  private static final String FILES = "db/seed/files/";

  private final AttachmentRepository repository;
  private final FileStorage fileStorage;

  public SampleAttachmentLoader(AttachmentRepository repository, FileStorage fileStorage) {
    this.repository = repository;
    this.fileStorage = fileStorage;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    for (Attachment attachment : repository.findByStorageKeyStartingWith(KEY_PREFIX)) {
      String fileName = attachment.getStorageKey().substring(KEY_PREFIX.length());
      ClassPathResource file = new ClassPathResource(FILES + fileName);
      try (InputStream in = file.getInputStream()) {
        String key = fileStorage.store(in, fileName);
        attachment.replaceContent(key, file.contentLength());
      } catch (IOException e) {
        throw new UncheckedIOException("샘플 첨부를 저장하지 못했습니다: " + fileName, e);
      }
    }
  }
}
