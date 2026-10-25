import { useInterval } from '@mantine/hooks';
import { useEffect, useState } from 'react';

/** 주어진 시각(밀리초)까지 남은 초(올림). 지났거나 없으면 0. 1초마다 다시 센다. */
export function useSecondsUntil(target: number | undefined): number {
  const [now, setNow] = useState(() => Date.now());
  const interval = useInterval(() => setNow(Date.now()), 1000);
  useEffect(() => {
    if (!target) return;
    setNow(Date.now());
    interval.start();
    return interval.stop;
    // interval 객체는 렌더마다 새로 만들어져 의존성에서 뺀다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [target]);
  if (!target) return 0;
  return Math.max(0, Math.ceil((target - now) / 1000));
}
