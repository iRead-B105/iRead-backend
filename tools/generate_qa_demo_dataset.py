#!/usr/bin/env python3
"""Generate the canonical local QA demo SQL and raw fixture assets.

The generator is intentionally deterministic.  Run it after changing the demo
contract, then review the generated SQL/JSON before executing the manual reset.
"""

from __future__ import annotations

import argparse
import json
import re
import uuid
from dataclasses import dataclass
from datetime import datetime, timedelta
from pathlib import Path
from typing import Any, Iterable


ROOT = Path(__file__).resolve().parents[1]
RESOURCE_ROOT = ROOT / "src" / "main" / "resources"
ASSET_ROOT = RESOURCE_ROOT / "assets" / "qa-demo"
SQL_PATH = RESOURCE_ROOT / "db" / "demo-data" / "qa-demo-reset.sql"


@dataclass(frozen=True)
class Student:
    no: int
    id: int
    name: str
    birthday: str
    gender: str
    school: str
    guardian: str
    phone: str
    email: str
    address: str
    memo: str
    completed_dates: tuple[str, ...]
    base_score: int
    reading_speed: int
    weakness: str
    strength: str


@dataclass(frozen=True)
class Plot:
    key: str
    title: str
    subjects: tuple[str, ...]
    days: tuple[tuple[tuple[str, str], ...], ...]
    branch_questions: tuple[str, ...]
    choices: tuple[tuple[str, str, str], ...]
    style_reference: str


@dataclass(frozen=True)
class StorySpec:
    id: int
    student_id: int
    template_id: int
    plot_key: str
    progress: int
    completed: bool
    created_at: str
    existing_images: tuple[str, ...]


STUDENTS = (
    Student(
        1,
        2001,
        "김도윤",
        "2018-04-12",
        "Boy",
        "시연초등학교",
        "김보호",
        "010-0000-3001",
        "demo@demo1.com",
        "가상시 데모구 읽기길 101",
        "[관찰] 받침 끝소리와 받침 뒤 모음 연음을 어려워합니다. [흥미] 자동차와 F1 레이싱에 집중도가 높습니다.",
        ("2026-07-10", "2026-07-14", "2026-07-17", "2026-07-21", "2026-07-24", "2026-07-28", "2026-07-31", "2026-08-04"),
        610,
        58,
        "받침·연음",
        "그림 단서 활용",
    ),
    Student(
        2,
        2002,
        "이서연",
        "2017-02-14",
        "Girl",
        "샛별초등학교",
        "이사랑",
        "010-0000-3002",
        "demo@demo2.com",
        "가상시 샛별구 배움로 202",
        "[관찰] 초성과 받침 위치의 ㄴ을 ㄷ 또는 ㅁ과 선택적으로 혼동합니다. 낱말 속 단서에서는 반응이 좋아집니다.",
        ("2026-07-09", "2026-07-13", "2026-07-16", "2026-07-20", "2026-07-23", "2026-07-27", "2026-07-30", "2026-08-03"),
        680,
        68,
        "ㄴ 소리 구별",
        "낱말 의미 이해",
    ),
    Student(
        3,
        2103,
        "박지호",
        "2016-01-27",
        "Boy",
        "샛별초등학교",
        "박다정",
        "010-0000-3003",
        "demo@demo3.com",
        "가상시 샛별구 이야기길 303",
        "[관찰] 읽기 정확도와 이해도는 높지만 속도가 느립니다. 반복 읽기로 자연스러운 유창성을 지원합니다.",
        ("2026-07-08", "2026-07-13", "2026-07-16", "2026-07-21", "2026-07-23", "2026-07-27", "2026-07-30", "2026-08-04"),
        870,
        48,
        "읽기 속도",
        "읽기 정확도와 이해",
    ),
)


PLOTS = {
    "rabbit": Plot(
        "rabbit",
        "토끼와 거북이 F1 경주",
        ("거북이 선수는", "토끼 선수는", "두 선수는", "숲속 친구들은", "안전 요원은"),
        ((("출발선의 푸른 깃발을", "주의 깊게 바라보았어요"), ("단단한 경주 바퀴를", "차분하게 점검했어요"), ("첫 번째 굽은 길을", "안전하게 지나갔어요")),),
        ("두 선수는 다음 코너에서 어떤 작전을 고를까요?", "마지막 직선에서는 어떤 방법으로 달릴까요?"),
        (("안쪽 길을 천천히 달려요", "바깥 길에서 속도를 높여요", "안전 요원에게 길을 물어요"), ("남은 힘으로 끝까지 달려요", "서로 응원하며 나란히 달려요", "잠시 바퀴를 다시 살펴봐요")),
        "rabbit-and-turtle.png",
    ),
    "byeol": Plot(
        "byeol",
        "별주부전",
        ("별주부는", "토끼는", "용궁 친구들은", "바다 거북은", "지혜로운 문어는"),
        ((("반짝이는 바닷길을", "천천히 안내했어요"), ("용왕의 아픈 사연을", "차분하게 들려주었어요"), ("서로의 걱정과 생각을", "솔직하게 나누었어요")),),
        ("토끼는 용궁에서 어떤 지혜를 먼저 낼까요?", "별주부는 누구에게 도움을 부탁할까요?"),
        (("용왕에게 다른 약을 찾아보자고 해요", "별주부와 육지로 함께 돌아가요", "용궁 친구들의 생각을 먼저 들어요"), ("지혜로운 문어에게 물어봐요", "바닷속 약초를 함께 찾아봐요", "토끼의 계획을 차분히 따라가요")),
        "byeoljubujeon.png",
    ),
    "pigs": Plot(
        "pigs",
        "아기돼지 삼형제",
        ("첫째 돼지는", "둘째 돼지는", "막내 돼지는", "세 형제는", "돼지 친구들은", "숲속 이웃들은"),
        (
            (("새집을 지을 빈터를", "꼼꼼하게 살펴보았어요"), ("필요한 도구와 재료를", "하나씩 정리했어요"), ("서로 잘하는 집안일을", "즐겁게 나누었어요")),
            (("가벼운 볏짚 묶음을", "차곡차곡 옮겼어요"), ("따뜻한 지붕의 모양을", "정성껏 완성했어요"), ("흔들리는 작은 벽면을", "힘을 모아 붙잡았어요")),
            (("튼튼한 나무 기둥을", "나란하게 세워 보았어요"), ("향긋한 나무 판자를", "빈틈없이 이어 붙였어요"), ("새집의 문과 창문을", "여러 번 점검했어요")),
            (("무거운 붉은 벽돌을", "천천히 날라 주었어요"), ("단단한 벽돌 벽면을", "반듯하게 쌓아 올렸어요"), ("따뜻한 굴뚝과 지붕을", "안전하게 완성했어요")),
            (("숲길의 낯선 발자국을", "조용히 따라가 보았어요"), ("멀리 들리는 늑대 소리를", "침착하게 확인했어요"), ("세 채의 문과 창문을", "빠짐없이 잠가 두었어요")),
            (("거센 바람에 흔들린 집을", "서로 도우며 지켜냈어요"), ("무너진 볏짚과 나무를", "안전한 곳으로 옮겼어요"), ("단단한 벽돌집의 문을", "힘을 합쳐 닫았어요")),
            (("벽돌집의 따뜻한 방을", "친구들과 함께 나누었어요"), ("놀란 마음과 걱정거리를", "차분하게 이야기했어요"), ("새로운 안전 약속들을", "큰 소리로 확인했어요")),
            (("무너진 두 채의 집터를", "다시 깨끗하게 정리했어요"), ("더 튼튼한 새 설계도를", "서로 의논해 만들었어요"), ("벽돌과 나무의 장점을", "알맞게 함께 사용했어요")),
            (("도움을 준 숲속 이웃을", "따뜻하게 맞아 주었어요"), ("안전한 집짓기 방법을", "친절하게 알려 주었어요"), ("남은 재료와 좋은 도구를", "이웃들과 함께 나누었어요")),
            (("완성된 튼튼한 마을을", "기쁘게 둘러보았어요"), ("용기와 협동의 경험을", "친구들에게 들려주었어요"), ("새로운 평화의 약속을", "다 함께 오래 지켰어요")),
        ),
        ("세 형제는 이제 어떤 재료를 먼저 고를까요?", "다음 집은 어떤 방법으로 더 튼튼히 할까요?"),
        (("벽돌을 차곡차곡 더 쌓아요", "나무 기둥을 먼저 점검해요", "이웃들과 안전 계획을 세워요"), ("창문과 문을 단단히 고쳐요", "세 집을 잇는 길을 만들어요", "남은 재료를 함께 정리해요")),
        "three-little-pigs.png",
    ),
    "sea": Plot(
        "sea",
        "노인과 바다",
        ("늙은 어부는", "어린 소년은", "푸른 청새치는", "바다 친구들은", "항구 사람들은"),
        (
            (("잔잔한 아침 바다를", "천천히 바라보았어요"), ("낡은 배와 긴 낚싯줄을", "꼼꼼하게 점검했어요"), ("멀리 반짝이는 물결을", "차분하게 따라갔어요")),
            (("커다란 청새치의 움직임을", "조용히 기다려 주었어요"), ("거센 파도와 바닷바람을", "용기 있게 견뎌냈어요"), ("노을빛 항구로 가는 길을", "별빛으로 찾아냈어요")),
        ),
        ("어부는 넓은 바다에서 어느 길을 고를까요?", "청새치와 어부는 다음에 무엇을 할까요?"),
        (("햇빛이 비치는 물결을 따라가요", "바다새가 나는 쪽을 살펴봐요", "잠시 배를 멈추고 지도를 봐요"), ("서로 힘을 아끼며 기다려요", "항구로 돌아갈 길을 함께 찾아요", "파도가 잔잔해질 때 움직여요")),
        "old-man-and-sea.png",
    ),
}


