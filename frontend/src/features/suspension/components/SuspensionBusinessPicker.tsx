import {
  Anchor,
  Badge,
  Checkbox,
  CloseButton,
  Group,
  Input,
  Paper,
  ScrollArea,
  SimpleGrid,
  Stack,
  Text,
  TextInput,
} from '@mantine/core';
import { IconSearch } from '@tabler/icons-react';
import { type ReactNode, useState } from 'react';

export interface BusinessOption<T extends string> {
  value: T;
  label: string;
}

interface Props<T extends string> {
  options: BusinessOption<T>[];
  value: T[];
  onChange: (value: T[]) => void;
  disabled?: boolean;
  error?: ReactNode;
  description?: ReactNode;
}

/** 이 수보다 많으면 검색 칸을 보인다. */
const SEARCH_FROM = 8;

/**
 * 일시중단 대상 업무 고르기. 업무가 수십 개여도 고르기 쉽게 검색, 보이는 것 모두 고르기·빼기, 고른 업무 목록(눌러 빼기)을 둔다. 목록은 업무 순서
 * 그대로이고 높이를 넘으면 안에서 스크롤한다.
 */
export function SuspensionBusinessPicker<T extends string>({
  options,
  value,
  onChange,
  disabled,
  error,
  description,
}: Props<T>) {
  const [query, setQuery] = useState('');
  const keyword = query.trim().toLowerCase();
  const visible = keyword
    ? options.filter((o) => o.label.toLowerCase().includes(keyword))
    : options;
  const selected = options.filter((o) => value.includes(o.value));
  const allVisibleSelected = visible.length > 0 && visible.every((o) => value.includes(o.value));

  const toggleVisible = () => {
    const codes = visible.map((o) => o.value);
    onChange(
      allVisibleSelected
        ? value.filter((v) => !codes.includes(v))
        : [...value, ...codes.filter((c) => !value.includes(c))],
    );
  };
  const remove = (code: T) => onChange(value.filter((v) => v !== code));

  return (
    <Input.Wrapper
      label="대상 업무"
      description={description}
      required
      error={error}
      data-testid="suspension-businesses"
    >
      <Paper withBorder radius="md" p="sm" mt={6}>
        <Stack gap="xs">
          <Group justify="space-between" gap="xs" wrap="nowrap">
            {options.length > SEARCH_FROM ? (
              <TextInput
                placeholder="업무 이름으로 찾기"
                aria-label="대상 업무 찾기"
                leftSection={<IconSearch size={16} stroke={1.8} />}
                rightSection={
                  query && (
                    <CloseButton size="sm" aria-label="찾기 지우기" onClick={() => setQuery('')} />
                  )
                }
                value={query}
                onChange={(e) => setQuery(e.currentTarget.value)}
                disabled={disabled}
                style={{ flex: 1 }}
              />
            ) : (
              <Text size="sm" c="dimmed">
                {options.length}개 업무
              </Text>
            )}
            <Anchor
              component="button"
              type="button"
              size="sm"
              onClick={toggleVisible}
              disabled={disabled || visible.length === 0}
            >
              {allVisibleSelected
                ? keyword
                  ? '찾은 업무 모두 빼기'
                  : '모두 빼기'
                : keyword
                  ? '찾은 업무 모두 고르기'
                  : '모두 고르기'}
            </Anchor>
          </Group>

          <ScrollArea.Autosize mah={220} type="auto" offsetScrollbars>
            {visible.length === 0 ? (
              <Text size="sm" c="dimmed" py="xs">
                {options.length === 0 ? '고를 업무가 없습니다.' : '찾는 업무가 없습니다.'}
              </Text>
            ) : (
              <Checkbox.Group value={value} onChange={(v) => onChange(v as T[])}>
                <SimpleGrid cols={{ base: 1, xs: 2, md: 3 }} spacing="xs" verticalSpacing={8}>
                  {visible.map((o) => (
                    <Checkbox key={o.value} value={o.value} label={o.label} disabled={disabled} />
                  ))}
                </SimpleGrid>
              </Checkbox.Group>
            )}
          </ScrollArea.Autosize>

          <Stack
            gap={6}
            pt="xs"
            style={{ borderTop: '1px solid var(--mantine-color-default-border)' }}
          >
            <Text size="sm" fw={600} data-testid="suspension-businesses-count">
              고른 업무 {selected.length}개
            </Text>
            {selected.length === 0 ? (
              <Text size="sm" c="dimmed">
                아직 고른 업무가 없습니다.
              </Text>
            ) : (
              <Group gap={6} aria-label="고른 업무">
                {selected.map((o) => (
                  <Badge
                    key={o.value}
                    variant="light"
                    size="lg"
                    radius="sm"
                    tt="none"
                    fw={500}
                    rightSection={
                      !disabled && (
                        <CloseButton
                          size="xs"
                          variant="transparent"
                          aria-label={`${o.label} 빼기`}
                          onClick={() => remove(o.value)}
                        />
                      )
                    }
                  >
                    {o.label}
                  </Badge>
                ))}
              </Group>
            )}
          </Stack>
        </Stack>
      </Paper>
    </Input.Wrapper>
  );
}
