import {
  AppShell,
  Badge,
  Box,
  Button,
  Group,
  Kbd,
  Menu,
  Skeleton,
  Stack,
  Text,
  Tooltip,
  UnstyledButton,
  useMantineTheme,
} from '@mantine/core';
import { useDisclosure, useHotkeys } from '@mantine/hooks';
import {
  IconFlask,
  IconLayoutGrid,
  IconLogout,
  IconMailForward,
  IconPaperclip,
  IconSearch,
  IconUser,
  IconWorld,
  type Icon,
} from '@tabler/icons-react';
import { Link, Outlet, useLocation, useParams } from 'react-router-dom';

import { useMe } from '../../features/account/hooks';
import { NotificationBell } from '../../features/notification/components/NotificationBell';
import { useUnreadCount } from '../../features/notification/hooks';
import { useBusinesses } from '../../features/organization/hooks';
import { CommandPalette } from '../../features/search/components/CommandPalette';
import { BrandMark } from '../../shared/BrandMark';
import { isDemoProfile, useDemoNetworkZone, useServerInfo } from '../hooks';
import { DEFAULT_BUSINESS_SLUG, FEATURE_MENU, SHORTCUT_MENU, SIDEBAR_GROUPS } from '../navigation';
import classes from './AppShellLayout.module.css';

/**
 * 앱 셸 (B안 메신저형). 상단 바 + 왼쪽 업무 레일 + 사이드바(묶음별 메뉴) + 흰 본문.
 * 레일 = 업무 영역(조회 범위), 사이드바 = 기능. 구조와 치수는 frontend/CLAUDE.md "앱 셸" 이 기준이다. 상단 가로 메뉴로 바꾸지 않는다.
 */