STORIES = (
    StorySpec(280001, 2001, 1, "rabbit", 9, False, "2026-08-04 15:00:00", ("37e5becf-afeb-4472-9b4b-f4ad31804ad7.jpg", "fac73006-704f-40b1-abf5-ce4b298d6e33.jpg")),
    StorySpec(280002, 2002, 5, "byeol", 4, False, "2026-08-03 15:00:00", ("098f386f-8b72-4940-b9b3-d4d197e42dbc.jpg",)),
    StorySpec(280003, 2103, 6, "pigs", 100, True, "2026-07-08 15:00:00", ("badf86e5-24c2-4401-920e-51e8f2ce00ac.jpg", "5863d881-12c4-44b1-ae36-7cafe2d60108.jpg")),
    StorySpec(280004, 2103, 3, "sea", 20, False, "2026-08-04 16:00:00", ("347242ee-73de-4179-bebc-95f4f41d3bdc.jpg", "8d07dd90-efa6-4885-9d54-92f40bd7fa9f.jpg")),
)


TRAINING_TYPES = {
    1: "VOWEL_TRACE",
    2: "CONSONANT_TRACE",
    3: "SYLLABLE_TRACE",
    4: "CONSONANT_SOUND_CHOICE",
    5: "VOWEL_SOUND_CHOICE",
    7: "SYLLABLE_INITIAL_CHOICE",
    8: "WORD_INITIAL_CHOICE",
    9: "SAME_INITIAL_WORD_CHOICE",
    10: "FINAL_CONSONANT_CHOICE",
    11: "WORD_FINAL_SOUND_CHOICE",
    12: "FINAL_CONSONANT_COMPARISON",
    13: "SIMILAR_SOUND_CHOICE",
    15: "SYLLABLE_BLEND",
    16: "BASIC_SYLLABLE_BUILD",
    17: "FINAL_SYLLABLE_BUILD",
    18: "DOUBLE_FINAL_BUILD",
    19: "FINAL_CONSONANT_DELETE",
    20: "SYLLABLE_DELETE",
    21: "SYLLABLE_REPLACE",
    22: "WORD_READING",
    23: "NONWORD_READING",
    25: "SENTENCE_READING",
    27: "SENTENCE_ASSEMBLY",
    28: "FILL_IN_THE_BLANK",
    29: "IMAGE_SENTENCE_MATCH",
    30: "SENTENCE_REPEAT",
    31: "WORD_CHAIN_READING",
    33: "REPEATED_SENTENCE_READING",
}

AUDIO_TRAINING_TYPES = {
    "WORD_READING",
    "NONWORD_READING",
    "SENTENCE_READING",
    "SENTENCE_REPEAT",
    "WORD_CHAIN_READING",
    "REPEATED_SENTENCE_READING",
}

GAZE_TRAINING_TYPES = AUDIO_TRAINING_TYPES | {
    "VOWEL_TRACE",
    "CONSONANT_TRACE",
    "SYLLABLE_TRACE",
}

REPORT_TRAINING_GAZE_IDS = {
    2001: (321074, 321084),
    2002: (322074, 322084),
    2103: (323073, 323083),
}

# 8개 완료 회차와 1개 다음 회차. 최신 develop의 선택 가능 훈련 28개만
# 사용하며, 매일 속도 지표를 만들 수 있도록 22 또는 25를 하나 이상 둔다.
# 30과 33은 같은 유사 읽기군이므로 한 회차에 함께 편성하지 않는다.
CURRICULUM_ROTATIONS = {
    2001: (
        (10, 17, 19, 22, 27),
        (11, 18, 20, 25, 28),
        (12, 13, 21, 22, 29),
        (10, 15, 17, 25, 30),
        (11, 18, 19, 22, 31),
        (12, 20, 21, 25, 33),
        (10, 13, 17, 22, 28),
        (11, 15, 18, 25, 29),
        (12, 19, 21, 22, 30),
    ),
    2002: (
        (2, 4, 7, 22, 16),
        (8, 9, 13, 25, 21),
        (3, 4, 7, 22, 28),
        (8, 9, 16, 25, 29),
        (2, 5, 13, 22, 27),
        (7, 15, 21, 25, 30),
        (4, 8, 9, 22, 31),
        (3, 13, 16, 25, 33),
        (5, 7, 21, 22, 28),
    ),
    2103: (
        (1, 15, 22, 27, 30),
        (3, 16, 25, 28, 31),
        (5, 17, 22, 29, 33),
        (7, 18, 25, 27, 30),
        (9, 20, 22, 28, 31),
        (11, 21, 25, 29, 33),
        (13, 15, 22, 27, 30),
        (16, 18, 25, 28, 31),
        (17, 20, 22, 29, 33),
    ),
}


ADVERBS = ("차분히", "서로", "오늘도", "용기 내어")


def hangul_count(value: str) -> int:
    return sum("가" <= character <= "힣" for character in value)


def sql_quote(value: str | None) -> str:
    if value is None:
        return "NULL"
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def json_sql(value: Any) -> str:
    return sql_quote(json.dumps(value, ensure_ascii=False, separators=(",", ":")))


def rows_sql(table: str, columns: Iterable[str], rows: Iterable[Iterable[Any]]) -> str:
    rendered = []
    for row in rows:
        values = []
        for value in row:
            if value is None:
                values.append("NULL")
            elif isinstance(value, bool):
                values.append("TRUE" if value else "FALSE")
            elif isinstance(value, (int, float)):
                values.append(str(value))
            else:
                values.append(sql_quote(str(value)))
        rendered.append("    (" + ", ".join(values) + ")")
    return f"INSERT INTO {table} ({', '.join(columns)})\nVALUES\n" + ",\n".join(rendered) + ";\n"


def story_sentence_pool(plot: Plot, day_index: int) -> list[str]:
    event_pairs = plot.days[min(day_index, len(plot.days) - 1)]
    candidates: list[str] = []

    def add(sentence: str) -> None:
        awkward_repetition = (
            "서로 서로" in sentence
            or ("차분히" in sentence and "차분하게" in sentence)
            or ("용기 내어" in sentence and "용기 있게" in sentence)
        )
        if (
            10 <= hangul_count(sentence) <= 22
            and not awkward_repetition
            and sentence not in candidates
        ):
            candidates.append(sentence)

    # 한 페이지의 세 문장이 서로 다른 사건과 주체를 다루도록 사건을 먼저
    # 순환한다. 기본 문장을 우선 배치해 같은 행동을 부사만 바꿔 반복하는
    # 인상을 줄이고, 부족한 수량만 자연스러운 부사형 문장으로 채운다.
    for subject_shift in range(len(plot.subjects)):
        for event_index, (object_phrase, action) in enumerate(event_pairs):
            subject = plot.subjects[(subject_shift + event_index) % len(plot.subjects)]
            add(f"{subject} {object_phrase} {action}.")

    for adverb_shift in range(len(ADVERBS)):
        for subject_shift in range(len(plot.subjects)):
            for event_index, (object_phrase, action) in enumerate(event_pairs):
                subject = plot.subjects[(subject_shift + event_index) % len(plot.subjects)]
                adverb = ADVERBS[(adverb_shift + subject_shift + event_index) % len(ADVERBS)]
                add(f"{subject} {adverb} {object_phrase} {action}.")

    # 목적어까지 포함하면 음절 상한을 넘는 조합에 한해 짧은 연결 문장으로
    # 10페이지 분량을 보충한다. 주체는 항상 남겨 문맥이 끊기지 않게 한다.
    for subject_shift in range(len(plot.subjects)):
        for event_index, (_, action) in enumerate(event_pairs):
            subject = plot.subjects[(subject_shift + event_index) % len(plot.subjects)]
            for adverb_shift in range(len(ADVERBS)):
                adverb = ADVERBS[(adverb_shift + subject_shift + event_index) % len(ADVERBS)]
                add(f"{subject} {adverb} {action}.")
    if len(candidates) < 30:
        raise ValueError(f"not enough valid sentences for {plot.key} day {day_index + 1}")
    return candidates


def story_pages(story: StorySpec) -> list[dict[str, Any]]:
    plot = PLOTS[story.plot_key]
    pages = []
    for page_no in range(1, story.progress + 1):
        day_index = (page_no - 1) // 10
        within_day = (page_no - 1) % 10
        pool = story_sentence_pool(plot, day_index)
        offset = (within_day * 3 + day_index * 7) % len(pool)
        branch_index = 0 if within_day == 3 else 1 if within_day == 8 else None
        branch_prompt = None
        if branch_index is not None:
            question = plot.branch_questions[branch_index]
            if not 10 <= hangul_count(question) <= 22:
                raise ValueError(f"invalid branch question: {question}")
            # 기존 이야기 계약에서는 분기 질문이 본문 세 문장에 합쳐지지
            # 않고, 진행률 한 칸을 차지하는 독립 질문 라인으로 저장된다.
            sentences = [question]
            branch_prompt = {
                "subtitle": question,
                "options": [
                    {"optionNo": number, "label": label}
                    for number, label in enumerate(plot.choices[branch_index], 1)
                ],
            }
        else:
            sentences = [pool[(offset + index) % len(pool)] for index in range(3)]
        for sentence in sentences:
            if not 10 <= hangul_count(sentence) <= 22:
                raise ValueError(f"invalid story sentence ({hangul_count(sentence)}): {sentence}")
        pages.append(
            {
                "pageNo": page_no,
                "sentences": sentences,
                "text": " ".join(sentences),
                "branchPrompt": branch_prompt,
                "choice": plot.choices[branch_index][(day_index + branch_index) % 3]
                if branch_index is not None and (story.completed or page_no < story.progress)
                else None,
            }
        )
    return pages


def scene_ranges(progress: int) -> list[tuple[int, int]]:
    ranges = []
    for day_start in range(1, progress + 1, 10):
        for start_offset, end_offset in ((0, 3), (4, 8), (9, 9)):
            start = day_start + start_offset
            if start > progress:
                continue
            ranges.append((start, min(day_start + end_offset, progress)))
    return ranges


def image_name(story: StorySpec, scene_index: int) -> str:
    if scene_index <= len(story.existing_images):
        return story.existing_images[scene_index - 1]
    deterministic_uuid = uuid.uuid5(
        uuid.NAMESPACE_URL,
        f"iread:qa-demo:story:{story.id}:scene:{scene_index}",
    )
    return f"{deterministic_uuid}.jpg"


def gaze_file_name(gaze_session_id: int) -> str:
    deterministic_uuid = f"00000000-0000-4000-8000-{gaze_session_id:012x}"
    return f"gaze-{gaze_session_id}-{deterministic_uuid}.json"


