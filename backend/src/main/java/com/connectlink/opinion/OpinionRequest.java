package com.connectlink.opinion;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.Business;
import com.connectlink.organization.Organization;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.hibernate.annotations.BatchSize;

/**
 * 의견 요청. 결제원 담당자가 한 업무 영역에서 만들고, 대상 기관({@link OpinionTarget})이 회신({@link OpinionReply})한다. 작성중에는
 * 작성한 쪽(결제원)에만 보이고, 회신중으로 열면 대상 기관 회신 담당자에게 알림이 간다. 회신중에 취소하면 마감 상태가 되고 {@code cancelled} 가 참이다.
 */
@Entity
@Table(name = "opinion_request")
public class OpinionRequest extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "business", nullable = false, length = 30)
  private Business business;

  /** 요청명. */
  @Column(name = "title", nullable = false, length = 200)
  private String title;

  /** 요청 목적·내용. */
  @Column(name = "content", nullable = false, length = 4000)
  private String content;

  /** 회신 기한(그날까지). 기한이 지나도 마감하기 전에는 회신을 받는다. */
  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private OpinionRequestStatus status;

  /** 작성한 결제원 담당자. 수정 요청 알림을 받는다. */
  @Column(name = "owner_email", nullable = false, length = 200)
  private String ownerEmail;

  @Column(name = "owner_name", nullable = false, length = 50)
  private String ownerName;

  @Column(name = "opened_at")
  private LocalDateTime openedAt;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  @Column(name = "cancelled", nullable = false)
  private boolean cancelled;

  @Column(name = "cancel_reason", length = 500)
  private String cancelReason;

  @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<OpinionQuestion> questions = new ArrayList<>();

  @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
  @BatchSize(size = 50)
  private List<OpinionTarget> targets = new ArrayList<>();

  protected OpinionRequest() {}

  /** 작성중 요청을 만든다. */
  public static OpinionRequest draft(
      Business business,
      String title,
      String content,
      LocalDate dueDate,
      String ownerEmail,
      String ownerName) {
    OpinionRequest request = new OpinionRequest();
    request.business = business;
    request.title = title;
    request.content = content;
    request.dueDate = dueDate;
    request.status = OpinionRequestStatus.DRAFT;
    request.ownerEmail = ownerEmail;
    request.ownerName = ownerName;
    return request;
  }

  /**
   * 요청명·내용·기한을 고친다. 작성중과 회신중에만 고칠 수 있다.
   *
   * @throws IllegalStateException 마감이면
   */
  public void change(String title, String content, LocalDate dueDate) {
    ensureEditable();
    this.title = title;
    this.content = content;
    this.dueDate = dueDate;
  }

  /** 회신중으로 연다. 대상 기관이 요청을 보고 회신할 수 있다. */
  public void open(LocalDateTime now) {
    if (status != OpinionRequestStatus.DRAFT) {
      throw new IllegalStateException("작성중 요청만 열 수 있습니다: " + status);
    }
    this.status = OpinionRequestStatus.OPEN;
    this.openedAt = now;
  }

  /** 마감한다. 더는 회신을 받지 않는다. */
  public void close(LocalDateTime now) {
    if (status != OpinionRequestStatus.OPEN) {
      throw new IllegalStateException("회신중 요청만 마감할 수 있습니다: " + status);
    }
    this.status = OpinionRequestStatus.CLOSED;
    this.closedAt = now;
  }

  /** 회신중 요청을 취소한다. 마감 상태가 되고 취소 사유를 남긴다. */
  public void cancel(String reason, LocalDateTime now) {
    close(now);
    this.cancelled = true;
    this.cancelReason = reason;
  }

  void ensureEditable() {
    if (status == OpinionRequestStatus.CLOSED) {
      throw new IllegalStateException("마감한 요청은 고칠 수 없습니다.");
    }
  }

  /** 질문을 통째로 바꾼다. 작성중에만 부른다(회신중에는 답이 질문을 가리킨다). */
  void replaceQuestions(List<OpinionQuestion.Draft> drafts) {
    if (status != OpinionRequestStatus.DRAFT) {
      throw new IllegalStateException("작성중 요청만 질문을 바꿀 수 있습니다: " + status);
    }
    questions.clear();
    for (int i = 0; i < drafts.size(); i++) {
      OpinionQuestion.Draft d = drafts.get(i);
      questions.add(new OpinionQuestion(this, i, d.type(), d.title(), d.required(), d.options()));
    }
  }

  OpinionTarget addTarget(Organization organization) {
    OpinionTarget target = new OpinionTarget(this, organization);
    targets.add(target);
    return target;
  }

  void removeTarget(OpinionTarget target) {
    targets.remove(target);
  }

  public Optional<OpinionTarget> findTarget(Long organizationId) {
    return targets.stream()
        .filter(t -> t.getOrganization().getId().equals(organizationId))
        .findFirst();
  }

  public boolean isOverdue(LocalDate today) {
    return status == OpinionRequestStatus.OPEN && today.isAfter(dueDate);
  }

  public Long getId() {
    return id;
  }

  public Business getBusiness() {
    return business;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public OpinionRequestStatus getStatus() {
    return status;
  }

  public String getOwnerEmail() {
    return ownerEmail;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public LocalDateTime getOpenedAt() {
    return openedAt;
  }

  public LocalDateTime getClosedAt() {
    return closedAt;
  }

  public boolean isCancelled() {
    return cancelled;
  }

  public String getCancelReason() {
    return cancelReason;
  }

  /** 질문. 정한 순서다. */
  public List<OpinionQuestion> getQuestions() {
    return questions.stream()
        .sorted(Comparator.comparingInt(OpinionQuestion::getSortOrder))
        .toList();
  }

  /** 대상 기관. 넣은 순서다. */
  public List<OpinionTarget> getTargets() {
    return List.copyOf(targets);
  }
}
