import { defineConfig, devices } from '@playwright/test';

/**
 * app.jar 를 demo 프로필로 띄워 놓고 실제 브라우저로 주요 흐름을 확인한다.
 * 먼저 `./gradlew build` 로 backend/build/libs/app.jar 를 만들어야 한다.
 * 캡처는 screenshots/ 에 남기며, 화면을 바꾼 작업은 이 캡처를 사용자에게 보여 준다.
 *
 * 로그인: setup 프로젝트(tests/auth.setup.ts)가 데모 관리자로 실제 로그인 API 를 불러 세션을 저장하고,
 * 나머지 시나리오는 그 세션으로 시작한다. 로그인 화면·계정 전환은 tests/login.spec.ts 가 빈 세션으로 따로 본다.
 */
const ADMIN_STATE = 'playwright/.auth/admin.json';

export default defineConfig({
  testDir: './tests',
  timeout: 30_000,
  fullyParallel: false,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? 'github' : 'list',
  use: {
    baseURL: 'http://localhost:8080',
    locale: 'ko-KR',
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
  projects: [
    { name: 'setup', testMatch: /auth\.setup\.ts$/ },
    {
      name: 'desktop',
      dependencies: ['setup'],
      use: {
        ...devices['Desktop Chrome'],
        storageState: ADMIN_STATE,
        viewport: { width: 1280, height: 800 },
        ...(process.env.PLAYWRIGHT_CHROMIUM_PATH
          ? { launchOptions: { executablePath: process.env.PLAYWRIGHT_CHROMIUM_PATH } }
          : {}),
      },
    },
  ],
  webServer: {
    command: 'java -jar ../backend/build/libs/app.jar',
    url: 'http://localhost:8080/api/v1/health',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
