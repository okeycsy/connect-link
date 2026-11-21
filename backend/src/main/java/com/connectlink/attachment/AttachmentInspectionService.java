package com.connectlink.attachment;

import com.connectlink.auth.CurrentUser;
import com.connectlink.organization.OrganizationType;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 첨부파일 반입 보안 검사. 결제원 밖 계정이 올린 파일은 검사({@link InboundFileInspector})를 통과해야 내려받을 수 있다. 결제원 계정이 올린 파일은
 * 내부망에서 만든 자료라 검사가 없다(반출 결재는 {@link AttachmentApprovalService}).
 */
@Service
public class AttachmentInspectionService {

  private final AttachmentRepository repository;
  private final InboundFileInspector inspector;
  private final Clock clock;

  public AttachmentInspectionService(
      AttachmentRepository repository, InboundFileInspector inspector, Clock clock) {
    this.repository = repository;
    this.inspector = inspector;
    this.clock = clock;
  }

  /** 이 사람이 올리는 파일은 반입 검사가 필요한가. 결제원 밖 계정의 파일이 그렇다. */
  public static boolean requiresInspection(CurrentUser user) {
    return user.organizationType() != OrganizationType.KFTC;
  }

  /** 새로 올린 파일의 검사를 요청한다. 검사가 바로 끝나면 결과까지 적는다. */
  void request(Attachment attachment) {
    attachment.requestInspection(LocalDateTime.now(clock));
    apply(attachment);
  }

  /**
   * 검사 중인 파일의 결과를 다시 묻는다. {@link AttachmentInspectionJob} 이 부른다.
   *
   * @return 이번에 검사가 끝난(통과·차단) 파일 수
   */
  @Transactional
  public int completePending() {
    List<Attachment> pending =
        repository.findByInspectionStatusOrderByIdAsc(AttachmentInspectionStatus.PENDING);
    int completed = 0;
    for (Attachment attachment : pending) {
      if (apply(attachment)) {
        completed++;
      }
    }
    return completed;
  }

  private boolean apply(Attachment attachment) {
    InboundFileInspector.Result result = inspector.inspect(attachment);
    if (result.status() == AttachmentInspectionStatus.PENDING) {
      return false;
    }
    attachment.completeInspection(result.status(), result.reason(), LocalDateTime.now(clock));
    return true;
  }
}
