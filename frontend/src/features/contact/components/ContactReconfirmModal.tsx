import { Anchor, Badge, Box, Button, Group, Modal, Stack, Text } from '@mantine/core';
import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

import { formatDate } from '../../../shared/format';
import { useBusinesses } from '../../organization/hooks';
import { useConfirmContact, useReconfirmDue } from '../hooks';
import classes from './ContactTable.module.css';

const SHOWN_KEY = 'contact.reconfirmShown';

function shownFor(email: string): boolean {
  try {
    return window.sessionStorage.getItem(SHOWN_KEY) === email;
  } catch {
    return false;
  }
}

function markShown(email: string) {
  try {
    window.sessionStorage.setItem(SHOWN_KEY, email);
  } catch {
    // 저장소를 못 쓰면 다음 화면 이동 때 다시 뜰 수 있다.
  }
}

/**
 * 로그인 뒤 최근 확인일이 6개월 지난 담당자 확인 알림(명세 "상시 변경"). 본인과, 기관 담당자는 자기 기관의 같은 업무 담당자 중 경과한
 * 사람을 보여 준다. 창(세션)마다 한 번만 뜨고, 그 자리에서 "변동 없음" 으로 확인하거나 연락처 화면으로 가서 고친다.
 */
export function ContactReconfirmModal({ email }: { email: string }) {
  const [opened, setOpened] = useState(false);
  const { data: due } = useReconfirmDue(!shownFor(email));
  const { data: businesses } = useBusinesses();
  const confirm = useConfirmContact();
  const [confirmed, setConfirmed] = useState<number[]>([]);

  useEffect(() => {
    if (due && due.length > 0 && !shownFor(email)) {
      markShown(email);
      setOpened(true);
    }
  }, [due, email]);

  const slugOf = (code: string) => businesses?.find((b) => b.code === code)?.slug ?? '';
  const remaining = (due ?? []).filter((c) => !confirmed.includes(c.id));

  return (
    <Modal
      opened={opened}
      onClose={() => setOpened(false)}
      title="담당자 정보를 확인해 주세요"
      centered
      size="lg"
    >
      <Stack gap="md" data-testid="contact-reconfirm">
        <Text size="md">
          최근 확인일에서 6개월이 지난 담당자입니다. 바뀐 것이 없으면 "변동 없음"을, 바뀌었으면
          연락처에서 고쳐 주세요.
        </Text>
        <Stack gap={8}>
          {(due ?? []).map((c) => {
            const done = confirmed.includes(c.id);
            return (
              <Box key={c.id} className={classes.campaignRow}>
                <Group justify="space-between" wrap="nowrap">
                  <div>
                    <Group gap={6}>
                      <Text size="md" fw={600}>
                        {c.name}
                      </Text>
                      {c.self && (
                        <Badge color="clay" size="xs">
                          나
                        </Badge>
                      )}
                      <Text size="sm" c="dimmed">
                        {c.organizationName} · {c.businessLabel}
                      </Text>
                    </Group>
                    <Text size="xs" c="dimmed" className={classes.numeric}>
                      최근 확인일 {formatDate(c.lastConfirmedAt)}
                    </Text>
                  </div>
                  {done ? (
                    <Badge color="sky">확인 완료</Badge>
                  ) : (
                    <Group gap="xs" wrap="nowrap">
                      <Anchor
                        component={Link}
                        to={`/${slugOf(c.business)}/contacts?id=${c.id}`}
                        size="sm"
                        fw={600}
                        onClick={() => setOpened(false)}
                      >
                        고치러 가기
                      </Anchor>
                      <Button
                        size="xs"
                        variant="light"
                        loading={confirm.isPending && confirm.variables === c.id}
                        onClick={() =>
                          confirm.mutate(c.id, {
                            onSuccess: () => setConfirmed((ids) => [...ids, c.id]),
                          })
                        }
                      >
                        변동 없음
                      </Button>
                    </Group>
                  )}
                </Group>
              </Box>
            );
          })}
        </Stack>
        {confirm.error && (
          <Text size="sm" c="red.6">
            {confirm.error.message}
          </Text>
        )}
        <Group justify="flex-end">
          <Button
            variant={remaining.length === 0 ? 'filled' : 'default'}
            onClick={() => setOpened(false)}
          >
            {remaining.length === 0 ? '닫기' : '나중에'}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
