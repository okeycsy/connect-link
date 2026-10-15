import {
  AppShell,
  Avatar,
  Badge,
  Box,
  Button,
  Center,
  Group,
  Kbd,
  Loader,
  Menu,
  NavLink,
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
  IconUsers,
  IconWorld,
} from '@tabler/icons-react';
import { Link, Navigate, Outlet, useLocation, useParams } from 'react-router-dom';

import { AccountSwitchModal } from '../../features/account/components/AccountSwitchModal';
import { canViewBusiness, isUnauthorized, useLogout, useMe } from '../../features/account/hooks';
import { NotificationBell } from '../../features/notification/components/NotificationBell';
import { useBusinesses } from '../../features/organization/hooks';
import { CommandPalette } from '../../features/search/components/CommandPalette';
import { ErrorState } from '../../shared/state/StateView';
import { isDemoProfile, useDemoNetworkZone, useServerInfo } from '../hooks';
import { DEFAULT_BUSINESS_SLUG, FEATURE_MENU } from '../navigation';
import classes from './AppShellLayout.module.css';

/**
 * 앱 셸. 상단 바 + 왼쪽 레일(업무 영역) + 보조 메뉴(기능) + 본문 시트.
 * 레일과 보조 메뉴는 docs/design/common-screens.md 의 확정 구조다. 상단 가로 메뉴로 바꾸지 않는다.
 * 로그인한 사용자만 들어온다. 로그인 전이거나 세션이 끝났으면(401) 지금 주소를 next 로 달고 로그인 화면으로 보낸다.
 * 레일은 사용자가 볼 수 있는 업무 영역만, 보조 메뉴의 관리는 관리자에게만 보인다(서버도 같은 규칙으로 막는다).
 */
