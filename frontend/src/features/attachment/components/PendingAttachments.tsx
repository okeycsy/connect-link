import { ActionIcon, Button, FileButton, Group, Stack, Text, ThemeIcon } from '@mantine/core';
import { IconFile, IconTrash, IconUpload } from '@tabler/icons-react';

import { formatBytes } from '../../../shared/format';

/**
 * 저장 전에 고른 첨부파일. 대상(의견 요청, 일시중단)은 저장해야 번호가 생기므로 저장한 뒤 이 순서대로 올린다(올리기는 쓰는 화면이 한다).
 */
export function PendingAttachments({
  files,
  onChange,
  pickLabel = '첨부자료 고르기',
}: {
  files: File[];
  onChange: (files: File[]) => void;
  /** 고르기 버튼 글자. */
  pickLabel?: string;
}) {
  return (
    <Stack gap="sm" data-testid="pending-attachments">
      <Group justify="space-between" align="center">
        <Text size="sm" c="dimmed">
          {files.length > 0 ? `${files.length}개, 저장하면 올립니다.` : '저장하면 함께 올립니다.'}
        </Text>
        <FileButton multiple onChange={(picked) => onChange([...files, ...picked])}>
          {(props) => (
            <Button {...props} size="xs" variant="light" leftSection={<IconUpload size={14} />}>
              {pickLabel}
            </Button>
          )}
        </FileButton>
      </Group>
      {files.length > 0 && (
        <Stack gap={6} role="list" aria-label="올릴 파일">
          {files.map((file, index) => (
            <Group
              key={`${file.name}-${index}`}
              gap="sm"
              wrap="nowrap"
              role="listitem"
              px="sm"
              py={6}
              style={{
                border: '1px solid var(--mantine-color-default-border)',
                borderRadius: 'var(--mantine-radius-sm)',
              }}
            >
              <ThemeIcon variant="light" color="clay" size={32} radius="sm">
                <IconFile size={18} stroke={1.6} />
              </ThemeIcon>
              <Stack gap={0} style={{ flex: 1, minWidth: 0 }}>
                <Text size="sm" fw={600} truncate="end">
                  {file.name}
                </Text>
                <Text size="xs" c="dimmed">
                  {formatBytes(file.size)}
                </Text>
              </Stack>
              <ActionIcon
                variant="subtle"
                color="gray"
                aria-label={`${file.name} 빼기`}
                onClick={() => onChange(files.filter((_, i) => i !== index))}
              >
                <IconTrash size={16} />
              </ActionIcon>
            </Group>
          ))}
        </Stack>
      )}
    </Stack>
  );
}
