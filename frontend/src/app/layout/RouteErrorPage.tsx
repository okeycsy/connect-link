import { Button } from '@mantine/core';
import { useRouteError } from 'react-router-dom';

import { StateView } from '../../shared/state/StateView';

/** 화면을 그리다 난 오류(렌더 오류 등). 라우터의 errorElement 로 걸린다. */
export function RouteErrorPage() {
  const error = useRouteError();
  const message = error instanceof Error ? error.message : undefined;
  return (
    <StateView
      kind="error"
      description={message ? `오류: ${message}` : undefined}
      action={
        <Button variant="light" mt="xs" onClick={() => window.location.reload()}>
          다시 불러오기
        </Button>
      }
    />
  );
}
