import { intro } from './01-intro.ts';
import { flow } from './02-flow.ts';
import { tests } from './03-tests.ts';
import { suspensions } from './04-suspensions.ts';
import { chat } from './05-chat.ts';
import { outro } from './06-outro.ts';
import type { Chapter, Scene } from './types.ts';

export type { Chapter, Scene } from './types.ts';

/**
 * 영상의 장(chapter) 순서. 장면을 고치면 그 장만 다시 찍는다:
 *   e2e/demo-video/run.sh --only=suspensions          # 장 id
 *   e2e/demo-video/run.sh --only=flow-branch,chat     # 장면 id 도 된다
 * 장 안의 장면들은 서로 이어지지만(예: 기관 회신 뒤 집계), 장끼리는 독립이라 어느 장이든 따로 찍을 수 있다.
 */
export const CHAPTERS: Chapter[] = [intro, flow, tests, suspensions, chat, outro];

export const SCENES: Scene[] = CHAPTERS.flatMap((c) => c.scenes);

/** 장 id 나 장면 id 목록을 장면 id 집합으로 푼다. 모르는 id 는 오류다. */
export function resolveSceneIds(ids: Iterable<string>): Set<string> {
  const out = new Set<string>();
  for (const id of ids) {
    const chapter = CHAPTERS.find((c) => c.id === id);
    if (chapter) {
      for (const s of chapter.scenes) out.add(s.id);
      continue;
    }
    if (!SCENES.some((s) => s.id === id)) {
      throw new Error(`모르는 장·장면 id: ${id} (장: ${CHAPTERS.map((c) => c.id).join(', ')})`);
    }
    out.add(id);
  }
  return out;
}

/** 장면이 속한 장. */
export function chapterOf(sceneId: string): Chapter | undefined {
  return CHAPTERS.find((c) => c.scenes.some((s) => s.id === sceneId));
}