def training_question(template_id: int, question_no: int) -> dict[str, Any]:
    training_type = TRAINING_TYPES[template_id]
    audio_targets = (
        "친구와 함께 천천히 읽어요.",
        "거북이는 코너에서 방향을 잡았어요.",
        "바람이 나뭇잎을 살며시 흔들어요.",
    )
    question: dict[str, Any] = {
        "questionNo": question_no,
        "type": training_type,
    }

    if training_type in AUDIO_TRAINING_TYPES:
        target = audio_targets[question_no - 1]
        question.update(
            {
                "requiredInputs": ["VOICE", "GAZE"],
                "content": {
                    "instruction": "처음부터 끝까지 또박또박 읽어 보세요.",
                    "sentence": target,
                },
                "analysisTargets": [{"text": target}],
                "answer": {"expectedText": target},
            }
        )
        return question

    if training_type in {"VOWEL_TRACE", "CONSONANT_TRACE", "SYLLABLE_TRACE"}:
        targets = {
            "VOWEL_TRACE": ("ㅏ", "ㅓ", "ㅗ"),
            "CONSONANT_TRACE": ("ㄴ", "ㄷ", "ㅁ"),
            "SYLLABLE_TRACE": ("난", "단", "만"),
        }[training_type]
        target = targets[question_no - 1]
        question.update(
            {
                "requiredInputs": ["VOICE", "GAZE"],
                "content": {"instruction": f"{target} 모양을 획순대로 따라 써 보세요.", "target": target},
                "answer": {"target": target},
            }
        )
        return question

    if training_type in {"SYLLABLE_BLEND", "SENTENCE_ASSEMBLY"}:
        cards = ["친구와", "함께", "책을", "읽어요"]
        question.update(
            {
                "content": {"instruction": "카드를 알맞은 순서로 놓아 문장을 만드세요.", "cards": cards},
                "answer": {"answerOrder": [0, 1, 2, 3]},
            }
        )
        return question

    if training_type in {"BASIC_SYLLABLE_BUILD", "FINAL_SYLLABLE_BUILD", "DOUBLE_FINAL_BUILD"}:
        question.update(
            {
                "content": {
                    "instruction": "초성·중성·종성을 골라 글자를 만드세요.",
                    "initialChoices": ["ㄴ", "ㄷ", "ㅁ"],
                    "medialChoices": ["ㅏ", "ㅓ", "ㅗ"],
                    "finalChoices": ["ㄴ", "ㄹ", "ㅁ"],
                },
                "answer": {
                    "initialAnswerIndex": 0,
                    "medialAnswerIndex": 0,
                    "finalAnswerIndex": 0,
                },
            }
        )
        return question

    if training_type == "FILL_IN_THE_BLANK":
        question.update(
            {
                "content": {
                    "inputType": "TEXT",
                    "prompt": "친구와 함께 책을 ___.",
                    "instruction": "빈칸에 알맞은 말을 직접 쓰세요.",
                },
                "answer": {"acceptedAnswers": ["읽어요"]},
            }
        )
        return question

    choices = ["ㄴ", "ㄷ", "ㅁ"]
    instruction = "알맞은 소리를 골라 보세요."
    if training_type in {"FINAL_CONSONANT_DELETE", "SYLLABLE_DELETE"}:
        choices = ["꽃", "꼬", "꼭"]
        instruction = "받침이나 음절을 뺀 알맞은 낱말을 고르세요."
    elif training_type == "SYLLABLE_REPLACE":
        choices = ["나비", "다비", "마비"]
        instruction = "첫 음절을 바꾼 알맞은 낱말을 고르세요."
    elif training_type == "IMAGE_SENTENCE_MATCH":
        choices = ["아이가 책을 읽어요.", "아이가 공을 차요.", "아이가 잠을 자요."]
        instruction = "그림 설명과 가장 잘 맞는 문장을 고르세요."
    answer_field = "deleteIndex" if training_type == "SYLLABLE_DELETE" else "answerIndex"
    question.update(
        {
            "content": {"instruction": instruction, "choices": choices},
            "answer": {answer_field: 0},
        }
    )
    return question


def generated_data(student: Student, template_id: int, day_no: int, sequence_no: int) -> dict[str, Any]:
    return {
        "schemaVersion": 2,
        "trainingType": TRAINING_TYPES[template_id],
        "persona": student.weakness,
        "questions": [training_question(template_id, question_no) for question_no in range(1, 4)],
        "demoMeta": {"learningDay": day_no, "sequenceNo": sequence_no},
    }


def question_attempt_tokens(question: dict[str, Any]) -> tuple[str, str, str, str]:
    if question["type"] in AUDIO_TRAINING_TYPES:
        tokens = question["analysisTargets"][0]["text"].rstrip(".").split()
    else:
        tokens = "문항 내용을 차분히 확인해요".split()
    if len(tokens) < 4:
        tokens.extend(["확인해요"] * (4 - len(tokens)))
    return tuple(tokens[:4])


def question_correct_answer(question: dict[str, Any]) -> str:
    answer = question["answer"]
    content = question["content"]
    if question["type"] in AUDIO_TRAINING_TYPES:
        return answer["expectedText"]
    if "answerIndex" in answer:
        return content["choices"][answer["answerIndex"]]
    if "deleteIndex" in answer:
        return content["choices"][answer["deleteIndex"]]
    if "answerOrder" in answer:
        return " ".join(content["cards"][index] for index in answer["answerOrder"])
    if "acceptedAnswers" in answer:
        return answer["acceptedAnswers"][0]
    if "target" in answer:
        return answer["target"]
    slots = (
        ("초성", "initialChoices", "initialAnswerIndex"),
        ("중성", "medialChoices", "medialAnswerIndex"),
        ("종성", "finalChoices", "finalAnswerIndex"),
    )
    return " · ".join(
        f"{label}: {content[choices][answer[index]]}"
        for label, choices, index in slots
        if index in answer
    )


def question_selected_answer(question: dict[str, Any], correct: bool) -> str:
    expected = question_correct_answer(question)
    if correct:
        return expected
    content = question["content"]
    if "choices" in content and len(content["choices"]) > 1:
        return content["choices"][1]
    if question["type"] in AUDIO_TRAINING_TYPES:
        return expected.rstrip(".") + " 중 일부를 놓쳐 읽음"
    if "cards" in content:
        return " ".join(reversed(content["cards"]))
    if question["type"] == "FILL_IN_THE_BLANK":
        return "보아요"
    return "획순 또는 글자 조합 일부가 다름"


def training_correct_count(student: Student, day_no: int, sequence_no: int) -> int:
    persona_base = {1: 7, 2: 8, 3: 10}[student.no]
    improvement = (day_no - 1) // 3
    variation = (day_no + sequence_no + student.no) % 2
    return min(12, persona_base + improvement + variation)


def training_accuracy(student: Student, day_no: int, sequence_no: int) -> int:
    return round(training_correct_count(student, day_no, sequence_no) / 12 * 1000)


def daily_accuracy(student: Student, day_no: int) -> float:
    values = [training_accuracy(student, day_no, sequence_no) / 10 for sequence_no in range(1, 6)]
    return round(sum(values) / len(values), 1)


def reading_speed(student: Student, day_no: int) -> int:
    return student.reading_speed + day_no - 1


def training_gaze_metrics(student: Student, training_id: int) -> dict[str, int]:
    variation = training_id % 100 % 4
    if student.no == 1:
        return {
            "totalVisitedDurationMs": 33_000 - variation * 70,
            "totalVisitedCount": 70 - variation,
            "reverseReadCount": 8 - variation,
            "avgVisitedDurationMs": 650 - variation * 5,
        }
    if student.no == 2:
        return {
            "totalVisitedDurationMs": 32_000 - variation * 60,
            "totalVisitedCount": 68 - variation,
            "reverseReadCount": 6 - variation,
            "avgVisitedDurationMs": 630 - variation * 5,
        }
    return {
        "totalVisitedDurationMs": 39_000 - variation * 80,
        "totalVisitedCount": 76 - variation,
        "reverseReadCount": 4 - variation,
        "avgVisitedDurationMs": 760 - variation * 5,
    }


def distribute(total: int, count: int) -> list[int]:
    quotient, remainder = divmod(total, count)
    return [quotient + (1 if index < remainder else 0) for index in range(count)]


def training_gaze_fixture(
    student: Student,
    training_id: int,
    gaze_session_id: int,
    generated: dict[str, Any],
    started: datetime,
    finished: datetime,
) -> dict[str, Any]:
    metrics = training_gaze_metrics(student, training_id)
    interval_ms = {1: 190, 2: 210, 3: 270}[student.no]
    samples_per_visit = {1: 3, 2: 3, 3: 4}[student.no]
    regression_distribution = distribute(metrics["reverseReadCount"], 3)
    captured_at_ms = 0
    samples = []
    regressions = []
    questions = []

    for question, regression_count in zip(
        generated["questions"], regression_distribution, strict=True
    ):
        question_no = question["questionNo"]
        tokens = list(question_attempt_tokens(question))
        questions.append(
            {
                "questionNo": question_no,
                "type": question["type"],
                "tokens": tokens,
            }
        )
        visits: list[tuple[int, int | None]] = [
            (token_index, None) for token_index in range(len(tokens))
        ]
        for regression_no in range(regression_count):
            from_index = 3 if regression_no % 2 == 0 else 2
            to_index = 1 if regression_no % 2 == 0 else 0
            visits.extend(((from_index, None), (to_index, from_index)))
        for visit_no, (token_index, regression_from) in enumerate(visits):
            if regression_from is not None:
                regressions.append(
                    {
                        "questionNo": question_no,
                        "fromTokenIndex": regression_from,
                        "toTokenIndex": token_index,
                        "capturedAtMs": captured_at_ms,
                    }
                )
            for sample_no in range(samples_per_visit):
                samples.append(
                    {
                        "questionNo": question_no,
                        "tokenIndex": token_index,
                        "text": tokens[token_index],
                        "capturedAtMs": captured_at_ms,
                        "presence": True,
                        "x": round(0.18 + token_index * 0.17 + sample_no * 0.005, 3),
                        "y": round(0.24 + question_no * 0.16 + (visit_no % 2) * 0.01, 3),
                    }
                )
                captured_at_ms += interval_ms
            captured_at_ms += 45

    return {
        "rawData": {
            "schemaVersion": "training-gaze-raw-v1",
            "synthetic": True,
            "studentId": student.id,
            "trainingId": training_id,
            "gazeSessionId": gaze_session_id,
            "trainingType": generated["trainingType"],
            "persona": student.weakness,
            "recordingStartedAt": started.isoformat(),
            "recordingEndedAt": finished.isoformat(),
            "questions": questions,
            "regressions": regressions,
            "samples": samples,
        }
    }


