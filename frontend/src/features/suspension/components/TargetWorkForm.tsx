import {
  Autocomplete,
  Button,
  Checkbox,
  Group,
  NumberInput,
  Stack,
  Switch,
  TextInput,
} from '@mantine/core';
import { useForm } from '@mantine/form';
import { notifications } from '@mantine/notifications';

import { ApiError, fieldErrorsOf } from '../../../api/client';
import { type BusinessCode, useBusinesses } from '../../organization/hooks';
import { type TargetWork, useSaveTargetWork } from '../hooks';

interface Values {
  name: string;
  category: string;
  sortOrder: number | string;
  active: boolean;
  businesses: BusinessCode[];
}

interface Props {
  /** 고칠 대상 업무. 없으면 새로 더한다. */
  work?: TargetWork;
  /** 묶음 입력의 추천값(이미 쓰는 묶음). */
  categories: string[];
  /** 새로 더할 때 순서 기본값. */
  nextSortOrder: number;
  onSaved: (saved: TargetWork) => void;
  onCancel: () => void;
}

/** 대상 업무 하나의 더하기·고치기 폼. 검증 메시지는 서버의 필드 오류를 그대로 보여 준다. */
export function TargetWorkForm({ work, categories, nextSortOrder, onSaved, onCancel }: Props) {
  const { data: businesses } = useBusinesses();
  const save = useSaveTargetWork();
  const form = useForm<Values>({
    initialValues: {
      name: work?.name ?? '',
      category: work?.category ?? '',
      sortOrder: work?.sortOrder ?? nextSortOrder,
      active: work?.active ?? true,
      businesses: work?.businesses.map((b) => b.code) ?? [],
    },
  });

  const submit = form.onSubmit(async (values) => {
    try {
      const saved = await save.mutateAsync({
        id: work?.id,
        body: {
          name: values.name,
          category: values.category.trim() || null,
          sortOrder: typeof values.sortOrder === 'number' ? values.sortOrder : 0,
          active: values.active,
          businesses: values.businesses,
        },
      });
      notifications.show({
        message: work ? '대상 업무를 저장했습니다.' : '대상 업무를 더했습니다.',
        color: 'clay',
      });
      onSaved(saved);
    } catch (error) {
      const fieldErrors = fieldErrorsOf(error);
      if (Object.keys(fieldErrors).length > 0) {
        form.setErrors(fieldErrors);
        return;
      }
      notifications.show({
        title: '저장하지 못했습니다',
        message: error instanceof ApiError ? error.message : '잠시 후 다시 시도해 주세요.',
        color: 'red',
      });
    }
  });

  return (
    <form onSubmit={submit} noValidate>
      <Stack gap="md">
        <TextInput
          label="업무 이름"
          placeholder="예: 타행환공동망"
          required
          maxLength={100}
          {...form.getInputProps('name')}
        />
        <Group grow align="flex-start">
          <Autocomplete
            label="묶음"
            description="등록 화면에서 이 이름으로 묶어 보입니다."
            placeholder="예: 지급결제"
            data={categories}
            maxLength={50}
            {...form.getInputProps('category')}
          />
          <NumberInput
            label="순서"
            description="작을수록 앞입니다."
            min={0}
            allowDecimal={false}
            required
            {...form.getInputProps('sortOrder')}
          />
        </Group>
        <Checkbox.Group
          label="관련 업무"
          description="고른 업무 영역에 일시중단이 보이고 그 업무 결제원 담당자가 알림을 받아 확인합니다. 비우면 결제원 운영·상황실·대외계가 맡습니다."
          {...form.getInputProps('businesses')}
        >
          <Group gap="md" mt={6}>
            {businesses?.map((b) => (
              <Checkbox key={b.code} value={b.code} label={b.label} />
            ))}
          </Group>
        </Checkbox.Group>
        <Switch
          label="사용 중"
          description="끄면 새 일시중단에서 고를 수 없습니다. 이미 고른 일시중단에는 남습니다."
          {...form.getInputProps('active', { type: 'checkbox' })}
        />
        <Group justify="flex-end" gap="xs">
          <Button variant="subtle" color="gray" onClick={onCancel} disabled={save.isPending}>
            닫기
          </Button>
          <Button type="submit" loading={save.isPending}>
            {work ? '저장' : '더하기'}
          </Button>
        </Group>
      </Stack>
    </form>
  );
}
