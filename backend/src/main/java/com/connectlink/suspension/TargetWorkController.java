package com.connectlink.suspension;

import com.connectlink.auth.CurrentUserProvider;
import com.connectlink.suspension.dto.TargetWorkResponse;
import com.connectlink.suspension.dto.TargetWorkSaveRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/target-works")
@Tag(name = "일시중단 대상 업무", description = "일시중단에서 고르는 대상 업무 기준정보")
public class TargetWorkController {

  private final TargetWorkService targetWorkService;
  private final CurrentUserProvider currentUserProvider;

  public TargetWorkController(
      TargetWorkService targetWorkService, CurrentUserProvider currentUserProvider) {
    this.targetWorkService = targetWorkService;
    this.currentUserProvider = currentUserProvider;
  }

  @GetMapping
  @Operation(
      summary = "대상 업무 목록 조회",
      description = "순서, 이름순. 기본은 사용 중인 것만이고, includeInactive 는 관리자만 쓴다.")
  public List<TargetWorkResponse> list(
      @Parameter(description = "사용 안 함까지 (관리자)") @RequestParam(defaultValue = "false")
          boolean includeInactive) {
    return targetWorkService.list(currentUserProvider.require(), includeInactive);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "대상 업무 등록 (관리자)", description = "이름이 겹치면 409 TARGET_WORK_NAME_DUPLICATE.")
  public TargetWorkResponse create(@Valid @RequestBody TargetWorkSaveRequest request) {
    return targetWorkService.create(currentUserProvider.require(), request);
  }

  @PutMapping("/{id}")
  @Operation(
      summary = "대상 업무 수정 (관리자)",
      description = "이름·분류·순서·사용 여부·관련 업무를 고친다. 이미 등록한 일시중단의 업무 이름과 관련 업무는 그대로다.")
  public TargetWorkResponse update(
      @PathVariable Long id, @Valid @RequestBody TargetWorkSaveRequest request) {
    return targetWorkService.update(currentUserProvider.require(), id, request);
  }
}