def azure_fixture(reference: str, score: int, student_id: int, test_id: int, question_no: int) -> dict[str, Any]:
    words = []
    offset = 0
    for token in reference.rstrip(".").split():
        duration = 4_000_000 + len(token) * 300_000
        words.append(
            {
                "Word": token,
                "Offset": offset,
                "Duration": duration,
                "PronunciationAssessment": {"AccuracyScore": score, "ErrorType": "None"},
            }
        )
        offset += duration
    return {
        "RecognitionStatus": "Success",
        "Offset": 0,
        "Duration": offset,
        "NBest": [
            {
                "Confidence": round(score / 100, 2),
                "Lexical": reference.rstrip("."),
                "Display": reference,
                "PronunciationAssessment": {
                    "AccuracyScore": score,
                    "FluencyScore": max(0, score - 4),
                    "CompletenessScore": 100,
                    "PronScore": max(0, score - 2),
                },
                "Words": words,
            }
        ],
        "_demo": {
            "schemaVersion": "azure-speech-pronunciation-v1",
            "studentId": student_id,
            "testId": test_id,
            "questionNo": question_no,
            "synthetic": True,
        },
    }


def gaze_metric_change(first: int, latest: int) -> dict[str, int]:
    return {"first": first, "latest": latest, "delta": latest - first}


def training_report_gaze_series(student: Student) -> dict[str, Any]:
    points = []
    for training_id in REPORT_TRAINING_GAZE_IDS[student.id]:
        day_no = (training_id % 100) // 10
        sequence_no = training_id % 10
        finished_minute = (sequence_no - 1) * 9 + 8
        metrics = training_gaze_metrics(student, training_id)
        points.append(
            {
                "gazeAnalysisResultId": 500_000 + training_id,
                "gazeSessionId": 400_000 + training_id,
                "sourceType": "TRAINING",
                "sourceId": training_id,
                "analyzedAt": (
                    f"{student.completed_dates[day_no - 1]}T15:{finished_minute:02d}:10"
                ),
                **metrics,
            }
        )
    first, latest = points[0], points[-1]
    changes = {
        key: gaze_metric_change(first[key], latest[key])
        for key in (
            "totalVisitedDurationMs",
            "totalVisitedCount",
            "reverseReadCount",
            "avgVisitedDurationMs",
        )
    }
    return {
        "status": "AVAILABLE",
        "comparisonAvailable": True,
        "points": points,
        "changes": changes,
        "descriptions": [
            f"{student.weakness} 페르소나에 맞춘 훈련 시선 변화입니다."
        ],
        "failedSessionCount": 0,
    }


def report_gaze_series(student: Student) -> dict[str, Any]:
    test_date = student.completed_dates[-2]
    points = []
    for sequence_no in range(1, 4):
        improvement_step = sequence_no - 1
        points.append(
            {
                "gazeAnalysisResultId": 351000 + student.no * 10 + sequence_no,
                "gazeSessionId": 350000 + student.no * 10 + sequence_no,
                "sourceType": "TEST",
                "sourceId": 341000 + student.no * 10 + sequence_no,
                "analyzedAt": f"{test_date}T14:{improvement_step * 10 + 7:02d}:10",
                "totalVisitedDurationMs": 24000 + student.no * 1800 - improvement_step * 600,
                "totalVisitedCount": 36 + student.no * 2 - improvement_step,
                "reverseReadCount": max(0, 5 - student.no - improvement_step),
                "avgVisitedDurationMs": 620 + student.no * 55 - improvement_step * 10,
            }
        )
    first, latest = points[0], points[-1]
    changes = {
        key: gaze_metric_change(first[key], latest[key])
        for key in (
            "totalVisitedDurationMs",
            "totalVisitedCount",
            "reverseReadCount",
            "avgVisitedDurationMs",
        )
    }
    return {
        "status": "AVAILABLE",
        "comparisonAvailable": True,
        "points": points,
        "changes": changes,
        "descriptions": [
            f"{student.weakness} 관련 검사에서 시선 이탈과 되읽기 변화를 확인했습니다."
        ],
        "failedSessionCount": 0,
    }


def report_snapshot(student: Student, days: int) -> dict[str, Any]:
    first_day_no = len(student.completed_dates) - days + 1
    growth = [
        {
            "date": date,
            "accuracy": daily_accuracy(student, first_day_no + index),
            "readingSpeed": reading_speed(student, first_day_no + index),
            "pronunciationScore": round((student.base_score + (first_day_no + index) * 10) / 10, 1),
        }
        for index, date in enumerate(student.completed_dates[-days:])
    ]
    metric_definitions = (
        ("ACCURACY", "accuracy", "읽기 정확도"),
        ("READING_SPEED", "readingSpeed", "읽기 속도"),
        ("PRONUNCIATION_SCORE", "pronunciationScore", "발음 점수"),
    )
    metric_changes = []
    descriptions = []
    for metric, key, label in metric_definitions:
        first = growth[0][key]
        latest = growth[-1][key]
        delta = round(latest - first, 2)
        direction = "INCREASED" if delta > 0 else "DECREASED" if delta < 0 else "UNCHANGED"
        metric_changes.append(
            {
                "metric": metric,
                "first": first,
                "latest": latest,
                "delta": delta,
                "direction": direction,
            }
        )
        direction_label = "증가" if delta > 0 else "감소" if delta < 0 else "유지"
        descriptions.append(f"{label}가 {first:.2f}에서 {latest:.2f}로 {direction_label}했습니다.")

    training_series = training_report_gaze_series(student)
    test_series = report_gaze_series(student)
    latest_point = test_series["points"][-1]
    return {
        "snapshotVersion": "teacher-report-v2",
        "calculationVersion": "reading-metrics-v1",
        "learningDays": days,
        "totalTrainingTimeMinutes": days * 45,
        "completedTrainingCount": days * 5,
        "averageAccuracy": round(sum(point["accuracy"] for point in growth) / len(growth), 1),
        "averageReadingSpeed": round(sum(point["readingSpeed"] for point in growth) / len(growth), 1),
        "readingSpeedUnit": "CORRECT_WORDS_PER_MINUTE",
        "growthHistory": growth,
        "growthComparisonStatus": "AVAILABLE",
        "automaticAnalysis": {
            "status": "AVAILABLE",
            "metricChanges": metric_changes,
            "descriptions": descriptions,
        },
        "areaAchievements": [
            {
                "area": student.strength,
                "achievement": min(98, round((student.base_score + 160) / 10, 1)),
            },
            {
                "area": student.weakness,
                "achievement": max(40, round((student.base_score - 40) / 10, 1)),
            },
            {"area": "문장 유창성", "achievement": student.reading_speed},
        ],
        "frequentlyIncorrectWords": [
            {
                "wordId": 390000 + student.no,
                "wordName": "꽃밭",
                "attemptCount": 4,
                "incorrectCount": 2 if student.no < 3 else 1,
                "incorrectRate": 50.0 if student.no < 3 else 25.0,
            }
        ],
        "improvedPatterns": [student.strength],
        "persistentDifficultyPatterns": [student.weakness],
        "gazeAnalysis": {
            "gazeAnalysisResultId": latest_point["gazeAnalysisResultId"],
            "totalDwellTime": latest_point["totalVisitedDurationMs"],
            "dwellCount": latest_point["totalVisitedCount"],
            "regressionCount": latest_point["reverseReadCount"],
            "averageFixationTime": latest_point["avgVisitedDurationMs"],
        },
        "gazeTrend": {
            "generatedAt": "2026-08-05T09:00:00",
            "training": training_series,
            "test": test_series,
        },
    }


