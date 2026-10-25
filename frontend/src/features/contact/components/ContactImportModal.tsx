import {
  Alert,
  Anchor,
  Badge,
  Button,
  FileInput,
  Group,
  List,
  Modal,
  ScrollArea,
  Stack,
  Table,
  Text,
} from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconFileSpreadsheet } from '@tabler/icons-react';
import { useState } from 'react';

import { fieldErrorsOf } from '../../../api/client';
import {
  type ContactImportResponse,
  useDownloadContactImportTemplate,
  useImportContacts,
} from '../hooks';

interface Props {
  /** 업무 영역 slug. */
  business: string;
  businessLabel: string;
  opened: boolean;
  onClose: () => void;
}

const STATUS_COLOR = { CREATE: 'sky', SKIP: 'gray', ERROR: 'red' } as const;

/**
 * 연락처 > 엑셀로 일괄 등록(결제원 담당자, 내부망). 양식을 내려받아 채운 파일을 고르면 먼저 검사해 줄마다 등록·건너뜀·오류를 보여 주고, 오류가
 * 없을 때만 등록한다. 검사와 등록의 규칙은 서버가 정한다.
 */
export function ContactImportModal({ business, businessLabel, opened, onClose }: Props) {
  const template = useDownloadContactImportTemplate();
  const importer = useImportContacts();
  const [file, setFile] = useState<File | null>(null);
  const [checked, setChecked] = useState<ContactImportResponse>();
  const [fileError, setFileError] = useState<string>();

  const close = () => {
    setFile(null);
    setChecked(undefined);
    setFileError(undefined);
    importer.reset();
    onClose();
  };

  const run = (dryRun: boolean) => {
    if (!file) return;
    importer.mutate(
      { business, file, dryRun },
      {
        onSuccess: (result) => {
          setChecked(result);
          setFileError(undefined);
          if (result.saved) {
            notifications.show({
              color: 'sky',
              message: `담당자 ${result.createCount}명을 등록했습니다.`,
            });
            close();
          }
        },
        onError: (error) => {
          setChecked(undefined);
          setFileError(fieldErrorsOf(error).file ?? error.message);
        },
      },
    );
  };

  const canSave = checked && checked.errorCount === 0 && checked.createCount > 0;

  return (
    <Modal opened={opened} onClose={close} title="엑셀로 일괄 등록" size="xl" centered>
      <Stack gap="md">
        <List size="sm" spacing={4} c="var(--cl-ink-soft)">
          <List.Item>
            <Anchor
              component="button"
              size="sm"
              fw={600}
              onClick={() =>
                template.mutate(business, {
                  onError: (e) => notifications.show({ color: 'red', message: e.message }),
                })
              }
            >
              {businessLabel} 일괄 등록 양식 내려받기
            </Anchor>
            로 받은 파일에 한 줄에 한 명씩 적어 올립니다. 기관은 3자리 기관 코드로 적습니다.
          </List.Item>
          <List.Item>먼저 검사하고, 오류가 하나도 없을 때만 등록합니다.</List.Item>
          <List.Item>같은 기관·업무에 같은 회사 이메일이 이미 있으면 그 줄은 건너뜁니다.</List.Item>
        </List>

        <FileInput
          label="채운 양식 파일"
          placeholder="xlsx 파일 고르기"
          accept=".xlsx"
          leftSection={<IconFileSpreadsheet size={16} />}
          value={file}
          error={fileError}
          clearable
          onChange={(next) => {
            setFile(next);
            setChecked(undefined);
            setFileError(undefined);
          }}
        />

        {checked && (
          <Stack gap="xs" data-testid="contact-import-result">
            <Group gap="xs">
              <Badge color="sky">등록 {checked.createCount}</Badge>
              <Badge color="gray">건너뜀 {checked.skipCount}</Badge>
              <Badge color={checked.errorCount > 0 ? 'red' : 'gray'}>
                오류 {checked.errorCount}
              </Badge>
            </Group>
            {checked.errorCount > 0 && (
              <Alert variant="light" color="red">
                오류가 있는 줄을 고쳐 다시 올려 주세요. 지금은 아무것도 등록하지 않았습니다.
              </Alert>
            )}
            <ScrollArea.Autosize mah={320} type="auto">
              <Table striped withTableBorder fz="sm">
                <Table.Thead>
                  <Table.Tr>
                    <Table.Th w={56}>줄</Table.Th>
                    <Table.Th>기관</Table.Th>
                    <Table.Th>이름</Table.Th>
                    <Table.Th>회사 이메일</Table.Th>
                    <Table.Th w={80}>결과</Table.Th>
                    <Table.Th>까닭</Table.Th>
                  </Table.Tr>
                </Table.Thead>
                <Table.Tbody>
                  {checked.rows.map((row) => (
                    <Table.Tr key={row.rowNumber}>
                      <Table.Td>{row.rowNumber}</Table.Td>
                      <Table.Td>{row.organizationName ?? row.organizationCode ?? '-'}</Table.Td>
                      <Table.Td>{row.name ?? '-'}</Table.Td>
                      <Table.Td style={{ overflowWrap: 'anywhere' }}>{row.email ?? '-'}</Table.Td>
                      <Table.Td>
                        <Badge color={STATUS_COLOR[row.status]} variant="light">
                          {row.statusLabel}
                        </Badge>
                      </Table.Td>
                      <Table.Td>
                        <Text size="xs">{row.message ?? ''}</Text>
                      </Table.Td>
                    </Table.Tr>
                  ))}
                </Table.Tbody>
              </Table>
            </ScrollArea.Autosize>
          </Stack>
        )}

        <Group justify="flex-end" gap="xs">
          <Button variant="default" onClick={close}>
            닫기
          </Button>
          <Button
            variant="light"
            disabled={!file}
            loading={importer.isPending && importer.variables?.dryRun}
            onClick={() => run(true)}
          >
            검사
          </Button>
          <Button
            disabled={!canSave}
            loading={importer.isPending && importer.variables?.dryRun === false}
            onClick={() => run(false)}
          >
            {canSave ? `${checked.createCount}명 등록` : '등록'}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
