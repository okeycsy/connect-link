import type { UseFormReturnType } from '@mantine/form';

import type { BusinessCode } from '../../../organization/hooks';
import type { SignUpRequest } from '../../hooks';

export type JobRoleCode = SignUpRequest['jobRole'];
export type ItRoleCode = NonNullable<NonNullable<SignUpRequest['itRoles']>[number]>;

/** 가입 화면 네 단계가 함께 쓰는 폼 값. 서버로는 {@link toSignUpRequest} 로 바꿔 한 번에 보낸다. */
export interface SignUpFormValues {
  email: string;
  code: string;
  /** Select 값이라 문자열이다. */
  organizationId: string | null;
  /** 고른 업무와 세부업무. 업무 순서로 둔다(서버 오류 경로 businesses.0.subBusiness 와 맞춘다). */
  businesses: { business: BusinessCode; subBusiness: string }[];
  jobRole: JobRoleCode | null;
  itRoles: ItRoleCode[];
  name: string;
  department: string;
  team: string;
  jobTitle: string;
  extensionPhone: string;
  mobilePhone: string;
  password: string;
  /** 화면에서만 확인한다(서버로 보내지 않는다). */
  passwordConfirm: string;
}

export type SignUpForm = UseFormReturnType<SignUpFormValues>;

export const SIGN_UP_INITIAL_VALUES: SignUpFormValues = {
  email: '',
  code: '',
  organizationId: null,
  businesses: [],
  jobRole: null,
  itRoles: [],
  name: '',
  department: '',
  team: '',
  jobTitle: '',
  extensionPhone: '',
  mobilePhone: '',
  password: '',
  passwordConfirm: '',
};

/** 화면 단계. 명세의 가입 항목을 docs/design/common-screens.md 의 4단계로 나눴다. */
export const SIGN_UP_STEPS = [
  { label: '회사 이메일 인증', description: '인증 코드 확인' },
  { label: '기관과 업무', description: '소속 기관, 담당 업무' },
  { label: '직무', description: '현업 또는 전산' },
  { label: '담당자 정보', description: '부서·연락처, 비밀번호' },
] as const;

/** 서버 오류 칸이 있는 단계. 신청이 실패하면 가장 앞 단계로 돌아가 그 칸 아래 메시지를 보여 준다. */
export function stepOfField(field: string): number {
  if (field === 'email' || field === 'code' || field === 'verificationToken') return 0;
  if (field === 'organizationId' || field.startsWith('businesses')) return 1;
  if (field === 'jobRole' || field.startsWith('itRoles')) return 2;
  return 3;
}

/**
 * 단계를 넘기기 전 화면 확인. 고르지 않은 칸만 본다(문구는 서버 검증과 같다). 형식 규칙(비밀번호, 전화번호)은 신청할 때 서버가 보고,
 * 화면은 그 메시지를 그대로 보여 준다.
 */
export function stepErrors(step: number, values: SignUpFormValues): Record<string, string> {
  const errors: Record<string, string> = {};
  if (step === 1) {
    if (!values.organizationId) errors.organizationId = '기관을 골라 주세요.';
    if (values.businesses.length === 0) errors.businesses = '담당 업무를 하나 이상 골라 주세요.';
  }
  if (step === 2) {
    if (!values.jobRole) errors.jobRole = '직무를 골라 주세요.';
    if (values.jobRole === 'IT' && values.itRoles.length === 0) {
      errors.itRoles = '전산 세부 직무를 하나 이상 골라 주세요.';
    }
  }
  if (step === 3 && values.password !== values.passwordConfirm) {
    errors.passwordConfirm = '비밀번호가 서로 다릅니다.';
  }
  return errors;
}

export function toSignUpRequest(
  values: SignUpFormValues,
  email: string,
  verificationToken: string,
): SignUpRequest {
  return {
    verificationToken,
    email,
    organizationId: Number(values.organizationId),
    businesses: values.businesses.map((b) => ({
      business: b.business,
      subBusiness: b.subBusiness.trim() || null,
    })),
    // 고르지 않았으면 null 그대로 보내 서버가 직무 칸 오류로 돌려준다(단계 확인이 먼저 막는다).
    jobRole: values.jobRole as JobRoleCode,
    itRoles: values.jobRole === 'IT' ? values.itRoles : [],
    name: values.name,
    department: values.department,
    team: values.team,
    jobTitle: values.jobTitle,
    extensionPhone: values.extensionPhone,
    mobilePhone: values.mobilePhone.trim() || null,
    password: values.password,
  };
}
