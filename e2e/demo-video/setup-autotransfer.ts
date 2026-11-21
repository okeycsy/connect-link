/**
 * 2차 심사 발표 영상(자동이체 해지 결과 배치파일 개선 과제)이 "이미 있었던 일"로 쓰는 데이터를 API 로 만든다.
 * 앱의 시드와 테스트 사이트(Render)는 바꾸지 않는다. run.sh 가 서버를 새로 띄운 뒤, 녹화 전에 한 번 부른다.
 *
 *   node e2e/demo-video/setup-autotransfer.ts          # DEMO_BASE_URL(기본 http://localhost:8080)
 *
 * 만드는 것(시나리오: docs/presentation/scenario.md 4절)
 * 1. 자동이체통합관리 참가기관 계정 3명(연락처에 이미 있는 담당자): KB국민은행 장서윤, 우체국 하윤슬, 저축은행중앙회 탁지유 → 문서준이 승인
 * 2. 의견 요청 "…실무 회의 참석 회신"(회신중) + 공지 "…실무 회의 개최 안내"(그 요청을 이어 둔 글). KB·우체국 참석, 저축은행중앙회 불참 회신
 * 3. 의견 요청 "…의견조사"(섹션 4개, 답별 이동, 회신중). KB·우체국·저축은행중앙회 회신
 * 4. 기관 테스트 "자동이체 해지 결과 배치파일 개선"(요구사항 5개, 대상 6곳, 테스트데이터 사전 요청). KB·우체국은 테스트데이터 생성 완료와 결과 일부
 * 5. KB국민은행 부담당 교체(백승호 → 남도윤, 변경 이력), 6. 자동이체통합관리 단체방 시행 안내 한 줄
 * 끝나면 만든 id 를 out/autotransfer.json 에 남긴다(장면 스크립트가 읽는다).
 */
