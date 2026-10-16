import { describe, expect, it } from 'vitest';

import { ApiError, fieldErrorsOf } from '../../../../api/client';
import { SIGN_UP_INITIAL_VALUES, stepErrors, stepOfField } from './signUpForm';

describe('가입 폼 규칙', () => {
  it('서버 오류 칸을 그 칸이 있는 단계로 돌려보낸다', () => {
    expect(stepOfField('email')).toBe(0);
    expect(stepOfField('organizationId')).toBe(1);
    expect(stepOfField('businesses.0.subBusiness')).toBe(1);
    expect(stepOfField('itRoles')).toBe(2);
    expect(stepOfField('extensionPhone')).toBe(3);
  });

  it('서버의 목록 칸 경로를 폼 경로로 바꾼다', () => {
    const error = new ApiError(400, {
      code: 'VALIDATION_FAILED',
      fieldErrors: [{ field: 'businesses[1].subBusiness', message: '세부업무는 200자까지' }],
    });
    expect(fieldErrorsOf(error)).toEqual({ 'businesses.1.subBusiness': '세부업무는 200자까지' });
  });

  it('고르지 않은 칸만 단계에서 막고 형식은 서버에 맡긴다', () => {
    expect(stepErrors(1, SIGN_UP_INITIAL_VALUES)).toEqual({
      organizationId: '기관을 골라 주세요.',
      businesses: '담당 업무를 하나 이상 골라 주세요.',
    });
    expect(stepErrors(2, { ...SIGN_UP_INITIAL_VALUES, jobRole: 'IT' })).toEqual({
      itRoles: '전산 세부 직무를 하나 이상 골라 주세요.',
    });
    expect(stepErrors(2, { ...SIGN_UP_INITIAL_VALUES, jobRole: 'BUSINESS' })).toEqual({});
    expect(
      stepErrors(3, { ...SIGN_UP_INITIAL_VALUES, password: 'short', passwordConfirm: 'short' }),
    ).toEqual({});
  });
});
