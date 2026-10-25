-- 기한 하루 전 자동 리마인드. 같은 기한으로 두 번 보내지 않으려고 보낸 때의 기한을 남긴다(기한을 바꾸면 새 기한으로 다시 보낸다).
ALTER TABLE opinion_request ADD COLUMN auto_reminded_for DATE;
ALTER TABLE test_plan ADD COLUMN end_reminded_for DATE;
ALTER TABLE test_plan ADD COLUMN test_data_reminded_for DATE;
