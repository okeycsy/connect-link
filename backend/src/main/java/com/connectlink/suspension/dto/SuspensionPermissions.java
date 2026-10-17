package com.connectlink.suspension.dto;

/**
 * 지금 사용자가 이 일시중단에 할 수 있는 일. 화면은 이 값으로 버튼을 보이고, 서버는 같은 규칙으로 다시 막는다.
 *
 * @param manage 결제원 쪽 화면(확인자, 변경 이력)을 쓰는지
 * @param edit 내용·공문을 고칠 수 있는지
 * @param delete 지울 수 있는지(등록한 본인)
 * @param startReview 확인을 시작할 수 있는지(아직 시작하지 않은 결제원 담당자, 검토 대기·검토 중)
 * @param confirm 조치·인수인계 사항을 써서 확정(확정 뒤에는 사항 고치기)할 수 있는지
 */
public record SuspensionPermissions(
    boolean manage, boolean edit, boolean delete, boolean startReview, boolean confirm) {}
