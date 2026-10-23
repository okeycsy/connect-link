-- 소통 채널 샘플. 전부 가상 데이터다. local, demo 프로필(과 테스트)에서만 읽는다.
-- 기관(R__1)·계정(R__2) 시드 뒤에 돈다. 업무마다 단체방이 있고, 계좌통합관리에는 결제원 담당자가 만든 초대방이 하나 더 있다.
-- 계좌통합관리 단체방에는 업무 밖 결제원 사람(CMS 박하늘)을 따로 초대해 두었고, 멘션·확인 공감·읽지 않은 메시지가 함께 보인다.
DELETE FROM chat_message_ack;
DELETE FROM chat_message_mention;
DELETE FROM chat_message;
DELETE FROM chat_read_mark;
DELETE FROM chat_room_member;
DELETE FROM chat_room;

INSERT INTO chat_room (business, type, name, created_by_name, created_at, created_by, updated_at, updated_by)
SELECT r.business, r.type, r.name, r.created_by_name,
       CURRENT_TIMESTAMP - r.days_ago * INTERVAL '1' DAY, 'seed', CURRENT_TIMESTAMP - r.days_ago * INTERVAL '1' DAY, 'seed'
FROM (VALUES
  (1, 'ACCOUNT_INFO',  'BUSINESS', '계좌통합관리 단체방',         '연결이', 30),
  (2, 'AUTO_TRANSFER', 'BUSINESS', '자동이체통합관리 단체방',     '연결이', 30),
  (3, 'LOAN_TRANSFER', 'BUSINESS', '대출이동 단체방',             '연결이', 30),
  (4, 'CMS',           'BUSINESS', 'CMS 단체방',                  '연결이', 30),
  (5, 'ACCOUNT_INFO',  'CUSTOM',   '오픈뱅킹 안심차단 전문 테스트', '홍성민',   5)
) AS r (seq, business, type, name, created_by_name, days_ago)
ORDER BY r.seq;

-- 초대한 사람. 초대방은 만든 홍성민이 먼저 들어가 있다.
INSERT INTO chat_room_member (room_id, email, name, organization_code, organization_name, organization_type, invited_by_name,
                              created_at, created_by, updated_at, updated_by)
SELECT c.id, u.email, u.name, o.code, o.name, o.type, m.invited_by_name,
       c.created_at, 'seed', c.created_at, 'seed'
FROM (VALUES
  ('계좌통합관리 단체방',         'park.cms@kftc.example.com',      '홍성민'),
  ('오픈뱅킹 안심차단 전문 테스트', 'hong.sungmin@kftc.example.com',  '홍성민'),
  ('오픈뱅킹 안심차단 전문 테스트', 'kim.ops@kftc.example.com',       '홍성민'),
  ('오픈뱅킹 안심차단 전문 테스트', 'choi.yujin@samsung.example.com', '홍성민'),
  ('오픈뱅킹 안심차단 전문 테스트', 'yoon.seoyeon@fsb.example.com',   '홍성민')
) AS m (room_name, email, invited_by_name)
JOIN chat_room c ON c.name = m.room_name
JOIN user_account u ON u.email = m.email
JOIN organization o ON o.id = u.organization_id;

-- 메시지. minutes_ago 가 클수록 먼저 보냈다(아이디도 그 순서다).
INSERT INTO chat_message (room_id, sender_email, sender_name, sender_organization_code, sender_organization_name, sender_organization_type,
                          content, with_attachments, attachments_expired, created_at, created_by, updated_at, updated_by)
SELECT c.id, u.email, u.name, o.code, o.name, o.type, m.content, FALSE, FALSE,
       CURRENT_TIMESTAMP - m.minutes_ago * INTERVAL '1' MINUTE, u.email,
       CURRENT_TIMESTAMP - m.minutes_ago * INTERVAL '1' MINUTE, u.email
