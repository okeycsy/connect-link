-- 일시중단은 업무 단위로만 받는다. 한 업무의 세부업무들은 같은 시스템을 쓰므로 세부업무 칸을 없앤다(SHY, 2026-10-21).
ALTER TABLE suspension DROP COLUMN sub_business;
