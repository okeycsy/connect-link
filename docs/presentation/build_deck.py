"""연결이 2차 심사 발표자료(PPTX) 생성기.

흐름: 자동이체통합관리 "해지 결과 배치파일 개선" 과제 하나를 준비 → 업무팀 → 개발팀 → 운영팀 → 며칠 뒤 일시중단 순으로 따라간다.
장마다 왼쪽에 지금 방식(as-is)의 불편함, 오른쪽에 연결이 영상 자리(to-be)를 둔다. 대본은 발표자 노트에 있다(scenario.md 와 같다).

    pip install python-pptx pillow
    python3 docs/presentation/build_deck.py                 # out/연결이_2차심사_발표자료.pptx
    python3 docs/presentation/build_deck.py --videos=DIR    # DIR/demo-<장 id>.mp4 가 있으면 영상 자리에 넣는다

글꼴은 나눔스퀘어(나눔스퀘어 / 나눔스퀘어 Bold / 나눔스퀘어 ExtraBold)를 쓴다. 저장소에는 글꼴을 넣지 않는다.
PDF 는 글꼴이 설치된 PC 에서 PowerPoint 나 LibreOffice 로 내보낸다(soffice --headless --convert-to pdf …).
"""
import argparse
import os

from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import MSO_ANCHOR, PP_ALIGN
from pptx.oxml.ns import qn
from pptx.util import Inches, Pt

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, '..', '..'))
ASSETS = os.path.join(HERE, 'assets')
BRAND = os.path.join(ROOT, 'frontend', 'src', 'assets', 'brand')

R, B, EB = '나눔스퀘어', '나눔스퀘어 Bold', '나눔스퀘어 ExtraBold'
INK = RGBColor(0x2B, 0x2F, 0x36)
GRAY = RGBColor(0x5F, 0x66, 0x72)
LGRAY = RGBColor(0x9A, 0xA8, 0xBF)
BROWN = RGBColor(0x6B, 0x34, 0x20)
ORANGE = RGBColor(0xC2, 0x60, 0x2F)
BLUE = RGBColor(0x1F, 0x6F, 0xEB)
DBLUE = RGBColor(0x16, 0x52, 0xC0)
LBLUE = RGBColor(0xEE, 0xF4, 0xFE)
BLUELINE = RGBColor(0xD6, 0xE4, 0xFB)
LORANGE = RGBColor(0xFD, 0xF2, 0xEA)
PANEL = RGBColor(0xF4, 0xF5, 0xF7)
LINE = RGBColor(0xDC, 0xE0, 0xE6)
RED = RGBColor(0xC8, 0x3B, 0x3B)
LRED = RGBColor(0xFB, 0xEE, 0xEE)
GREEN = RGBColor(0x2E, 0x8B, 0x57)
LGREEN = RGBColor(0xEF, 0xF6, 0xF1)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
PALE = RGBColor(0xC9, 0xCF, 0xD8)
PEACH = RGBColor(0xFF, 0xC9, 0xA6)

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
BLANK = prs.slide_layouts[6]
prs.core_properties.author = '연결이'
prs.core_properties.last_modified_by = '연결이'
prs.core_properties.title = '연결이 2차 심사 발표자료'


# ───────────────────────────── 그리기 도우미

def set_font(run, face, size, color):
    run.font.size = Pt(size)
    run.font.color.rgb = color
    run.font.name = face
    rPr = run._r.get_or_add_rPr()
    for tag in ('a:ea', 'a:cs'):
        el = rPr.find(qn(tag))
        if el is None:
            el = rPr.makeelement(qn(tag), {})
            rPr.append(el)
        el.set('typeface', face)


