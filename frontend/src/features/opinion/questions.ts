import type { OpinionAnswer, OpinionQuestion, OpinionQuestionType } from './hooks';

/** 화면에서 만드는 질문 한 개. 선택지는 객관식·체크박스일 때만 보낸다. */
export interface QuestionDraft {
  /** 화면 안 키. */
  key: string;
  type: OpinionQuestionType;
  title: string;
  required: boolean;
  options: string[];
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
