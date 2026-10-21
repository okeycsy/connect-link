import {
  ActionIcon,
  Badge,
  Button,
  Card,
  Checkbox,
  Divider,
  Group,
  Radio,
  Select,
  Stack,
  Switch,
  Text,
  Textarea,
  TextInput,
  Tooltip,
} from '@mantine/core';
import type { UseFormReturnType } from '@mantine/form';
import {
  IconArrowDown,
  IconArrowUp,
  IconCornerDownRight,
  IconPlus,
  IconTrash,
  IconX,
} from '@tabler/icons-react';

import type { OpinionQuestionType } from '../hooks';
import {
  isChoice,
  JUMP_END,
  JUMP_NEXT,
  type JumpValue,
  QUESTION_TYPE_OPTIONS,
  type QuestionDraft,
  type SectionDraft,
  sectionName,
  sortBySection,
} from '../questions';

interface FormShape {
  sections: SectionDraft[];
  questions: QuestionDraft[];
}

interface Props<V extends FormShape> {
  form: UseFormReturnType<V>;
  /** 회신을 받기 시작한 요청이면 섹션·질문을 고치지 못한다(서버도 막는다). */
  locked: boolean;
  newKey: () => string;
}

/**
 * 의견 요청의 섹션과 질문 만들기. 구글 폼처럼 질문마다 유형(객관식 하나 고르기, 체크박스 여러 개 고르기, 주관식)과 필수 여부를 정하고, 객관식이면
 * 선택지를 적는다. 섹션을 더하면 질문을 섹션으로 묶고, 섹션마다 "이 섹션 다음" 갈 곳을, 객관식에서 "답변에 따라 섹션 이동"을 켜면 선택지마다 갈 곳을
 * 정한다. 질문 목록은 늘 섹션 순서대로 묶어 둔다(서버 오류의 질문 번호와 같은 순서).
 */
