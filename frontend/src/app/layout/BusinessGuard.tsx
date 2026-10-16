import { Center, Loader } from '@mantine/core';
import { Navigate, Outlet, useParams } from 'react-router-dom';

import { canViewBusiness, homeBusinessSlugOf, useMe } from '../../features/account/hooks';
import { useBusinesses } from '../../features/organization/hooks';
import { StateView } from '../../shared/state/StateView';

/**
 * 주소의 업무 영역이 있는 업무이고 사용자가 볼 수 있는 업무인지 확인한다. 아니면 사용자의 첫 업무 홈으로 보낸다.
 * 볼 수 있는 업무가 하나도 없는 계정이면 권한 없음 안내를 보인다. 셸이 로그인 사용자를 읽은 뒤에만 그려진다.
 */
export function BusinessGuard() {
  const { business } = useParams<{ business: string }>();
  const { data: businesses, isPending } = useBusinesses();
  const { data: me } = useMe();

  if (isPending || !me) {
    return (
      <Center h="100%" mih={240}>
        <Loader size="sm" />
      </Center>
    );
  }

  const known = businesses?.some((b) => b.slug === business) ?? false;
  if (known && canViewBusiness(me, business)) {
    return <Outlet />;
  }
  const home = homeBusinessSlugOf(me);
  if (!canViewBusiness(me, home)) {
    return (
      <StateView
        kind="forbidden"
        description="이 계정에는 담당 업무가 없습니다. 결제원 담당자에게 문의해 주세요."
      />
    );
  }
  return <Navigate to={`/${home}/home`} replace />;
}