import { request, type APIRequestContext } from '@playwright/test';
import { mkdir, writeFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const BASE_URL = process.env.DEMO_BASE_URL ?? 'http://localhost:8080';
const PASSWORD = 'demo1234!';

export const AT = {
  business: 'AUTO_TRANSFER',
  slug: 'auto-transfer',
  kftc: 'moon.seojun@kftc.example.com',
  ops: 'kim.ops@kftc.example.com',
  shinhan: 'lee.junho@shinhan.example.com',
  meetingNotice: '자동이체 해지 결과 배치파일 개선 실무 회의 개최 안내',
  meetingRequest: '자동이체 해지 결과 배치파일 개선 실무 회의 참석 회신',
  survey: '자동이체 해지 결과 배치파일 개선 의견조사',
  test: '자동이체 해지 결과 배치파일 개선',
} as const;

/** 가입시켜 회신·결과를 넣을 기관 담당자(연락처 시드에 이미 있는 사람, 가상). */
const ORG_USERS = [
  { code: '004', email: 'jang.seoyun@kb.example.com', name: '장서윤', department: '디지털금융부', team: '수신결제팀', jobTitle: '차장', phone: '02-0000-2301', sub: '자동이체 통합조회' },
  { code: '071', email: 'ha.yunseul@epost.example.com', name: '하윤슬', department: '결제사업부', team: '자동납부팀', jobTitle: '대리', phone: '02-0000-8101', sub: '카드 자동납부' },
  { code: '050', email: 'tak.jiyu@fsb.example.com', name: '탁지유', department: '수신업무부', team: '전자금융팀', jobTitle: '주임', phone: '02-0000-6201', sub: null },
];
const TARGET_CODES = ['004', '088', '240', '071', '050', '048'];

function day(offset: number) {
  const d = new Date(Date.now() + 9 * 3600_000 + offset * 86400_000); // 서울 기준 날짜
  return d.toISOString().slice(0, 10);
}

async function session(email?: string) {
  const ctx = await request.newContext({ baseURL: BASE_URL });
  await ctx.get('/api/v1/health');
  if (email) {
    const res = await ctx.post('/api/v1/auth/login', { data: { email, password: PASSWORD }, headers: await csrf(ctx) });
    if (!res.ok()) throw new Error(`로그인 실패 ${email}: ${res.status()} ${await res.text()}`);
  }
  return ctx;
}

async function csrf(ctx: APIRequestContext) {
  const token = (await ctx.storageState()).cookies.find((c) => c.name === 'XSRF-TOKEN')?.value;
  return { 'X-XSRF-TOKEN': token ?? '' };
}

async function call<T = any>(ctx: APIRequestContext, method: 'get' | 'post' | 'put', url: string, data?: unknown): Promise<T> {
  const res = await ctx[method](url, method === 'get' ? undefined : { data: data ?? {}, headers: await csrf(ctx) });
  if (!res.ok()) throw new Error(`${method.toUpperCase()} ${url} → ${res.status()} ${await res.text()}`);
  const body = await res.text();
  return (body ? JSON.parse(body) : undefined) as T;
}

async function signUp(orgIds: Record<string, number>, u: (typeof ORG_USERS)[number]) {
  const ctx = await session();
  await call(ctx, 'post', '/api/v1/email-verifications', { email: u.email });
  const mails = await call<{ body: string }[]>(ctx, 'get', `/api/v1/demo/mailbox?email=${encodeURIComponent(u.email)}`);
  const code = /인증 코드: (\d{6})/.exec(mails[0]?.body ?? '')?.[1];
  if (!code) throw new Error('인증 코드를 못 찾았습니다: ' + u.email);
  const { verificationToken } = await call(ctx, 'post', '/api/v1/email-verifications/confirm', { email: u.email, code });
  await call(ctx, 'post', '/api/v1/sign-ups', {
    verificationToken,
    email: u.email,
    organizationId: orgIds[u.code],
    businesses: [{ business: AT.business, subBusiness: u.sub }],
    jobRole: 'BUSINESS',
    itRoles: [],
    name: u.name,
    department: u.department,
    team: u.team,
    jobTitle: u.jobTitle,
    extensionPhone: u.phone,
    mobilePhone: null,
    password: PASSWORD,
    privacyNoticeConfirmed: true,
  });
  await ctx.dispose();
}

/** 내 회신을 임시저장하고 제출한다. answers: 질문 제목 → 고른 선택지 이름들 또는 글. */
async function reply(email: string, requestId: number, answers: Record<string, string[] | string>) {
  const ctx = await session(email);
  const mine = await call(ctx, 'get', `/api/v1/opinion-requests/${requestId}/my-reply`);
  const detail = await call(ctx, 'get', `/api/v1/opinion-requests/${requestId}`);
  const questions: any[] = detail.questions ?? mine.questions ?? [];
  const list = Object.entries(answers).map(([title, value]) => {
    const q = questions.find((x) => x.title === title);
    if (!q) throw new Error('질문을 못 찾았습니다: ' + title);
    if (typeof value === 'string') return { questionId: q.id, optionIds: [], text: value };
    const optionIds = value.map((label) => {
      const o = q.options.find((x: any) => x.label === label);
      if (!o) throw new Error('선택지를 못 찾았습니다: ' + label);
      return o.id;
    });
    return { questionId: q.id, optionIds, text: null };
  });
  await call(ctx, 'post', `/api/v1/opinion-requests/${requestId}/my-reply/submission`, { answers: list });
  await ctx.dispose();
}

async function main() {
  const kftc = await session(AT.kftc);
  const orgs = await call<any[]>(kftc, 'get', '/api/v1/organizations');
  const orgIds: Record<string, number> = Object.fromEntries(orgs.map((o) => [o.code, o.id]));

  // 1. 기관 담당자 가입 → 문서준 승인
  for (const u of ORG_USERS) await signUp(orgIds, u);
  const pending = await call(kftc, 'get', `/api/v1/sign-ups?business=${AT.slug}&status=PENDING_APPROVAL&size=100`);
  for (const a of pending.content) {
    if (ORG_USERS.some((u) => u.email === a.email)) await call(kftc, 'post', `/api/v1/sign-ups/${a.id}/approval`, {});
  }

  // 대상 기관별 회신 담당자: 자동이체통합관리 연락처 전원
  const contacts = await call(kftc, 'get', `/api/v1/contacts?business=${AT.slug}&size=200`);
  const byOrg: Record<number, string[]> = {};
  for (const c of contacts.content ?? contacts) {
    const orgId = c.organizationId ?? c.organization?.id;
    if (c.email && orgId) (byOrg[orgId] ??= []).push(c.email);
  }
  const targets = TARGET_CODES.map((code) => ({ organizationId: orgIds[code], recipientEmails: byOrg[orgIds[code]] ?? [] }));

  // 2. 실무 회의 참석 회신 + 개최 안내 공지
  const meeting = await call(kftc, 'post', '/api/v1/opinion-requests', {
    business: AT.business,
    title: AT.meetingRequest,
    content: '해지 결과 배치파일 개선 실무 회의 참석 여부와 참석자를 알려 주세요. 회의 안내는 공지·자료실에 있습니다.',
    dueDate: day(3),
    sections: [],
    questions: [
      { sectionIndex: 0, type: 'SINGLE_CHOICE', title: '회의에 참석하십니까?', required: true, options: [{ label: '참석' }, { label: '불참' }] },
      { sectionIndex: 0, type: 'TEXT', title: '참석자 이름과 직위를 적어 주세요.', required: false, options: [] },
    ],
    targets,
  });
  await call(kftc, 'post', `/api/v1/opinion-requests/${meeting.id}/opening`, {});
  const notice = await call(kftc, 'post', '/api/v1/notices', {
    business: AT.business,
    category: 'NOTICE',
    title: AT.meetingNotice,
    content:
      '자동이체 해지 처리결과 파일을 당일 2회(13시·18시) 회신하고 불가 사유코드를 더하는 개선안을 논의합니다.\n' +
      `일시: ${day(7)} 14:00, 장소: 금융결제원 본관 회의실(가상)\n참석 여부와 참석자는 아래 "회신하기"로 알려 주세요.`,
    pinned: true,
    effectiveDate: null,
    opinionRequestId: meeting.id,
  });
  await reply('jang.seoyun@kb.example.com', meeting.id, { '회의에 참석하십니까?': ['참석'], '참석자 이름과 직위를 적어 주세요.': '장서윤 차장' });
  await reply('ha.yunseul@epost.example.com', meeting.id, { '회의에 참석하십니까?': ['참석'], '참석자 이름과 직위를 적어 주세요.': '하윤슬 대리' });
  await reply('tak.jiyu@fsb.example.com', meeting.id, { '회의에 참석하십니까?': ['불참'] });

  // 3. 의견조사(섹션·답별 이동)
  const survey = await call(kftc, 'post', '/api/v1/opinion-requests', {
    business: AT.business,
    title: AT.survey,
    content:
      '실무 회의에서 논의한 개선안(처리결과 파일 당일 2회 회신, 불가 사유코드 추가)에 대한 기관별 의견을 받습니다. 회신 내용으로 업무 요건을 확정합니다.',
    dueDate: day(10),
    sections: [
      { title: '당일 2회 회신', description: null, jump: null },
      { title: '적용 계획', description: '회신할 수 있는 기관만 답합니다.', jump: { type: 'SECTION', sectionIndex: 3 } },
      { title: '어려운 이유', description: '회신하기 어려운 기관만 답합니다.', jump: null },
      { title: '공통', description: null, jump: null },
    ],
    questions: [
      {
        sectionIndex: 0,
        type: 'SINGLE_CHOICE',
        title: '처리결과 파일을 당일 2회(13시·18시) 회신할 수 있습니까?',
        required: true,
        options: [
          { label: '가능', jump: { type: 'SECTION', sectionIndex: 1 } },
          { label: '어려움', jump: { type: 'SECTION', sectionIndex: 2 } },
        ],
      },
      { sectionIndex: 1, type: 'SINGLE_CHOICE', title: '적용할 수 있는 시기를 골라 주세요.', required: true, options: [{ label: '1분기' }, { label: '2분기' }, { label: '하반기' }] },
      { sectionIndex: 2, type: 'TEXT', title: '어려운 이유와 대안을 적어 주세요.', required: true, options: [] },
      {
        sectionIndex: 3,
        type: 'MULTIPLE_CHOICE',
        title: '추가 개발이 필요한 불가 사유코드를 모두 골라 주세요.',
        required: false,
        options: [{ label: '미납 잔액 있음' }, { label: '기관에서 직접 해지 필요' }, { label: '출금계좌 해지됨' }],
      },
      { sectionIndex: 3, type: 'TEXT', title: '개발에 필요한 기간을 적어 주세요.', required: true, options: [] },
    ],
    targets,
  });
  await call(kftc, 'post', `/api/v1/opinion-requests/${survey.id}/opening`, {});
  const q1 = '처리결과 파일을 당일 2회(13시·18시) 회신할 수 있습니까?';
  await reply('jang.seoyun@kb.example.com', survey.id, {
    [q1]: ['가능'],
    '적용할 수 있는 시기를 골라 주세요.': ['2분기'],
    '추가 개발이 필요한 불가 사유코드를 모두 골라 주세요.': ['미납 잔액 있음', '출금계좌 해지됨'],
    '개발에 필요한 기간을 적어 주세요.': '약 2개월',
  });
  await reply('ha.yunseul@epost.example.com', survey.id, {
    [q1]: ['어려움'],
    '어려운 이유와 대안을 적어 주세요.': '18시 회신은 야간 배치와 겹쳐 어렵습니다. 13시 1회 추가를 제안합니다.',
    '추가 개발이 필요한 불가 사유코드를 모두 골라 주세요.': ['기관에서 직접 해지 필요'],
    '개발에 필요한 기간을 적어 주세요.': '약 3개월',
  });
  await reply('tak.jiyu@fsb.example.com', survey.id, {
    [q1]: ['가능'],
    '적용할 수 있는 시기를 골라 주세요.': ['2분기'],
    '추가 개발이 필요한 불가 사유코드를 모두 골라 주세요.': ['미납 잔액 있음', '기관에서 직접 해지 필요', '출금계좌 해지됨'],
    '개발에 필요한 기간을 적어 주세요.': '중앙회 공동 개발로 약 2개월',
  });

  // 4. 기관 테스트
  const plan = await call(kftc, 'post', '/api/v1/test-plans', {
    business: AT.business,
    subBusiness: '자동이체 해지',
    title: AT.test,
    description:
      '확정 요건: 처리결과 파일 당일 2회(13시·18시) 회신, 불가 사유코드 3종 추가. 기관별로 신규 레이아웃 파일 송수신과 사유코드 처리를 확인합니다.',
    startDate: day(0),
    endDate: day(14),
    items: [
      { title: '신규 레이아웃 해지 요청 파일 수신', description: '불가 사유코드 칸이 더해진 레이아웃으로 받는지 확인합니다.' },
      { title: '13시 1차 처리결과 파일 회신', description: null },
      { title: '18시 2차 처리결과 파일 회신', description: null },
      { title: '불가 사유코드별 처리', description: '미납 잔액 있음, 기관에서 직접 해지 필요, 출금계좌 해지됨 세 가지를 모두 확인합니다.' },
      { title: '오류 파일 재송신', description: null },
    ],
    organizationIds: TARGET_CODES.map((c) => orgIds[c]),
    targetContacts: TARGET_CODES.map((code) => ({ organizationId: orgIds[code], emails: byOrg[orgIds[code]] ?? [] })),
    testDataRequest: { dueDate: day(0), instruction: '불가 사유코드별로 해지 테스트 계좌를 하나씩(3개 이상) 만들어 계좌번호와 상태를 적어 주세요.' },
  });
  const items: { id: number }[] = plan.items;
  const kbOrg = orgIds['004'];
  const postOrg = orgIds['071'];
  for (const [email, orgId, note] of [
    ['jang.seoyun@kb.example.com', kbOrg, '사유코드별 테스트 계좌 3개 생성'],
    ['ha.yunseul@epost.example.com', postOrg, '테스트 계좌 4개 생성(사유코드 3종 + 정상 해지)'],
  ] as const) {
    const ctx = await session(email);
    await call(ctx, 'put', `/api/v1/test-plans/${plan.id}/targets/${orgId}/test-data`, { status: 'CREATED', note });
    await ctx.dispose();
  }
  {
    const ctx = await session('jang.seoyun@kb.example.com');
    await call(ctx, 'put', `/api/v1/test-plans/${plan.id}/targets/${kbOrg}/results`, {
      results: [
        { itemId: items[0].id, result: 'PASSED', note: null },
        { itemId: items[1].id, result: 'PASSED', note: null },
        { itemId: items[2].id, result: 'IN_PROGRESS', note: '18시 배치 일정 조정 중' },
      ],
    });
    await ctx.dispose();
  }
  {
    const ctx = await session('ha.yunseul@epost.example.com');
    await call(ctx, 'put', `/api/v1/test-plans/${plan.id}/targets/${postOrg}/results`, {
      results: [{ itemId: items[0].id, result: 'PASSED', note: null }],
    });
    await ctx.dispose();
  }

  // 5. 연락처 담당자 교체 이력: KB국민은행 장서윤이 부담당을 백승호에서 남도윤으로 바꾼다(prep 장면의 변경 이력, 이전 담당자 휴대폰은 가려서 보인다).
  {
    const ctx = await session('jang.seoyun@kb.example.com');
    const base = {
      business: AT.business,
      organizationId: orgIds['004'],
      subBusiness: '자동이체 해지',
      department: '디지털금융부',
      jobTitle: '대리',
      jobRole: 'BUSINESS',
      itRoles: [],
      rank: 'SECONDARY',
    };
    const before = await call(ctx, 'post', '/api/v1/contacts', {
      ...base,
      name: '백승호',
      team: '수신결제팀',
      extensionPhone: '02-0000-2320',
      mobilePhone: '010-0000-2320',
      email: 'baek.seungho@kb.example.com',
    });
    await call(ctx, 'put', `/api/v1/contacts/${before.id}`, {
      ...base,
      name: '남도윤',
      team: '자동납부팀',
      extensionPhone: '02-0000-2324',
      mobilePhone: '010-0000-2324',
      email: 'nam.doyun@kb.example.com',
    });
    await ctx.dispose();
  }

  // 6. 자동이체통합관리 단체방: 시행 당일 안내 한 줄(ops 장면이 이어서 지연 안내를 쓴다).
  {
    const rooms = await call<any>(kftc, 'get', `/api/v1/chat-rooms?business=${AT.slug}`);
    const room = (rooms.content ?? rooms).find((r: any) => r.business === AT.business && r.type === 'BUSINESS');
    if (!room) throw new Error('자동이체통합관리 단체방을 못 찾았습니다');
    await call(kftc, 'post', `/api/v1/chat-rooms/${room.id}/messages`, {
      content: '오늘부터 해지 처리결과 파일을 당일 2회(13시·18시) 회신합니다. 기관별로 수신 여부를 확인해 주세요.',
      mentionOrganizationCodes: [],
    });
  }

  const out = { meetingRequestId: meeting.id, noticeId: notice.id, surveyId: survey.id, testPlanId: plan.id, orgIds };
  await mkdir(path.join(here, 'out'), { recursive: true });
  await writeFile(path.join(here, 'out', 'autotransfer.json'), JSON.stringify(out, null, 2));
  console.log('자동이체 시나리오 데이터를 만들었습니다:', out);
  await kftc.dispose();
}

if (process.argv[1] === fileURLToPath(import.meta.url)) {
  main().catch((e) => {
    console.error(e);
    process.exit(1);
  });
}
