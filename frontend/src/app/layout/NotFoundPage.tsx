import { Button } from '@mantine/core';
import { Link, useParams } from 'react-router-dom';

import { StateView } from '../../shared/state/StateView';
import { DEFAULT_BUSINESS_SLUG } from '../navigation';

/** 업무 영역은 맞지만 기능 주소가 없을 때. */
export function NotFoundPage() {
  const { business } = useParams<{ business: string }>();
  return (
    <StateView
      kind="notFound"
      action={
        <Button
          component={Link}
          to={`/${business ?? DEFAULT_BUSINESS_SLUG}/home`}
          variant="light"
          mt="xs"
        >
          홈으로
        </Button>
      }
    />
  );
}