def text(slide, x, y, w, h, paras, size=14, color=INK, face=R, align=PP_ALIGN.LEFT,
         anchor=MSO_ANCHOR.TOP, spacing=1.15):
    """paras: 문자열, 또는 문단 목록. 문단은 문자열이거나 (글, 글꼴, 색, 크기) 조각의 목록."""
    tb = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.margin_left = tf.margin_right = Inches(0.02)
    tf.margin_top = tf.margin_bottom = Inches(0.0)
    tf.vertical_anchor = anchor
    if isinstance(paras, str):
        paras = [paras]
    for i, p in enumerate(paras):
        para = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        para.alignment = align
        para.line_spacing = spacing
        runs = [(p,)] if isinstance(p, str) else p
        for r in runs:
            run = para.add_run()
            run.text = r[0]
            set_font(run, r[1] if len(r) > 1 and r[1] else face, r[3] if len(r) > 3 and r[3] else size,
                     r[2] if len(r) > 2 and r[2] is not None else color)
    return tb


def box(slide, x, y, w, h, fill=None, line=None, radius=0.08, shape=MSO_SHAPE.ROUNDED_RECTANGLE, lw=1):
    sh = slide.shapes.add_shape(shape, Inches(x), Inches(y), Inches(w), Inches(h))
    if shape == MSO_SHAPE.ROUNDED_RECTANGLE:
        sh.adjustments[0] = min(0.5, radius / min(w, h))
    if fill is None:
        sh.fill.background()
    else:
        sh.fill.solid()
        sh.fill.fore_color.rgb = fill
    if line is None:
        sh.line.fill.background()
    else:
        sh.line.color.rgb = line
        sh.line.width = Pt(lw)
    sh.shadow.inherit = False
    return sh


def pill(slide, x, y, w, h, label, fill, color, size=11, face=B):
    box(slide, x, y, w, h, fill=fill, radius=h / 2)
    text(slide, x, y, w, h, label, size=size, color=color, face=face, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)


def num(slide, x, y, n, fill, d=0.3, size=10.5):
    box(slide, x, y, d, d, fill=fill, shape=MSO_SHAPE.OVAL)
    text(slide, x, y, d, d, str(n), size=size, face=EB, color=WHITE, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)


def notes(slide, body):
    slide.notes_slide.notes_text_frame.text = body.strip()


def header(slide, section, title, section_color=ORANGE):
    if section:
        text(slide, 0.6, 0.38, 9, 0.35, section, size=13, color=section_color, face=EB)
    text(slide, 0.6, 0.68, 10.6, 0.7, title, size=28, color=INK, face=EB)
    slide.shapes.add_picture(os.path.join(BRAND, 'logo.png'), Inches(11.55), Inches(0.42), Inches(1.25))


def page_no(slide, n):
    text(slide, 12.3, 7.05, 0.6, 0.3, str(n), size=10, color=LGRAY, align=PP_ALIGN.RIGHT)


def timeline(slide, active, y=7.0):
    """아래쪽 진행 막대: 지금 어느 단계인지."""
    steps = ['준비', '업무팀', '개발팀', '운영팀', '며칠 뒤']
    x = 0.6
    for i, s in enumerate(steps):
        on = i == active
        w = 1.05
        pill(slide, x, y, w, 0.3, s, BROWN if on else PANEL, WHITE if on else LGRAY, size=10, face=B if on else R)
        x += w + 0.12


VIDEOS = None


def video(slide, chapter, label, seconds, x, y, w):
    """영상 자리. --videos 로 영상 폴더를 주면 그 장 영상을 넣고(재생하면 전체 화면), 없으면 화면 캡처 위에 재생 표시를 그린다."""
    h = w * 9 / 16
    poster = os.path.join(ASSETS, f'poster-{chapter}.jpg')
    box(slide, x - 0.05, y - 0.05, w + 0.1, h + 0.1, fill=WHITE, line=LINE, radius=0.1)
    path = os.path.join(VIDEOS, f'demo-{chapter}.mp4') if VIDEOS else None
    if path and os.path.exists(path):
        slide.shapes.add_movie(path, Inches(x), Inches(y), Inches(w), Inches(h), poster_frame_image=poster, mime_type='video/mp4')
        _play_full_screen(slide)
    else:
        slide.shapes.add_picture(poster, Inches(x), Inches(y), Inches(w), Inches(h))
        veil = box(slide, x, y, w, h, fill=INK, shape=MSO_SHAPE.RECTANGLE)
        veil.fill.transparency = 0.0
        _alpha(veil, 45)
        box(slide, x + w / 2 - 0.45, y + h / 2 - 0.45, 0.9, 0.9, fill=WHITE, shape=MSO_SHAPE.OVAL)
        tri = slide.shapes.add_shape(MSO_SHAPE.ISOSCELES_TRIANGLE, Inches(x + w / 2 - 0.13), Inches(y + h / 2 - 0.2), Inches(0.4), Inches(0.4))
        tri.rotation = 90
        tri.fill.solid()
        tri.fill.fore_color.rgb = ORANGE
        tri.line.fill.background()
    lab = f'▶ 연결이 시연 · {label}'
    pill(slide, x + 0.18, y + 0.18, 0.125 * len(lab) + 0.45, 0.36, lab, ORANGE, WHITE, size=11)
    pill(slide, x + w - 1.1, y + 0.18, 0.92, 0.36, f'{seconds}초', WHITE, INK, size=11)
    return h


