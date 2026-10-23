package com.connectlink.testplan;

import com.connectlink.common.AuditedEntity;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * 기관 결과 한 칸에 결제원 담당자와 그 기관 담당자가 이어서 다는 답글 하나. 결제원 의견 아래에 시간순으로 한 줄로 이어진다(답글의 답글은 따로 두지 않는다). 쓴 사람은
 * 그때의 이름·직급과 결제원 쪽인지를 남겨 "결제원(김민찬 과장)", "KB국민은행(김민준 대리)"처럼 보인다.
 */
@Entity
@Table(name = "test_item_comment")
public class TestItemComment extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "test_item_result_id")
  private TestItemResult result;

  @Column(name = "body", nullable = false, length = 2000)
  private String body;

  /** 결제원 담당자가 썼는지. 아니면 결과 행의 대상 기관 담당자다. */
  @Column(name = "author_kftc", nullable = false)
  private boolean authorKftc;

  @Column(name = "author_email", nullable = false, length = 200)
  private String authorEmail;

  @Column(name = "author_name", nullable = false, length = 50)
  private String authorName;

  /** 쓴 사람의 그때 직급. 없으면 null. */
  @Column(name = "author_job_title", length = 50)
  private String authorJobTitle;

  @Column(name = "written_at", nullable = false)
  private LocalDateTime writtenAt;

  protected TestItemComment() {}

  TestItemComment(
      TestItemResult result,
      String body,
      boolean authorKftc,
      String email,
      String name,
      String jobTitle,
      LocalDateTime now) {
    this.result = result;
    this.body = body;
    this.authorKftc = authorKftc;
    this.authorEmail = email;
    this.authorName = name;
    this.authorJobTitle = jobTitle;
    this.writtenAt = now;
  }

  public Long getId() {
    return id;
  }

  public TestItemResult getResult() {
    return result;
  }

  public String getBody() {
    return body;
  }

  public boolean isAuthorKftc() {
    return authorKftc;
  }

  public String getAuthorEmail() {
    return authorEmail;
  }

  public String getAuthorName() {
    return authorName;
  }

  public String getAuthorJobTitle() {
    return authorJobTitle;
  }

  public LocalDateTime getWrittenAt() {
    return writtenAt;
  }
}
