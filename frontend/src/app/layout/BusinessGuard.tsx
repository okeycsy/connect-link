import { Center, Loader } from '@mantine/core';
import { Navigate, Outlet, useParams } from 'react-router-dom';

import { useBusinesses } from '../../features/organization/hooks';
import { DEFAULT_BUSINESS_SLUG } from '../navigation';

/** 주소의 업무 영역 slug 가 서버 목록에 있는지 확인한다. 없으면 기본 업무로 보낸다. */
export function BusinessGuard() {
  const { business } = useParams<{ business: string }>();
  const { data: businesses, isPending } = useBusinesses();

  if (isPending) {
    return (
      <Center h="100%" mih={240}>
        <Loader size="sm" />
      </Center>
    );
  }

  const known = businesses?.some((b) => b.slug === business) ?? false;
  if (!known) {
    return <Navigate to={`/${DEFAULT_BUSINESS_SLUG}/home`} replace />;
  }
  return <Outlet />;
}