export function QuestionBuilder<V extends FormShape>({ form, locked, newKey }: Props<V>) {
  // useForm 의 경로 함수는 값 모양을 모르므로 섹션·질문 경로만 이 이름으로 쓴다.
  const f = form as unknown as UseFormReturnType<FormShape>;
  const { sections, questions } = f.values;
  const multi = sections.length > 1;
  const sectionOrder = new Map(sections.map((s, i) => [s.key, i]));

  const addQuestion = (sectionIndex: number) => {
    const at = questions.filter(
      (q) => (sectionOrder.get(q.sectionKey) ?? 0) <= sectionIndex,
    ).length;
    f.insertListItem(
      'questions',
      {
        key: newKey(),
        sectionKey: sections[sectionIndex].key,
        type: 'SINGLE_CHOICE',
        title: '',
        required: false,
        branching: false,
        options: [
          { label: '', jump: JUMP_NEXT },
          { label: '', jump: JUMP_NEXT },
        ],
      } satisfies QuestionDraft,
      at,
    );
  };

  const addSection = () =>
    f.insertListItem('sections', {
      key: newKey(),
      title: '',
      description: '',
      jump: JUMP_NEXT,
    } satisfies SectionDraft);

  /** 섹션을 지운다. 질문은 앞 섹션(첫 섹션이면 다음 섹션)으로 옮기고, 이 섹션으로 가던 이동은 기본으로 돌린다. */
  const removeSection = (index: number) => {
    const removed = sections[index].key;
    const into = sections[index === 0 ? 1 : index - 1].key;
    const reset = (jump: JumpValue) => (jump === removed ? JUMP_NEXT : jump);
    f.setValues({
      sections: sections.filter((_, i) => i !== index).map((s) => ({ ...s, jump: reset(s.jump) })),
      questions: questions.map((q) => ({
        ...q,
        sectionKey: q.sectionKey === removed ? into : q.sectionKey,
        options: q.options.map((o) => ({ ...o, jump: reset(o.jump) })),
      })),
    });
  };

  const moveSection = (from: number, to: number) => {
    const next = [...sections];
    const [moved] = next.splice(from, 1);
    next.splice(to, 0, moved);
    f.setValues({ sections: next, questions: sortBySection(next, questions) });
  };

  /** 질문을 한 칸 옮긴다. 섹션의 첫(끝) 질문이면 앞(뒤) 섹션으로 넘어간다. */
  const moveQuestion = (index: number, direction: -1 | 1) => {
    const q = questions[index];
    const neighbor = questions[index + direction];
    if (neighbor && neighbor.sectionKey === q.sectionKey) {
      f.reorderListItem('questions', { from: index, to: index + direction });
      return;
    }
    const target = sections[(sectionOrder.get(q.sectionKey) ?? 0) + direction];
    if (target) f.setFieldValue(`questions.${index}.sectionKey`, target.key);
  };

  if (locked) return <LockedForm sections={sections} questions={questions} />;

  return (
    <Stack gap="md">
      {questions.length === 0 && (
        <Text size="sm" c="dimmed">
          넣은 질문이 없습니다. 회신 요청을 시작하려면 질문이 하나 이상 있어야 합니다.
        </Text>
      )}
      {sections.map((section, si) => {
        const mine = questions
          .map((q, index) => ({ q, index }))
          .filter(({ q }) => q.sectionKey === section.key);
        return (
          <Stack key={section.key} gap="sm" data-testid="section-block">
            {multi && (
              <Card withBorder p="md" data-testid="section-card">
                <Stack gap="xs">
                  <Group justify="space-between" wrap="nowrap">
                    <Badge variant="light">
                      섹션 {si + 1} / {sections.length}
                    </Badge>
                    <Group gap={4} wrap="nowrap">
                      <Tooltip label="섹션 위로">
                        <ActionIcon
                          variant="subtle"
                          color="gray"
                          aria-label={`섹션 ${si + 1} 위로`}
                          disabled={si === 0}
                          onClick={() => moveSection(si, si - 1)}
                        >
                          <IconArrowUp size={16} stroke={1.8} />
                        </ActionIcon>
                      </Tooltip>
                      <Tooltip label="섹션 아래로">
                        <ActionIcon
                          variant="subtle"
                          color="gray"
                          aria-label={`섹션 ${si + 1} 아래로`}
                          disabled={si === sections.length - 1}
                          onClick={() => moveSection(si, si + 1)}
                        >
                          <IconArrowDown size={16} stroke={1.8} />
                        </ActionIcon>
                      </Tooltip>
                      <Tooltip label="섹션 빼기(질문은 옆 섹션으로 옮깁니다)">
                        <ActionIcon
                          variant="subtle"
                          color="red"
                          aria-label={`섹션 ${si + 1} 빼기`}
                          onClick={() => removeSection(si)}
                        >
                          <IconTrash size={16} stroke={1.8} />
                        </ActionIcon>
                      </Tooltip>
                    </Group>
                  </Group>
                  <TextInput
                    aria-label={`섹션 ${si + 1} 제목`}
                    placeholder="섹션 제목(예: 반영 계획)"
                    maxLength={200}
                    {...f.getInputProps(`sections.${si}.title`)}
                  />
                  <Textarea
                    aria-label={`섹션 ${si + 1} 설명`}
                    placeholder="설명(선택)"
                    autosize
                    minRows={1}
                    maxLength={2000}
                    {...f.getInputProps(`sections.${si}.description`)}
                  />
                </Stack>
              </Card>
            )}

            {mine.map(({ q, index }) => (
              <QuestionCard
                key={q.key}
                f={f}
                question={q}
                index={index}
                sectionIndex={si}
                sections={sections}
                canMoveUp={index > 0 || si > 0}
                canMoveDown={index < questions.length - 1 || si < sections.length - 1}
                onMove={(direction) => moveQuestion(index, direction)}
              />
            ))}

            <Group gap="sm" justify="space-between" align="flex-end">
              <Button
                variant="light"
                leftSection={<IconPlus size={16} stroke={1.8} />}
                disabled={questions.length >= 30}
                onClick={() => addQuestion(si)}
              >
                {multi ? `섹션 ${si + 1}에 질문 추가` : '질문 추가'}
              </Button>
              {multi &&
                (si < sections.length - 1 ? (
                  <Select
                    label={`섹션 ${si + 1} 다음`}
                    w={260}
                    allowDeselect={false}
                    data={jumpChoices(sections, si, '다음 섹션으로 계속')}
                    {...f.getInputProps(`sections.${si}.jump`)}
                  />
                ) : (
                  <Text size="sm" c="dimmed">
                    마지막 섹션입니다. 이 섹션을 마치면 회신이 끝납니다.
                  </Text>
                ))}
            </Group>
            {multi && si < sections.length - 1 && <Divider />}
          </Stack>
        );
      })}
      <Group>
        <Button
          variant="default"
          leftSection={<IconPlus size={16} stroke={1.8} />}
          disabled={sections.length >= 30}
          onClick={addSection}
        >
          섹션 추가
        </Button>
        <Text size="xs" c="dimmed">
          섹션으로 질문을 묶으면 객관식 답에 따라 다른 섹션으로 넘어가게 할 수 있습니다.
        </Text>
      </Group>
    </Stack>
  );
}

