import { Avatar, Badge, Group, Loader, Stack, Text, UnstyledButton } from '@mantine/core';
import { IconChevronRight } from '@tabler/icons-react';

import type { DemoAccountResponse } from '../hooks';
import classes from './DemoAccountList.module.css';

interface Props {
  accounts: DemoAccountResponse[];
  /** 고르면 이 이메일로 실제 로그인 API 를 부른다. */
  onPick: (account: DemoAccountResponse) => void;
  /** 로그인 중인 계정. 그 줄에 진행 표시를 하고 다른 줄은 잠근다. */
  pendingEmail?: string;
  /** 지금 로그인한 계정(계정 전환에서 표시). */
  currentEmail?: string;
}

/** 역할별 데모 계정 고르기. 로그인 화면과 데모 도구의 계정 전환이 함께 쓴다. */
export function DemoAccountList({ accounts, onPick, pendingEmail, currentEmail }: Props) {
  return (
    <Stack gap={4} data-testid="demo-accounts">
      {accounts.map((account) => {
        const current = account.email === currentEmail;
        const pending = account.email === pendingEmail;
        return (
          <UnstyledButton
            key={account.email}
            className={classes.row}
            data-current={current || undefined}
            disabled={Boolean(pendingEmail) || current}
            onClick={() => onPick(account)}
            aria-label={`${account.name} (${account.roleLabel})로 로그인`}
          >
            <Group gap="sm" wrap="nowrap">
              <Avatar radius="xl" size={32} color="navy" variant={current ? 'filled' : 'light'}>
                {account.name.slice(0, 1)}
              </Avatar>
              <div className={classes.body}>
                <Group gap={6} wrap="nowrap">
                  <Text fw={700} size="sm">
                    {account.name}
                  </Text>
                  <Text size="xs" c="dimmed" truncate>
                    {account.roleLabel}
                  </Text>
                  {account.admin && (
                    <Badge size="xs" variant="light">
                      관리자
                    </Badge>
                  )}
                  {current && (
                    <Badge size="xs" variant="outline" color="gray">
                      지금 계정
                    </Badge>
                  )}
                </Group>
                <Text size="xs" c="dimmed" truncate>
                  {account.organizationName} · {account.businessLabels.join(', ')}
                </Text>
              </div>
              {pending ? (
                <Loader size={14} />
              ) : (
                <IconChevronRight size={16} className={classes.chevron} />
              )}
            </Group>
          </UnstyledButton>
        );
      })}
    </Stack>
  );
}
