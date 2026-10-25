package com.connectlink.notice;

import com.connectlink.common.AuditedEntity;
import com.connectlink.organization.Business;
import java.time.LocalDate;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * 공지·자료실 글 하나. 업무({@code business})가 있으면 그 업무 담당자(결제원·참가기관)만 보고, 없으면 전 업무 공통 공지라 모두 본다. 규약·시행세칙은
 * 업무별로만 둔다. 첨부는 {@code AttachmentOwnerType.NOTICE} 다.
 */
@Entity
@Table(name = "notice")
public class Notice extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** 없으면 전 업무 공통. */
  @Enumerated(EnumType.STRING)
  @Column(name = "business", length = 30)
  private Business business;

  @Enumerated(EnumType.STRING)
  @Column(name = "category", nullable = false, length = 30)
  private NoticeCategory category;

  @Column(name = "title", nullable = false, length = 200)
  private String title;

  @Column(name = "content", nullable = false, length = 4000)
  private String content;

  /** 목록 맨 위에 고정. */
  @Column(name = "pinned", nullable = false)
  private boolean pinned;

  /** 규약·시행세칙의 시행일. 없으면 null. */
  @Column(name = "effective_date")
  private LocalDate effectiveDate;

  /** 이어 둔 의견 요청(예: 회의 참석 회신). 없으면 null. */
  @Column(name = "opinion_request_id")
  private Long opinionRequestId;

  @Column(name = "author_email", nullable = false, length = 200)
  private String authorEmail;

  @Column(name = "author_name", nullable = false, length = 50)
  private String authorName;

  protected Notice() {}

  public Notice(Business business, String authorEmail, String authorName) {
    this.business = business;
    this.authorEmail = authorEmail;
    this.authorName = authorName;
  }

  void change(
      NoticeCategory category,
      String title,
      String content,
      boolean pinned,
      LocalDate effectiveDate,
      Long opinionRequestId) {
    this.category = category;
    this.opinionRequestId = opinionRequestId;
    this.title = title;
    this.content = content;
    this.pinned = pinned;
    this.effectiveDate = effectiveDate;
    touch();
  }

  public Long getOpinionRequestId() {
    return opinionRequestId;
  }

  public Long getId() {
    return id;
  }

  public Business getBusiness() {
    return business;
  }

  public boolean isCommon() {
    return business == null;
  }

  public NoticeCategory getCategory() {
    return category;
  }

  public String getTitle() {
    return title;
  }

  public String getContent() {
    return content;
  }

  public boolean isPinned() {
    return pinned;
  }

  public LocalDate getEffectiveDate() {
    return effectiveDate;
  }

  public String getAuthorEmail() {
    return authorEmail;
  }

  public String getAuthorName() {
    return authorName;
  }
}
