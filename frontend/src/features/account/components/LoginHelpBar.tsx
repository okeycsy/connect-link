import { Box, Button, Text } from '@mantine/core';
import { IconBook2, IconChevronRight, IconHeadset, IconMail } from '@tabler/icons-react';
import { Link } from 'react-router-dom';

import { GUIDE_TOPICS } from './guideTopics';
import classes from './LoginHelpBar.module.css';

/**
 * 로그인 화면 맨 아래 띠: 왼쪽 이용 가이드(제목을 누르면 /guide, 가운데 기능별 가이드 링크 여섯 개), 오른쪽 문의하기(문의 메일).
 * 문의 메일 주소가 없으면 문의하기 칸을 뺀다. 휴대폰 폭에서는 세 칸이 위아래로 쌓인다.
 */
export function LoginHelpBar({ inquiryEmail }: { inquiryEmail?: string }) {
  return (
    <Box component="section" aria-label="이용 안내" className={classes.bar}>
      <div className={classes.lead}>
        <Box className={classes.icon} aria-hidden>
          <IconBook2 size={22} stroke={1.7} />
        </Box>
        <div>
          <Text component={Link} to="/guide" fw={700} fz={16} c="var(--cl-ink)">
            이용 가이드
          </Text>
          <Text fz={13} c="var(--cl-ink-muted)" lh={1.5}>
            처음이신가요?
            <br />각 업무별 이용 방법을 확인해 보세요.
          </Text>
        </div>
      </div>

      <ul className={classes.links} aria-label="기능별 이용 가이드">
        {GUIDE_TOPICS.map((topic) => (
          <li key={topic.id}>
            <Link to={`/guide/${topic.id}`} className={classes.link}>
              <span>{topic.title}</span>
              <IconChevronRight size={16} stroke={1.7} aria-hidden />
            </Link>
          </li>
        ))}
      </ul>

      {inquiryEmail && (
        <div className={classes.inquiry}>
          <div className={classes.lead}>
            <Box className={classes.icon} aria-hidden>
              <IconHeadset size={22} stroke={1.7} />
            </Box>
            <div>
              <Text fw={700} fz={16} c="var(--cl-ink)">
                문의하기
              </Text>
              <Text fz={13} c="var(--cl-ink-muted)" lh={1.5}>
                이용 중 궁금한 점이 있으시면
                <br />
                언제든지 문의해 주세요.
              </Text>
            </div>
          </div>
          <Button
            component="a"
            href={`mailto:${inquiryEmail}`}
            variant="outline"
            leftSection={<IconMail size={16} />}
          >
            문의 메일 보내기
          </Button>
        </div>
      )}
    </Box>
  );
}
