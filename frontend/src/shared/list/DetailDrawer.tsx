import { Drawer, Group, Stack, Text } from '@mantine/core';
import type { ReactNode } from 'react';

interface Props {
  opened: boolean;
  onClose: () => void;
  title: ReactNode;
  /** 제목 아래 작은 보조 설명(코드, 구분 등). */
  subtitle?: ReactNode;
  children: ReactNode;
  /** 아래쪽에 고정되는 버튼 줄. */
  footer?: ReactNode;
}

/** 목록 화면의 오른쪽 상세 패널. 행을 누르면 열리고 목록은 그대로 남는다. 태블릿 폭에서는 화면 대부분을 덮는다. */
export function DetailDrawer({ opened, onClose, title, subtitle, children, footer }: Props) {
  return (
    <Drawer
      opened={opened}
      onClose={onClose}
      position="right"
      size={480}
      padding="lg"
      title={
        <div>
          <Text fw={700} size="xl" component="div">
            {title}
          </Text>
          {subtitle && (
            <Text size="sm" c="dimmed" component="div">
              {subtitle}
            </Text>
          )}
        </div>
      }
      overlayProps={{ backgroundOpacity: 0.25, blur: 2 }}
      styles={{ content: { display: 'flex', flexDirection: 'column' }, body: { flex: 1 } }}
    >
      <Stack justify="space-between" h="100%" gap="lg">
        <div>{children}</div>
        {footer && (
          <Group justify="flex-end" gap="xs">
            {footer}
          </Group>
        )}
      </Stack>
    </Drawer>
  );
}
