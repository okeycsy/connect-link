import { BRANCHED_OPINION, BRANCHED_OPINION_ID, MEETING_NOTICE, asKftc, asOrg, type Chapter } from './types.ts';

/** 01 연락처 → 의견 요청 → 분기 질문·기관 회신 → 공지에서 회의 참석 → 자동 집계·리마인드·발송함. 기능이 서로 이어지는 흐름. */
export const flow: Chapter = {
  id: 'flow',
  title: '01 현행화된 연락처, 참가기관 의견조사까지 바로 연계',
  scenes: [
    {
      id: 'c-flow',
      kind: 'card',
      duration: 6600,
      card: { tone: 'light', chapter: '01 현행화된 연락처,\n참가기관 의견조사까지 바로 연계', title: '현행화한 연락처로 묻고,\n회신은 저절로 모입니다.' },
      narration: '첫째, 현행화된 기관 담당자 연락처가 참가기관 의견조사 대상에 바로 반영.',
    },
    {
      id: 'flow-contacts',
      kind: 'app',
      summary:
        'KB국민은행 담당자(김민준)로 전환 → 홈에 뜬 6개월 경과 확인 창 강조 → "변동 없음" → 확인 완료 → 닫기 → 연락처 → 정기 변경 배너 강조 → "현행화 완료"',
      run: async (d, page) => {
        await asOrg(d, { reconfirm: true });
        await page.goto('/account-info/home');
        const reconfirm = page.getByTestId('contact-reconfirm');
        await reconfirm.waitFor();
        await d.pause(300);
        d.say('연락처는 로그인할 때 묻고, 변동 없음 한 번이면 끝.');
        await d.focus(reconfirm, {
          note: '<b>KB국민은행 담당자 김민준</b>으로 로그인하면, 6개월 지난 담당자를 바로 묻습니다.',
          scale: 1.15,
        });
        await d.pause(250);
        await d.note('<b>변동 없음</b> 한 번이면 끝.');
        await d.click(reconfirm.getByRole('button', { name: '변동 없음' }));
        await reconfirm.getByText('확인 완료').waitFor();
        await d.pause(500);
        await d.unfocus();
        await d.click(reconfirm.getByRole('button', { name: '닫기' }));
        await d.pause(300);
        await d.click(page.getByRole('navigation').getByRole('link', { name: '연락처', exact: true }));
        const table = page.getByTestId('contact-table');
        await table.getByText('남궁민').waitFor();
        const banner = page.getByTestId('campaign-banner');
        d.say('정기 변경 주간엔 현행화 완료 한 번이면 됩니다.');
        await d.focus(banner, { note: '정기 변경 주간에는 <b>현행화 완료</b>를 누르면 됩니다.', scale: 1.2 });
        await d.click(banner.getByRole('button', { name: '현행화 완료' }));
        await banner.getByText(/KB국민은행 현행화 완료/).waitFor();
        await d.pause(350);
        await d.note('<b>자기 기관만</b> 보고 고칩니다. 바뀐 내역은 이력으로 남습니다.');
        await d.pause(1100);
        await d.unfocus();
        await d.pause(400);
      },
    },
    {
      id: 'flow-editor',
      kind: 'app',
      summary:
        '결제원(홍성민)으로 전환 → 새 의견 요청 화면 → "대상 기관과 회신 담당자"의 세부업무 칩과 기관 목록을 강조 → 칩 "내 계좌 한눈에" 클릭 → 신한은행 박지훈·신서준 등 그 세부업무 담당자가 자동으로 체크됨',
      run: async (d, page) => {
        await asKftc(d);
        d.say('우리 원 업무 담당자는 참가기관 의견조사 대상을 현행화한 연락처에서 세부업무별로 골라 지정합니다.');
        await page.goto('/account-info/opinions/new');
        const chips = page.getByRole('group', { name: '세부업무로 고르기' });
        await chips.waitFor();
        await d.pause(600);
        const shinhan = page.getByTestId('target-088');
        await d.focus([chips, shinhan], {
          note: '다시 결제원 담당자. 의견 요청의 대상은 <b>방금 현행화한 연락처</b>에서 고릅니다.',
          scale: 1.2,
          center: 0.5,
        });
        await d.pause(950);
        await d.note('세부 업무 하나를 누르면 해당 업무 담당자가 <b>바로 수신자로</b> 지정됩니다.');
        await d.click(chips.getByText('내 계좌 한눈에', { exact: true }));
        await page.getByRole('checkbox', { name: '신한은행 박지훈' }).waitFor();
        await d.pause(3000);
        await d.unfocus();
        await d.pause(2150);
      },
    },
    {
      id: 'flow-branch',
      kind: 'app',
      summary:
        'KB국민은행(김민준)으로 전환 → 의견 요청 "내 계좌 한눈에 응답 전문 항목 추가 검토"의 "내 회신"을 비춘 채 "반영 가능" 선택 → 섹션 2(반영 계획)가 나타남 → "반영 어려움"으로 바꿈 → 섹션 3(이유)으로 바뀜 → 이유 입력 → 최종제출',
      run: async (d, page) => {
        await asOrg(d);
        d.say('참가기관 의견조사 화면입니다. 구글폼처럼 답에 따라 다음 질문이 바뀝니다.');
        await page.goto(`/account-info/opinions/${BRANCHED_OPINION_ID}`);
        await page.getByRole('heading', { name: BRANCHED_OPINION }).waitFor();
        const reply = page.getByTestId('my-reply');
        await reply.waitFor();
        await d.focus(reply, {
          note: '구글폼처럼, 답에 따라 <b>다음 질문이 달라집니다</b>. 반영 가능이면 계획을, 어려우면 이유를.',
          scale: 1.08,
          center: 0.42,
        });
        await d.pause(600);
        await d.click(reply.getByRole('radio', { name: '반영 가능' }));
        await reply.getByText('섹션 2. 반영 계획').waitFor();
        await d.pause(3450);
        await d.click(reply.getByRole('radio', { name: '반영 어려움' }));
        await reply.getByText('섹션 3. 반영이 어려운 이유').waitFor();
        await d.pause(650);
        d.say('해당 없는 질문은 보이지 않습니다.');
        await d.note('해당 없는 질문은 보이지 않아 <b>정확한 답</b>을 받습니다. 웹에서 바로 제출.');
        await d.type(reply.getByRole('textbox', { name: /반영하기 어려운 이유/ }), '내년 상반기 전산 개편 중이라 하반기 반영을 제안합니다.', {
          delay: 2,
        });
        await d.clickUntil(reply.getByRole('button', { name: '최종제출' }), page.getByTestId('submitted-answers'));
        await d.pause(100);
        await d.unfocus();
        await d.pause(100);
      },
    },
    {
      id: 'notice-meeting',
      kind: 'app',
      summary:
        '같은 기관 담당자(김민준) → 공지·자료실 → "계좌통합관리업무 전문 변경 회의 개최 안내" 글 → 글에 이어 둔 의견 요청 칸 강조 → "회신하기" → "내 회신"을 비춘 채 "참석" 선택 → 참석자 이름·직위 입력 → 최종제출',
      run: async (d, page) => {
        await asOrg(d);
        await page.goto('/account-info/notices');
        const list = page.getByTestId('notice-list');
        await list.getByText(MEETING_NOTICE).waitFor();
        d.say('회의도, 공지 메뉴를 보다가, 개최 안내에서 바로 참석 여부를 회신합니다.');
        await d.focus(list.getByText(MEETING_NOTICE), { note: '공지·자료실에서 <b>회의 개최 안내</b>를 보다가,', scale: 1.15, hold: 800 });
        await d.click(list.getByText(MEETING_NOTICE));
        const linked = page.getByTestId('notice-opinion');
        await linked.waitFor();
        await d.pause(100);
        await d.focus(linked, { note: '글에 이어 둔 <b>회신하기</b>로 참석 여부를 바로 답합니다.', scale: 1.3, hold: 1100 });
        await d.clickUntil(page.getByRole('link', { name: '회신하기' }), page.getByText('회의에 참석하십니까?'));
        const reply = page.getByTestId('my-reply');
        await reply.waitFor();
        d.say('참석 여부와 참석자를 답하면 끝.');
        await d.focus(reply, {
          note: '회의 참석 여부와 참석자를 공지를 보다가 <b>바로 답합니다</b>. 별도로 회신 메일을 작성하지 않아도 됩니다.',
          scale: 1.1,
          center: 0.42,
        });
        await d.click(reply.getByRole('radio', { name: '참석', exact: true }));
        await d.pause(100);
        await d.type(reply.getByRole('textbox', { name: /참석자 이름과 직위/ }), '김민준 대리, 남궁민 과장', { delay: 4 });
        await d.pause(100);
        await d.clickUntil(reply.getByRole('button', { name: '최종제출' }), page.getByTestId('submitted-answers'));
        await d.pause(100);
        d.say('회신 메일 없이, 우리 원 집계에 바로 들어갑니다.');
        await d.note('제출한 답은 <b>결제원 집계</b>에 바로 들어갑니다.');
        await d.pause(1800);
        await d.unfocus();
        await d.pause(200);
      },
    },
    {
      id: 'flow-collation',
      kind: 'app',
      summary:
        '결제원(홍성민)으로 전환 → 같은 요청의 "회신 현황" 탭 → 질문별 답 집계(건너뜀 포함) 강조 → "미회신 기관 리마인드" 창 강조 → 다음: 메일 확인 → 메일 미리보기 → "N곳에 보내기" → "N곳에 보냈습니다" 강조',
      run: async (d, page) => {
        await asKftc(d);
        await page.goto(`/account-info/opinions/${BRANCHED_OPINION_ID}`);
        await page.getByRole('heading', { name: BRANCHED_OPINION }).waitFor();
        await d.click(page.getByRole('tab', { name: '회신 현황' }));
        const summary = page.getByTestId('question-summary');
        await summary.waitFor();
        d.say('질문별 답이 자동으로 모입니다.');
        await d.focus(summary, { note: '<b>질문별 답</b>이 자동으로 모입니다. 건너뛴 질문도 보입니다.', scale: 1.25, hold: 1400 });
        const remind = page.getByRole('dialog', { name: '리마인드 메일 보내기' });
        await d.clickUntil(page.getByRole('button', { name: '미회신 기관 리마인드' }), remind);
        d.say('미회신 기관엔 메일을 보내 리마인드합니다.');
        await d.focus(remind, { note: '미회신 기관에는 메일을 <b>미리 보고</b> 리마인드.', scale: 1.1, hold: 1100 });
        const check = page.getByRole('dialog', { name: '리마인드 메일 확인' });
        await d.clickUntil(remind.getByRole('button', { name: '다음: 메일 확인' }), check.getByTestId('mail-preview'));
        await d.pause(300);
        const sentNotice = page.getByTestId('remind-sent');
        await d.clickUntil(check.getByRole('button', { name: /곳에 보내기$/ }), sentNotice);
        await d.pause(300);
        // 발송함은 03 일시중단 장에서 매터모스트 알림과 함께 보여 준다.
        d.say('메일을 열어 취합하던 일이 사라집니다.');
        await d.focus(sentNotice, {
          note: '리마인드·알림 메일이 <b>자동으로</b> 나갑니다. 메일을 하나하나 열어 취합하던 일이 사라집니다.',
          scale: 1.2,
          hold: 2200,
        });
        await d.pause(150);
      },
    },
  ],
};
