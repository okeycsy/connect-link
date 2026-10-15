package com.connectlink.history;

import com.connectlink.common.Labeled;
import com.connectlink.history.dto.FieldChange;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 변경 전·후를 비교해 {@link FieldChange} 목록을 만드는 도우미. 값이 같은 항목은 넣지 않으므로, 결과가 비어 있으면 실제로 바뀐 것이 없다는 뜻이다.
 *
 * <pre>
 * List&lt;FieldChange&gt; changes = FieldChanges.compare()
 *     .field("name", "기관명", before.getName(), after.getName())
 *     .field("active", "사용 여부", before.isActive(), after.isActive())
 *     .toList();
 * </pre>
 */
public final class FieldChanges {

  private final List<FieldChange> changes = new ArrayList<>();

  private FieldChanges() {}

  public static FieldChanges compare() {
    return new FieldChanges();
  }

  /** 두 값이 다를 때만 항목을 더한다. null 은 빈 값으로 본다. */
  public FieldChanges field(String field, String label, Object before, Object after) {
    String b = text(before);
    String a = text(after);
    if (!Objects.equals(b, a)) {
      changes.add(new FieldChange(field, label, b, a));
    }
    return this;
  }

  /** 등록(변경 전 없음)이나 삭제(변경 후 없음)처럼 비교 없이 값 하나만 남길 때. */
  public FieldChanges value(String field, String label, Object before, Object after) {
    changes.add(new FieldChange(field, label, text(before), text(after)));
    return this;
  }

  public boolean isEmpty() {
    return changes.isEmpty();
  }

  public List<FieldChange> toList() {
    return List.copyOf(changes);
  }

  /**
   * 목록 값을 "가, 나, 다" 꼴 한 줄로. 참여 업무, 전산 세부 직무처럼 여러 개 고르는 항목에 쓴다. enum 목록은 선언 순서로 늘어놓아 집합의 순서(요청 JSON
   * 의 HashSet 등)와 무관하게 같은 값이 같은 글자가 되게 한다.
   */
  public static String join(Collection<?> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }
    Stream<?> stream = values.stream();
    if (values.stream().allMatch(v -> v instanceof Enum<?>)) {
      stream = stream.sorted(Comparator.comparingInt(v -> ((Enum<?>) v).ordinal()));
    }
    return stream.map(FieldChanges::text).collect(Collectors.joining(", "));
  }

  private static String text(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean bool) {
      return bool ? "예" : "아니요";
    }
    if (value instanceof Labeled labeled) {
      return labeled.getLabel();
    }
    String s = value.toString();
    return s.isBlank() ? null : s;
  }
}
