import { Card, Group, Pagination, Stack, Text } from '@mantine/core';
import { useState } from 'react';
import { useParams, useSearchParams } from 'react-router-dom';

import { ChipFilter } from '../../../shared/list/ChipFilter';
import { PageHeader } from '../../../shared/PageHeader';
import { ErrorState } from '../../../shared/state/StateView';
import { useCurrentBusiness } from '../../organization/hooks';
import { SignUpApplicationDrawer } from '../components/review/SignUpApplicationDrawer';
import { SignUpApplicationTable } from '../components/review/SignUpApplicationTable';
import { type SignUpReviewStatus, useSignUpApplications, useSignUpCounts } from '../hooks';

const PAGE_SIZE = 20;

/**
 * 관리 > 가입 승인. 지금 업무 영역(레일)을 신청에 넣은 가입 신청을 상태별로 본다. 기본은 승인 대기이고, 행을 누르면 오른쪽
 * 패널에서 신청 내용을 보고 승인하거나 반려한다. 처리할 수 있는 사람(관리자, 그 업무의 결제원 담당자)은 서버가 정한다.
 */
export function SignUpApprovalPage() {
  const { business: slug = '' } = useParams<{ business: string }>();
  const current = useCurrentBusiness();
  const [status, setStatus] = useState<SignUpReviewStatus>('PENDING_APPROVAL');
  const [page, setPage] = useState(0);
  // 알림·홈 카드에서 ?id={계정} 으로 들어오면 그 신청의 패널을 바로 연다.
  const [searchParams, setSearchParams] = useSearchParams();
  const [selectedId, setSelectedId] = useState<number | null>(() => {
    const id = Number(searchParams.get('id'));
    return Number.isInteger(id) && id > 0 ? id : null;
  });

  const { data, isPending, isError, error, isFetching } = useSignUpApplications({
    business: slug,
    status,
    page,
    size: PAGE_SIZE,
  });
  const { data: counts } = useSignUpCounts(slug);
  const countLabel = (label: string, n: number | undefined) =>
    n === undefined ? label : `${label} ${n}`;

  return (
    <Stack gap="md">
      <PageHeader
        title="가입 승인"
        description={`${current?.label ?? '이 업무'} 담당으로 들어온 가입 신청입니다. 이메일 도메인과 기관이 맞는지 보고 승인하거나 반려합니다. 승인한 기관 담당자는 상세에서 탈퇴 처리할 수 있습니다.`}
      />

      <Card>
        <Group justify="space-between" align="center" mb="sm" gap="sm">
          <ChipFilter<SignUpReviewStatus>
            label="상태"
            value={status}
            onChange={(v) => {
              setStatus(v);
              setPage(0);
            }}
            options={[
              { value: 'PENDING_APPROVAL', label: countLabel('승인 대기', counts?.pending) },
              { value: 'ACTIVE', label: countLabel('승인', counts?.approved) },
              { value: 'REJECTED', label: countLabel('반려', counts?.rejected) },
            ]}
          />
          {data && (
            <Text size="sm" c="dimmed" data-testid="sign-up-count">
              {data.totalElements}건
            </Text>
          )}
        </Group>

        {isError && <ErrorState error={error} homeLink={`/${slug}/home`} />}
        <div style={{ opacity: isFetching && data ? 0.6 : 1, transition: 'opacity 120ms' }}>
          <SignUpApplicationTable
            rows={data?.content}
            loading={isPending}
            status={status}
            selectedId={selectedId}
            currentBusiness={slug}
            onSelect={(a) => setSelectedId(a.id)}
          />
        </div>
        {data && data.totalPages > 1 && (
          <Group justify="center" mt="md">
            <Pagination
              total={data.totalPages}
              value={page + 1}
              onChange={(p) => setPage(p - 1)}
              size="sm"
            />
          </Group>
        )}
      </Card>

      <SignUpApplicationDrawer
        applicationId={selectedId}
        onClose={() => {
          setSelectedId(null);
          if (searchParams.has('id')) setSearchParams({}, { replace: true });
        }}
      />
    </Stack>
  );
}
