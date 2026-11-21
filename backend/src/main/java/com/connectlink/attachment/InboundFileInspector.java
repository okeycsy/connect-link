package com.connectlink.attachment;

/**
 * 외부망에서 올라온 파일을 내부망에 들이기 전에 하는 보안 검사. 운영에서는 사내 망연계(자료전송)·무해화 솔루션 연동으로 구현하고, 프로토타입 구현은 {@link
 * SimulatedInboundFileInspector} 다.
 *
 * <p>검사는 바로 끝나지 않을 수 있다. 올릴 때 한 번 부르고, 결과가 {@link Result#pending()} 이면 {@link
 * AttachmentInspectionJob} 이 끝날 때까지 다시 부른다. 구현은 같은 파일로 여러 번 불려도 되게 만든다(처음에는 넘기고, 그 뒤에는 결과를 묻는다).
 */
public interface InboundFileInspector {

  Result inspect(Attachment attachment);

  /**
   * 검사 결과.
   *
   * @param status {@code PENDING}(아직 검사 중), {@code PASSED}(통과), {@code BLOCKED}(차단)
   * @param reason 차단 사유. 차단이 아니면 null 이다
   */
  record Result(AttachmentInspectionStatus status, String reason) {

    public static Result pending() {
      return new Result(AttachmentInspectionStatus.PENDING, null);
    }

    public static Result passed() {
      return new Result(AttachmentInspectionStatus.PASSED, null);
    }

    public static Result blocked(String reason) {
      return new Result(AttachmentInspectionStatus.BLOCKED, reason);
    }
  }
}
