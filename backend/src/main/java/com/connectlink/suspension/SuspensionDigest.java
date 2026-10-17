package com.connectlink.suspension;

import com.connectlink.common.AuditedEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

/** 전 영업일 취합 전달 한 번. 보낸 영업일마다 하나라서 같은 날 두 번 보내지 않는다. */
@Entity
@Table(name = "suspension_digest")
public class SuspensionDigest extends AuditedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "sent_on", nullable = false)
  private LocalDate sentOn;

  @Column(name = "from_date", nullable = false)
  private LocalDate fromDate;

  @Column(name = "to_date", nullable = false)
  private LocalDate toDate;

  @Column(name = "item_count", nullable = false)
  private int itemCount;

  /** 받은 채널. 쉼표로 잇는다. */
  @Column(name = "recipients", nullable = false, length = 500)
  private String recipients;

  @Column(name = "body", nullable = false, length = 8000)
  private String body;

  @Column(name = "sent_at", nullable = false)
  private LocalDateTime sentAt;

  protected SuspensionDigest() {}

  SuspensionDigest(
      LocalDate sentOn,
      LocalDate fromDate,
      LocalDate toDate,
      int itemCount,
      List<String> recipients,
      String body,
      LocalDateTime sentAt) {
    this.sentOn = sentOn;
    this.fromDate = fromDate;
    this.toDate = toDate;
    this.itemCount = itemCount;
    this.recipients = String.join(",", recipients);
    this.body = body;
    this.sentAt = sentAt;
  }

  public LocalDate getSentOn() {
    return sentOn;
  }

  public LocalDate getFromDate() {
    return fromDate;
  }

  public LocalDate getToDate() {
    return toDate;
  }

  public int getItemCount() {
    return itemCount;
  }

  public List<String> getRecipients() {
    return recipients.isEmpty() ? List.of() : Arrays.asList(recipients.split(","));
  }

  public String getBody() {
    return body;
  }

  public LocalDateTime getSentAt() {
    return sentAt;
  }
}
