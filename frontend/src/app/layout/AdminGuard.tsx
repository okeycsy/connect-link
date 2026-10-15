import { Outlet } from 'react-router-dom';

import { useMe } from '../../features/account/hooks';
import { StateView } from '../../shared/state/StateView';

/** 관리 그룹(/{업무}/admin/…)은 관리자만 본다. 보조 메뉴에서도 숨기지만 주소로 들어와도 막고, 데이터는 서버가 따로 막는다. */
export function AdminGuard() {
  const { data: me } = useMe();
  if (!me?.admin) {
    return <StateView kind="forbidden" description="관리 메뉴는 관리자만 볼 수 있습니다." />;
  }
  return <Outlet />;
}