def build() -> tuple[str, dict[str, Any], dict[str, Any], dict[Path, Any]]:
    sql: list[str] = [
        "-- Generated by tools/generate_qa_demo_dataset.py. Do not edit by hand.\n",
        "-- Manual reset only; this file is not executed on every server start.\n\n",
        "DELETE FROM auth_refresh_sessions WHERE teacher_id = 1001;\n",
        "DELETE FROM password_reset_tokens WHERE teacher_id = 1001;\n",
        "DELETE FROM gaze_analysis_results WHERE gaze_session_id IN (SELECT id FROM gaze_sessions WHERE student_id IN (2001, 2002, 2103));\n",
        "DELETE FROM gaze_sessions WHERE student_id IN (2001, 2002, 2103);\n",
        "DELETE FROM word_attempt_logs WHERE student_id IN (2001, 2002, 2103);\n",
        "DELETE FROM test_datas WHERE test_id IN (SELECT id FROM tests WHERE test_curriculum_id IN (SELECT id FROM test_curriculums WHERE student_id IN (2001, 2002, 2103)));\n",
        "DELETE FROM tests WHERE test_curriculum_id IN (SELECT id FROM test_curriculums WHERE student_id IN (2001, 2002, 2103));\n",
        "DELETE FROM test_curriculums WHERE student_id IN (2001, 2002, 2103);\n",
        "DELETE FROM training_datas WHERE train_id IN (SELECT id FROM trainings WHERE daily_curriculum_id IN (SELECT id FROM daily_curriculums WHERE student_id IN (2001, 2002, 2103)));\n",
        "DELETE FROM trainings WHERE daily_curriculum_id IN (SELECT id FROM daily_curriculums WHERE student_id IN (2001, 2002, 2103));\n",
        "DELETE FROM daily_curriculums WHERE student_id IN (2001, 2002, 2103);\n",
        "DELETE FROM reports WHERE student_id IN (2001, 2002, 2103);\n",
        "DELETE FROM student_feature_profiles WHERE student_id IN (2001, 2002, 2103);\n",
        "DELETE FROM story_page_edit_audits WHERE story_line_id IN (SELECT id FROM story_lines WHERE scene_id IN (SELECT scene_id FROM story_scenes WHERE story_id IN (SELECT id FROM stories WHERE student_id IN (2001, 2002, 2103))));\n",
        "DELETE FROM story_choices WHERE story_line_id IN (SELECT id FROM story_lines WHERE scene_id IN (SELECT scene_id FROM story_scenes WHERE story_id IN (SELECT id FROM stories WHERE student_id IN (2001, 2002, 2103))));\n",
        "DELETE FROM story_lines WHERE scene_id IN (SELECT scene_id FROM story_scenes WHERE story_id IN (SELECT id FROM stories WHERE student_id IN (2001, 2002, 2103)));\n",
        "DELETE FROM story_scenes WHERE story_id IN (SELECT id FROM stories WHERE student_id IN (2001, 2002, 2103));\n",
        "DELETE FROM characters WHERE story_id IN (SELECT id FROM stories WHERE student_id IN (2001, 2002, 2103));\n",
        "DELETE FROM stories WHERE student_id IN (2001, 2002, 2103);\n\n",
        "UPDATE teachers SET email='test@test.com', password='$2a$10$vfiy7KBnt1J1WNY1e/AMpuGU2Jbf95qaYXAkZ50CC0HK06Zuu1TIi', name='시연교수자', organization='ssafy' WHERE id=1001;\n",
    ]

    for student in STUDENTS:
        sql.append(
            "UPDATE students SET "
            f"name={sql_quote(student.name)}, birthday={sql_quote(student.birthday)}, gender={sql_quote(student.gender)}, "
            f"school={sql_quote(student.school)}, guardian={sql_quote(student.guardian)}, guardian_contact={sql_quote(student.phone)}, "
            f"guardian_email={sql_quote(student.email)}, address={sql_quote(student.address)}, "
            f"image_url={sql_quote('/images/student-profile-boy.png' if student.gender == 'Boy' else '/images/student-profile-girl.png')}, "
            f"teacher_memo={sql_quote(student.memo)} WHERE id={student.id};\n"
        )

    raw_assets: dict[Path, Any] = {}
    curriculum_rows = []
    training_rows = []
    training_data_rows = []
    training_attempt_rows = []
    training_gaze_rows = []
    training_analysis_rows = []
    training_attempt_id = 380000
    for student in STUDENTS:
        for day_no, date in enumerate(student.completed_dates, 1):
            curriculum_id = 310000 + student.no * 100 + day_no
            curriculum_started = datetime.fromisoformat(f"{date} 15:00:00")
            curriculum_completed = curriculum_started + timedelta(minutes=45)
            curriculum_rows.append(
                (
                    curriculum_id,
                    student.id,
                    "COMPLETED",
                    curriculum_started.strftime("%Y-%m-%d %H:%M:%S"),
                    curriculum_completed.strftime("%Y-%m-%d %H:%M:%S"),
                )
            )
            templates = CURRICULUM_ROTATIONS[student.id][day_no - 1]
            for sequence_no, template_id in enumerate(templates, 1):
                training_id = 320000 + student.no * 1000 + day_no * 10 + sequence_no
                started = curriculum_started + timedelta(minutes=(sequence_no - 1) * 9)
                finished = started + timedelta(minutes=8)
                generated = generated_data(student, template_id, day_no, sequence_no)
                correct_count = training_correct_count(student, day_no, sequence_no)
                attempt_correctness = [
                    (attempt_index * 5 + day_no + sequence_no + student.no) % 12 < correct_count
                    for attempt_index in range(12)
                ]
                question_results = []
                attempt_index = 0
                is_audio_training = TRAINING_TYPES[template_id] in AUDIO_TRAINING_TYPES
                has_gaze_training = TRAINING_TYPES[template_id] in GAZE_TRAINING_TYPES
                voice_duration_ms = round(
                    correct_count * 60_000 / reading_speed(student, day_no)
                ) if is_audio_training else 0
                for question in generated["questions"]:
                    question_no = question["questionNo"]
                    question_correctness = attempt_correctness[attempt_index:attempt_index + 4]
                    correct_attempts = sum(question_correctness)
                    question_is_correct = correct_attempts >= 3
                    question_results.append(
                        {
                            "questionNo": question_no,
                            "questionNumber": question_no,
                            "questionType": question["type"],
                            "isCorrect": question_is_correct,
                            "selectedAnswer": question_selected_answer(question, question_is_correct),
                            "correctAnswer": question_correct_answer(question),
                            "score": round(correct_attempts / 4 * 100),
                        }
                    )
                    for token_index, token in enumerate(question_attempt_tokens(question)):
                        is_correct = attempt_correctness[attempt_index]
                        speech_start = round(attempt_index * voice_duration_ms / 12) if is_audio_training else None
                        speech_end = round((attempt_index + 1) * voice_duration_ms / 12) if is_audio_training else None
                        training_attempt_id += 1
                        training_attempt_rows.append(
                            (
                                training_attempt_id,
                                student,
                                training_id,
                                question_no,
                                token_index,
                                token,
                                has_gaze_training,
                                is_audio_training,
                                is_correct,
                                finished.strftime("%Y-%m-%d %H:%M:%S"),
                                speech_start,
                                speech_end,
                            )
                        )
                        attempt_index += 1
                score = training_accuracy(student, day_no, sequence_no)
                result = {
                    "schemaVersion": 2,
                    "questions": question_results,
                    "learningAssessment": (
                        f"{student.weakness} 중심 훈련에서 {score / 10:.1f}% 정확도를 기록했습니다."
                    ),
                }
                training_rows.append(
                    (
                        training_id,
                        template_id,
                        curriculum_id,
                        sequence_no,
                        started.strftime("%Y-%m-%d %H:%M:%S"),
                        started.strftime("%Y-%m-%d %H:%M:%S"),
                        finished.strftime("%Y-%m-%d %H:%M:%S"),
                        "COMPLETED",
                        json.dumps(result, ensure_ascii=False, separators=(",", ":")),
                        score,
                    )
                )
                training_data_rows.append(
                    (
                        330000 + student.no * 1000 + day_no * 10 + sequence_no,
                        training_id,
                        json.dumps(generated, ensure_ascii=False, separators=(",", ":")),
                        started.strftime("%Y-%m-%d %H:%M:%S"),
                    )
                )
                if has_gaze_training:
                    gaze_session_id = 400_000 + training_id
                    gaze_name = gaze_file_name(gaze_session_id)
                    metrics = training_gaze_metrics(student, training_id)
                    raw_assets[Path("gaze") / str(student.id) / gaze_name] = training_gaze_fixture(
                        student,
                        training_id,
                        gaze_session_id,
                        generated,
                        started,
                        finished,
                    )
                    training_gaze_rows.append(
                        (
                            gaze_session_id,
                            student.id,
                            None,
                            training_id,
                            None,
                            "TRAINING",
                            started.strftime("%Y-%m-%d %H:%M:%S"),
                            finished.strftime("%Y-%m-%d %H:%M:%S"),
                            f"/gaze/{student.id}/{gaze_name}",
                            "COMPLETED",
                            "SUCCESS",
                            started.strftime("%Y-%m-%d %H:%M:%S"),
                        )
                    )
                    duration_parts = distribute(metrics["totalVisitedDurationMs"], 3)
                    count_parts = distribute(metrics["totalVisitedCount"], 3)
                    regression_parts = distribute(metrics["reverseReadCount"], 3)
                    sentence_metrics = [
                        {
                            "questionNo": question_no,
                            "dwellDurationMs": duration_parts[question_no - 1],
                            "fixationCount": count_parts[question_no - 1],
                            "regressionCount": regression_parts[question_no - 1],
                        }
                        for question_no in range(1, 4)
                    ]
                    training_analysis_rows.append(
                        (
                            500_000 + training_id,
                            gaze_session_id,
                            metrics["totalVisitedDurationMs"],
                            metrics["totalVisitedCount"],
                            metrics["reverseReadCount"],
                            metrics["avgVisitedDurationMs"],
                            json.dumps(sentence_metrics, ensure_ascii=False, separators=(",", ":")),
                            json.dumps(
                                raw_assets[Path("gaze") / str(student.id) / gaze_name]["rawData"]["regressions"],
                                ensure_ascii=False,
                                separators=(",", ":"),
                            ),
                            json.dumps(
                                {
                                    "source": "qa-demo",
                                    "persona": student.weakness,
                                    "calculationVersion": "training-gaze-v1",
                                    "rawSchemaVersion": "training-gaze-raw-v1",
                                },
                                ensure_ascii=False,
                                separators=(",", ":"),
                            ),
                            finished.strftime("%Y-%m-%d %H:%M:%S"),
                        )
                    )
        curriculum_id = 310000 + student.no * 100 + 90
        curriculum_rows.append((curriculum_id, student.id, "NOT_STARTED", "2026-08-05 09:00:00", None))
        templates = CURRICULUM_ROTATIONS[student.id][8]
        for sequence_no, template_id in enumerate(templates, 1):
            training_id = 320000 + student.no * 1000 + 900 + sequence_no
            training_rows.append((training_id, template_id, curriculum_id, sequence_no, "2026-08-05 09:00:00", None, None, "NOT_STARTED" if sequence_no == 1 else "NOT_READY", None, None))
            training_data_rows.append((330000 + student.no * 1000 + 900 + sequence_no, training_id, json.dumps(generated_data(student, template_id, 9, sequence_no), ensure_ascii=False, separators=(",", ":")), "2026-08-05 09:00:00"))

    sql.append("\n" + rows_sql("daily_curriculums", ("id", "student_id", "status", "created_at", "completed_at"), curriculum_rows))
    sql.append(rows_sql("trainings", ("id", "training_template_id", "daily_curriculum_id", "sequence_no", "created_at", "started_at", "finished_at", "status", "result", "accuracy"), training_rows))
    sql.append(rows_sql("training_datas", ("id", "train_id", "generated_data", "created_at"), training_data_rows))
    for attempt in training_attempt_rows:
        token = attempt[5]
        sql.append(
            f"INSERT INTO words (content, length) VALUES ({sql_quote(token)}, {len(token)}) "
            "ON DUPLICATE KEY UPDATE length=VALUES(length);\n"
        )
    for (
        log_id,
        student,
        training_id,
        question_no,
        token_index,
        token,
        has_gaze,
        has_audio,
        is_correct,
        created_at,
        speech_start,
        speech_end,
    ) in training_attempt_rows:
        pronunciation_score = 900 if is_correct else 550
        sql.append(
            "INSERT INTO word_attempt_logs (id, student_id, word_id, story_line_id, training_id, test_id, use_location, surface_text, has_gaze_data, has_audio_data, fixation_duration_ms, fixation_count, gaze_start_offset_ms, gaze_end_offset_ms, is_skipped, regression_count, pronunciation_accuracy_score, speech_start_offset_ms, speech_end_offset_ms, is_correct, created_at, total_score, question_no, target_index, token_index, is_final) "
            f"SELECT {log_id}, {student.id}, id, NULL, {training_id}, NULL, 'TRAINING', {sql_quote(token)}, {'TRUE' if has_gaze else 'FALSE'}, {'TRUE' if has_audio else 'FALSE'}, NULL, NULL, NULL, NULL, FALSE, 0, {pronunciation_score if has_audio else 'NULL'}, {speech_start if has_audio else 'NULL'}, {speech_end if has_audio else 'NULL'}, {'TRUE' if is_correct else 'FALSE'}, {sql_quote(created_at)}, {1000 if is_correct else 0}, {question_no}, 0, {token_index}, TRUE FROM words WHERE content={sql_quote(token)};\n"
        )
    sql.append(rows_sql("gaze_sessions", ("id", "student_id", "test_id", "training_id", "story_id", "content_type", "started_at", "ended_at", "data_url", "status", "calibration_status", "created_at"), training_gaze_rows))
    sql.append(rows_sql("gaze_analysis_results", ("id", "gaze_session_id", "total_visited_duration", "total_visited_count", "reverse_read_count", "avg_visited_duration", "sentence_metrics", "regressions", "analysis_meta", "created_at"), training_analysis_rows))

    test_curriculum_rows = []
    test_rows = []
    test_data_rows = []
    word_attempt_selects = []
    test_gaze_rows = []
    test_analysis_rows = []
    attempt_id = 360000
    for student in STUDENTS:
        tc_id = 340000 + student.no
        test_date = student.completed_dates[-2]
        test_curriculum_rows.append((tc_id, student.id, "COMPLETED", f"{test_date} 14:00:00", f"{test_date} 14:40:00"))
        for sequence_no, template_id in enumerate((11, 15, 20), 1):
            test_id = 341000 + student.no * 10 + sequence_no
            improvement_step = sequence_no - 1
            test_start_minute = improvement_step * 10
            gaze_start_minute = test_start_minute + 5
            test_end_minute = test_start_minute + 7
            questions = [
                {"questionNo": 1, "type": "SENTENCE_READING", "text": "바람이 나뭇잎을 살며시 흔들어요.", "analysisTargets": [{"text": "바람이 나뭇잎을 살며시 흔들어요."}]},
                {"questionNo": 2, "type": "CONSONANT_SOUND_CHOICE", "content": {"instruction": "알맞은 소리를 고르세요.", "choices": ["ㄴ", "ㄷ", "ㅁ"]}, "answer": {"answerIndex": 0}},
                {"questionNo": 3, "type": "SENTENCE_READING", "text": "친구와 함께 천천히 책을 읽었어요.", "analysisTargets": [{"text": "친구와 함께 천천히 책을 읽었어요."}]},
            ]
            score = min(96, round((student.base_score + sequence_no * 15) / 10))
            result_questions = []
            analyses = []
            links = []
            for question in questions:
                qno = question["questionNo"]
                audio = qno in (1, 3)
                question_score = score - (2 if qno == 2 and student.no < 3 else 0)
                result_questions.append({"questionNumber": qno, "question": question.get("text", "알맞은 소리를 고르세요."), "selectedAnswer": question.get("text", "ㄴ"), "correctAnswer": question.get("text", "ㄴ"), "isCorrect": True, "score": question_score, "solvingTimeSeconds": 10 + student.no * 2 + sequence_no + qno, "gazeDepartureCount": 0 if student.no == 3 else (qno + sequence_no) % 2})
                reference = question.get("text", question.get("content", {}).get("instruction", ""))
                pronunciation_score = None
                if audio:
                    pronunciation_score = max(45, min(98, score + qno - (8 if student.no == 1 else 3 if student.no == 2 else 0)))
                    raw_name = f"student-{student.id}-test-{test_id}-q{qno}.json"
                    raw_assets[Path("pronunciation") / str(student.id) / raw_name] = azure_fixture(reference, pronunciation_score, student.id, test_id, qno)
                    analyses.append({"questionNo": qno, "targetIndex": 0, "tokenIndex": None, "referenceText": reference, "pronunciationAccuracyScore": pronunciation_score, "fluencyScore": pronunciation_score - 4, "completenessScore": 100, "pronScore": pronunciation_score - 2, "confidence": round(pronunciation_score / 100, 2), "analysisVersion": "azure-speech-v1", "insertionCount": 0, "attemptNo": 1, "passed": pronunciation_score >= 70, "questionCompleted": True, "rawAssetPath": f"assets/qa-demo/pronunciation/{student.id}/{raw_name}"})
                for token_index, token in enumerate(reference.rstrip(".").split()):
                    attempt_id += 1
                    if audio:
                        links.append({"wordAttemptLogId": attempt_id, "questionNo": qno, "targetIndex": 0, "tokenIndex": token_index, "isFinal": True, "referenceText": token, "pronunciationAccuracyScore": pronunciation_score, "pronunciationErrorType": "None", "pronunciationAnalysisVersion": "azure-speech-v1", "wordReadTimeMs": 520 + token_index * 30})
                    word_attempt_selects.append((attempt_id, student, test_id, qno, token_index, token, pronunciation_score, question_score, test_date, audio))
            result = {"schemaVersion": 2, "overallScore": score, "areaScores": [{"area": student.strength, "score": min(98, score + 5)}, {"area": student.weakness, "score": max(40, score - 8)}, {"area": "읽기 유창성", "score": max(40, student.reading_speed)}], "questions": result_questions, "pronunciationAnalyses": analyses, "wordAttempts": links}
            test_rows.append((test_id, tc_id, template_id, "COMPLETED", json.dumps(result, ensure_ascii=False, separators=(",", ":")), score, f"{test_date} 14:{test_start_minute:02d}:00", f"{test_date} 14:{test_start_minute:02d}:00", f"{test_date} 14:{test_end_minute:02d}:00", sequence_no))
            test_data_rows.append((342000 + student.no * 10 + sequence_no, test_id, json.dumps({"schemaVersion": 2, "questions": questions}, ensure_ascii=False, separators=(",", ":")), f"{test_date} 14:00:00"))
            gaze_id = 350000 + student.no * 10 + sequence_no
            gaze_name = gaze_file_name(gaze_id)
            raw_assets[Path("gaze") / str(student.id) / gaze_name] = {"rawData": {"schemaVersion": "test-gaze-raw-v1", "studentId": student.id, "testId": test_id, "samples": [{"questionNo": question["questionNo"], "tokenIndex": token, "capturedAtMs": question["questionNo"] * 1000 + token * 250, "presence": True, "x": round(0.2 + token * 0.08, 2), "y": round(0.3 + question["questionNo"] * 0.1, 2)} for question in questions for token in range(len(question.get("text", question.get("content", {}).get("instruction", "")).rstrip(".").split()))]}}
            test_gaze_rows.append((gaze_id, student.id, test_id, None, None, "TEST", f"{test_date} 14:{gaze_start_minute:02d}:00", f"{test_date} 14:{test_end_minute:02d}:00", f"/gaze/{student.id}/{gaze_name}", "COMPLETED", "SUCCESS", f"{test_date} 14:{gaze_start_minute:02d}:00"))
            test_analysis_rows.append((351000 + student.no * 10 + sequence_no, gaze_id, 24000 + student.no * 1800 - improvement_step * 600, 36 + student.no * 2 - improvement_step, max(0, 5 - student.no - improvement_step), 620 + student.no * 55 - improvement_step * 10, json.dumps([{"questionNo": q, "dwellDurationMs": 7600 + q * 300 - improvement_step * 180, "fixationCount": 10 + q, "regressionCount": max(0, (0 if student.no == 3 else 1) - improvement_step)} for q in range(1, 4)], ensure_ascii=False, separators=(",", ":")), json.dumps([], separators=(",", ":")), json.dumps({"source": "qa-demo", "persona": student.weakness, "calculationVersion": "test-gaze-v1"}, ensure_ascii=False, separators=(",", ":")), f"{test_date} 14:{test_end_minute:02d}:10"))

    sql.append("\n" + rows_sql("test_curriculums", ("id", "student_id", "status", "created_at", "completed_at"), test_curriculum_rows))
    sql.append(rows_sql("tests", ("id", "test_curriculum_id", "training_template_id", "status", "result", "accuracy", "created_at", "started_at", "finished_at", "sequence_no"), test_rows))
    sql.append(rows_sql("test_datas", ("id", "test_id", "generated_data", "created_at"), test_data_rows))
    for _, _, _, _, _, token, _, _, _, _ in word_attempt_selects:
        sql.append(f"INSERT INTO words (content, length) VALUES ({sql_quote(token)}, {len(token)}) ON DUPLICATE KEY UPDATE length=VALUES(length);\n")
    for log_id, student, test_id, qno, token_index, token, pronunciation_score, question_score, date, audio in word_attempt_selects:
        pronunciation_value = "NULL" if pronunciation_score is None else str(pronunciation_score * 10)
        speech_start = str(token_index * 600) if audio else "NULL"
        speech_end = str((token_index + 1) * 600) if audio else "NULL"
        attempt_score = (pronunciation_score if pronunciation_score is not None else question_score) * 10
        sql.append(
            "INSERT INTO word_attempt_logs (id, student_id, word_id, story_line_id, training_id, test_id, use_location, surface_text, has_gaze_data, has_audio_data, fixation_duration_ms, fixation_count, gaze_start_offset_ms, gaze_end_offset_ms, is_skipped, regression_count, pronunciation_accuracy_score, speech_start_offset_ms, speech_end_offset_ms, is_correct, created_at, total_score, question_no, target_index, token_index, is_final) "
            f"SELECT {log_id}, {student.id}, id, NULL, NULL, {test_id}, 'TEST', {sql_quote(token)}, TRUE, {'TRUE' if audio else 'FALSE'}, 620, 2, {token_index * 600}, {(token_index + 1) * 600}, FALSE, 0, {pronunciation_value}, {speech_start}, {speech_end}, TRUE, {sql_quote(date + ' 14:08:00')}, {attempt_score}, {qno}, 0, {token_index}, TRUE FROM words WHERE content={sql_quote(token)};\n"
        )
    sql.append(rows_sql("gaze_sessions", ("id", "student_id", "test_id", "training_id", "story_id", "content_type", "started_at", "ended_at", "data_url", "status", "calibration_status", "created_at"), test_gaze_rows))
    sql.append(rows_sql("gaze_analysis_results", ("id", "gaze_session_id", "total_visited_duration", "total_visited_count", "reverse_read_count", "avg_visited_duration", "sentence_metrics", "regressions", "analysis_meta", "created_at"), test_analysis_rows))

    story_rows = []
    scene_rows = []
    line_rows = []
    choice_rows = []
    character_rows = []
    story_gaze_rows = []
    story_analysis_rows = []
    scene_prompts = {"schemaVersion": "qa-demo-scene-prompts-v1", "scenes": []}
    scene_id = 281000
    line_id = 282000
    choice_id = 283000
    for story_index, story in enumerate(STORIES, 1):
        plot = PLOTS[story.plot_key]
        pages = story_pages(story)
        story_rows.append((story.id, story.student_id, story.template_id, story.created_at, "COMPLETED" if story.completed else "IN_PROGRESS", story.progress))
        line_by_page = {}
        for scene_index, (start_page, end_page) in enumerate(scene_ranges(story.progress), 1):
            scene_id += 1
            file_name = image_name(story, scene_index)
            scene_rows.append((scene_id, story.id, f"/uploads/images/{file_name}", scene_index, story.created_at))
            day_index = (start_page - 1) // 10
            event_pairs = plot.days[min(day_index, len(plot.days) - 1)]
            scene_event = event_pairs[(scene_index - 1) % len(event_pairs)]
            scene_prompts["scenes"].append({"storyId": story.id, "plot": plot.title, "sceneNo": scene_index, "pages": [start_page, end_page], "fileName": file_name, "existing": scene_index <= len(story.existing_images), "styleReference": plot.style_reference, "description": f"{scene_event[0]} {scene_event[1]}"})
            for local_sequence, page_no in enumerate(range(start_page, end_page + 1), 1):
                page = pages[page_no - 1]
                line_id += 1
                line_by_page[page_no] = line_id
                timestamp = (datetime.fromisoformat(story.created_at) + timedelta(minutes=page_no)).strftime("%Y-%m-%d %H:%M:%S")
                line_rows.append((line_id, scene_id, page["branchPrompt"] is not None, json.dumps({"text": page["text"], "sentences": page["sentences"], "pageNo": page_no}, ensure_ascii=False, separators=(",", ":")), json.dumps(page["branchPrompt"], ensure_ascii=False, separators=(",", ":")) if page["branchPrompt"] else None, local_sequence, timestamp, timestamp, 0))
                if page["choice"]:
                    choice_id += 1
                    choice_rows.append((choice_id, line_id, page["choice"], timestamp))
        character_rows.append((284000 + story_index, story.student_id, story.id, f"/uploads/images/{image_name(story, 1)}", story.created_at, plot.subjects[0].replace("는", "").replace("은", "")))

        student = next(item for item in STUDENTS if item.id == story.student_id)
        gaze_id = 290100 + story_index
        gaze_name = gaze_file_name(gaze_id)
        samples = []
        replay_words = []
        source_pages = []
        sentence_metrics = []
        regressions = []
        captured = 300
        for page in pages:
            tokens = page["text"].split()
            source_pages.append(
                {
                    "pageNo": page["pageNo"],
                    "storyLineId": line_by_page[page["pageNo"]],
                    "text": page["text"],
                    "tokens": tokens,
                }
            )
            first_offset = captured
            # 한 페이지마다 2번 단어를 건너뛴 뒤 1번 단어로 되돌아온다.
            # 1번·3번·되돌아온 1번은 180ms 간격의 연속 샘플 3개로
            # 기록해 백엔드의 체류 판정(250ms 이하 연속 간격)을 만족한다.
            visit_indexes = [0, 1, 3, *range(4, len(tokens)), 1] if len(tokens) >= 4 else list(range(len(tokens)))
            page_sample_start = len(samples)
            for visit_no, token_index in enumerate(visit_indexes):
                token = tokens[token_index]
                dwell_visit = len(tokens) >= 4 and (token_index == 3 or token_index == 1)
                sample_count = 3 if dwell_visit else 1
                visit_started = captured
                for sample_no in range(sample_count):
                    sample_at = visit_started + sample_no * 180
                    samples.append(
                        {
                            "pageNo": page["pageNo"],
                            "storyLineId": line_by_page[page["pageNo"]],
                            "tokenIndex": token_index,
                            "text": token,
                            "capturedAtMs": sample_at,
                            "presence": True,
                            "x": round(0.16 + (token_index % 8) * 0.09, 2),
                            "y": round(0.28 + (page["pageNo"] % 5) * 0.09, 2),
                        }
                    )
                dwell_ms = 440 if dwell_visit else 80
                replay_words.append(
                    {
                        "pageNo": page["pageNo"],
                        "storyLineId": line_by_page[page["pageNo"]],
                        "tokenIndex": token_index,
                        "text": token,
                        "dwellMs": dwell_ms,
                        "visitCount": 1,
                        "skipped": False,
                        "regressionCount": 1 if visit_no == len(visit_indexes) - 1 and token_index == 1 else 0,
                    }
                )
                captured += (520 if dwell_visit else 180) + student.no * 15
            page_sample_count = len(samples) - page_sample_start
            if len(tokens) >= 4:
                regressions.append(
                    {
                        "fromTargetIndex": page["pageNo"] - 1,
                        "toTargetIndex": page["pageNo"] - 1,
                        "fromTokenIndex": len(tokens) - 1,
                        "toTokenIndex": 1,
                        "offsetMs": captured - first_offset,
                    }
                )
            sentence_metrics.append(
                {
                    "storyLineId": line_by_page[page["pageNo"]],
                    "pageNo": page["pageNo"],
                    "sequenceNo": page["pageNo"],
                    "surfaceText": page["text"],
                    "dwellDurationMs": captured - first_offset,
                    "fixationCount": page_sample_count,
                    "regressionCount": 1 if len(tokens) >= 4 else 0,
                    "firstGazeOffsetMs": first_offset,
                    "lastGazeOffsetMs": captured,
                }
            )
        raw_assets[Path("gaze") / str(story.student_id) / gaze_name] = {
            "rawData": {
                "schemaVersion": "story-gaze-raw-v2",
                "studentId": story.student_id,
                "storyId": story.id,
                "pageCount": story.progress,
                "sourceTextCoverage": "FULL",
                "pages": source_pages,
                "replayWords": replay_words,
                "samples": samples,
            }
        }
        story_gaze_rows.append((gaze_id, story.student_id, None, None, story.id, "STORY", story.created_at, (datetime.fromisoformat(story.created_at) + timedelta(milliseconds=captured)).strftime("%Y-%m-%d %H:%M:%S"), f"/gaze/{story.student_id}/{gaze_name}", "COMPLETED", "SUCCESS", story.created_at))
        story_analysis_rows.append((291100 + story_index, gaze_id, captured - 300, len(samples), len(regressions), round((captured - 300) / max(1, len(samples))), json.dumps(sentence_metrics, ensure_ascii=False, separators=(",", ":")), json.dumps(regressions, ensure_ascii=False, separators=(",", ":")), json.dumps({"source": "qa-demo", "persona": student.weakness, "sourceTextCoverage": "FULL", "calculationVersion": "story-gaze-word-v2"}, ensure_ascii=False, separators=(",", ":")), (datetime.fromisoformat(story.created_at) + timedelta(milliseconds=captured + 1000)).strftime("%Y-%m-%d %H:%M:%S")))

    sql.append("\n" + rows_sql("stories", ("id", "student_id", "story_template_id", "created_at", "status", "progress"), story_rows))
    sql.append(rows_sql("story_scenes", ("scene_id", "story_id", "image_url", "sequence_no", "created_at"), scene_rows))
    sql.append(rows_sql("story_lines", ("id", "scene_id", "has_choices", "content", "branch_prompt", "sequence_no", "created_at", "read_at", "revision"), line_rows))
    sql.append(rows_sql("story_choices", ("id", "story_line_id", "content", "created_at"), choice_rows))
    sql.append(rows_sql("characters", ("id", "student_id", "story_id", "image_url", "created_at", "name"), character_rows))
    sql.append(rows_sql("gaze_sessions", ("id", "student_id", "test_id", "training_id", "story_id", "content_type", "started_at", "ended_at", "data_url", "status", "calibration_status", "created_at"), story_gaze_rows))
    sql.append(rows_sql("gaze_analysis_results", ("id", "gaze_session_id", "total_visited_duration", "total_visited_count", "reverse_read_count", "avg_visited_duration", "sentence_metrics", "regressions", "analysis_meta", "created_at"), story_analysis_rows))

    report_rows = []
    for student in STUDENTS:
        for period_no, (start_date, end_date, days, count) in enumerate((("2026-07-08 00:00:00", "2026-08-04 23:59:59", 8, 40), ("2026-07-22 00:00:00", "2026-08-04 23:59:59", 4, 20)), 1):
            snapshot = report_snapshot(student, days)
            assert snapshot["completedTrainingCount"] == count
            report_rows.append((370000 + student.no * 10 + period_no, student.id, start_date, end_date, json.dumps(snapshot, ensure_ascii=False, separators=(",", ":")), f"{student.name} 시연용 {'한 달' if period_no == 1 else '2주'} 보고서", "2026-08-05 09:00:00"))
    sql.append("\n" + rows_sql("reports", ("id", "student_id", "start_date", "end_date", "snapshot_data", "teacher_memo", "created_at"), report_rows))

    feature_rows = []
    feature_codes = (("SENTENCE.FLUENCY", 1), ("PHONOLOGY.LIAISON.CODA_TO_SILENT_ONSET", 2))
    for student in STUDENTS:
        for feature_code, feature_no in feature_codes:
            profile_id = 299000 + student.no * 10 + feature_no
            feature_rows.append((profile_id, student, feature_code, feature_no))
    for profile_id, student, feature_code, feature_no in feature_rows:
        sql.append(f"INSERT INTO student_feature_profiles (id, student_id, reading_features_id, accuracy_rate, avg_pronunciation_scor, pronunciation_error_rate, avg_fixation_duration_ms, avg_fixation_count, avg_regression_count, skip_rate, avg_reading_time_ms, weakness_score, confidence, evidence_count, last_evidence_at, analyzed_at) SELECT {profile_id}, {student.id}, id, {(student.base_score + feature_no * 20) / 1000:.4f}, {student.base_score + feature_no * 20}, {(1000 - student.base_score) / 10:.2f}, {620 + student.no * 60}, {4 + student.no / 10:.2f}, {max(0.5, 2.2 - student.no / 2):.2f}, 0.04, {12000 + student.no * 900}, {max(180, 900 - student.base_score)}, 0.9200, 40, '2026-08-04 17:00:00', '2026-08-04 17:05:00' FROM reading_features WHERE feature_code={sql_quote(feature_code)};\n")

    manifest = {
        "schemaVersion": "qa-demo-assets-v2",
        "maxImageBytes": 1_048_576,
        "images": [scene["fileName"] for scene in scene_prompts["scenes"]],
        "gaze": sorted(str(path.relative_to("gaze")).replace("\\", "/") for path in raw_assets if path.parts[0] == "gaze"),
        "pronunciation": sorted(str(path.relative_to("pronunciation")).replace("\\", "/") for path in raw_assets if path.parts[0] == "pronunciation"),
        "staleImages": [
            "1b6e8aba-1076-43fb-a9f7-40b4ba68cac6.jpg",
            "2f4abe13-8f84-4d87-b711-06cfe13674c5.jpg",
            "5cce6a09-9535-4e1f-8507-52652f2deca9.jpg",
            "77b0b1b1-2794-40d4-903f-54b00f2b03fd.jpg",
            "dcfbbd01-bc15-4691-bddb-dd9314826709.jpg",
            *[
                f"qa-demo-story-{story.id}-scene-{scene_index:02d}.jpg"
                for story in STORIES
                for scene_index in range(
                    len(story.existing_images) + 1,
                    len(scene_ranges(story.progress)) + 1,
                )
            ],
        ],
        "staleGaze": [
            "2001/gaze-290101-a0010000-0000-4000-8000-000000000001.json",
            "2002/gaze-290102-a0020000-0000-4000-8000-000000000002.json",
            "2103/gaze-290103-a0030000-0000-4000-8000-000000000003.json",
            *[
                f"{student.id}/test-gaze-{350000 + student.no * 10 + sequence_no}.json"
                for student in STUDENTS
                for sequence_no in range(1, 4)
            ],
            *[
                f"{story.student_id}/story-gaze-{290100 + story_index}-full.json"
                for story_index, story in enumerate(STORIES, 1)
            ],
        ],
    }
    return "".join(sql), manifest, scene_prompts, raw_assets


