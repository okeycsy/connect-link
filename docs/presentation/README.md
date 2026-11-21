# 2차 심사 발표자료

자동이체통합관리 "해지 결과 배치파일 개선" 과제 하나를 준비 → 업무팀 → 개발팀 → 운영팀 → 며칠 뒤 일시중단 순으로 따라가는 5분 발표.
시연은 미리 찍은 화면 녹화 영상(`e2e/demo-video/`, 별도 PR)으로 장표 사이에 넣는다.

| 파일 | 내용 |
| --- | --- |
| `scenario.md` | 전체 흐름, 시간 배분, 장별 as-is 대본과 영상 장면, 영상에 쓰는 데이터. 대본은 여기가 기준이다 |
| `build_deck.py` | 발표자료(PPTX) 생성기. 장표마다 발표자 노트에 시간·대본이 있다 |
| `assets/poster-*.jpg` | 영상 자리에 임시로 보이는 화면 캡처. 녹화가 끝나면 영상 첫 장면으로 바꾼다 |

## 만들기

```bash
pip install python-pptx pillow
python3 docs/presentation/build_deck.py                                   # docs/presentation/out/연결이_2차심사_발표자료.pptx
python3 docs/presentation/build_deck.py --videos=e2e/demo-video/out       # demo-<장 id>.mp4 를 영상 자리에 넣는다
soffice --headless --convert-to pdf --outdir docs/presentation/out docs/presentation/out/연결이_2차심사_발표자료.pptx
```

- 글꼴은 나눔스퀘어(나눔스퀘어 / 나눔스퀘어 Bold / 나눔스퀘어 ExtraBold). 저장소에는 넣지 않는다. PDF 는 글꼴이 설치된 곳에서 내보내면 글꼴이 들어간다.
- PPT 로 낼 때는 PowerPoint 에서 파일 → 옵션 → 저장 → "파일의 글꼴 포함"을 켜고 저장한다.
- 영상 장 id: `prep`(준비) `biz`(업무팀) `dev`(개발팀) `ops`(운영팀) `susp`(며칠 뒤 일시중단).
- 넣은 영상은 PowerPoint 슬라이드 쇼에서 재생하면 화면 가득(전체 화면 재생) 보인다. 끝나면 장표로 돌아온다.
- 실명·내부 정보는 넣지 않는다. 사람·기관 데이터와 과제 내용은 모두 가상이다.
