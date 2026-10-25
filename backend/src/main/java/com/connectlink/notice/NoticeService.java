package com.connectlink.notice;

import com.connectlink.attachment.AttachmentOwnerType;
import com.connectlink.attachment.AttachmentService;
import com.connectlink.auth.CurrentUser;
import com.connectlink.common.BusinessException;
import com.connectlink.common.PageResponse;
import com.connectlink.notice.dto.NoticeResponse;
import com.connectlink.notice.dto.NoticeSaveRequest;
import com.connectlink.organization.Business;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.criteria.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공지·자료실. 업무 영역(레일)의 글과 전 업무 공통 공지를 함께 보인다. 권한은 {@link NoticeAccessPolicy}. */
@Service
@Transactional
public class NoticeService {

  /** 고정 글 먼저, 그다음 최근 글. */
  static final Sort SORT = Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("id"));

  private final NoticeRepository repository;
  private final NoticeAccessPolicy accessPolicy;
  private final AttachmentService attachmentService;

  public NoticeService(
      NoticeRepository repository,
      NoticeAccessPolicy accessPolicy,
      AttachmentService attachmentService) {
    this.repository = repository;
    this.accessPolicy = accessPolicy;
    this.attachmentService = attachmentService;
  }

  /** 이 업무의 글과 공통 공지. category 가 있으면 그 분류만이다. */
  @Transactional(readOnly = true)
  public PageResponse<NoticeResponse> list(
      CurrentUser user, Business business, NoticeCategory category, Pageable pageable) {
    accessPolicy.ensureCanList(user, business);
    Specification<Notice> spec =
        (root, query, cb) -> {
          List<Predicate> predicates = new ArrayList<>();
          predicates.add(
              cb.or(cb.equal(root.get("business"), business), cb.isNull(root.get("business"))));
          if (category != null) {
            predicates.add(cb.equal(root.get("category"), category));
          }
          return cb.and(predicates.toArray(Predicate[]::new));
        };
    return PageResponse.from(
        repository.findAll(
            spec, PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), SORT)),
        n -> toResponse(user, n));
  }

  @Transactional(readOnly = true)
  public NoticeResponse get(CurrentUser user, Long id) {
    Notice notice = load(id);
    accessPolicy.ensureCanView(user, notice);
    return toResponse(user, notice);
  }

  public NoticeResponse create(CurrentUser user, NoticeSaveRequest body) {
    accessPolicy.ensureCanManage(user, body.business());
    ensureCategoryFits(body.business(), body.category());
    Notice notice = new Notice(body.business(), user.email(), user.name());
    apply(notice, body);
    return toResponse(user, repository.save(notice));
  }

  public NoticeResponse update(CurrentUser user, Long id, NoticeSaveRequest body) {
    Notice notice = load(id);
    accessPolicy.ensureCanManage(user, notice.getBusiness());
    ensureCategoryFits(notice.getBusiness(), body.category());
    apply(notice, body);
    return toResponse(user, notice);
  }

  public void delete(CurrentUser user, Long id) {
    Notice notice = load(id);
    accessPolicy.ensureCanManage(user, notice.getBusiness());
    attachmentService.deleteAllOf(AttachmentOwnerType.NOTICE, notice.getId());
    repository.delete(notice);
  }

  Notice load(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> BusinessException.notFound("NOTICE_NOT_FOUND", "글을 찾을 수 없습니다."));
  }

  private static void apply(Notice notice, NoticeSaveRequest body) {
    notice.change(
        body.category(),
        body.title().trim(),
        body.content().trim(),
        body.pinned(),
        body.category().isRegulation() ? body.effectiveDate() : null);
  }

  /** 규약·시행세칙은 업무 자료라 공통으로 둘 수 없다. */
  private static void ensureCategoryFits(Business business, NoticeCategory category) {
    if (business == null && category.isRegulation()) {
      throw BusinessException.onField(
          HttpStatus.BAD_REQUEST,
          "NOTICE_COMMON_REGULATION",
          "category",
          "규약·시행세칙은 업무를 정해서 올려 주세요. 전 업무 공통으로는 공지만 올립니다.");
    }
  }

  private NoticeResponse toResponse(CurrentUser user, Notice n) {
    return new NoticeResponse(
        n.getId(),
        n.getBusiness(),
        n.getBusiness() == null ? null : n.getBusiness().getLabel(),
        n.getCategory(),
        n.getCategory().getLabel(),
        n.getTitle(),
        n.getContent(),
        n.isPinned(),
        n.getEffectiveDate(),
        n.getAuthorName(),
        n.getCreatedAt(),
        n.getUpdatedAt(),
        accessPolicy.canManage(user, n.getBusiness()));
  }
}
