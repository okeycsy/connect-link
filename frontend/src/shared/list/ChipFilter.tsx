import { Chip, Group, Text } from '@mantine/core';

export interface ChipOption<V extends string> {
  value: V;
  label: string;
}

interface Props<V extends string> {
  /** 칩 묶음의 이름. 화면에 작게 보이고 접근성 이름으로도 쓴다. */
  label: string;
  value: V;
  options: ChipOption<V>[];
  onChange: (value: V) => void;
}

/** 목록 화면의 필터 칩 묶음(하나만 선택). 구분, 상태, 업무 같은 짧은 선택지에 쓴다. */
export function ChipFilter<V extends string>({ label, value, options, onChange }: Props<V>) {
  return (
    <Group gap={6} wrap="nowrap" role="group" aria-label={label}>
      <Text size="xs" c="dimmed" fw={600}>
        {label}
      </Text>
      <Chip.Group value={value} onChange={(v) => onChange(v as V)}>
        <Group gap={4} wrap="nowrap">
          {options.map((o) => (
            <Chip key={o.value} value={o.value} size="xs">
              {o.label}
            </Chip>
          ))}
        </Group>
      </Chip.Group>
    </Group>
  );
}
