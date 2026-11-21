package com.connectlink.suspension;

import com.connectlink.attachment.AttachmentOwnerAccess;
import com.connectlink.attachment.AttachmentOwnerInfo;
import com.connectlink.attachment.AttachmentOwnerType;
import com.connectlink.auth.CurrentUser;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일시중단 공문 첨부 권한. 일시중단을 볼 수 있으면 내려받고, 고칠 수 있으면 올리고 지운다. 기관 일시중단은 공문이 꼭 있어야 하므로 마지막 공문은 지우지 못한다(결제원
 * 작업 안내는 지울 수 있다).
 */
@Component
@Transactional(readOnly = true)
class SuspensionAttachmentAccess implements AttachmentOwnerAccess {

  private final SuspensionRepository repository;
  private final SuspensionAccessPolicy accessPolicy;

  SuspensionAttachmentAccess(SuspensionRepository repository, SuspensionAccessPolicy accessPolicy) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
  }

  @Override
  public AttachmentOwnerType ownerType() {
    return AttachmentOwnerType.SUSPENSION;
  }

  @Override
  public boolean canView(CurrentUser user, Long ownerId) {
    return repository.findById(ownerId).map(s -> accessPolicy.canView(user, s)).orElse(false);
  }

  @Override
  public boolean canEdit(CurrentUser user, Long ownerId) {
    return repository.findById(ownerId).map(s -> accessPolicy.canEdit(user, s)).orElse(false);
  }

  @Override
  public Optional<String> requiredMessage(Long ownerId) {
    return repository
        .findById(ownerId)
        .filter(s -> !s.isKftcWork())
        .map(s -> "일시중단에는 공문이 하나 이상 있어야 합니다. 바꾸려면 새 공문을 먼저 올린 뒤 지워 주세요.");
  }

  @Override
  public AttachmentOwnerInfo describe(Long ownerId) {
    return repository
        .findById(ownerId)
        .map(
            s ->
                new AttachmentOwnerInfo(
                    s.isKftcWork() ? "결제원 작업 안내" : s.getOrganization().getName() + " 일시중단",
                    s.getBusinesses().stream().sorted().findFirst().orElse(null),
                    "suspensions/" + s.getId()))
        .orElseGet(() -> AttachmentOwnerInfo.unknown(ownerType()));
  }
}
