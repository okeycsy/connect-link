package com.connectlink.opinion;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.Organization;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;

/** 의견 요청의 대상 기관 한 곳. 회신 담당자({@link OpinionTargetRecipient})와 고른 회신 양식을 가진다. */
@Entity
@Table(name = "opinion_target")
public class OpinionTarget extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "request_id", nullable = false)
  private OpinionRequest request;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "organization_id", nullable = false)
  private Organization organization;

  /** 이 기관이 쓸 회신 양식. 없으면 양식 없이 회신한다. */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "form_id")
  private OpinionReplyForm form;

  @OneToMany(mappedBy = "target", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<OpinionTargetRecipient> recipients = new ArrayList<>();

  protected OpinionTarget() {}

  OpinionTarget(OpinionRequest request, Organization organization) {
    this.request = request;
    this.organization = organization;
  }

  void changeForm(OpinionReplyForm form) {
    this.form = form;
  }

  /**
   * 회신 담당자를 바꾼다. 남는 사람은 행을 그대로 둔다(지우고 다시 넣으면 (대상, 계정) 유일 제약에 걸린다).
   *
   * @param requested 계정 ID → 고를 때의 담당자 정보
   */
  void replaceRecipients(Map<Long, RecipientSnapshot> requested) {
    recipients.removeIf(r -> !requested.containsKey(r.getAccountId()));
    requested.forEach(
        (accountId, snapshot) -> {
          if (recipients.stream().noneMatch(r -> r.getAccountId().equals(accountId))) {
            recipients.add(new OpinionTargetRecipient(this, accountId, snapshot));
          }
        });
  }

  /** 탈퇴한 계정을 회신 담당자에서 뺀다. 뺐으면 true. */
  boolean removeRecipient(Long accountId) {
    return recipients.removeIf(r -> r.getAccountId().equals(accountId));
  }

  public Long getId() {
    return id;
  }

  public OpinionRequest getRequest() {
    return request;
  }

  public Organization getOrganization() {
    return organization;
  }

  public OpinionReplyForm getForm() {
    return form;
  }

  /** 회신 담당자. 이름순이다. */
  public List<OpinionTargetRecipient> getRecipients() {
    return recipients.stream()
        .sorted(Comparator.comparing(OpinionTargetRecipient::getName))
        .toList();
  }

  /** 담당자를 고를 때 남기는 값. */
  record RecipientSnapshot(String email, String name, String subBusiness) {}
}