export function AppShellLayout() {
  const theme = useMantineTheme();
  const { business: businessSlug } = useParams<{ business: string }>();
  const location = useLocation();
  const { data: businesses } = useBusinesses();
  const { data: me, error: meError } = useMe();
  const logout = useLogout();
  const [switchOpened, { open: openSwitch, close: closeSwitch }] = useDisclosure(false);
  const { data: serverInfo } = useServerInfo();
  const demo = isDemoProfile(serverInfo?.profile);
  const [searchOpened, { open: openSearch, close: closeSearch }] = useDisclosure(false);
  const [demoZone, setDemoZone] = useDemoNetworkZone();
  useHotkeys([['mod+K', openSearch]]);

  const railWidth = theme.other.railWidth as number;
  const subMenuWidth = theme.other.subMenuWidth as number;
  const headerHeight = theme.other.headerHeight as number;

  const currentBusiness = businesses?.find((b) => b.slug === businessSlug);
  const currentFeature = location.pathname.split('/')[2] ?? 'home';

  if (isUnauthorized(meError)) {
    const next = location.pathname === '/' ? '' : location.pathname + location.search;
    return <Navigate to={next ? `/login?next=${encodeURIComponent(next)}` : '/login'} replace />;
  }
  if (!me) {
    return (
      <Center h="100vh" p="lg">
        {meError ? <ErrorState error={meError} /> : <Loader size="sm" />}
      </Center>
    );
  }

  const railBusinesses = businesses?.filter((b) => canViewBusiness(me, b.slug));
  const featureMenu = FEATURE_MENU.filter((item) => !item.adminOnly || me.admin);

  return (
    <AppShell
      header={{ height: headerHeight }}
      navbar={{ width: railWidth + subMenuWidth, breakpoint: 'sm' }}
      padding={0}
      className={classes.shell}
    >
      <AppShell.Header className={classes.chrome} withBorder={false}>
        <Group h="100%" px="md" justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Box className={classes.logo}>KFTC</Box>
            <Text c="white" fw={700} size="xl">
              연결고리
            </Text>
          </Group>
          <UnstyledButton className={classes.search} aria-label="전역 검색" onClick={openSearch}>
            <Group gap="xs" wrap="nowrap">
              <IconSearch size={16} />
              <Text size="md" style={{ flex: 1 }}>
                기관, 담당자, 요청 검색
              </Text>
              <Kbd size="xs">Ctrl</Kbd>
              <Kbd size="xs">K</Kbd>
            </Group>
          </UnstyledButton>
          <Group gap="sm" wrap="nowrap">
            {demo && (
              <Menu position="bottom-end" shadow="card" width={200}>
                <Menu.Target>
                  <Button
                    size="compact-sm"
                    variant="white"
                    color="navy"
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
                    to={`/${businessSlug ?? DEFAULT_BUSINESS_SLUG}/demo/outbox`}
                    leftSection={<IconMailForward size={16} />}
                  >
                    발송함
                  </Menu.Item>
                  <Menu.Item
                    component={Link}
                    to={`/${businessSlug ?? DEFAULT_BUSINESS_SLUG}/demo/states`}
                    leftSection={<IconLayoutGrid size={16} />}
                  >
                    상태 화면
                  </Menu.Item>
                  <Menu.Item
                    component={Link}
                    to={`/${businessSlug ?? DEFAULT_BUSINESS_SLUG}/demo/attachments`}
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
                  <Menu.Item leftSection={<IconUsers size={16} />} onClick={openSwitch}>
                    계정 전환
                  </Menu.Item>
                </Menu.Dropdown>
              </Menu>
            )}
            {me.networkZone === 'EXTERNAL' && (
              <Badge variant="white" color="red" size="sm" data-testid="network-zone-badge">
                인터넷망
              </Badge>
            )}
            <NotificationBell />
            <Menu position="bottom-end" shadow="card" width={240}>
              <Menu.Target>
                <UnstyledButton aria-label="사용자 메뉴">
                  <Avatar radius="xl" size={32} color="navy" variant="filled">
                    {me.name.slice(0, 1)}
                  </Avatar>
                </UnstyledButton>
              </Menu.Target>
              <Menu.Dropdown>
                <Box px="sm" py="xs">
                  <Text fw={700} data-testid="user-name">
                    {me.name}
                  </Text>
                  <Text size="xs" c="dimmed">
                    {me.organizationName} · {me.roleLabel}
                  </Text>
                  <Text size="xs" c="dimmed">
                    {me.email}
                  </Text>
                  {me.admin && (
                    <Badge size="xs" variant="light" mt={6}>
                      관리자
                    </Badge>
                  )}
                </Box>
                <Menu.Divider />
                <Menu.Item
                  component={Link}
                  to={`/${businessSlug ?? DEFAULT_BUSINESS_SLUG}/me`}
                  leftSection={<IconUser size={16} />}
                >
                  내 정보
                </Menu.Item>
                <Menu.Item
                  leftSection={<IconLogout size={16} />}
                  onClick={() => logout.mutate()}
                  disabled={logout.isPending}
                >
                  로그아웃
                </Menu.Item>
              </Menu.Dropdown>
            </Menu>
          </Group>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar withBorder={false} className={classes.navbar}>
        <Box className={`${classes.rail} ${classes.chrome}`} w={railWidth}>
          <Stack gap={6} align="center" pt="sm">
            {railBusinesses
              ? railBusinesses.map((b) => {
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
              : [0, 1, 2, 3].map((i) => <Skeleton key={i} h={40} w={40} radius="md" />)}
          </Stack>
        </Box>

        <Box className={classes.subMenu} w={subMenuWidth}>
          <Text size="xs" fw={700} c="dimmed" tt="uppercase" px="md" pt="md" pb="xs">
            {currentBusiness?.label ?? '업무 영역'}
          </Text>
          {featureMenu.map((item) => (
            <NavLink
              key={item.key}
              component={Link}
              to={`/${businessSlug ?? ''}/${item.path}`}
              label={item.label}
              leftSection={<item.icon size={18} stroke={1.6} />}
              active={currentFeature === item.path}
              className={classes.subMenuItem}
            />
          ))}
        </Box>
      </AppShell.Navbar>

      <CommandPalette opened={searchOpened} onClose={closeSearch} />
      {demo && (
        <AccountSwitchModal opened={switchOpened} onClose={closeSwitch} currentEmail={me.email} />
      )}

      <AppShell.Main className={classes.chrome}>
        <Box className={classes.sheet} p="lg">
          <Outlet />
        </Box>
      </AppShell.Main>
    </AppShell>
  );
}