export function AppShellLayout() {
  const theme = useMantineTheme();
  const { business: businessSlug } = useParams<{ business: string }>();
  const location = useLocation();
  const { data: businesses } = useBusinesses();
  const { data: me } = useMe();
  const { data: unread } = useUnreadCount();
  const { data: serverInfo } = useServerInfo();
  const demo = isDemoProfile(serverInfo?.profile);
  const [searchOpened, { open: openSearch, close: closeSearch }] = useDisclosure(false);
  const [demoZone, setDemoZone] = useDemoNetworkZone();
  useHotkeys([['mod+K', openSearch]]);

  const railWidth = theme.other.railWidth as number;
  const sidebarWidth = theme.other.sidebarWidth as number;
  const headerHeight = theme.other.headerHeight as number;

  const currentBusiness = businesses?.find((b) => b.slug === businessSlug);
  const currentFeature = location.pathname.split('/')[2] ?? 'home';
  const base = `/${businessSlug ?? DEFAULT_BUSINESS_SLUG}`;
  const unreadCount = unread?.count ?? 0;

  return (
    <AppShell
      header={{ height: headerHeight }}
      navbar={{ width: railWidth + sidebarWidth, breakpoint: 'sm' }}
      padding={0}
    >
      <AppShell.Header className={classes.header}>
        <Group h="100%" px="md" gap="lg" wrap="nowrap">
          <Box
            component={Link}
            to={`${base}/home`}
            className={classes.brand}
            aria-label="연결고리 홈"
          >
            <BrandMark />
          </Box>
          <UnstyledButton className={classes.search} aria-label="전역 검색" onClick={openSearch}>
            <Group gap="xs" wrap="nowrap">
              <IconSearch size={16} />
              <Text size="md" c="dimmed" style={{ flex: 1 }} truncate>
                {currentBusiness ? `${currentBusiness.label}에서 ` : ''}기관, 담당자, 요청 검색
              </Text>
              <Kbd size="xs">Ctrl</Kbd>
              <Kbd size="xs">K</Kbd>
            </Group>
          </UnstyledButton>
          <Group gap="sm" wrap="nowrap">
            {demo && (
              <Menu position="bottom-end" width={220}>
                <Menu.Target>
                  <Button
                    size="compact-sm"
                    variant="light"
                    leftSection={<IconFlask size={14} />}
                    aria-label="데모 도구"
                  >
                    데모
                  </Button>
                </Menu.Target>
                <Menu.Dropdown>
                  <Menu.Label>데모 도구</Menu.Label>
                  <Menu.Item
                    component={Link}
                    to={`${base}/demo/outbox`}
                    leftSection={<IconMailForward size={16} />}
                  >
                    발송함
                  </Menu.Item>
                  <Menu.Item
                    component={Link}
                    to={`${base}/demo/states`}
                    leftSection={<IconLayoutGrid size={16} />}
                  >
                    상태 화면
                  </Menu.Item>
                  <Menu.Item
                    component={Link}
                    to={`${base}/demo/attachments`}
                    leftSection={<IconPaperclip size={16} />}
                  >
                    첨부파일
                  </Menu.Item>
                  <Menu.Item
                    leftSection={<IconWorld size={16} />}
                    onClick={() => setDemoZone(demoZone === 'INTERNAL' ? 'EXTERNAL' : 'INTERNAL')}
                  >
                    망 전환: {demoZone === 'INTERNAL' ? '내부망 → 인터넷망' : '인터넷망 → 내부망'}
                  </Menu.Item>
                  <Menu.Item disabled leftSection={<IconUser size={16} />}>
                    계정 전환 (계정 모듈 후)
                  </Menu.Item>
                </Menu.Dropdown>
              </Menu>
            )}
            {me?.networkZone === 'EXTERNAL' && (
              <Badge color="red" size="md" data-testid="network-zone-badge">
                인터넷망
              </Badge>
            )}
            <NotificationBell />
            <Menu position="bottom-end" width={240}>
              <Menu.Target>
                <UnstyledButton aria-label="사용자 메뉴" className={classes.user}>
                  <Box className={classes.userAvatar} aria-hidden>
                    {me?.name?.slice(0, 1) ?? ''}
                    <span className={classes.online} />
                  </Box>
                  <Text size="md" fw={500} visibleFrom="md">
                    {me?.name ?? ''}
                  </Text>
                </UnstyledButton>
              </Menu.Target>
              <Menu.Dropdown>
                <Box px="sm" py="xs">
                  <Text fw={700}>{me?.name ?? '로그인 전'}</Text>
                  <Text size="xs" c="dimmed">
                    {me ? `${me.organizationName} · ${me.email}` : ''}
                  </Text>
                  {me?.admin && (
                    <Badge size="xs" mt={6}>
                      관리자
                    </Badge>
                  )}
                </Box>
                <Menu.Divider />
                <Menu.Item component={Link} to={`${base}/me`} leftSection={<IconUser size={16} />}>
                  내 정보
                </Menu.Item>
                <Menu.Item disabled leftSection={<IconLogout size={16} />}>
                  로그아웃 (계정 모듈 후)
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar withBorder={false} className={classes.navbar}>
        <Box className={classes.rail} w={railWidth}>
          <Stack gap={12} align="center">
            {businesses
              ? businesses.map((b) => {
                  const active = b.slug === businessSlug;
                  return (
                    <Tooltip key={b.code} label={b.label} position="right" withArrow>
                      <UnstyledButton
                        component={Link}
                        to={`/${b.slug}/${currentFeature}`}
                        className={classes.railToken}
                        data-active={active || undefined}
                        aria-label={b.label}
                        aria-current={active ? 'page' : undefined}
                      >
                        {b.shortLabel}
                      </UnstyledButton>
                    </Tooltip>
                  );
                })
              : [0, 1, 2, 3].map((i) => <Skeleton key={i} h={44} w={44} radius="md" />)}
          </Stack>
        </Box>

        <Box className={classes.sidebar} w={sidebarWidth}>
          <Box px={10} mb={18}>
            <Text fw={700} fz={18} c="var(--cl-ink)">
              {currentBusiness?.label ?? '업무 영역'}
            </Text>
            <Text size="sm" c="dimmed" truncate>
              {me?.organizationName ?? ''}
            </Text>
          </Box>
          <Stack gap={18}>
            {SIDEBAR_GROUPS.map((group) => (
              <div key={group}>
                <Text className={classes.groupTitle}>{group}</Text>
                {FEATURE_MENU.filter((m) => m.group === group).map((item) => (
                  <SidebarItem
                    key={item.key}
                    to={`${base}/${item.path}`}
                    label={item.label}
                    icon={item.icon}
                    active={currentFeature === item.path}
                  />
                ))}
                {group === '바로가기' &&
                  SHORTCUT_MENU.map((item) => (
                    <SidebarItem
                      key={item.key}
                      to={`${base}/${item.path}`}
                      label={item.label}
                      icon={item.icon}
                      active={currentFeature === item.path}
                      badge={item.key === 'notifications' ? unreadCount : 0}
                    />
                  ))}
              </div>
            ))}
          </Stack>
        </Box>
      </AppShell.Navbar>

      <CommandPalette opened={searchOpened} onClose={closeSearch} />

      <AppShell.Main className={classes.main}>
        <Box className={classes.page}>
          <Outlet />
        </Box>
      </AppShell.Main>
    </AppShell>
  );
}

interface SidebarItemProps {
  to: string;
  label: string;
  icon: Icon;
  active: boolean;
  /** 0 보다 크면 오른쪽에 빨간 숫자 배지. */
  badge?: number;
}

/** 사이드바 한 줄. 높이 36, radius 8, 활성은 clay-1 바탕 + clay-8 글자. */
function SidebarItem({ to, label, icon: ItemIcon, active, badge = 0 }: SidebarItemProps) {
  return (
    <UnstyledButton
      component={Link}
      to={to}
      className={classes.item}
      data-active={active || undefined}
      data-unread={badge > 0 || undefined}
      aria-current={active ? 'page' : undefined}
    >
      <ItemIcon size={18} stroke={1.7} className={classes.itemIcon} aria-hidden />
      <span className={classes.itemLabel}>{label}</span>
      {badge > 0 && (
        <Badge size="sm" variant="filled" color="red" aria-label={`읽지 않음 ${badge}건`}>
          {badge}
        </Badge>
      )}
    </UnstyledButton>
  );
}
