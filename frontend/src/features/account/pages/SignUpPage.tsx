import { Alert, Button, Card, Group, Stack, Stepper } from '@mantine/core';
import { useForm } from '@mantine/form';
import { IconArrowLeft } from '@tabler/icons-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { fieldErrorsOf } from '../../../api/client';
import { isDemoProfile, useServerInfo } from '../../../app/hooks';
import { PageHeader } from '../../../shared/PageHeader';
import { useBusinesses, useJobRoles, useOrganizations } from '../../organization/hooks';
import { EntryContent, EntryLayout } from '../components/EntryLayout';
import { EmailStep, type SentCode } from '../components/signup/EmailStep';
import { JobRoleStep } from '../components/signup/JobRoleStep';
import { OrganizationStep } from '../components/signup/OrganizationStep';
import { ProfileStep } from '../components/signup/ProfileStep';
import {
  SIGN_UP_INITIAL_VALUES,
  SIGN_UP_STEPS,
  type SignUpFormValues,
  stepErrors,
  stepOfField,
  toSignUpRequest,
} from '../components/signup/signUpForm';
import { type EmailVerificationConfirmResponse, useSignUp } from '../hooks';

const LAST_STEP = SIGN_UP_STEPS.length - 1;

/**
 * 회원가입 4단계: ① 회사 이메일 인증 ② 기관과 업무 ③ 직무 ④ 담당자 정보와 비밀번호. 결제원 직원과 기관 담당자가 같은 절차를 쓴다.
 * 단계마다 서버로 보내지 않고 마지막에 한 번에 신청하며, 서버가 모든 칸을 다시 검증한다. 신청하면 승인 대기 화면으로 간다.
 */
export function SignUpPage() {
  const navigate = useNavigate();
  const { data: serverInfo } = useServerInfo();
  const demo = isDemoProfile(serverInfo?.profile);
  const signUp = useSignUp();
  // 뒤 단계의 선택지(기관, 업무, 직무)를 미리 읽어 단계를 넘길 때 기다리지 않게 한다.
  useOrganizations();
  useBusinesses();
  useJobRoles();
  const [active, setActive] = useState(0);
  const [sent, setSent] = useState<SentCode>();
  const [verified, setVerified] = useState<EmailVerificationConfirmResponse>();
  const form = useForm<SignUpFormValues>({ initialValues: SIGN_UP_INITIAL_VALUES });

  const onVerified = (result: EmailVerificationConfirmResponse) => {
    setVerified(result);
    // 이메일 도메인이 같은 기관을 미리 골라 둔다. 바꿀 수 있다.
    const [suggested] = result.suggestedOrganizationIds;
    if (!form.values.organizationId && suggested !== undefined) {
      form.setFieldValue('organizationId', String(suggested));
    }
  };

  const resetVerification = () => {
    setSent(undefined);
    setVerified(undefined);
    form.setFieldValue('code', '');
  };

  const next = () => {
    const errors = stepErrors(active, form.values);
    if (Object.keys(errors).length > 0) {
      form.setErrors(errors);
      return;
    }
    if (active < LAST_STEP) {
      setActive(active + 1);
      return;
    }
    if (!verified) {
      setActive(0);
      return;
    }
    signUp.mutate(toSignUpRequest(form.values, verified.email, verified.verificationToken), {
      onSuccess: (result) => navigate('/sign-up/pending', { replace: true, state: result }),
      onError: (error) => {
        const fields = fieldErrorsOf(error);
        const keys = Object.keys(fields);
        if (keys.length === 0) return;
        form.setErrors(fields);
        // 인증이 끝났거나 이미 가입한 이메일이면 이메일 인증부터 다시 한다.
        if (keys.includes('email')) resetVerification();
        setActive(Math.min(...keys.map(stepOfField)));
      },
    });
  };

  const serverAlert =
    signUp.isError && Object.keys(fieldErrorsOf(signUp.error)).length === 0
      ? signUp.error.message
      : undefined;

  return (
    <EntryLayout>
      <Stack gap="md">
        <PageHeader
          title="회원가입"
          description="회사 이메일을 인증하고 소속과 담당 업무를 적으면, 담당 업무의 결제원 담당자가 승인합니다."
        />
        <EntryContent>
          <form
            noValidate
            onSubmit={(event) => {
              event.preventDefault();
              next();
            }}
          >
            <Stack gap="lg">
              <Stepper
                active={active}
                onStepClick={setActive}
                allowNextStepsSelect={false}
                size="sm"
              >
                {SIGN_UP_STEPS.map((s) => (
                  <Stepper.Step key={s.label} label={s.label} description={s.description} />
                ))}
              </Stepper>

              <Card data-testid={`sign-up-step-${active + 1}`}>
                {active === 0 && (
                  <EmailStep
                    form={form}
                    sent={sent}
                    verified={verified}
                    demo={demo}
                    onSent={setSent}
                    onVerified={onVerified}
                    onReset={resetVerification}
                  />
                )}
                {active === 1 && (
                  <OrganizationStep
                    form={form}
                    suggestedOrganizationIds={verified?.suggestedOrganizationIds ?? []}
                  />
                )}
                {active === 2 && <JobRoleStep form={form} />}
                {active === 3 && <ProfileStep form={form} email={verified?.email ?? ''} />}
              </Card>

              {serverAlert && (
                <Alert variant="light" color="red">
                  {serverAlert}
                </Alert>
              )}

              <Group justify="space-between">
                {active > 0 ? (
                  <Button
                    variant="light"
                    color="gray"
                    leftSection={<IconArrowLeft size={16} />}
                    onClick={() => setActive(active - 1)}
                  >
                    이전
                  </Button>
                ) : (
                  <span />
                )}
                <Button
                  type="submit"
                  disabled={active === 0 && !verified}
                  loading={signUp.isPending}
                >
                  {active === LAST_STEP ? '가입 신청' : '다음'}
                </Button>
              </Group>
            </Stack>
          </form>
        </EntryContent>
      </Stack>
    </EntryLayout>
  );
}
