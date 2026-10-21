import type {
  OpinionAnswer,
  OpinionJump,
  OpinionQuestion,
  OpinionQuestionRequest,
  OpinionQuestionType,
  OpinionSection,
} from './hooks';

/**
 * 화면에서 고르는 섹션 이동. '' 은 바로 다음 섹션(선택지면 섹션 설정대로), 'END' 는 회신 끝내기, 그 밖은 갈 섹션의 화면 안 키다. 섹션 순서를 바꿔도
 * 가리키는 섹션이 그대로 남도록 키로 둔다.
 */
export type JumpValue = string;
export const JUMP_NEXT: JumpValue = '';
export const JUMP_END: JumpValue = 'END';

/** 화면에서 만드는 섹션 하나. */
export interface SectionDraft {
  key: string;
  title: string;
  description: string;
  /** 이 섹션 다음 이동. */
  jump: JumpValue;
}

/** 화면에서 만드는 선택지 하나. */
export interface OptionDraft {
  label: string;
  /** 이 답을 고르면 갈 섹션. 하나 고르기에서 "답변에 따라 섹션 이동"을 켰을 때만 보낸다. */
  jump: JumpValue;
}

/** 화면에서 만드는 질문 한 개. 선택지는 객관식·체크박스일 때만 보낸다. 질문 목록은 늘 섹션 순서대로 묶여 있다. */
export interface QuestionDraft {
  /** 화면 안 키. */
  key: string;
  /** 묶인 섹션의 키. */
  sectionKey: string;
  type: OpinionQuestionType;
  title: string;
  required: boolean;
  /** 답변에 따라 섹션 이동(하나 고르기에서만). */
  branching: boolean;
  options: OptionDraft[];
}

/** 질문 유형 이름. 서버 라벨과 같다. */
export const QUESTION_TYPE_OPTIONS: { value: OpinionQuestionType; label: string }[] = [
  { value: 'SINGLE_CHOICE', label: '객관식' },
  { value: 'MULTIPLE_CHOICE', label: '체크박스' },
  { value: 'TEXT', label: '주관식' },
];

export function isChoice(type: OpinionQuestionType): boolean {
  return type !== 'TEXT';
}

/** 답 하나를 글로. 객관식은 고른 선택지를 질문 순서대로 쉼표로 잇는다(서버 취합표와 같다). */
export function answerText(question: OpinionQuestion, answer: OpinionAnswer | undefined): string {
  if (!answer) return '';
  if (question.type === 'TEXT') return answer.text ?? '';
  const picked = new Set(answer.optionIds);
  return question.options
    .filter((o) => picked.has(o.id))
    .map((o) => o.label)
    .join(', ');
}

/** 섹션 이름. "섹션 2" 또는 "섹션 2. 제목". */
export function sectionName(index: number, title?: string | null): string {
  return title?.trim() ? `섹션 ${index + 1}. ${title.trim()}` : `섹션 ${index + 1}`;
}

/** 이동을 사람이 읽는 글로(서버 응답의 순서 값). 없으면 null. */
export function jumpText(jump: OpinionJump | null | undefined, sections: OpinionSection[]) {
  if (!jump) return null;
  if (jump.type === 'END') return '회신 끝내기';
  const target = sections[jump.sectionIndex ?? -1];
  return `${sectionName(jump.sectionIndex ?? 0, target?.title)}(으)로 이동`;
}

// ---- 편집 화면 값 ↔ 요청 본문 ----

/** 서버 응답의 이동을 화면 값으로. 섹션 키는 순서대로 준 것이다. */
export function jumpValueOf(
  jump: OpinionJump | null | undefined,
  sectionKeys: string[],
): JumpValue {
  if (!jump) return JUMP_NEXT;
  if (jump.type === 'END') return JUMP_END;
  return sectionKeys[jump.sectionIndex ?? -1] ?? JUMP_NEXT;
}

function jumpBody(value: JumpValue, sections: SectionDraft[]): OpinionJump | null {
  if (value === JUMP_NEXT) return null;
  if (value === JUMP_END) return { type: 'END', sectionIndex: null };
  const index = sections.findIndex((s) => s.key === value);
  return index < 0 ? null : { type: 'SECTION', sectionIndex: index };
}

