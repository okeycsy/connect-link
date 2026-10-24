import { Alert, Badge, Group, Skeleton, Stack, Tabs } from '@mantine/core';
import { IconHistory, IconInfoCircle } from '@tabler/icons-react';

import { ChangeHistoryList } from '../../history/components/ChangeHistoryList';
import { DetailDrawer } from '../../../shared/list/DetailDrawer';
import { OrgMonogram } from '../../../shared/OrgMonogram';
import { useOrganization, type BusinessCode, type OrganizationDetail } from '../hooks';
import { OrganizationForm } from './OrganizationForm';

interface Props {
  /** 보고 있는 기관. null 이면 닫힘(creating 이 아닐 때). */
  organizationId: number | null;
  creating: boolean;
  defaultBusiness?: BusinessCode;
  onClose: () => void;
  onSaved: (saved: OrganizationDetail) => void;
}

/** 기관 상세·수정·등록 패널. 목록 오른쪽에 열린다. */
export function OrganizationDrawer({
  organizationId,
  creating,
  defaultBusiness,
  onClose,
  onSaved,
}: Props) {
  const { data, isPending, isError } = useOrganization(creating ? null : organizationId);
  const opened = creating || organizationId != null;

  if (creating) {
    return (
      <DetailDrawer
        opened
        onClose={onClose}
        title="기관 등록"
        subtitle="새 참가기관이나 이용기관을 더합니다."
      >
        <OrganizationForm defaultBusiness={defaultBusiness} onSaved={onSaved} onCancel={onClose} />
      </DetailDrawer>
    );
  }

  return (
    <DetailDrawer
      opened={opened}
      onClose={onClose}
      title={
        data ? (
          <Group gap="sm" wrap="nowrap">
            <OrgMonogram name={data.name} type={data.type} size={36} code={data.code} />
            <span>{data.name}</span>
            {!data.active && (
              <Badge variant="outline" color="gray">
                중지
              </Badge>
            )}
          </Group>
        ) : (
          '기관 상세'
        )
      }
      subtitle={
        data ? [data.code, data.typeLabel, data.sectorLabel].filter(Boolean).join(' · ') : undefined
      }
    >
      {isError && <Alert color="red">기관 정보를 불러오지 못했습니다.</Alert>}
      {isPending || !data ? (
        <Stack gap="sm">
          <Skeleton h={56} />
          <Skeleton h={56} />
          <Skeleton h={72} />
        </Stack>
      ) : (
        <Tabs defaultValue="info" keepMounted={false}>
          <Tabs.List mb="md">
            <Tabs.Tab value="info" leftSection={<IconInfoCircle size={14} />}>
              정보
            </Tabs.Tab>
            <Tabs.Tab value="history" leftSection={<IconHistory size={14} />}>
              변경 이력
            </Tabs.Tab>
          </Tabs.List>
          <Tabs.Panel value="info">
            {/* 다른 기관을 고르거나 저장돼 값이 바뀌면 폼을 새로 만든다. */}
            <OrganizationForm
              key={`${data.id}-${data.updatedAt}`}
              organization={data}
              onSaved={onSaved}
              onCancel={onClose}
            />
          </Tabs.Panel>
          <Tabs.Panel value="history">
            <ChangeHistoryList targetType="ORGANIZATION" targetId={data.id} />
          </Tabs.Panel>
        </Tabs>
      )}
    </DetailDrawer>
  );
}