/** 이동 고르기 칸의 값들. 지금 섹션보다 앞의 섹션은 고르지 못한다(이미 고른 값은 오류로 보인다). */
function jumpChoices(sections: SectionDraft[], from: number, nextLabel: string) {
  return [
    { value: JUMP_NEXT, label: nextLabel },
    ...sections
      .map((s, i) => ({
        value: s.key,
        label: `${sectionName(i, s.title)}(으)로 이동`,
        disabled: i <= from,
      }))
      .filter((_, i) => i !== from),
    { value: JUMP_END, label: '회신 끝내기' },
  ];
}

function QuestionCard({
  f,
  question: q,
  index,
  sectionIndex,
  sections,
  canMoveUp,
  canMoveDown,
  onMove,
}: {
  f: UseFormReturnType<FormShape>;
  question: QuestionDraft;
  index: number;
  sectionIndex: number;
  sections: SectionDraft[];
  canMoveUp: boolean;
  canMoveDown: boolean;
  onMove: (direction: -1 | 1) => void;
}) {
  const branching = q.type === 'SINGLE_CHOICE' && q.branching && sections.length > 1;
  return (
    <Card bg="var(--cl-sunken)" bd="none" p="md" data-testid="question-card">
      <Stack gap="sm">
        <Group gap="sm" align="flex-start" wrap="nowrap">
          <Text fw={700} mt={6} w={24} style={{ flexShrink: 0 }}>
            {index + 1}.
          </Text>
          <TextInput
            aria-label={`${index + 1}번 질문`}
            placeholder="질문을 적어 주세요."
            flex={1}
            maxLength={500}
            {...f.getInputProps(`questions.${index}.title`)}
          />
          <Select
            aria-label={`${index + 1}번 질문 유형`}
            data={QUESTION_TYPE_OPTIONS}
            w={130}
            allowDeselect={false}
            value={q.type}
            onChange={(value) => {
              if (!value) return;
              const type = value as OpinionQuestionType;
              f.setFieldValue(`questions.${index}.type`, type);
              if (isChoice(type) && q.options.length < 2) {
                f.setFieldValue(
                  `questions.${index}.options`,
                  [
                    ...q.options,
                    { label: '', jump: JUMP_NEXT },
                    { label: '', jump: JUMP_NEXT },
                  ].slice(0, 2),
                );
              }
            }}
          />
        </Group>

        {isChoice(q.type) && (
          <Stack gap={6} pl={32}>
            {q.options.map((_option, optionIndex) => (
              <Group key={optionIndex} gap="sm" wrap="nowrap" align="flex-start">
                {q.type === 'SINGLE_CHOICE' ? (
                  <Radio checked={false} readOnly tabIndex={-1} aria-hidden mt={8} />
                ) : (
                  <Checkbox checked={false} readOnly tabIndex={-1} aria-hidden mt={8} />
                )}
                <TextInput
                  aria-label={`${index + 1}번 질문 선택지 ${optionIndex + 1}`}
                  placeholder={`선택지 ${optionIndex + 1}`}
                  flex={1}
                  size="sm"
                  maxLength={200}
                  {...f.getInputProps(`questions.${index}.options.${optionIndex}.label`)}
                />
                {branching && (
                  <Select
                    aria-label={`${index + 1}번 질문 선택지 ${optionIndex + 1} 다음`}
                    size="sm"
                    w={{ base: 160, sm: 280 }}
                    allowDeselect={false}
                    leftSection={<IconCornerDownRight size={14} stroke={1.8} />}
                    data={jumpChoices(sections, sectionIndex, '섹션 설정대로 계속')}
                    {...f.getInputProps(`questions.${index}.options.${optionIndex}.jump`)}
                  />
                )}
                <ActionIcon
                  variant="subtle"
                  color="gray"
                  mt={4}
                  aria-label={`${index + 1}번 질문 선택지 ${optionIndex + 1} 빼기`}
                  disabled={q.options.length <= 2}
                  onClick={() => f.removeListItem(`questions.${index}.options`, optionIndex)}
                >
                  <IconX size={16} stroke={1.8} />
                </ActionIcon>
              </Group>
            ))}
            <Group gap="sm">
              <Button
                size="xs"
                variant="subtle"
                leftSection={<IconPlus size={14} stroke={1.8} />}
                disabled={q.options.length >= 20}
                onClick={() =>
                  f.insertListItem(`questions.${index}.options`, { label: '', jump: JUMP_NEXT })
                }
              >
                선택지 추가
              </Button>
            </Group>
            {f.errors[`questions.${index}.options`] && (
              <Text size="xs" c="red">
                {f.errors[`questions.${index}.options`]}
              </Text>
            )}
          </Stack>
        )}
        {q.type === 'TEXT' && (
          <Text size="sm" c="dimmed" pl={32}>
            기관이 글로 답합니다.
          </Text>
        )}
        {f.errors[`questions.${index}.sectionIndex`] && (
          <Text size="xs" c="red" pl={32}>
            {f.errors[`questions.${index}.sectionIndex`]}
          </Text>
        )}

        <Group justify="flex-end" gap="xs">
          {q.type === 'SINGLE_CHOICE' && sections.length > 1 && (
            <Switch
              label="답변에 따라 섹션 이동"
              size="sm"
              checked={q.branching}
              onChange={(e) => {
                const on = e.currentTarget.checked;
                f.setFieldValue(`questions.${index}.branching`, on);
                if (!on) {
                  f.setFieldValue(
                    `questions.${index}.options`,
                    q.options.map((o) => ({ ...o, jump: JUMP_NEXT })),
                  );
                }
              }}
              aria-label={`${index + 1}번 질문 답변에 따라 섹션 이동`}
            />
          )}
          <Switch
            label="필수"
            size="sm"
            checked={q.required}
            onChange={(e) =>
              f.setFieldValue(`questions.${index}.required`, e.currentTarget.checked)
            }
            aria-label={`${index + 1}번 질문 필수`}
          />
          <Tooltip label="위로">
            <ActionIcon
              variant="subtle"
              color="gray"
              aria-label={`${index + 1}번 질문 위로`}
              disabled={!canMoveUp}
              onClick={() => onMove(-1)}
            >
              <IconArrowUp size={16} stroke={1.8} />
            </ActionIcon>
          </Tooltip>
          <Tooltip label="아래로">
            <ActionIcon
              variant="subtle"
              color="gray"
              aria-label={`${index + 1}번 질문 아래로`}
              disabled={!canMoveDown}
              onClick={() => onMove(1)}
            >
              <IconArrowDown size={16} stroke={1.8} />
            </ActionIcon>
          </Tooltip>
          <Tooltip label="질문 빼기">
            <ActionIcon
              variant="subtle"
              color="red"
              aria-label={`${index + 1}번 질문 빼기`}
              onClick={() => f.removeListItem('questions', index)}
            >
              <IconTrash size={16} stroke={1.8} />
            </ActionIcon>
          </Tooltip>
        </Group>
      </Stack>
    </Card>
  );
}

