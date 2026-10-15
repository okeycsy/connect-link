import {
  Badge,
  Box,
  Group,
  Kbd,
  Loader,
  Modal,
  ScrollArea,
  Text,
  TextInput,
  UnstyledButton,
} from '@mantine/core';
import { useDebouncedValue } from '@mantine/hooks';
import { IconArrowRight, IconSearch } from '@tabler/icons-react';
import { useEffect, useMemo, useState, type KeyboardEvent } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { ADMIN_MENU, FEATURE_MENU } from '../../../app/navigation';
import { useSearch } from '../hooks';
import classes from './CommandPalette.module.css';

interface Props {
  opened: boolean;
  onClose: () => void;
}

interface PaletteItem {
  key: string;
  group: string;
  title: string;
  subtitle?: string | null;
  link: string;
  sample?: boolean;
}

/**
 * 전역 검색 팔레트(Ctrl K). 바로 가기(보조 메뉴·관리 항목)는 화면에서, 기관·담당자·의견 요청·기관 테스트·일시중단은 서버(/api/v1/search)에서 찾는다.
 * 결과는 레일에서 고른 업무 영역 범위다. ↑↓ 로 고르고 Enter 로 연다.
 */
export function CommandPalette({ opened, onClose }: Props) {
  const navigate = useNavigate();
  const { business } = useParams<{ business: string }>();
  const [query, setQuery] = useState('');
  const [debounced] = useDebouncedValue(query.trim(), 200);
  const [active, setActive] = useState(0);
  const { data, isFetching } = useSearch(business, debounced);

  const items = useMemo<PaletteItem[]>(() => {
    const needle = query.trim().toLowerCase();
    const shortcuts: PaletteItem[] = [
      ...FEATURE_MENU.map((m) => ({
        key: `menu-${m.key}`,
        group: '바로 가기',
        title: m.label,
        subtitle: m.description,
        link: `/${business}/${m.path}`,
      })),
      ...ADMIN_MENU.filter((m) => m.path).map((m) => ({
        key: `admin-${m.key}`,
        group: '바로 가기',
        title: `관리 › ${m.label}`,
        subtitle: m.description,
        link: `/${business}/${m.path}`,
      })),
    ].filter((s) => !needle || `${s.title} ${s.subtitle}`.toLowerCase().includes(needle));

    const remote: PaletteItem[] =
      needle && data?.query === debounced
        ? data.groups.flatMap((g) =>
            g.hits.map((h, i) => ({
              key: `${g.category}-${i}`,
              group: g.label,
              title: h.title,
              subtitle: h.subtitle,
              link: h.link,
              sample: g.source === 'SAMPLE',
            })),
          )
        : [];
    return [...remote, ...shortcuts];
  }, [query, debounced, data, business]);

  useEffect(() => setActive(0), [items.length, debounced]);

  const close = () => {
    onClose();
    setQuery('');
  };

  const go = (item: PaletteItem | undefined) => {
    if (!item) return;
    navigate(item.link);
    close();
  };

  const onKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      setActive((a) => Math.min(a + 1, items.length - 1));
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      setActive((a) => Math.max(a - 1, 0));
    } else if (e.key === 'Enter') {
      e.preventDefault();
      go(items[active]);
    }
  };

  const groups = useMemo(() => {
    const order: string[] = [];
    const byGroup = new Map<string, PaletteItem[]>();
    items.forEach((it) => {
      if (!byGroup.has(it.group)) {
        order.push(it.group);
        byGroup.set(it.group, []);
      }
      byGroup.get(it.group)!.push(it);
    });
    return order.map((g) => ({ label: g, items: byGroup.get(g)! }));
  }, [items]);

  const searching = debounced.length > 0;
  const noResult = searching && items.length === 0 && !isFetching;

  return (
    <Modal
      opened={opened}
      onClose={close}
      withCloseButton={false}
      padding={0}
      size={620}
      yOffset={72}
      radius="md"
      overlayProps={{ backgroundOpacity: 0.3, blur: 3 }}
      transitionProps={{ transition: 'pop', duration: 120 }}
      aria-label="전역 검색"
    >
      <Box data-testid="command-palette">
        <TextInput
          variant="unstyled"
          size="lg"
          placeholder="기관, 담당자, 의견 요청, 기관 테스트, 일시중단 검색"
          aria-label="검색어"
          value={query}
          onChange={(e) => setQuery(e.currentTarget.value)}
          onKeyDown={onKeyDown}
          data-autofocus
          leftSection={<IconSearch size={18} />}
          rightSection={isFetching ? <Loader size="xs" /> : <Kbd size="xs">Esc</Kbd>}
          rightSectionWidth={48}
          classNames={{ root: classes.inputRoot, input: classes.input }}
        />
        <ScrollArea.Autosize mah={440} type="auto">
          <Box px="sm" py="xs" role="listbox" aria-label="검색 결과">
            {noResult && (
              <Text c="dimmed" ta="center" py="lg" size="sm">
                '{debounced}' 에 맞는 결과가 없습니다.
              </Text>
            )}
            {groups.map((g) => (
              <Box key={g.label} mb={6}>
                <Group gap={6} px="xs" pt={6} pb={2}>
                  <Text size="xs" fw={700} c="dimmed" tt="uppercase">
                    {g.label}
                  </Text>
                  {g.items[0].sample && (
                    <Badge size="xs" variant="outline" color="gray">
                      샘플
                    </Badge>
                  )}
                </Group>
                {g.items.map((it) => {
                  const index = items.indexOf(it);
                  return (
                    <UnstyledButton
                      key={it.key}
                      className={classes.item}
                      data-active={index === active || undefined}
                      role="option"
                      aria-selected={index === active}
                      aria-label={it.title}
                      onMouseEnter={() => setActive(index)}
                      onClick={() => go(it)}
                    >
                      <div className={classes.itemText}>
                        <Text size="md" fw={600} truncate>
                          {it.title}
                        </Text>
                        {it.subtitle && (
                          <Text size="xs" c="dimmed" truncate>
                            {it.subtitle}
                          </Text>
                        )}
                      </div>
                      <IconArrowRight size={14} className={classes.arrow} />
                    </UnstyledButton>
                  );
                })}
              </Box>
            ))}
          </Box>
        </ScrollArea.Autosize>
        <Group justify="space-between" px="md" py={8} className={classes.footer}>
          <Group gap="md">
            <Text size="xs" c="dimmed">
              <Kbd size="xs">↑</Kbd> <Kbd size="xs">↓</Kbd> 이동
            </Text>
            <Text size="xs" c="dimmed">
              <Kbd size="xs">Enter</Kbd> 열기
            </Text>
          </Group>
          <Text size="xs" c="dimmed">
            결과는 현재 업무 영역 안에서 찾습니다.
          </Text>
        </Group>
      </Box>
    </Modal>
  );
}
