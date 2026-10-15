import {
  ActionIcon,
  AppShell,
  Avatar,
  Box,
  Group,
  Indicator,
  Kbd,
  NavLink,
  Skeleton,
  Stack,
  Text,
  Tooltip,
  UnstyledButton,
  useMantineTheme,
} from '@mantine/core';
import { IconBell, IconSearch } from '@tabler/icons-react';
import { Link, Outlet, useLocation, useParams } from 'react-router-dom';

import { useBusinesses } from '../../features/organization/hooks';
import { FEATURE_MENU } from '../navigation';
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
          <UnstyledButton className={classes.search} aria-label="전역 검색">
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
            <Indicator color="red" size={8} offset={4} disabled>
              <ActionIcon variant="subtle" color="gray" aria-label="알림">
                <IconBell size={20} color="white" />
              </ActionIcon>
            </Indicator>
            <Avatar radius="xl" size={32} color="navy" variant="filled">
              게
            </Avatar>
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

      <AppShell.Main className={classes.chrome}>
        <Box className={classes.sheet} p="lg">
          <Outlet />
        </Box>
      </AppShell.Main>
    </AppShell>
  );
}
