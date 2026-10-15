package com.connectlink.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * 프로토타입 저장소. 설정된 디렉터리(기본은 OS 임시 디렉터리) 아래 {@code yyyyMM/uuid.ext} 로 둔다. 키에는 원래 이름이 들어가지 않으므로 경로 조작이
 * 불가능하고, 디렉터리 밖을 가리키는 키는 열지 않는다.
 */
@Component
public class TempDirFileStorage implements FileStorage {

  private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");

  private final Path root;

  public TempDirFileStorage(AttachmentProperties properties) throws IOException {
    this.root = Path.of(properties.getDir()).toAbsolutePath().normalize();
    Files.createDirectories(root);
  }

  @Override
  public String store(InputStream content, String originalName) throws IOException {
    String key =
        LocalDate.now().format(MONTH) + "/" + UUID.randomUUID() + extensionOf(originalName);
    Path target = resolve(key);
    Files.createDirectories(target.getParent());
    Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
    return key;
  }

  @Override
  public Resource open(String key) {
    return new PathResource(resolve(key));
  }

  @Override
  public void delete(String key) throws IOException {
    Files.deleteIfExists(resolve(key));
  }

  private Path resolve(String key) {
    Path path = root.resolve(key).normalize();
    if (!path.startsWith(root)) {
      throw new IllegalArgumentException("저장소 밖을 가리키는 키입니다.");
    }
    return path;
  }

  /** 확장자만 남겨 어떤 종류의 파일인지 디렉터리에서 알아볼 수 있게 한다. 이름의 나머지는 버린다. */
  private static String extensionOf(String name) {
    if (name == null) {
      return "";
    }
    int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length() - 1 || name.length() - dot > 11) {
      return "";
    }
    String ext = name.substring(dot + 1);
    return ext.chars().allMatch(c -> Character.isLetterOrDigit(c)) ? "." + ext.toLowerCase() : "";
  }
}
