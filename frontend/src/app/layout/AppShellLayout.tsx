import {
  AppShell,
  Avatar,
  Badge,
  Box,
  Button,
  Group,
  Kbd,
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
import { IconFlask, IconLogout, IconMailForward, IconSearch, IconUser } from '@tabler/icons-react';
import { Link, Outlet, useLocation, useParams } from 'react-router-dom';

import { useMe } from '../../features/account/hooks';
import { NotificationBell } from '../../features/notification/components/NotificationBell';
import { useBusinesses } from '../../features/organization/hooks';
import { CommandPalette } from '../../features/search/components/CommandPalette';
import { isDemoProfile, useServerInfo } from '../hooks';
import { DEFAULT_BUSINESS_SLUG, FEATURE_MENU } from '../navigation';
import classes from './AppShellLayout.module.css';

/**
 * 앱 셸. 상단 바 + 왼쪽 레일(업무 영역) + 보조 메뉴(기능) + 본문 시트.
 * 레일과 보조 메뉴는 docs/design/common-screens.md 의 확정 구조다. 상단 가로 메뉴로 바꾸지 않는다.
 */
export function AppShellLayout() {
  const theme = useMantineTheme();
  const { business: businessSlug } = useParams<{ business: string }>();
  const location = useLocation();
  const { data: businesses } = useBusinesses();
  const { data: me } = useMe();
  const { data: serverInfo } = useServerInfo();
  const demo = isDemoProfile(serverInfo?.profile);
  const [searchOpened, { open: openSearch, close: closeSearch }] = useDisclosure(false);
  useHotkeys([['mod+K', openSearch]]);

  const railWidth = theme.other.railWidth as number;
  const subMenuWidth = theme.other.subMenuWidth as number;
  const headerHeight = theme.other.headerHeight as number;

  const currentBusiness = businesses?.find((b) => b.slug === businessSlug);
  const currentFeature = location.pathname.split('/')[2] ?? 'home';

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
                  <Menu.Item disabled leftSection={<IconUser size={16} />}>
                    계정 전환 (계정 모듈 후)
                  </Menu.Item>
                </Menu.Dropdown>
              </Menu>
            )}
            <NotificationBell />
            <Menu position="bottom-end" shadow="card" width={240}>
              <Menu.Target>
                <UnstyledButton aria-label="사용자 메뉴">
                  <Avatar radius="xl" size={32} color="navy" variant="filled">
                    {me?.name?.slice(0, 1) ?? ''}
                  </Avatar>
                </UnstyledButton>
              </Menu.Target>
              <Menu.Dropdown>
                <Box px="sm" py="xs">
                  <Text fw={700}>{me?.name ?? '로그인 전'}</Text>
                  <Text size="xs" c="dimmed">
                    {me ? `${me.organizationName} · ${me.email}` : ''}
                  </Text>
                  {me?.admin && (
                    <Badge size="xs" variant="light" mt={6}>
                      관리자
                    </Badge>
                  )}
                </Box>
                <Menu.Divider />
                <Menu.Item disabled leftSection={<IconUser size={16} />}>
                  내 정보 (준비 중)
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
        <Box className={`${classes.rail} ${classes.chrome}`} w={railWidth}>
          <Stack gap={6} align="center" pt="sm">
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
              : [0, 1, 2, 3].map((i) => <Skeleton key={i} h={40} w={40} radius="md" />)}
          </Stack>
        </Box>

        <Box className={classes.subMenu} w={subMenuWidth}>
          <Text size="xs" fw={700} c="dimmed" tt="uppercase" px="md" pt="md" pb="xs">
            {currentBusiness?.label ?? '업무 영역'}
          </Text>
          {FEATURE_MENU.map((item) => (
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

      <AppShell.Main className={classes.chrome}>
        <Box className={classes.sheet} p="lg">
          <Outlet />
        </Box>
      </AppShell.Main>
    </AppShell>
  );
}
