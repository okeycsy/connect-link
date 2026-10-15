import { Card, Stack, Text, Title } from '@mantine/core';

import { AttachmentPanel } from '../../features/attachment/components/AttachmentPanel';

/**
 * 데모 도구: 첨부파일 공통 부품을 한곳에서 써 본다. 대상은 DEMO(1) 이며 demo·local 프로필에서만 권한이 열린다.
 * 실제 화면(의견 요청 첨부자료, 기관 회신 근거자료, 일시중단 공문)은 담당 프로젝트가 같은 부품을 대상만 바꿔 쓴다.
 */
export function DemoAttachmentsPage() {
  return (
    <Stack gap="md">
      <div>
        <Title order={2}>첨부파일</Title>
        <Text c="dimmed" size="lg">
          의견 요청·기관 회신·일시중단 화면이 쓰는 공통 첨부 부품입니다. 파일은 서버 임시 디렉터리에
          저장되고 재시작하면 사라집니다.
        </Text>
      </div>
      <Card maw={640}>
        <AttachmentPanel ownerType="DEMO" ownerId={1} editable uploadLabel="파일 올리기" />
      </Card>
    </Stack>
  );
}
