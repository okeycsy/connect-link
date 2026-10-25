package com.connectlink.opinion;

import com.connectlink.account.AccountWithdrawnEvent;
import com.connectlink.contact.BusinessContact;
import com.connectlink.contact.ContactRank;
import com.connectlink.contact.ContactService;
import com.connectlink.opinion.OpinionTarget.RecipientSnapshot;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정이 탈퇴하면 같은 이메일의 회신 담당자를 의견 요청에서 뺀다(연락처도 함께 지워진다). 탈퇴와 같은 트랜잭션이라 실패하면 탈퇴도 되돌아간다. 그 사람이 쓴 회신은 기관의
 * 회신이라 남긴다(이름은 쓸 때 그대로).
 *
 * <p>작성중·회신중 요청에서 그 기관의 회신 담당자가 아무도 남지 않으면 같은 기관·같은 업무 담당자 연락처에서 후임자를 넣는다. 같은 세부업무 담당자를 먼저, 없으면
 * 주담당자를, 그것도 없으면 그 기관 담당자 전부를 넣는다. 회신중이면 후임자에게 의견 요청 메일이 가고, 요청을 만든 결제원 담당자에게 알린다.
 */
@Component
class OpinionAccountListener {

  private final OpinionTargetRecipientRepository recipientRepository;
  private final ContactService contactService;
  private final OpinionNotifier notifier;

  OpinionAccountListener(
      OpinionTargetRecipientRepository recipientRepository,
      ContactService contactService,
      OpinionNotifier notifier) {
    this.recipientRepository = recipientRepository;
    this.contactService = contactService;
    this.notifier = notifier;
  }

  @EventListener
  @Transactional
  public void onWithdrawn(AccountWithdrawnEvent event) {
    String email = event.email().toLowerCase(Locale.ROOT);
    for (OpinionTargetRecipient recipient : recipientRepository.findByEmail(email)) {
      OpinionTarget target = recipient.getTarget();
      String subBusiness = recipient.getSubBusiness();
      target.removeRecipient(email);
      OpinionRequestStatus status = target.getRequest().getStatus();
      if (target.getRecipients().isEmpty() && status != OpinionRequestStatus.CLOSED) {
        handOver(target, email, subBusiness);
      }
    }
  }

  private void handOver(OpinionTarget target, String withdrawnEmail, String subBusiness) {
    OpinionRequest request = target.getRequest();
    Long organizationId = target.getOrganization().getId();
    List<BusinessContact> contacts =
        contactService.listByBusiness(request.getBusiness()).stream()
            .filter(c -> organizationId.equals(c.organizationId()))
            .filter(c -> !c.email().equals(withdrawnEmail))
            .toList();
    List<BusinessContact> successors =
        contacts.stream()
            .filter(c -> subBusiness != null && Objects.equals(subBusiness, c.subBusiness()))
            .toList();
    if (successors.isEmpty()) {
      successors = contacts.stream().filter(c -> c.rank() == ContactRank.PRIMARY).toList();
    }
    if (successors.isEmpty()) {
      successors = contacts;
    }
    if (successors.isEmpty()) {
      return;
    }
    Map<String, RecipientSnapshot> recipients = new LinkedHashMap<>();
    successors.stream()
        .sorted(Comparator.comparing(BusinessContact::name))
        .forEach(
            c ->
                recipients.put(
                    c.email(), new RecipientSnapshot(c.email(), c.name(), c.subBusiness())));
    target.replaceRecipients(recipients);
    if (request.getStatus() == OpinionRequestStatus.OPEN) {
      notifier.opened(request, recipients.keySet());
    }
    notifier.handedOver(
        request,
        target.getOrganization().getName(),
        successors.stream().map(BusinessContact::name).sorted().toList());
  }
}
