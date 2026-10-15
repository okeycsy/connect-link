import { useQuery } from '@tanstack/react-query';

import { api, unwrap } from '../../api/client';
import type { components } from '../../api/schema';

export type ChangeHistory = components['schemas']['ChangeHistoryResponse'];
export type FieldChange = components['schemas']['FieldChange'];
export type HistoryTargetType = ChangeHistory['targetType'];

export const historyKeys = {
  all: ['change-histories'] as const,
  list: (targetType: HistoryTargetType, targetId: number, size: number) =>
    ['change-histories', targetType, targetId, size] as const,
};

/**
 * 대상 하나의 변경 이력(최신순). 대상을 고친 뒤에는 historyKeys.all 을 무효화해야 새 이력이 보인다.
 * size 로 "이전 한 건까지만" 같은 제한을 건다.
 */
export function useChangeHistories(
  targetType: HistoryTargetType,
  targetId: number | null,
  size = 20,
) {
  return useQuery({
    queryKey: historyKeys.list(targetType, targetId ?? 0, size),
    queryFn: async () =>
      unwrap(
        await api.GET('/api/v1/change-histories', {
          params: { query: { targetType, targetId: targetId!, size } },
        }),
      ),
    enabled: targetId != null,
  });
}