/** 편집 화면의 섹션·질문을 요청 본문으로. 질문은 섹션 순서대로 놓는다(서버 오류의 질문 번호와 같은 순서). */
export function formBody(sections: SectionDraft[], questions: QuestionDraft[]) {
  const sectionIndex = new Map(sections.map((s, i) => [s.key, i]));
  return {
    sections: sections.map((s) => ({
      title: s.title,
      description: s.description,
      jump: jumpBody(s.jump, sections),
    })),
    questions: sortBySection(sections, questions).map((q): OpinionQuestionRequest => ({
      sectionIndex: sectionIndex.get(q.sectionKey) ?? 0,
      type: q.type,
      title: q.title,
      required: q.required,
      options: isChoice(q.type)
        ? q.options.map((o) => ({
            label: o.label,
            jump:
              q.type === 'SINGLE_CHOICE' && q.branching && sections.length > 1
                ? jumpBody(o.jump, sections)
                : null,
          }))
        : [],
    })),
  };
}

/** 질문을 섹션 순서대로(같은 섹션 안에서는 지금 순서). */
export function sortBySection(sections: SectionDraft[], questions: QuestionDraft[]) {
  const order = new Map(sections.map((s, i) => [s.key, i]));
  return questions
    .map((q, i) => ({ q, i }))
    .sort(
      (a, b) => (order.get(a.q.sectionKey) ?? 0) - (order.get(b.q.sectionKey) ?? 0) || a.i - b.i,
    )
    .map(({ q }) => q);
}

/**
 * 뒤로 가는 이동을 찾는다(서버도 막는다). 섹션 순서를 바꾸면 생길 수 있어 저장 전에 칸 아래에 알린다. 키는 폼 경로다.
 */
export function backwardJumpErrors(sections: SectionDraft[], questions: QuestionDraft[]) {
  const order = new Map(sections.map((s, i) => [s.key, i]));
  const message = '이 섹션보다 뒤에 있는 섹션으로만 이동할 수 있습니다.';
  const errors: Record<string, string> = {};
  const backward = (value: JumpValue, from: number) =>
    value !== JUMP_NEXT && value !== JUMP_END && (order.get(value) ?? -1) <= from;
  sections.forEach((s, i) => {
    if (backward(s.jump, i)) errors[`sections.${i}.jump`] = message;
  });
  questions.forEach((q, qi) => {
    if (q.type !== 'SINGLE_CHOICE' || !q.branching) return;
    const from = order.get(q.sectionKey) ?? 0;
    q.options.forEach((o, oi) => {
      if (backward(o.jump, from)) errors[`questions.${qi}.options.${oi}.jump`] = message;
    });
  });
  return errors;
}

// ---- 회신 경로 ----

/**
 * 고른 답으로 지나는 섹션 순서(서버 OpinionQuestionPath 와 같은 규칙). 첫 섹션부터 가되, 섹션을 마치면 그 섹션의 하나 고르기 질문에서 고른
 * 선택지의 이동(여럿이면 뒤의 질문)을, 없으면 섹션의 이동을 따른다. 이동은 뒤로만 간다.
 *
 * @param chosen 질문 ID 별 고른 선택지 ID
 */
export function sectionPath(
  sections: OpinionSection[],
  questions: OpinionQuestion[],
  chosen: (questionId: number) => number[],
): Set<number> {
  const path = new Set<number>();
  let index = 0;
  while (index < sections.length) {
    path.add(index);
    let jump: OpinionJump | null = null;
    for (const q of questions) {
      if (q.sectionIndex !== index || q.type !== 'SINGLE_CHOICE') continue;
      const picked = new Set(chosen(q.id));
      const option = q.options.find((o) => picked.has(o.id) && o.jump);
      if (option?.jump) jump = option.jump;
    }
    jump = jump ?? sections[index].jump ?? null;
    if (!jump) index++;
    else if (jump.type === 'END') break;
    else index = Math.max(index + 1, jump.sectionIndex ?? 0);
  }
  return path;
}

/** 고른 답으로 지나는 질문 ID. 지나는 섹션의 질문이다. */
export function questionPath(
  sections: OpinionSection[],
  questions: OpinionQuestion[],
  chosen: (questionId: number) => number[],
): Set<number> {
  const visited = sectionPath(sections, questions, chosen);
  return new Set(questions.filter((q) => visited.has(q.sectionIndex)).map((q) => q.id));
}
