import { Alert, Loader, Modal, ScrollArea, Text } from '@mantine/core';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { homeBusinessSlugOf, useDemoAccounts, useLogin } from '../hooks';
import { DemoAccountList } from './DemoAccountList';

interface Props {
  opened: boolean;
  onClose: () => void;
  currentEmail?: string;
}

/**
 * 데모 도구: 계정 전환. 고른 데모 계정으로 실제 로그인 API 를 다시 불러 세션을 바꾸고, 그 사용자의 첫 업무 홈으로 간다.
 * 이전 사용자의 화면 데이터는 로그인 훅이 모두 버린다.
 */
export function AccountSwitchModal({ opened, onClose, currentEmail }: Props) {
  const navigate = useNavigate();
  const { data: demo, isPending } = useDemoAccounts();
  const login = useLogin();
  const [pickedEmail, setPickedEmail] = useState<string>();

  return (
    <Modal opened={opened} onClose={onClose} title="계정 전환" size="md">
      <Text size="sm" c="dimmed" mb="sm">
        역할별 데모 계정으로 다시 로그인합니다. 메뉴와 볼 수 있는 업무 영역이 계정에 따라
        달라집니다.
      </Text>
      {login.isError && (
        <Alert variant="light" color="red" mb="sm">
          {login.error.message}
        </Alert>
      )}
      {isPending || !demo ? (
        <Loader size="sm" />
      ) : (
        <ScrollArea.Autosize
          mah={420}
          type="auto"
          scrollbars="y"
          scrollbarSize={6}
          offsetScrollbars
        >
          <DemoAccountList
            accounts={demo.accounts}
            currentEmail={currentEmail}
            pendingEmail={pickedEmail}
            onPick={(account) => {
              setPickedEmail(account.email);
              login.mutate(
                { email: account.email, password: demo.password },
                {
                  onSuccess: (me) => {
                    onClose();
                    navigate(`/${homeBusinessSlugOf(me)}/home`);
                  },
                  onSettled: () => setPickedEmail(undefined),
                },
              );
            }}
          />
        </ScrollArea.Autosize>
      )}
    </Modal>
  );
}