def _play_full_screen(slide):
    """영상을 재생하면 화면 가득 보이게 한다(PowerPoint 비디오 재생 옵션 "전체 화면 재생")."""
    for node in slide._element.iter(qn('p:video')):
        node.set('fullScrn', '1')


def _alpha(shape, percent_opaque):
    """도형 채우기 투명도(불투명도 %)."""
    solid = shape.fill._xPr.find(qn('a:solidFill'))
    clr = solid[0]
    a = clr.makeelement(qn('a:alpha'), {'val': str(percent_opaque * 1000)})
    clr.append(a)


def chapter_slide(n, active, section, title, asis_quote, asis_steps, tobe, video_chapter, video_label, seconds, color, light, notes_text):
    s = prs.slides.add_slide(BLANK)
    header(s, section, title, section_color=color)
    # 왼쪽: as-is
    box(s, 0.6, 1.55, 4.55, 5.2, fill=WHITE, line=LINE, radius=0.16)
    text(s, 0.9, 1.75, 4, 0.35, [[('AS-IS  ', EB, GRAY, 14), ('지금은', B, LGRAY, 12)]])
    box(s, 0.9, 2.2, 3.95, 1.25, fill=LRED, radius=0.14)
    text(s, 1.1, 2.2, 3.6, 1.25, asis_quote, size=14.5, face=B, color=RED, anchor=MSO_ANCHOR.MIDDLE, spacing=1.3)
    y = 3.7
    for i, st in enumerate(asis_steps):
        num(s, 0.9, y + 0.02, i + 1, LGRAY, d=0.27, size=9.5)
        text(s, 1.3, y, 3.6, 0.62, st, size=12.5, color=INK, spacing=1.2)
        y += 0.72
    # 오른쪽: to-be 영상 + 요점
    h = video(s, video_chapter, video_label, seconds, 5.55, 1.6, 7.18)
    yy = 1.6 + h + 0.22
    text(s, 5.55, yy, 1.0, 0.35, 'TO-BE', size=13, face=EB, color=BLUE, anchor=MSO_ANCHOR.MIDDLE)
    x = 6.35
    for t in tobe:
        w = 0.13 * len(t) + 0.35
        pill(s, x, yy, w, 0.35, t, light, color, size=10.5)
        x += w + 0.1
    timeline(s, active)
    page_no(s, n)
    notes(s, notes_text)
    return s


