import { Alert, Badge, Button, Divider, Group, Skeleton, Stack, Text } from '@mantine/core';
import { notifications } from '@mantine/notifications';
import { IconAlertTriangle, IconCircleCheck, IconUserOff } from '@tabler/icons-react';
import { useState, type ReactNode } from 'react';

import { formatDateTime, formatPhone } from '../../../../shared/format';
import { DetailDrawer } from '../../../../shared/list/DetailDrawer';
import { OrgMonogram } from '../../../../shared/OrgMonogram';
import { type SignUpApplicationResponse, useSignUpApplication } from '../../hooks';
import { SignUpStatusBadge } from './SignUpStatusBadge';
import { ApproveSignUpModal, RejectSignUpModal } from './SignUpReviewModals';
import { WithdrawAccountModal } from './WithdrawAccountModal';

interface Props {
  /** 보고 있는 신청. null 이면 닫힘. */
  applicationId: number | null;
  onClose: () => void;
}

/**
 * 가입 신청 상세 패널. 승인 대기 신청이면 아래에 반려·승인 버튼이 있고, 승인한(사용 중) 참가기관 담당자면 탈퇴 처리 버튼이 있다. 모두 확인 창을 한 번
 * 거친다.
 */
export function SignUpApplicationDrawer({ applicationId, onClose }: Props) {
  const { data, isPending, isError } = useSignUpApplication(applicationId);
  const [confirm, setConfirm] = useState<'approve' | 'reject' | 'withdraw' | null>(null);

  const done = (message: string) => {
    setConfirm(null);
    onClose();
    notifications.show({ message, color: 'clay' });
  };

  return (
    <>
      <DetailDrawer
        opened={applicationId != null}
        onClose={onClose}
        title={
          data ? (
            <Group gap="sm" wrap="nowrap">
              <OrgMonogram
                name={data.organizationName}
                type={data.organizationType}
                size={36}
                code={data.organizationCode}
              />
              <span>{data.name}</span>
              <SignUpStatusBadge application={data} />
            </Group>
          ) : (
            '가입 신청'
          )
        }
        subtitle={data ? `${data.organizationName} · ${data.organizationTypeLabel}` : undefined}
        footer={
          data?.status === 'PENDING_APPROVAL' ? (
            <>
              <Button color="red" variant="light" onClick={() => setConfirm('reject')}>
                반려
              </Button>
              <Button onClick={() => setConfirm('approve')}>승인</Button>
            </>
          ) : (
            data?.status === 'ACTIVE' &&
            data.organizationType === 'FINANCE_ORGANIZATION' && (
              <Button
                color="red"
                variant="light"
                leftSection={<IconUserOff size={16} />}
                onClick={() => setConfirm('withdraw')}
              >
                탈퇴 처리
              </Button>
            )
          )
        }
      >
        {isError && <Alert color="red">가입 신청을 불러오지 못했습니다.</Alert>}
        {isPending || !data ? (
          <Stack gap="sm">
            <Skeleton h={56} />
            <Skeleton h={56} />
            <Skeleton h={72} />
          </Stack>
        ) : (
          <ApplicationDetail application={data} />
        )}
      </DetailDrawer>

      {data && (
        <>
          <ApproveSignUpModal
            application={data}
            opened={confirm === 'approve'}
            onClose={() => setConfirm(null)}
            onDone={(r) => done(`${r.name} 님의 가입을 승인했습니다. 승인 메일을 보냈습니다.`)}
          />
          <RejectSignUpModal
            application={data}
            opened={confirm === 'reject'}
            onClose={() => setConfirm(null)}
            onDone={(r) => done(`${r.name} 님의 가입 신청을 반려했습니다. 반려 메일을 보냈습니다.`)}
          />
          <WithdrawAccountModal
            application={data}
            opened={confirm === 'withdraw'}
            onClose={() => setConfirm(null)}
            onDone={() => done(`${data.name} 님을 탈퇴 처리했습니다. 탈퇴 메일을 보냈습니다.`)}
          />
        </>
      )}
    </>
  );
}

function ApplicationDetail({ application: a }: { application: SignUpApplicationResponse }) {
  const emailDomain = a.email.slice(a.email.indexOf('@') + 1);
  return (
    <Stack gap="md" data-testid="sign-up-detail">
      {a.emailDomainMatches ? (
        <Alert variant="light" color="sky" icon={<IconCircleCheck size={18} />}>
          이메일 도메인({emailDomain})이 {a.organizationName}에 등록된 도메인과 같습니다.
        </Alert>
      ) : (
        <Alert variant="light" color="amber" icon={<IconAlertTriangle size={18} />}>
          {a.organizationEmailDomain
            ? `이메일 도메인(${emailDomain})이 ${a.organizationName}에 등록된 도메인(${a.organizationEmailDomain})과 다릅니다.`
            : `${a.organizationName}에 등록된 이메일 도메인이 없습니다.`}{' '}
          소속을 확인하고 처리해 주세요.
        </Alert>
      )}

      <Stack gap={8}>
        <Field label="회사 이메일" value={a.email} mono />
        <Field label="기관" value={`${a.organizationName} (${a.organizationTypeLabel})`} />
        <Field
          label="담당 업무"
          value={
            <Stack gap={4}>
              {a.businesses.map((b) => (
                <Group key={b.code} gap={6} wrap="nowrap">
                  <Badge size="sm" variant="light" color="clay">
                    {b.label}
                  </Badge>
                  {b.subBusiness && (
                    <Text size="sm" c="dimmed">
                      {b.subBusiness}
                    </Text>
                  )}
                </Group>
              ))}
            </Stack>
          }
        />
        <Field label="직무" value={a.jobRoleLabel} />
        <Field label="부서 · 팀 · 직급" value={`${a.department} · ${a.team} · ${a.jobTitle}`} />
        <Field label="내선" value={formatPhone(a.extensionPhone)} mono />
        <Field label="휴대폰" value={a.mobilePhone ? formatPhone(a.mobilePhone) : '-'} mono />
        <Field label="신청 일시" value={formatDateTime(a.appliedAt)} />
      </Stack>

      {a.reviewedAt && (
        <>
          <Divider />
          <Stack gap={8}>
            <Field
              label={a.status === 'REJECTED' ? '반려' : '승인'}
              value={`${a.reviewerName ?? ''} · ${formatDateTime(a.reviewedAt)}`}
            />
            {a.rejectionReason && <Field label="반려 사유" value={a.rejectionReason} />}
          </Stack>
        </>
      )}
    </Stack>
  );
}

function Field({
  label,
  value,
  mono = false,
}: {
  label: string;
  value: ReactNode;
  mono?: boolean;
}) {
  return (
    <Group gap="sm" wrap="nowrap" align="flex-start">
      <Text size="sm" c="dimmed" w={110} style={{ flex: '0 0 auto' }}>
        {label}
      </Text>
      {typeof value === 'string' ? (
        <Text size="md" ff={mono ? 'monospace' : undefined} style={{ wordBreak: 'break-all' }}>
          {value}
        </Text>
      ) : (
        value
      )}
    </Group>
  );
}