def validate(sql: str, manifest: dict[str, Any], scene_prompts: dict[str, Any], raw_assets: dict[Path, Any]) -> None:
    assert len(manifest["images"]) == 39
    assert len(set(manifest["images"])) == 39
    assert all(
        re.fullmatch(
            r"[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}\.jpg",
            file_name,
        )
        for file_name in manifest["images"]
    )
    assert len(manifest["pronunciation"]) == 18
    assert len(manifest["gaze"]) == 57
    assert sql.count("INSERT INTO word_attempt_logs") == 1_548
    assert all(
        re.fullmatch(r"\d+/gaze-\d+-[0-9a-f-]{36}\.json", relative_path)
        for relative_path in manifest["gaze"]
    )
    assert len(scene_prompts["scenes"]) == 39
    assert sum(story.progress for story in STORIES) == 133
    assert [story.progress for story in STORIES] == [9, 4, 100, 20]
    assert sql.count("'COMPLETED',") >= 24
    for student in STUDENTS:
        assert len(student.completed_dates) == 8
        assert student.school.endswith("초등학교")
        rotations = CURRICULUM_ROTATIONS[student.id]
        assert len(rotations) == 9
        assert len({frozenset(rotation) for rotation in rotations}) == 9
        for rotation in rotations:
            assert len(rotation) == 5
            assert len(set(rotation)) == 5
            assert set(rotation) <= set(TRAINING_TYPES)
            assert 22 in rotation or 25 in rotation
            assert not ({30, 33} <= set(rotation))
    training_gaze_counts = {student.id: 0 for student in STUDENTS}
    test_gaze_counts = {student.id: 0 for student in STUDENTS}
    for path, payload in raw_assets.items():
        raw = payload.get("rawData", {})
        if raw.get("schemaVersion") == "test-gaze-raw-v1":
            test_gaze_counts[raw["studentId"]] += 1
            assert raw["samples"]
            continue
        if raw.get("schemaVersion") != "training-gaze-raw-v1":
            continue
        student_id = raw["studentId"]
        training_gaze_counts[student_id] += 1
        assert raw["gazeSessionId"] == 400_000 + raw["trainingId"]
        assert raw["synthetic"] is True
        assert len(raw["questions"]) == 3
        assert raw["samples"]
        assert len(raw["regressions"]) == training_gaze_metrics(
            next(student for student in STUDENTS if student.id == student_id),
            raw["trainingId"],
        )["reverseReadCount"]
        assert [sample["capturedAtMs"] for sample in raw["samples"]] == sorted(
            sample["capturedAtMs"] for sample in raw["samples"]
        )
        question_token_counts = {
            question["questionNo"]: len(question["tokens"])
            for question in raw["questions"]
        }
        assert all(
            0 <= sample["tokenIndex"] < question_token_counts[sample["questionNo"]]
            for sample in raw["samples"]
        )
        relative_path = str(path.relative_to("gaze")).replace("\\", "/")
        assert f"/gaze/{relative_path}" in sql
    assert training_gaze_counts == {2001: 11, 2002: 15, 2103: 18}
    assert test_gaze_counts == {2001: 3, 2002: 3, 2103: 3}
    generated_training_ids = {
        payload["rawData"]["trainingId"]
        for payload in raw_assets.values()
        if payload.get("rawData", {}).get("schemaVersion") == "training-gaze-raw-v1"
    }
    assert all(
        set(report_training_ids) <= generated_training_ids
        for report_training_ids in REPORT_TRAINING_GAZE_IDS.values()
    )
    assert sql.count('"rawSchemaVersion":"training-gaze-raw-v1"') == 44
    assert sql.count('"training":{"status":"AVAILABLE"') == 6
    for story in STORIES:
        pages = story_pages(story)
        assert len(pages) == story.progress
        for page in pages:
            if page["branchPrompt"] is None:
                assert len(page["sentences"]) == 3
            else:
                assert len(page["sentences"]) == 1
                assert page["sentences"][0].endswith("?")
                assert len(page["branchPrompt"]["options"]) == 3
            for sentence in page["sentences"]:
                assert 10 <= hangul_count(sentence) <= 22
                assert "서로 서로" not in sentence
                assert not ("차분히" in sentence and "차분하게" in sentence)
                assert not ("용기 내어" in sentence and "용기 있게" in sentence)
    for path, payload in raw_assets.items():
        if path.parts[0] != "gaze" or payload["rawData"]["schemaVersion"] != "story-gaze-raw-v2":
            continue
        raw = payload["rawData"]
        story = next(item for item in STORIES if item.id == raw["storyId"])
        pages = story_pages(story)
        assert raw["sourceTextCoverage"] == "FULL"
        assert len(raw["pages"]) == len(pages)
        for expected, source in zip(pages, raw["pages"], strict=True):
            expected_tokens = expected["text"].split()
            assert source["pageNo"] == expected["pageNo"]
            assert source["text"] == expected["text"]
            assert source["tokens"] == expected_tokens
            page_samples = [
                sample for sample in raw["samples"]
                if sample["pageNo"] == expected["pageNo"]
            ]
            assert page_samples
            assert all(
                0 <= sample["tokenIndex"] < len(expected_tokens)
                for sample in page_samples
            )
            if len(expected_tokens) >= 4:
                sampled_indexes = [sample["tokenIndex"] for sample in page_samples]
                assert 2 not in sampled_indexes
                assert sampled_indexes.count(1) >= 6
                assert sampled_indexes.count(3) >= 3


