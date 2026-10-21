package com.connectlink.opinion;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 기관이 답할 질문의 경로. 구글 폼처럼 첫 섹션부터 차례로 가되, 섹션을 마치면 그 섹션의 이동({@link OpinionSection#nextJump})을 따른다. 이동은
 * 뒤로만 가므로 경로는 반드시 끝난다. 경로 밖 질문은 건너뛴 질문이라 답을 받지 않고 필수도 따지지 않는다.
 */
final class OpinionQuestionPath {

  private OpinionQuestionPath() {}

  /**
   * @param sections 요청의 섹션(순서대로)
   * @param chosen 질문 ID 별 고른 선택지 ID
   * @return 경로에 있는 질문 ID(경로 순서)
   */
  static Set<Long> of(List<OpinionSection> sections, Map<Long, Set<Long>> chosen) {
    Set<Long> path = new LinkedHashSet<>();
    int index = 0;
    while (index < sections.size()) {
      OpinionSection section = sections.get(index);
      section.getQuestions().forEach(q -> path.add(q.getId()));
      OpinionQuestion.Jump jump = section.nextJump(chosen);
      if (jump == null) {
        index++;
      } else if (jump.type() == OpinionJumpType.END) {
        break;
      } else {
        // 저장할 때 뒤로만 가게 막지만, 혹시라도 멈추지 않게 앞으로만 간다.
        index = Math.max(index + 1, jump.order());
      }
    }
    return path;
  }
}