/** 회신을 받기 시작한 요청의 섹션·질문(읽기). */
function LockedForm({ sections, questions }: FormShape) {
  if (questions.length === 0) {
    return (
      <Text size="sm" c="dimmed">
        넣은 질문이 없습니다.
      </Text>
    );
  }
  const multi = sections.length > 1;
  const label = (value: JumpValue, fallback: string) => {
    if (value === JUMP_NEXT) return fallback;
    if (value === JUMP_END) return '회신 끝내기';
    const i = sections.findIndex((s) => s.key === value);
    return `${sectionName(i, sections[i]?.title)}(으)로 이동`;
  };
  return (
    <Stack gap="sm">
      {sections.map((section, si) => (
        <Stack key={section.key} gap="xs">
          {multi && (
            <Text fw={700}>
              {sectionName(si, section.title)}
              {section.description && (
                <Text span size="sm" c="dimmed" fw={400}>
                  {' '}
                  · {section.description}
                </Text>
              )}
            </Text>
          )}
          {questions.map((q, index) =>
            q.sectionKey !== section.key ? null : (
              <Card key={q.key} bg="var(--cl-sunken)" bd="none" p="md" data-testid="question-card">
                <Stack gap={2}>
                  <Text fw={600}>
                    {index + 1}. {q.title}
                  </Text>
                  <Text size="sm" c="dimmed">
                    {QUESTION_TYPE_OPTIONS.find((o) => o.value === q.type)?.label}
                    {q.required ? ' · 필수' : ''}
                  </Text>
                  {q.options.map((o, oi) => (
                    <Text key={oi} size="sm">
                      · {o.label}
                      {q.branching && o.jump !== JUMP_NEXT && (
                        <Text span size="sm" c="dimmed">
                          {' '}
                          → {label(o.jump, '')}
                        </Text>
                      )}
                    </Text>
                  ))}
                </Stack>
              </Card>
            ),
          )}
          {multi && si < sections.length - 1 && (
            <Text size="sm" c="dimmed">
              섹션 {si + 1} 다음: {label(section.jump, '다음 섹션으로 계속')}
            </Text>
          )}
        </Stack>
      ))}
    </Stack>
  );
}
