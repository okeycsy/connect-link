-- 기관 회신은 요청 질문에 대한 답뿐이다(SHY 요청). 의견 유형(동의·조건부 동의·이견)과 세부의견 칸을 없앤다.
-- 결제원 담당자가 같은 것을 물으려면 객관식·주관식 질문으로 넣는다.
ALTER TABLE opinion_reply DROP CONSTRAINT ck_opinion_reply_type;
ALTER TABLE opinion_reply DROP CONSTRAINT ck_opinion_reply_draft_type;
ALTER TABLE opinion_reply DROP COLUMN reply_type;
ALTER TABLE opinion_reply DROP COLUMN content;
ALTER TABLE opinion_reply DROP COLUMN draft_type;
ALTER TABLE opinion_reply DROP COLUMN draft_content;
