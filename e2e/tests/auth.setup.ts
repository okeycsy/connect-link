import { expect, test as setup } from '@playwright/test';

/** 시나리오 공통 세션. 로그인 화면을 거치지 않고 로그인 API 로 데모 관리자 세션을 만들어 저장한다. */
setup('데모 관리자로 로그인해 세션을 저장한다', async ({ request }) => {
  // 아무 요청이나 한 번 보내면 서버가 CSRF 토큰 쿠키(XSRF-TOKEN)를 준다.
  const health = await request.get('/api/v1/health');
  expect(health.ok()).toBeTruthy();
  const { cookies } = await request.storageState();
  const token = cookies.find((c) => c.name === 'XSRF-TOKEN')?.value;
  expect(token).toBeTruthy();

  const login = await request.post('/api/v1/auth/login', {
    data: { email: 'admin@kftc.example.com', password: 'demo1234!' },
    headers: { 'X-XSRF-TOKEN': token! },
  });
  expect(login.ok()).toBeTruthy();
  expect((await login.json()).admin).toBe(true);

  await request.storageState({ path: 'playwright/.auth/admin.json' });
});
