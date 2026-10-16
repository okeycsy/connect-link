import { Outlet } from 'react-router-dom';

import { canUseAdminMenu, useMe } from '../../features/account/hooks';
import { StateView } from '../../shared/state/StateView';

interface Props {
  /** 관리자만 보는 항목(기준정보 등). 없으면 관리 메뉴를 쓰는 결제원 담당자도 들어온다. */
  adminOnly?: boolean;
}

/**
 * 관리 그룹(/{업무}/admin/…)을 지킨다. 관리 메뉴는 관리자와 결제원 담당자가, adminOnly 항목은 관리자만 본다. 보조 메뉴에서도
 * 숨기지만 주소로 들어와도 막고, 데이터는 서버가 따로 막는다.
 */
export function AdminGuard({ adminOnly = false }: Props) {
  const { data: me } = useMe();
  if (!me || !canUseAdminMenu(me)) {
    return (
      <StateView
        kind="forbidden"
        description="관리 메뉴는 결제원 담당자와 관리자만 볼 수 있습니다."
      />
    );
  }
  if (adminOnly && !me.admin) {
    return <StateView kind="forbidden" description="이 관리 항목은 관리자만 볼 수 있습니다." />;
  }
  return <Outlet />;
}
