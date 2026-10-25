import {
  Button,
  Checkbox,
  Group,
  Modal,
  Radio,
  Select,
  Stack,
  Text,
  Textarea,
  TextInput,
} from '@mantine/core';
import { DatePickerInput } from '@mantine/dates';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';
import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';

import { fieldErrorsOf } from '../../../api/client';
import type { BusinessResponse } from '../../organization/hooks';
import { NOTICE_CATEGORY_LABEL, type Notice, type NoticeCategory, useSaveNotice } from '../hooks';

interface Props {
  opened: boolean;
  onClose: () => void;
  business: BusinessResponse;
  /** 고칠 글. 없으면 새 글이다. */
  notice: Notice | null;
  /** 전 업무 공통 공지를 올릴 수 있는지(관리자). */
  canPostCommon: boolean;
}

interface Values {
  scope: 'BUSINESS' | 'COMMON';
  category: NoticeCategory;
  title: string;
  content: string;
  pinned: boolean;
  effectiveDate: string | null;
  /** 회신 받기(예/아니오). 예로 새로 정하면 저장한 뒤 의견 요청 작성 화면으로 간다. */
  reply: 'YES' | 'NO';
}

/** 공지·자료실 글 쓰기·고치기. 범위(이 업무/전 업무 공통)는 새 글에서만 고른다. 첨부는 저장한 뒤 글 화면에서 올린다. */
export function NoticeFormModal({ opened, onClose, business, notice, canPostCommon }: Props) {
  const save = useSaveNotice();
  const form = useForm<Values>({
    initialValues: {
      scope: 'BUSINESS',
      category: 'NOTICE',
      title: '',
      content: '',
      pinned: false,
      effectiveDate: null,
      reply: 'NO',
    },
  });
  const navigate = useNavigate();
  const linked = notice?.opinionRequest ?? null;

  useEffect(() => {
    if (!opened) return;
    form.setValues({
      scope: notice && notice.business == null ? 'COMMON' : 'BUSINESS',
      category: notice?.category ?? 'NOTICE',
      title: notice?.title ?? '',
      content: notice?.content ?? '',
      pinned: notice?.pinned ?? false,
      effectiveDate: notice?.effectiveDate ?? null,
      reply: notice?.opinionRequest ? 'YES' : 'NO',
    });
    form.resetDirty();
    save.reset();
    // 열 때만 채운다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [opened, notice]);

  const fieldErrors = fieldErrorsOf(save.error);
  const common = form.values.scope === 'COMMON';
  const regulation = form.values.category !== 'NOTICE';
  const categoryOptions = (Object.keys(NOTICE_CATEGORY_LABEL) as NoticeCategory[])
    .filter((c) => !common || c === 'NOTICE')
    .map((c) => ({ value: c, label: NOTICE_CATEGORY_LABEL[c] }));

  const submit = form.onSubmit((v) =>
    save.mutate(
      {
        id: notice?.id ?? null,
        body: {
          business: v.scope === 'COMMON' ? null : business.code,
          category: v.category,
          title: v.title,
          content: v.content,
          pinned: v.pinned,
          effectiveDate: v.category === 'NOTICE' ? null : v.effectiveDate,
          // 이미 이은 요청은 예면 그대로 두고 아니오면 끊는다. 새로 예를 고르면 요청을 만든 뒤 잇는다.
          opinionRequestId: v.scope !== 'COMMON' && v.reply === 'YES' && linked ? linked.id : null,
        },
      },
      {
        onSuccess: (saved) => {
          const startRequest = v.scope !== 'COMMON' && v.reply === 'YES' && !linked;
          notifications.show({
            message: startRequest
              ? '글을 올렸습니다. 회신 받을 의견 요청을 만들어 주세요.'
              : notice
                ? '글을 고쳤습니다.'
                : '글을 올렸습니다.',
            color: 'clay',
          });
          onClose();
          if (startRequest) navigate(`/${business.slug}/opinions/new?notice=${saved.id}`);
        },
      },
    ),
  );

  return (
    <Modal opened={opened} onClose={onClose} title={notice ? '글 고치기' : '글 쓰기'} size="lg">
      <form onSubmit={submit}>
        <Stack gap="md">
          {notice == null && canPostCommon && (
            <Select
              label="올릴 곳"
              data={[
                { value: 'BUSINESS', label: `${business.label} 담당자에게` },
                { value: 'COMMON', label: '전 업무 공통 공지' },
              ]}
              allowDeselect={false}
              {...form.getInputProps('scope')}
              onChange={(v) => {
                form.setFieldValue('scope', (v ?? 'BUSINESS') as Values['scope']);
                if (v === 'COMMON') form.setFieldValue('category', 'NOTICE');
              }}
            />
          )}
          {notice != null && (
            <Text size="sm" c="dimmed">
              {notice.business == null ? '전 업무 공통 공지' : `${notice.businessLabel} 담당자에게`}
            </Text>
          )}
          <Select
            label="분류"
            data={categoryOptions}
            allowDeselect={false}
            {...form.getInputProps('category')}
            error={fieldErrors.category}
          />
          <TextInput
            label="제목"
            required
            maxLength={200}
            {...form.getInputProps('title')}
            error={fieldErrors.title}
          />
          <Textarea
            label="내용"
            required
            autosize
            minRows={6}
            maxLength={4000}
            {...form.getInputProps('content')}
            error={fieldErrors.content}
          />
          {regulation && (
            <DatePickerInput
              label="시행일"
              placeholder="없으면 비워 두세요"
              clearable
              w={{ base: '100%', sm: 220 }}
              valueFormat="YYYY-MM-DD"
              {...form.getInputProps('effectiveDate')}
              error={fieldErrors.effectiveDate}
            />
          )}
          {!common && (
            <Radio.Group
              label="회신 받기"
              description={
                linked
                  ? `"${linked.title}" 의견 요청으로 회신을 받고 있습니다. 아니오로 바꾸면 글에서 회신 칸이 빠집니다(의견 요청은 남습니다).`
                  : '회의 참석 여부처럼 기관 회신이 필요하면 예를 고르세요. 올린 뒤 의견 요청 작성 화면으로 넘어갑니다.'
              }
              {...form.getInputProps('reply')}
            >
              <Group gap="lg" mt={6}>
                <Radio value="YES" label="예" />
                <Radio value="NO" label="아니오" />
              </Group>
            </Radio.Group>
          )}
          <Checkbox
            label="목록 맨 위에 고정"
            {...form.getInputProps('pinned', { type: 'checkbox' })}
          />
          {save.isError && Object.keys(fieldErrors).length === 0 && (
            <Text size="sm" c="red">
              {save.error.message}
            </Text>
          )}
          <Group justify="flex-end">
            <Button variant="default" onClick={onClose}>
              닫기
            </Button>
            <Button type="submit" loading={save.isPending}>
              {notice ? '저장' : '올리기'}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