def build(out):
    # ───────────────────────────── 1. 표지
    s = prs.slides.add_slide(BLANK)
    box(s, 7.9, -1.2, 7.0, 7.0, fill=LBLUE, shape=MSO_SHAPE.OVAL)
    box(s, 10.2, 4.3, 4.6, 4.6, fill=LORANGE, shape=MSO_SHAPE.OVAL)
    s.shapes.add_picture(os.path.join(BRAND, 'logo-mascot.png'), Inches(7.75), Inches(1.75), Inches(5.0))
    text(s, 0.9, 1.55, 7, 0.45, '바이브코딩 챌린지 2차 심사', size=15, color=ORANGE, face=EB)
    text(s, 0.9, 2.1, 7.2, 1.9, [[('메일·전화·엑셀로 오가던', EB, INK, 40)], [('참가기관 협업을 ', EB, INK, 40), ('한 곳에서', EB, BLUE, 40)]], spacing=1.1)
    text(s, 0.9, 4.05, 7, 0.5, [[('연결이', EB, BROWN, 22), ('  우리 원과 참가기관을 잇는 업무별 협업 포털', R, GRAY, 17)]])
    box(s, 0.9, 4.85, 6.3, 0.012, fill=LINE, shape=MSO_SHAPE.RECTANGLE)
    text(s, 0.9, 5.05, 7, 1.2, [[('신규 과제 하나', B, INK, 15), ('를 처음부터 끝까지', R, GRAY, 15)],
                                [('업무팀 · 개발팀 · 운영팀과 함께 따라가 봅니다.', R, GRAY, 15)]], spacing=1.35)
    notes(s, """
[0:00–0:10] 표지
안녕하십니까. 우리 원과 참가기관을 잇는 협업 포털, 연결이입니다.
오늘은 신규 과제 하나를 처음부터 끝까지, 업무팀·개발팀·운영팀과 함께 따라가 보겠습니다.
""")

    # ───────────────────────────── 2. 오늘의 과제
    s = prs.slides.add_slide(BLANK)
    header(s, '오늘의 과제 (가상)', '자동이체 해지 결과 배치파일 개선')
    box(s, 0.6, 1.55, 6.0, 2.95, fill=PANEL, radius=0.16)
    text(s, 0.9, 1.75, 5.5, 0.35, '지금', size=13, face=EB, color=RED)
    text(s, 0.9, 2.15, 5.5, 2.2, [
        [('고객이 자동이체 해지를 신청하면', R, INK, 14)],
        [('기관의 처리결과 파일은 ', R, INK, 14), ('다음 영업일에 하루 한 번', B, RED, 14)],
        [('해지가 안 되면 사유는 ', R, INK, 14), ('"처리 불가" 하나뿐', B, RED, 14)],
        [('→ 고객은 늦게, 이유도 모른 채 알게 되어 민원', R, GRAY, 13)]], spacing=1.5)
    box(s, 6.73, 1.55, 6.0, 2.95, fill=LBLUE, radius=0.16)
    text(s, 7.03, 1.75, 5.5, 0.35, '개선안', size=13, face=EB, color=BLUE)
    num(s, 7.03, 2.25, 1, BLUE)
    text(s, 7.45, 2.2, 5.1, 0.8, [[('처리결과 회신을 ', R, INK, 14), ('당일 2회(13시 · 18시)', B, DBLUE, 14)]])
    num(s, 7.03, 3.0, 2, BLUE)
    text(s, 7.45, 2.95, 5.1, 1.3, [[('결과 파일에 ', R, INK, 14), ('불가 사유코드', B, DBLUE, 14), (' 추가', R, INK, 14)],
                                    [('미납 잔액 있음 · 기관에서 직접 해지 필요 · 출금계좌 해지됨', R, GRAY, 12)]], spacing=1.45)
    # 추진 흐름
    text(s, 0.6, 4.78, 6, 0.4, '추진 흐름', size=15, face=EB, color=INK)
    flow = [('준비', '담당자 연락처', BROWN, LORANGE), ('업무팀', '회의 · 의견조사 · 요건 확정', ORANGE, LORANGE),
            ('개발팀', '개발 요청 · 테스트', BLUE, LBLUE), ('운영팀', '시행 첫날 모니터링', GREEN, LGREEN),
            ('며칠 뒤', '일시중단', GREEN, LGREEN)]
    x = 0.6
    for i, (t, d, c, bg) in enumerate(flow):
        w = 2.25
        box(s, x, 5.25, w, 1.3, fill=bg, radius=0.14)
        text(s, x + 0.2, 5.4, w - 0.3, 0.4, f'{i:d}  {t}' if i else f'0  {t}', size=15, face=EB, color=c)
        text(s, x + 0.2, 5.85, w - 0.3, 0.6, d, size=12, color=GRAY)
        if i < len(flow) - 1:
            text(s, x + w, 5.25, 0.22, 1.3, '›', size=22, face=EB, color=LGRAY, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
        x += w + 0.22
    text(s, 0.6, 6.75, 12, 0.35, [[('등장인물  ', B, GRAY, 11.5), ('결제원 문서준(자동이체 업무·개발) · 김태호(운영)   참가기관 신한은행 이준호 외 자동이체통합관리 참여 6곳   (모두 가상)', R, GRAY, 11.5)]])
    page_no(s, 2)
    notes(s, """
[0:10–0:35] 오늘의 과제
자동이체통합관리업무에서 신규 과제를 하나 추진한다고 해 보겠습니다.
고객이 자동이체 해지를 신청하면 기관의 처리결과가 다음 날 하루 한 번만 오고, 안 됐을 때 이유도 "처리 불가" 하나뿐입니다.
그래서 결과 파일을 당일 두 번 받고, 불가 사유코드를 더하기로 합니다.
이 과제를 준비부터 업무팀, 개발팀, 운영팀, 그리고 며칠 뒤 일시중단까지 따라가 보겠습니다.
""")

    # ───────────────────────────── 3. 준비
    chapter_slide(
        3, 0, '⓪ 준비', '먼저, 기관 담당자에게 연락해야 합니다',
        '"작년 명부로 보낸 메일 3통이\n반송됐습니다."',
        ['같은 업무도 업무 · 개발 · 운영팀이 각자 방식으로 따로 관리하는 명부', '인사이동은 연락해 봐야 알게 됨', '반기마다 현행화 요청 → 회신 → 수기 반영',
         '기관에서도 담당자 찾는 문의가 끊이지 않음'],
        ['기관 직접 현행화', '6개월마다 재확인', '반기 자동 요청', '기관도 담당자 조회'],
        'prep', '최신 담당자 연락처', 25, BROWN, LORANGE, """
[0:35–1:10] ⓪ 준비
과제를 시작하려면 먼저 기관 담당자에게 연락해야 합니다. 그런데 같은 업무라도 업무·개발·운영팀이 명부를 각자 따로 관리하다 보니, 작년 명부로 메일을 보냈더니 세 통이 반송됩니다. 기관에서도 우리 원 담당자가 누구냐는 문의가 끊이지 않습니다.
(영상 prep 재생, 약 25초)
연결이의 연락처는 기관이 직접 현행화합니다. 6개월이 지나면 로그인할 때 다시 묻고, 반기마다 정기 변경을 자동으로 요청합니다. 그래서 지금 이 명부가 최신이고, 기관도 여기서 우리 원 담당자를 바로 찾습니다.
""")

    # ───────────────────────────── 4. 업무팀
    chapter_slide(
        4, 1, '① 업무팀', '회의 열고, 의견 묻고, 요건을 확정합니다',
        '"회신은 독촉해야 오고,\n받으면 하나하나 옮겨 적습니다."',
        ['회의 안내 · 의견조사 메일 → 회신 올 때까지 리마인드 · 독촉', '받은 회신을 내부망으로 옮겨 하나하나 읽기', '취합표는 담당자가 직접 작성'],
        ['공지에서 회신', '답별 분기 질문', '미회신 리마인드', '자동 취합표'],
        'biz', '회의 · 의견조사 · 요건 확정', 55, ORANGE, LORANGE, """
[1:10–2:15] ① 업무팀
업무팀은 먼저 실무 회의를 엽니다. 지금은 안내 메일과 의견조사 메일을 보내고, 회신이 올 때까지 리마인드하고 독촉합니다. 회신을 받아도 내부망으로 옮겨 하나하나 읽고, 취합표를 직접 만들어야 합니다.
(영상 biz 재생, 약 55초)
연결이에서는 회의 공지에 참석 회신을 이어 두면 기관이 공지에서 바로 답하고, 리마인드는 미회신 기관에만 한 번에 보냅니다.
의견조사는 구글폼처럼, 당일 2회 회신이 가능하다는 기관에는 적용 시기를, 어렵다는 기관에는 이유를 묻습니다.
답은 회신 즉시 기관별 취합표로 모이고, 이걸로 요건을 확정합니다. 당일 두 번, 사유코드 세 종, 2분기 시행.
""")

    # ───────────────────────────── 5. 개발팀
    chapter_slide(
        5, 2, '② 개발팀', '개발을 요청하고, 테스트를 함께 봅니다',
        '"어제 테스트했는데\n왜 X로 되어 있죠?"',
        ['테스트데이터 · 결과를 메일로 모아 수행률 직접 계산', '매일 17시 기준 현황표를 메일로 배포', '공유한 순간 지난 정보 → 문의 전화 · 재배포'],
        ['테스트데이터 사전 요청', '기관이 결과 직접 등록', '양쪽이 같은 화면 실시간'],
        'dev', '테스트데이터 · 실시간 현황', 45, BLUE, LBLUE, """
[2:15–3:10] ② 개발팀
요건이 확정되면 개발팀이 참가기관에 개발과 테스트를 요청합니다.
지금은 테스트데이터와 결과를 메일로 모아 매일 17시 기준 현황표를 돌립니다. 공유하는 순간 이미 지난 정보라, "어제 테스트했는데 왜 X죠?" 하는 전화가 옵니다.
(영상 dev 재생, 약 45초)
연결이에서는 테스트를 열 때 테스트데이터까지 미리 요청하고, 기관이 결과를 직접 올립니다. 올리는 순간 우리 원 현황이 바뀌고, 기관도 자기 수행률과 우리 원 의견을 같은 화면에서 봅니다. 기준 시각이라는 게 없어집니다.
""")

    # ───────────────────────────── 6. 운영팀
    chapter_slide(
        6, 3, '③ 운영팀 · 시행 첫날', '해지 요청 파일이 늦게 나갑니다',
        '"파일 왜 안 오나요?" 전화가\n여러 은행에서 한꺼번에 옵니다.',
        ['타 기관의 수신 지연으로 기관에 파일이 늦게 나감', '은행마다 걸려 오는 전화에 같은 설명 반복', '어느 기관에 안내했는지 따로 메모'],
        ['단체방에 한 번 공지', '참여 기관 모두 동시 확인', '기관별 [확인] 현황'],
        'ops', '소통 채널 지연 공지', 25, GREEN, LGREEN, """
[3:10–3:45] ③ 운영팀 · 시행 첫날
드디어 시행 첫날, 운영팀이 모니터링을 합니다. 그런데 타 기관의 수신이 늦어져 해지 요청 파일이 기관에 늦게 나갑니다.
지금은 파일이 왜 안 오냐는 전화가 여러 은행에서 한꺼번에 걸려 오고, 기관마다 같은 설명을 되풀이합니다.
(영상 ops 재생, 약 25초)
연결이에서는 업무 단체방에 지연 사실과 예상 시각을 한 번 올리면 참여 기관 모두가 동시에 봅니다. 기관은 확인을 누르고, 어느 기관이 확인했는지 기관 이름으로 한눈에 보입니다.
""")

    # ───────────────────────────── 7. 일시중단
    chapter_slide(
        7, 4, '④ 며칠 뒤 · 일시중단', '"자동이체 서비스 중단" 공문이 왔습니다',
        '"페이인포 계좌이동인가요,\n지로 실시간 납부자자동이체인가요?"',
        ['공문으로만 접수, 업무명은 "자동이체 서비스 중단" 식', '어느 업무가 멈추는지 기관에 다시 확인', '확인한 내용을 운영 · 관제에 수기 전파'],
        ['우리 원 업무 목록에서 선택', '담당자 즉시 알림', '확정하면 매터모스트로'],
        'susp', '일시중단 등록 · 확정', 35, GREEN, LGREEN, """
[3:45–4:25] ④ 며칠 뒤 · 일시중단
며칠 뒤, 신한은행에서 전산 작업으로 일시중단 공문이 옵니다. 업무명이 "자동이체 서비스 중단"이라, 자동이체통합관리(페이인포)의 계좌이동 업무인지, 지로의 실시간 납부자자동이체 업무인지 다시 물어야 합니다.
(영상 susp 재생, 약 35초)
연결이에서는 기관이 우리 원 업무 목록에서 멈추는 업무를 직접 고릅니다. 이번에는 계좌이동입니다. 다시 물을 필요가 없습니다.
관련 업무 담당자에게 바로 알림이 가고, 운영 담당자가 조치와 인수인계를 써서 확정하면 운영·관제 매터모스트 채널로 전파됩니다.
""")

    # ───────────────────────────── 8. 보안
    s = prs.slides.add_slide(BLANK)
    header(s, '안심하고 쓰도록', '업무자료는 내부망에, 참가기관은 자기 정보만')
    # 시스템 구성도(그림). 참가기관은 인터넷 → DMZ 웹 게이트웨이, 우리 원 직원은 내부망에서 바로 접속한다.
    h = 5.65
    w = h * 2348 / 1322
    s.shapes.add_picture(os.path.join(ASSETS, 'system-architecture.png'), Inches((13.333 - w) / 2), Inches(1.5), Inches(w), Inches(h))
    page_no(s, 8)
    notes(s, """
[4:25–4:37] 보안
참가기관은 인터넷에서 DMZ를 거쳐, 우리 원 직원은 내부망에서 같은 서버에 접속합니다. 이 모든 과정에서 업무자료는 내부망을 벗어나지 않습니다. 엑셀 다운로드나 명부 수정은 내부망에서만 되고, 화면에서 숨긴 게 아니라 서버가 막습니다. 우리 원이 올린 파일은 팀장 결재를 거쳐야 참가기관에 보입니다.
""")

    # ───────────────────────────── 9. 기대효과
    s = prs.slides.add_slide(BLANK)
    header(s, '기대효과', '한 업무 198시간, 우리 원 전체로는 1만 8,612시간')
    box(s, 0.6, 1.6, 4.0, 2.35, fill=LORANGE, radius=0.18)
    text(s, 0.9, 1.82, 3.5, 0.4, '업무 1개 기준 · 연간', size=13, face=B, color=ORANGE)
    text(s, 0.9, 2.2, 3.5, 1.0, [[('약 198', EB, BROWN, 54), ('시간', EB, BROWN, 22)]])
    text(s, 0.9, 3.25, 3.5, 0.5, '참가기관 63곳 기준 추정', size=11.5, color=GRAY)
    box(s, 0.6, 4.1, 4.0, 2.35, fill=INK, radius=0.18)
    text(s, 0.9, 4.32, 3.5, 0.4, '우리 원 94개 업무 적용 시 · 연간', size=13, face=B, color=PEACH)
    text(s, 0.9, 4.7, 3.6, 1.0, [[('1만 8,612', EB, WHITE, 40), ('시간', EB, WHITE, 18)]])
    text(s, 0.9, 5.75, 3.6, 0.5, '직원 약 9명의 1년 근무시간', size=12, color=PALE)
    rows = [('구분', '절감 업무', '산정 근거', '연간'),
            ('준비 · 공통', '연락처 현행화', '3분 × 63곳 × 2명 × 세부업무 5 × 연 2회', '63시간'),
            ('업무팀', '의견 회신 취합', '기관당 6분 × 63곳 × 연 10건', '63시간'),
            ('개발팀', '테스트 결과 취합', '기관당 6분 × 63곳 × 연 10건', '63시간'),
            ('운영팀', '일시중단 확인 · 취합', '주 1회 10분 × 52주', '약 9시간')]
    y = 1.6
    colx, colw = [4.95, 6.15, 7.85, 11.45], [1.15, 1.65, 3.6, 1.25]
    for i, r in enumerate(rows):
        if i == 0:
            box(s, 4.85, y, 7.88, 0.42, fill=PANEL, radius=0.08)
        for j, c in enumerate(r):
            text(s, colx[j], y, colw[j], 0.42, c, size=12 if i == 0 else 13, face=B if i == 0 or j in (0, 3) else R,
                 color=GRAY if i == 0 else (BROWN if j == 3 else INK), anchor=MSO_ANCHOR.MIDDLE,
                 align=PP_ALIGN.RIGHT if j == 3 else PP_ALIGN.LEFT)
        if i:
            box(s, 4.85, y + 0.42, 7.88, 0.01, fill=LINE, shape=MSO_SHAPE.RECTANGLE)
        y += 0.47
    text(s, 4.95, 4.15, 7, 0.4, '오늘 본 과제는 자동이체통합관리 하나였습니다', size=15, face=EB, color=INK)
    spread = [('모든 업무', '참가기관과 일하는 업무라면\n업무 구분만 더하면 같은 화면'),
              ('모든 직무', '현업 · 개발 · 운영 · 상황실이\n한 포털에서 각자 필요한 기능'),
              ('익숙한 방식', '설치 없이 PC · 휴대폰 웹,\n구글폼 · 메신저처럼')]
    for i, (t, d) in enumerate(spread):
        x = 4.85 + i * 2.66
        box(s, x, 4.6, 2.52, 1.85, fill=LBLUE, radius=0.14)
        text(s, x + 0.22, 4.78, 2.2, 0.4, t, size=15, face=EB, color=DBLUE)
        text(s, x + 0.22, 5.25, 2.2, 1.1, d.split('\n'), size=12, color=GRAY, spacing=1.35)
    page_no(s, 9)
    notes(s, """
[4:37–4:55] 기대효과
오늘 본 과정, 연락처 현행화와 의견 취합, 테스트 결과 취합, 일시중단 취합에서 줄어드는 시간이 업무 하나에서 해마다 약 198시간입니다.
오늘은 자동이체통합관리 하나였지만, 참가기관과 일하는 우리 원 94개 업무로 넓히면 1만 8,612시간, 직원 9명의 1년입니다.
업무 구분만 더하면 어느 부서든 같은 화면으로 씁니다.
""")

    # ───────────────────────────── 10. 마무리
    s = prs.slides.add_slide(BLANK)
    box(s, -1.5, 3.8, 6.0, 6.0, fill=LORANGE, shape=MSO_SHAPE.OVAL)
    box(s, 9.2, -2.0, 6.0, 6.0, fill=LBLUE, shape=MSO_SHAPE.OVAL)
    s.shapes.add_picture(os.path.join(BRAND, 'logo-mascot.png'), Inches(4.17), Inches(0.95), Inches(5.0))
    text(s, 1.0, 3.75, 11.33, 0.8, [[('흩어진 메일 대신, ', EB, INK, 34), ('연결이', EB, BLUE, 34)]], align=PP_ALIGN.CENTER)
    text(s, 1.0, 4.55, 11.33, 0.5, '연락처 · 의견 요청 · 기관 테스트 · 일시중단 · 소통 채널 · 공지를 한 곳에서', size=16, color=GRAY, align=PP_ALIGN.CENTER)
    box(s, 3.67, 5.45, 6.0, 0.95, fill=PANEL, radius=0.18)
    text(s, 3.67, 5.45, 6.0, 0.95, [[('직접 써 보기  ', B, ORANGE, 13), ('connect-link-xmwv.onrender.com', EB, INK, 16)],
                                   [("'데모 계정으로 들어가기' · 가상 데이터", R, GRAY, 11.5)]],
         align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE, spacing=1.3)
    text(s, 1.0, 6.75, 11.33, 0.4, '감사합니다', size=15, face=B, color=LGRAY, align=PP_ALIGN.CENTER)
    notes(s, """
[4:55–5:00] 마무리
메일과 전화, 엑셀로 흩어져 있던 참가기관 협업을 하나로 잇는 것, 연결이였습니다. 감사합니다.
""")

    os.makedirs(os.path.dirname(out), exist_ok=True)
    prs.save(out)
    print('saved', out)


if __name__ == '__main__':
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default=os.path.join(HERE, 'out', '연결이_2차심사_발표자료.pptx'))
    ap.add_argument('--videos', default=None, help='demo-<장 id>.mp4 가 있는 폴더(녹화영상 PR 의 e2e/demo-video/out)')
    a = ap.parse_args()
    VIDEOS = a.videos
    build(a.out)