def write_outputs(sql: str, manifest: dict[str, Any], scene_prompts: dict[str, Any], raw_assets: dict[Path, Any]) -> None:
    SQL_PATH.write_text(sql, encoding="utf-8", newline="\n")
    ASSET_ROOT.mkdir(parents=True, exist_ok=True)
    (ASSET_ROOT / "manifest.json").write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    (ASSET_ROOT / "scene-prompts.json").write_text(json.dumps(scene_prompts, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    desired_json_paths = {
        (ASSET_ROOT / relative_path).resolve()
        for relative_path in raw_assets
    }
    for generated_directory in (ASSET_ROOT / "gaze", ASSET_ROOT / "pronunciation"):
        if not generated_directory.exists():
            continue
        for existing_path in generated_directory.rglob("*.json"):
            if existing_path.resolve() not in desired_json_paths:
                existing_path.unlink()
    for relative_path, payload in raw_assets.items():
        target = ASSET_ROOT / relative_path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--check", action="store_true", help="validate without writing generated files")
    args = parser.parse_args()
    outputs = build()
    validate(*outputs)
    if not args.check:
        write_outputs(*outputs)
        print(f"generated {SQL_PATH}")
        print("generated 39 scene mappings, 44 training gaze JSON, full story/test gaze JSON, and 18 Azure fixtures")
    else:
        print("QA demo dataset contract validation passed")


if __name__ == "__main__":
    main()
