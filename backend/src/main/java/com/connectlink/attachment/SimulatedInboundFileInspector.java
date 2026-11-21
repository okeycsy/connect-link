package com.connectlink.attachment;

import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/**
 * 프로토타입의 반입 보안 검사. 실제 솔루션을 거치지 않고, 검사를 요청한 뒤 {@code app.attachment.inspection.simulated-delay}(기본
 * 3초)가 지나면 통과로 본다. 파일 내용은 들여다보지 않는다. 운영에서는 이 빈을 사내 망연계·무해화 솔루션 연동 구현으로 바꾼다.
 */
@Component
class SimulatedInboundFileInspector implements InboundFileInspector {

  private final AttachmentProperties properties;
  private final Clock clock;

  SimulatedInboundFileInspector(AttachmentProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public Result inspect(Attachment attachment) {
    LocalDateTime done =
        attachment.getInspectionRequestedAt().plus(properties.getInspection().getSimulatedDelay());
    return LocalDateTime.now(clock).isBefore(done) ? Result.pending() : Result.passed();
  }
}