FROM (VALUES
  (1, '계좌통합관리 단체방', 'hong.sungmin@kftc.example.com',  4320, '안녕하세요, 계좌통합관리 단체방입니다. 장애·작업 공지는 이 방에서 함께 확인해 주세요.'),
  (2, '계좌통합관리 단체방', 'kim.minjun@kb.example.com',      4300, 'KB국민은행 김민준입니다. 잘 부탁드립니다.'),
  (3, '계좌통합관리 단체방', 'hong.sungmin@kftc.example.com',  180,  '@KB국민은행 @삼성증권 내일 10시에 계좌정보 조회 전문 응답코드 변경을 반영합니다. 확인 부탁드립니다.'),
  (4, '계좌통합관리 단체방', 'kim.ops@kftc.example.com',       120,  '반영 시간 동안 운영팀이 상황을 함께 봅니다. 이상이 있으면 바로 알려 주세요.'),
  (5, '계좌통합관리 단체방', 'choi.yujin@samsung.example.com', 25,   '@결제원 삼성증권은 오늘 18시 이후에 변경 전문 테스트가 가능합니다.'),
  (6, '오픈뱅킹 안심차단 전문 테스트', 'hong.sungmin@kftc.example.com', 2880, '오픈뱅킹 안심차단 전문 테스트 케이스를 이 방에서 주고받겠습니다.'),
  (7, '오픈뱅킹 안심차단 전문 테스트', 'yoon.seoyeon@fsb.example.com',  2800, '테스트 케이스 받았습니다. 금요일까지 결과 올리겠습니다.')
) AS m (seq, room_name, email, minutes_ago, content)
JOIN chat_room c ON c.name = m.room_name
JOIN user_account u ON u.email = m.email
JOIN organization o ON o.id = u.organization_id
ORDER BY m.seq;

INSERT INTO chat_message_mention (message_id, organization_code, created_at, created_by, updated_at, updated_by)
SELECT msg.id, n.code, msg.created_at, msg.created_by, msg.created_at, msg.created_by
FROM (VALUES
  ('@KB국민은행 @삼성증권 내일 10시에%', '004'),
  ('@KB국민은행 @삼성증권 내일 10시에%', '240'),
  ('@결제원 삼성증권은%',               'KFTC')
) AS n (content_prefix, code)
JOIN chat_message msg ON msg.content LIKE n.content_prefix;

-- 확인 공감. KB국민은행 김민준이 응답코드 변경 공지를 확인했다.
INSERT INTO chat_message_ack (message_id, email, name, organization_code, organization_name, created_at, created_by, updated_at, updated_by)
SELECT msg.id, u.email, u.name, o.code, o.name,
       CURRENT_TIMESTAMP - INTERVAL '150' MINUTE, u.email, CURRENT_TIMESTAMP - INTERVAL '150' MINUTE, u.email
FROM chat_message msg
JOIN user_account u ON u.email = 'kim.minjun@kb.example.com'
JOIN organization o ON o.id = u.organization_id
WHERE msg.content LIKE '@KB국민은행 @삼성증권 내일 10시에%';

-- 읽음 표시. 홍성민은 단체방의 운영팀 메시지까지 읽어 삼성증권의 결제원 멘션 하나가 읽지 않은 것으로 남는다.
INSERT INTO chat_read_mark (room_id, email, last_read_message_id, created_at, created_by, updated_at, updated_by)
SELECT msg.room_id, r.email, msg.id, CURRENT_TIMESTAMP, 'seed', CURRENT_TIMESTAMP, 'seed'
FROM (VALUES
  ('hong.sungmin@kftc.example.com',  '반영 시간 동안 운영팀이%'),
  ('kim.minjun@kb.example.com',      '@KB국민은행 @삼성증권 내일 10시에%'),
  ('kim.ops@kftc.example.com',       '반영 시간 동안 운영팀이%'),
  ('choi.yujin@samsung.example.com', '@결제원 삼성증권은%'),
  ('hong.sungmin@kftc.example.com',  '테스트 케이스 받았습니다.%'),
  ('yoon.seoyeon@fsb.example.com',   '테스트 케이스 받았습니다.%')
) AS r (email, content_prefix)
JOIN chat_message msg ON msg.content LIKE r.content_prefix;
