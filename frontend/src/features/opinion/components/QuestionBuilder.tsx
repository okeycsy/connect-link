import {
  ActionIcon,
  Button,
  Card,
  Checkbox,
  Group,
  Radio,
  Select,
  Stack,
  Switch,
  Text,
  TextInput,
  Tooltip,
} from '@mantine/core';
import type { UseFormReturnType } from '@mantine/form';
import { IconArrowDown, IconArrowUp, IconPlus, IconTrash, IconX } from '@tabler/icons-react';

import type { OpinionQuestionType } from '../hooks';
import { isChoice, QUESTION_TYPE_OPTIONS, type QuestionDraft } from '../questions';

interface Props<V extends { questions: QuestionDraft[] }> {
  form: UseFormReturnType<V>;
  /** 회신을 받기 시작한 요청이면 질문을 고치지 못한다(서버도 막는다). */
  locked: boolean;
  newKey: () => string;
}

/**
 * 의견 요청의 질문 만들기. 구글 폼처럼 질문마다 유형(객관식 하나 고르기, 체크박스 여러 개 고르기, 주관식)과 필수 여부를 정하고, 객관식이면
 * 선택지를 적는다. 기관 회신은 이 질문들에 대한 답뿐이다.
 */
export function QuestionBuilder<V extends { questions: QuestionDraft[] }>({
  form,
  locked,
  newKey,
}: Props<V>) {
  // useForm 의 경로 함수는 값 모양을 모르므로 질문 목록 경로만 이 이름으로 쓴다.
  const f = form as unknown as UseFormReturnType<{ questions: QuestionDraft[] }>;
  const questions = f.values.questions;

  const add = () =>
    f.insertListItem('questions', {
      key: newKey(),
      type: 'SINGLE_CHOICE',
      title: '',
      required: false,
      options: ['', ''],
    } satisfies QuestionDraft);

  if (locked && questions.length === 0) {
    return (
      <Text size="sm" c="dimmed">
        넣은 질문이 없습니다.
      </Text>
    );
  }

  return (
    <Stack gap="sm">
      {questions.length === 0 && (
        <Text size="sm" c="dimmed">
          넣은 질문이 없습니다. 회신 요청을 시작하려면 질문이 하나 이상 있어야 합니다.
        </Text>
      )}
      {questions.map((q, index) => (
        <Card key={q.key} bg="var(--cl-sunken)" bd="none" p="md" data-testid="question-card">
          <Stack gap="sm">
            <Group gap="sm" align="flex-start" wrap="nowrap">
              <Text fw={700} mt={locked ? 0 : 6} w={24} style={{ flexShrink: 0 }}>
                {index + 1}.
              </Text>
              {locked ? (
                <Stack gap={2} flex={1}>
                  <Text fw={600}>{q.title}</Text>
                  <Text size="sm" c="dimmed">
                    {QUESTION_TYPE_OPTIONS.find((o) => o.value === q.type)?.label}
                    {q.required ? ' · 필수' : ''}
                  </Text>
                </Stack>
              ) : (
                <>
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
                          [...q.options, '', ''].slice(0, 2),
                        );
                      }
                    }}
                  />
                </>
              )}
            </Group>

            {isChoice(q.type) && (
              <Stack gap={6} pl={32}>
                {q.options.map((label, optionIndex) => (
                  <Group key={optionIndex} gap="sm" wrap="nowrap">
                    {q.type === 'SINGLE_CHOICE' ? (
                      <Radio checked={false} readOnly tabIndex={-1} aria-hidden />
                    ) : (
                      <Checkbox checked={false} readOnly tabIndex={-1} aria-hidden />
                    )}
                    {locked ? (
                      <Text size="sm">{label}</Text>
                    ) : (
                      <>
                        <TextInput
                          aria-label={`${index + 1}번 질문 선택지 ${optionIndex + 1}`}
                          placeholder={`선택지 ${optionIndex + 1}`}
                          flex={1}
                          size="sm"
                          maxLength={200}
                          {...f.getInputProps(`questions.${index}.options.${optionIndex}`)}
                        />
                        <ActionIcon
                          variant="subtle"
                          color="gray"
                          aria-label={`${index + 1}번 질문 선택지 ${optionIndex + 1} 빼기`}
                          disabled={q.options.length <= 2}
                          onClick={() =>
                            f.removeListItem(`questions.${index}.options`, optionIndex)
                          }
                        >
                          <IconX size={16} stroke={1.8} />
                        </ActionIcon>
                      </>
                    )}
                  </Group>
                ))}
                {!locked && (
                  <Group gap="sm">
                    <Button
                      size="xs"
                      variant="subtle"
                      leftSection={<IconPlus size={14} stroke={1.8} />}
                      disabled={q.options.length >= 20}
                      onClick={() => f.insertListItem(`questions.${index}.options`, '')}
                    >
                      선택지 추가
                    </Button>
                  </Group>
                )}
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

            {!locked && (
              <Group justify="flex-end" gap="xs">
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
                    disabled={index === 0}
                    onClick={() => f.reorderListItem('questions', { from: index, to: index - 1 })}
                  >
                    <IconArrowUp size={16} stroke={1.8} />
                  </ActionIcon>
                </Tooltip>
                <Tooltip label="아래로">
                  <ActionIcon
                    variant="subtle"
                    color="gray"
                    aria-label={`${index + 1}번 질문 아래로`}
                    disabled={index === questions.length - 1}
                    onClick={() => f.reorderListItem('questions', { from: index, to: index + 1 })}
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
            )}
          </Stack>
        </Card>
      ))}
      {!locked && (
        <Group>
          <Button
            variant="light"
            leftSection={<IconPlus size={16} stroke={1.8} />}
            disabled={questions.length >= 30}
            onClick={add}
          >
            질문 추가
          </Button>
        </Group>
      )}
    </Stack>
  );
}
