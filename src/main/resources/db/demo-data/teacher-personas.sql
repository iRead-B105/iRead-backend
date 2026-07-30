CREATE TEMPORARY TABLE demo_personas (
    persona_no INT PRIMARY KEY,
    student_id BIGINT NOT NULL,
    persona_title VARCHAR(80) NOT NULL,
    teacher_memo VARCHAR(1000) NOT NULL,
    strength_area VARCHAR(80) NOT NULL,
    weakness_area VARCHAR(80) NOT NULL,
    base_accuracy INT NOT NULL,
    trend_delta INT NOT NULL,
    reading_speed INT NOT NULL,
    past_gaze_failure BOOLEAN NOT NULL,
    story_status VARCHAR(30) NOT NULL,
    story_progress INT NOT NULL
);

INSERT INTO demo_personas
    (persona_no, student_id, persona_title, teacher_memo, strength_area, weakness_area,
     base_accuracy, trend_delta, reading_speed, past_gaze_failure, story_status, story_progress)
VALUES
    (1, 2001, '湲???먯깋???쒖옉??珥덇린 ?숈뒿??,
     '[愿李? 洹몃┝ ?⑥꽌?먮뒗 ?곴레?곸쑝濡?諛섏쓳?섏?留??먯쓬怨?紐⑥쓬???뚮━ ?곌껐?먯꽌 留앹꽕?꾩씠 ?덉뒿?덈떎. [吏?? ??踰덉뿉 5遺??대궡濡?湲곕낯 紐⑥쓬怨?泥レ냼由щ? 吏앹쭞怨? ?깃났??湲?먮뒗 洹몃┝ ?⑥꽌 ?놁씠 ?ㅼ떆 ?뺤씤?⑸땲?? [?ㅼ쓬 ?뺤씤] 2二??숈븞 ?뺥솗?꾨낫??李몄뿬 吏???쒓컙怨??낅┰ 諛섏쓳 ?섎? ?곗꽑 湲곕줉?⑸땲??',
     '洹몃┝ ?⑥꽌 ?쒖슜', '?먯쓬쨌紐⑥쓬 ?곌껐', 570, 45, 34, FALSE, 'IN_PROGRESS', 35),
    (2, 2101, '袁몄????깆옣?섎뒗 洹좏삎???숈뒿??,
     '[愿李? ?뚯젅 ?쎄린? 臾몄옣 ?댄빐媛 ?④퍡 ?깆옣?섎ŉ 理쒓렐 ?뺥솗?꾩? ?쎄린 ?띾룄媛 紐⑤몢 ?덉젙?곸쑝濡??곸듅?덉뒿?덈떎. [吏?? ?꾩옱 ?쒖씠?꾨? ?좎??섎릺 湲?臾몄옣? ?섎? ?⑥쐞 ?쒖떆 ????踰????먯뿰?ㅻ읇寃??쎄쾶 ?⑸땲?? [?ㅼ쓬 ?뺤씤] 2二???臾몄옣 湲몄씠瑜??섎젮 ?명씉 ?좎? ?щ?瑜??뺤씤?⑸땲??',
     '?뚯젅 ?쎄린', '湲?臾몄옣 ?명씉', 720, 35, 58, FALSE, 'COMPLETED', 100),
    (3, 2102, '湲곗큹 寃?щ? 留덉튇 ?좉퇋 ?꾩엯 ?숈뒿??,
     '[愿李? ?좉퇋 ?꾩엯 ??湲곗큹 寃?щ? 留덉낀怨??깃????몄떇? ?묓샇?섏?留?諛섎났 ?먮즺媛 ?꾩쭅 ?곸뒿?덈떎. [吏?? 泥?3?뚭린???먮え쨌?뚯젅쨌吏㏃? ?깅쭚??媛숈? 鍮꾩쑉濡??쒖떆??湲곗??좎쓣 ?섏쭛?⑸땲?? [?ㅼ쓬 ?뺤씤] ?깃툒???④퀎 議곗젙 ?놁씠 ?ㅻ쪟媛 諛섎났?섎뒗 ?좏삎遺???ㅼ쓬 而ㅻ━?섎읆??諛섏쁺?⑸땲??',
     '?깃????몄떇', '?숈뒿 洹쇨굅 異뺤쟻', 680, 20, 42, FALSE, 'IN_PROGRESS', 20),
    (4, 2103, '???곸뿭??泥댄뿕??醫낇빀 ?쒖뿰 ?숈뒿??,
     '[愿李? 湲곗큹 ?뚯슫遺??臾몄옣 ?좎갹?깃퉴吏 ???곸뿭??寃쏀뿕?덇퀬 ?댁슜 ?댄빐媛 媛???덉젙?곸엯?덈떎. 諛쏆묠 ?깅쭚怨??섎? ?⑥쐞 ?딆뼱 ?쎄린?먯꽌 ?띾룄媛 ?붾뱾由쎈땲?? [吏?? 諛쏆묠 ?깅쭚???ы븿??2~3臾몄옣 ?쎄린瑜?諛섎났?섍퀬 ?쒖꽑 ?섎룎?꾧?湲곕? ?④퍡 愿李고빀?덈떎. [?ㅼ쓬 ?뺤씤] 2二???媛숈? 湲몄씠??臾몄옣?쇰줈 ?뺥솗?꾩? ?명씉??鍮꾧탳?⑸땲??',
     '?댁슜 ?댄빐', '諛쏆묠쨌?딆뼱 ?쎄린', 810, 28, 72, TRUE, 'IN_PROGRESS', 60),
    (5, 2104, '?쒖꽑 蹂댁젙 ?ㅽ뙣瑜?洹밸났???뚮났???숈뒿??,
     '[愿李? 珥덇린 ?쒖꽑 蹂댁젙 ?ㅽ뙣 ???ъ륫?뺤뿉?쒕뒗 ?덉젙?곸쑝濡??섏쭛?섏뿀怨??섏씫湲곗? ?댄깉 ?잛닔??媛먯냼?덉뒿?덈떎. [吏?? 留??뚭린 ?쒖옉 ???먯꽭? ?붾㈃ 嫄곕━瑜??뺤씤????吏㏃? 臾몄옣?먯꽌 ?깃났 寃쏀뿕???쒓났?⑸땲?? [?ㅼ쓬 ?뺤씤] 蹂댁젙 ?ㅽ뙣 ?щ?? 臾몄옣 ?앷퉴吏 ?쎌? 鍮꾩쑉???④퍡 湲곕줉???뚮났 異붿씠瑜??좎??⑸땲??',
     '?ъ떆??吏?띿꽦', '?쒖꽑 怨좎젙 ?덉젙??, 640, 52, 49, TRUE, 'COMPLETED', 100),
    (6, 2105, '?뺥솗?섏?留?泥쒖쿇???쎈뒗 ?좎쨷???숈뒿??,
     '[愿李? ?뺣떟 ?뺥솗?꾨뒗 ?믪?留??깅쭚留덈떎 ?뺤씤 ?쒓컙??湲몄뼱 臾몄옣 ?먮쫫???먯＜ ?딄퉩?덈떎. [吏?? ?쒓컙 ?쒗븳? ?먯? ?딄퀬 ?숈씪??吏㏃? 臾몄옣????李⑤? ?쎌뼱 ??踰덉㎏遺??臾띠뼱 ?쎄린瑜??좊룄?⑸땲?? [?ㅼ쓬 ?뺤씤] ?뺥솗?꾨? ?좎??섎㈃??遺꾨떦 ?쎌? ?깅쭚 ?섍? ?꾨쭔?섍쾶 利앷??섎뒗吏 ?뺤씤?⑸땲??',
     '?믪? ?뺥솗??, '?쎄린 ?먮룞???띾룄', 870, 18, 38, FALSE, 'IN_PROGRESS', 50),
    (7, 2106, '鍮좊Ⅴ寃??쎌?留??꾨씫????? ?띾룄 ?곗꽑???숈뒿??,
     '[愿李? ?쎄린 ?띾룄??鍮좊Ⅴ吏留?議곗궗? 諛쏆묠??嫄대꼫?곗뼱 ?뺥솗???먯떎???쎈땲?? [吏?? 臾몄옣 寃쎄퀎? 諛쏆묠 ?깅쭚???쒓컖?곸쑝濡??쒖떆?섍퀬 泥??쎄린???섎룄?곸쑝濡??띾룄瑜???땅?덈떎. [?ㅼ쓬 ?뺤씤] ?띾룄蹂대떎 ?꾨씫 ?깅쭚 ?섍? 以꾩뼱?쒕뒗吏瑜??곗꽑 吏?쒕줈 蹂닿퀬 ?쒖떆 ?⑥꽌瑜??먯감 ?쒓굅?⑸땲??',
     '鍮좊Ⅸ ?쎄린 ?띾룄', '?깅쭚쨌諛쏆묠 ?꾨씫', 610, 30, 84, TRUE, 'IN_PROGRESS', 70),
    (8, 2107, '?댁빞湲?紐곗엯?꾧? ?믪? ?쒖궗???숈뒿??,
     '[愿李? ?댁빞湲??좏깮怨??댁슜 ?덉륫???곴레?곸씠硫?留λ씫???댁슜???댄빐媛 媛뺥빀?덈떎. 泥섏쓬 蹂대뒗 ?깅쭚? 臾몃㎘?쇰줈 異붿륫?섍퀬 湲???⑥꽌瑜??볦튂??寃쎌슦媛 ?덉뒿?덈떎. [吏?? ?댁빞湲??꾪썑?????깅쭚 3媛쒕? 遺꾨━???대룆????蹂몃Ц?먯꽌 ?ㅼ떆 李얘쾶 ?⑸땲?? [?ㅼ쓬 ?뺤씤] 臾몃㎘ ?놁씠??媛숈? ?깅쭚???뺥솗???쎈뒗吏 ?뺤씤?⑸땲??',
     '?댁빞湲??댄빐쨌?덉륫', '???깅쭚 ?대룆', 760, 32, 66, FALSE, 'COMPLETED', 100),
    (9, 2108, '鍮꾩쓬??諛쒖쓬??吏묒쨷?섎뒗 諛쒖쓬 援먯젙???숈뒿??,
     '[愿李? 援?Ъ쨌?쎈뒗泥섎읆 鍮꾩쓬?붽? ?ы븿???깅쭚?먯꽌 媛숈? 諛쒖쓬 ?ㅻ쪟媛 諛섎났?섎ŉ ?뚯젅 遺꾨━ ?⑥꽌?먮뒗 ??諛섏쓳?⑸땲?? [吏?? ?쒓린? ?ㅼ젣 諛쒖쓬???섎????쒖떆?섍퀬 ?먮┛ 紐⑤쾾 諛쒖쓬 ????踰덈쭔 ?ъ떆?꾪빀?덈떎. [?ㅼ쓬 ?뺤씤] ?곗뒿 ?깅쭚怨????깅쭚?먯꽌 ?ㅻ쪟媛 ?④퍡 媛먯냼?섎뒗吏 援щ텇??湲곕줉?⑸땲??',
     '?뚯젅 遺꾨━', '鍮꾩쓬??諛쒖쓬', 590, 42, 46, FALSE, 'IN_PROGRESS', 45),
    (10, 2109, '寃??湲댁옣???꾪솕??媛???먯떊媛??뚮났???숈뒿??,
     '[愿李? ?덈젴?먯꽌???덉젙?곸씠吏留?寃???붾㈃?먯꽌 ?묐떟 ?쒖옉????퀬 ?뺥솗?꾨룄 ?④퍡 ??븘吏묐땲?? [吏?? 寃?????곗뒿 臾명빆?쇰줈 ?깃났 寃쏀뿕???쒓났?섍퀬 以묐┰?곸씤 ?덈궡留??ъ슜?⑸땲?? [?ㅼ쓬 ?뺤씤] 寃???먯닔肉??꾨땲??泥??묐떟 ?쒓컙怨?以묐떒 ?잛닔媛 以꾩뼱?쒕뒗吏瑜??④퍡 鍮꾧탳?⑸땲??',
     '?덈젴 李몄뿬??, '寃???곹솴 湲댁옣', 670, 48, 52, TRUE, 'COMPLETED', 100),
    (11, 2110, '臾몄옣 ?섎? ?곌껐??媛뺥븳 ?댄빐 以묒떖???숈뒿??,
     '[愿李? 以묒떖 ?댁슜 李얘린? 洹몃┝-臾몄옣 ?곌껐? ?덉젙?곸씠吏留??뚮━ ?댁뼱 ?쎌쓣 ??臾몄옣 遺?몃? 異⑸텇??諛섏쁺?섏? ?딆뒿?덈떎. [吏?? ?쇳몴? 留덉묠?쒖뿉???명씉 ?쒖떆瑜??????섎?媛 ?щ씪吏???덈? 鍮꾧탳?⑸땲?? [?ㅼ쓬 ?뺤씤] ?쒖떆 ?놁씠???듭뼇怨?硫덉땄???좎??섎뒗吏 ?뱀쓬 湲곕줉?쇰줈 ?뺤씤?⑸땲??',
     '以묒떖 ?댁슜 ?댄빐', '臾몄옣 ?듭뼇쨌?명씉', 800, 24, 64, FALSE, 'IN_PROGRESS', 80),
    (12, 2111, '寃밸컺移④낵 ?섏씫湲곌? ??? 吏묒쨷 吏?먰삎 ?숈뒿??,
     '[愿李? 寃밸컺移??깅쭚?먯꽌 ?쒖꽑 癒몃Т由꾧낵 ?섏씫湲곌? 留롮?留??ъ떆???덈궡?????섏슜?⑸땲?? [吏?? ?깅쭚???뚯젅 ?⑥쐞濡??섎늻怨?理쒕? 3???덉뿉??留덉?留??쒕룄留??됯???諛섏쁺?⑸땲?? [?ㅼ쓬 ?뺤씤] 媛숈? ?깅쭚??癒몃Т由??쒓컙怨??ъ떆???잛닔媛 ?④퍡 以꾩뼱?쒕뒗吏 ?뺤씤?⑸땲??',
     '?ъ떆???섏슜??, '寃밸컺移㉱룸릺?쎄린', 520, 55, 41, TRUE, 'IN_PROGRESS', 30),
    (13, 2002, '紐⑥쓬 援щ퀎??鍮좊Ⅸ ?쒖옉 ?④퀎 ?숈뒿??,
     '[愿李? 湲곕낯 紐⑥쓬 ?곕씪 ?곌린? ?뚮━ 援щ퀎? 鍮좊Ⅴ寃??듯엳吏留??뚯젅???⑹튌 ??移대뱶 ?쒖꽌瑜?諛붽씀??寃쎌슦媛 ?덉뒿?덈떎. [吏?? 紐⑥쓬 媛뺤젏???쒖슜????湲???뚯젅 ?⑹꽦遺???쒖옉?섍퀬 ?먭??쎌쑝濡??쎈뒗 ?쒖꽌瑜?吏싰쾶 ?⑸땲?? [?ㅼ쓬 ?뺤씤] ?꾩? ?놁씠 移대뱶 ?쒖꽌瑜??좎???臾명빆 ?섏? 泥??쒕룄 ?깃났瑜좎쓣 湲곕줉?⑸땲??',
     '湲곕낯 紐⑥쓬 援щ퀎', '?뚯젅 ?⑹꽦 ?쒖꽌', 740, 38, 45, FALSE, 'IN_PROGRESS', 55);

CREATE TEMPORARY TABLE demo_numbers (seq INT PRIMARY KEY);
INSERT INTO demo_numbers (seq)
VALUES (1), (2), (3), (4), (5), (6), (7), (8), (9), (10), (11), (12), (13);
CREATE TEMPORARY TABLE demo_story_numbers AS
SELECT seq FROM demo_numbers;
CREATE TEMPORARY TABLE demo_scene_numbers AS
SELECT seq FROM demo_numbers;
CREATE TEMPORARY TABLE demo_line_numbers AS
SELECT seq FROM demo_numbers;
CREATE TEMPORARY TABLE demo_trend_numbers AS
SELECT seq FROM demo_numbers;
CREATE TEMPORARY TABLE demo_token_numbers AS
SELECT seq FROM demo_numbers;

CREATE TEMPORARY TABLE demo_features AS
SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS feature_no
FROM reading_features;
SET @demo_feature_count = (SELECT COUNT(*) FROM demo_features);

CREATE TEMPORARY TABLE demo_reading_templates AS
SELECT
    id,
    ROW_NUMBER() OVER (ORDER BY id) AS template_no
FROM training_templates
WHERE JSON_UNQUOTE(JSON_EXTRACT(prompt, '$.questionType')) IN (
    'WORD_GRID_READING',
    'SENTENCE_READING',
    'PASSAGE_READING'
);
SET @demo_reading_template_count = (SELECT COUNT(*) FROM demo_reading_templates);

UPDATE students student
JOIN demo_personas persona ON persona.student_id = student.id
SET student.teacher_memo = persona.teacher_memo,
    student.guardian = COALESCE(student.guardian, CONCAT(LEFT(student.name, 1), '蹂댄샇??)),
    student.guardian_contact = COALESCE(
        student.guardian_contact,
        CONCAT('010-0000-', RIGHT(CONCAT('0000', student.id), 4))
    ),
    student.guardian_email = COALESCE(
        student.guardian_email,
        CONCAT('guardian', student.id, '@example.invalid')
    ),
    student.address = COALESCE(student.address, '?쒖슱???곕え援??쎄린留덉쓣'),
    student.image_url = COALESCE(student.image_url, '/images/student-profile.png');

INSERT INTO daily_curriculums (id, student_id, status, created_at, completed_at)
SELECT
    120000 + persona.persona_no * 10 + number.seq,
    persona.student_id,
    CASE WHEN number.seq < 3 THEN 'COMPLETED' ELSE 'NOT_STARTED' END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-05-18 09:00:00')
        WHEN 2 THEN TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-27 08:00:00')
        ELSE TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-29 08:00:00')
    END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-05-25 17:00:00')
        WHEN 2 THEN TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 15:00:00')
        ELSE NULL
    END
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
WHERE NOT EXISTS (
    SELECT 1 FROM daily_curriculums existing
    WHERE existing.id = 120000 + persona.persona_no * 10 + number.seq
);

UPDATE daily_curriculums curriculum
JOIN demo_personas persona ON persona.student_id = curriculum.student_id
SET curriculum.status = 'IN_PROGRESS',
    curriculum.completed_at = NULL
WHERE curriculum.status = 'NOT_STARTED'
  AND curriculum.id <> 120000 + persona.persona_no * 10 + 3;

UPDATE daily_curriculums curriculum
JOIN demo_personas persona
  ON curriculum.id = 120000 + persona.persona_no * 10 + 2
SET curriculum.created_at = TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-27 08:00:00'),
    curriculum.completed_at = TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 15:00:00');

UPDATE daily_curriculums curriculum
JOIN demo_personas persona
  ON curriculum.id = 120000 + persona.persona_no * 10 + 3
SET curriculum.status = 'NOT_STARTED',
    curriculum.created_at = TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-29 08:00:00'),
    curriculum.completed_at = NULL;

INSERT INTO trainings
    (id, training_template_id, daily_curriculum_id, sequence_no, created_at,
     started_at, finished_at, status, result, accuracy)
SELECT
    130000 + persona.persona_no * 100 + number.seq,
    template.id,
    120000 + persona.persona_no * 10 + CEIL(number.seq / 4),
    MOD(number.seq - 1, 4) + 1,
    CASE
        WHEN number.seq <= 4 THEN TIMESTAMPADD(DAY, persona.persona_no + number.seq, '2026-05-18 09:00:00')
        WHEN number.seq <= 8 THEN TIMESTAMPADD(
            HOUR, persona.persona_no + (number.seq - 5) * 2, '2026-07-27 09:00:00')
        ELSE TIMESTAMPADD(HOUR, number.seq, TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-29 08:00:00'))
    END,
    CASE WHEN number.seq <= 8 THEN
        CASE
            WHEN number.seq <= 4 THEN TIMESTAMPADD(DAY, persona.persona_no + number.seq, '2026-05-18 09:05:00')
            ELSE TIMESTAMPADD(
                HOUR, persona.persona_no + (number.seq - 5) * 2, '2026-07-27 09:05:00')
        END
    END,
    CASE WHEN number.seq <= 8 THEN
        CASE
            WHEN number.seq <= 4 THEN TIMESTAMPADD(DAY, persona.persona_no + number.seq, '2026-05-18 09:12:00')
            ELSE TIMESTAMPADD(
                HOUR, persona.persona_no + (number.seq - 5) * 2, '2026-07-27 09:12:00')
        END
    END,
    CASE WHEN number.seq <= 8 THEN 'COMPLETED'
         WHEN number.seq = 9 THEN 'NOT_STARTED'
         ELSE 'NOT_READY' END,
    CASE WHEN number.seq <= 8 THEN JSON_OBJECT(
        'learningAssessment', CONCAT(persona.persona_title, '??', template.name, ' ?섑뻾 寃곌낵?낅땲??'),
        'retryCount', CASE WHEN persona.base_accuracy + number.seq * persona.trend_delta / 4 < 700 THEN 2 ELSE 0 END,
        'questions', JSON_ARRAY(
            JSON_OBJECT('questionNumber', 1, 'question', CONCAT(persona.weakness_area, ' 愿??泥?踰덉㎏ 臾명빆'),
                        'isCorrect', TRUE, 'selectedAnswer', '諛붾Ⅴ寃??쎌뿀?듬땲??', 'correctAnswer', '諛붾Ⅴ寃??쎌뿀?듬땲??'),
            JSON_OBJECT('questionNumber', 2, 'question', CONCAT(template.name, ' ??踰덉㎏ 臾명빆'),
                        'isCorrect', number.seq % 3 <> 0,
                        'selectedAnswer', CASE WHEN number.seq % 3 <> 0 THEN '?뺥솗???묐떟' ELSE '鍮꾩듂???뚮━濡??쎌쓬' END,
                        'correctAnswer', '?뺥솗???묐떟'),
            JSON_OBJECT('questionNumber', 3, 'question', CONCAT(persona.strength_area, ' ?뺤씤 臾명빆'),
                        'isCorrect', TRUE, 'selectedAnswer', '?앷퉴吏 ?쎌뿀?듬땲??', 'correctAnswer', '?앷퉴吏 ?쎌뿀?듬땲??')
        )
    ) END,
    CASE WHEN number.seq <= 8 THEN
        LEAST(970, GREATEST(450,
            persona.base_accuracy
            + FLOOR((number.seq - 1) / 4) * persona.trend_delta
            + (MOD(number.seq * 17 + persona.persona_no * 11, 70) - 35)
        ))
    END
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 12
JOIN training_templates template
  ON template.id = 1 + MOD(persona.persona_no * 3 + number.seq - 2, 34)
WHERE NOT EXISTS (
    SELECT 1 FROM trainings existing
    WHERE existing.id = 130000 + persona.persona_no * 100 + number.seq
);

UPDATE trainings training
JOIN demo_personas persona
  ON training.id BETWEEN 130000 + persona.persona_no * 100 + 5
                     AND 130000 + persona.persona_no * 100 + 8
SET training.created_at = TIMESTAMPADD(
        HOUR, persona.persona_no + (training.id - (130000 + persona.persona_no * 100 + 5)) * 2,
        '2026-07-27 09:00:00'),
    training.started_at = TIMESTAMPADD(
        HOUR, persona.persona_no + (training.id - (130000 + persona.persona_no * 100 + 5)) * 2,
        '2026-07-27 09:05:00'),
    training.finished_at = TIMESTAMPADD(
        HOUR, persona.persona_no + (training.id - (130000 + persona.persona_no * 100 + 5)) * 2,
        '2026-07-27 09:12:00');

INSERT INTO training_datas (id, train_id, generated_data, created_at)
SELECT
    135000 + persona.persona_no * 100 + number.seq,
    130000 + persona.persona_no * 100 + number.seq,
    JSON_OBJECT(
        'schemaVersion', 2,
        'trainingType', template.name,
        'personaFocus', persona.weakness_area,
        'questions', JSON_ARRAY(
            JSON_OBJECT('questionId', CONCAT('persona-', persona.persona_no, '-', number.seq, '-1'),
                        'questionNo', 1,
                        'type', 'SENTENCE_READING',
                        'requiredInputs', JSON_ARRAY('VOICE'),
                        'content', JSON_OBJECT('tokens', JSON_ARRAY('援?Ъ')),
                        'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '援?Ъ')),
                        'answer', JSON_OBJECT('expectedText', '援?Ъ')),
            JSON_OBJECT('questionId', CONCAT('persona-', persona.persona_no, '-', number.seq, '-2'),
                        'questionNo', 2,
                        'type', 'SENTENCE_READING',
                        'requiredInputs', JSON_ARRAY('VOICE'),
                        'content', JSON_OBJECT('tokens', JSON_ARRAY('移쒓뎄?', '?꾩꽌愿??, '媛묐땲??')),
                        'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '移쒓뎄? ?꾩꽌愿??媛묐땲??')),
                        'answer', JSON_OBJECT('expectedText', '移쒓뎄? ?꾩꽌愿??媛묐땲??')),
            JSON_OBJECT('questionId', CONCAT('persona-', persona.persona_no, '-', number.seq, '-3'),
                        'questionNo', 3,
                        'type', 'SENTENCE_READING',
                        'requiredInputs', JSON_ARRAY('VOICE'),
                        'content', JSON_OBJECT('tokens', JSON_ARRAY('移쒓뎄?', '?④퍡', '梨낆쓣', '?쎈뒗', '?댁슜?낅땲??')),
                        'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '移쒓뎄? ?④퍡 梨낆쓣 ?쎈뒗 ?댁슜?낅땲??')),
                        'answer', JSON_OBJECT('expectedText', '移쒓뎄? ?④퍡 梨낆쓣 ?쎈뒗 ?댁슜?낅땲??'))
        )
    ),
    TIMESTAMPADD(MINUTE, 1, training.created_at)
FROM demo_personas persona
JOIN demo_numbers number
JOIN trainings training ON training.id = 130000 + persona.persona_no * 100 + number.seq
JOIN training_templates template ON template.id = training.training_template_id
WHERE NOT EXISTS (
    SELECT 1 FROM training_datas existing
    WHERE existing.id = 135000 + persona.persona_no * 100 + number.seq
);

-- 紐⑤뱺 ?덈젴 ?좏삎??????섎Ⅴ?뚮굹蹂?湲곗큹 ?섑뻾 湲곕줉???쒓났?쒕떎.
-- 理쒓렐 而ㅻ━?섎읆???숈씪 ?덈젴怨?鍮꾧탳?????덈룄濡?湲곗? 湲곕줉? ?꾩옱 ?뚯감蹂대떎 ?욎꽑??
INSERT INTO daily_curriculums (id, student_id, status, created_at, completed_at)
SELECT
    220000 + persona.persona_no,
    persona.student_id,
    'COMPLETED',
    TIMESTAMPADD(MINUTE, persona.persona_no, '2026-06-24 09:00:00'),
    TIMESTAMPADD(MINUTE, persona.persona_no, '2026-06-24 12:30:00')
FROM demo_personas persona
WHERE NOT EXISTS (
    SELECT 1 FROM daily_curriculums existing
    WHERE existing.id = 220000 + persona.persona_no
);

INSERT INTO trainings
    (id, training_template_id, daily_curriculum_id, sequence_no, created_at,
     started_at, finished_at, status, result, accuracy)
SELECT
    230000 + persona.persona_no * 100 + template.id,
    template.id,
    220000 + persona.persona_no,
    template.id,
    TIMESTAMPADD(MINUTE, template.id * 5 + persona.persona_no, '2026-06-24 09:00:00'),
    TIMESTAMPADD(MINUTE, template.id * 5 + persona.persona_no + 1, '2026-06-24 09:00:00'),
    TIMESTAMPADD(MINUTE, template.id * 5 + persona.persona_no + 4, '2026-06-24 09:00:00'),
    'COMPLETED',
    JSON_OBJECT(
        'learningAssessment', CONCAT(
            persona.persona_title, '??湲곗큹 ?섑뻾 湲곕줉?낅땲?? ',
            persona.weakness_area, ' 愿??諛섏쓳? ?ㅼ쓬 ?뚯감?먯꽌 ?ㅼ떆 ?뺤씤?⑸땲??'
        ),
        'retryCount', CASE
            WHEN persona.base_accuracy < 600 THEN 2
            WHEN MOD(template.id + persona.persona_no, 5) = 0 THEN 1
            ELSE 0
        END,
        'questions', JSON_ARRAY(
            JSON_OBJECT(
                'questionNumber', 1,
                'question', CONCAT(template.name, ' 湲곗큹 ?뺤씤 臾명빆'),
                'isCorrect', TRUE,
                'selectedAnswer', '泥?臾명빆???뺥솗???섑뻾?덉뒿?덈떎.',
                'correctAnswer', '泥?臾명빆???뺥솗???섑뻾?덉뒿?덈떎.'
            ),
            JSON_OBJECT(
                'questionNumber', 2,
                'question', CONCAT(persona.weakness_area, ' ?뺤씤 臾명빆'),
                'isCorrect', MOD(template.id + persona.persona_no, 4) <> 0,
                'selectedAnswer', CASE
                    WHEN MOD(template.id + persona.persona_no, 4) <> 0
                    THEN '?뺥솗???묐떟'
                    ELSE '?⑥꽌瑜??뺤씤?????섏젙??
                END,
                'correctAnswer', '?뺥솗???묐떟'
            ),
            JSON_OBJECT(
                'questionNumber', 3,
                'question', CONCAT(persona.strength_area, ' ?쒖슜 臾명빆'),
                'isCorrect', TRUE,
                'selectedAnswer', '?앷퉴吏 ?섑뻾?덉뒿?덈떎.',
                'correctAnswer', '?앷퉴吏 ?섑뻾?덉뒿?덈떎.'
            )
        )
    ),
    LEAST(980, GREATEST(380, ROUND(
        (
            persona.base_accuracy
            + MOD(template.id * 19 + persona.persona_no * 13, 90)
            - 45
        ),
        2
    )))
FROM demo_personas persona
JOIN training_templates template
WHERE NOT EXISTS (
    SELECT 1 FROM trainings existing
    WHERE existing.id = 230000 + persona.persona_no * 100 + template.id
);

INSERT INTO training_datas (id, train_id, generated_data, created_at)
SELECT
    240000 + persona.persona_no * 100 + template.id,
    230000 + persona.persona_no * 100 + template.id,
    JSON_OBJECT(
        'version', 2,
        'trainingType', template.name,
        'personaFocus', persona.weakness_area,
        'questions', JSON_ARRAY(
            JSON_OBJECT(
                'questionId', CONCAT('baseline-', persona.persona_no, '-', template.id, '-1'),
                'questionNo', 1,
                'problem', JSON_OBJECT('targetText', CONCAT(template.name, '??湲곗큹 臾명빆?낅땲??')),
                'answer', JSON_OBJECT('correctText', '?쒖떆???⑥꽌??留욊쾶 ?묐떟?⑸땲??')
            ),
            JSON_OBJECT(
                'questionId', CONCAT('baseline-', persona.persona_no, '-', template.id, '-2'),
                'questionNo', 2,
                'problem', JSON_OBJECT(
                    'targetText', CONCAT(persona.weakness_area, '???좎쓽?섏뿬 ?쎌뼱 蹂댁꽭??')
                ),
                'answer', JSON_OBJECT('correctText', '泥쒖쿇???뺤씤?섎ŉ ?뺥솗???쎌뒿?덈떎.')
            )
        )
    ),
    TIMESTAMPADD(MINUTE, template.id * 5 + persona.persona_no, '2026-06-24 09:00:00')
FROM demo_personas persona
JOIN training_templates template
WHERE NOT EXISTS (
    SELECT 1 FROM training_datas existing
    WHERE existing.id = 240000 + persona.persona_no * 100 + template.id
);

-- 理쒓렐 30?쇱뿉 ?쒕줈 ?ㅻⅨ ??踰덉쓽 ?쎄린 湲곕줉??留뚮뱾???뺥솗?꽷룹씫湲??띾룄 異붿씠瑜??쒓났?쒕떎.
INSERT INTO daily_curriculums (id, student_id, status, created_at, completed_at)
SELECT
    250000 + persona.persona_no * 10 + number.seq,
    persona.student_id,
    'COMPLETED',
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-04 09:00:00')
        WHEN 2 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-14 09:00:00')
        ELSE TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-24 09:00:00')
    END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-04 09:20:00')
        WHEN 2 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-14 09:20:00')
        ELSE TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-24 09:20:00')
    END
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
WHERE NOT EXISTS (
    SELECT 1 FROM daily_curriculums existing
    WHERE existing.id = 250000 + persona.persona_no * 10 + number.seq
);

INSERT INTO trainings
    (id, training_template_id, daily_curriculum_id, sequence_no, created_at,
     started_at, finished_at, status, result, accuracy)
SELECT
    251000 + persona.persona_no * 10 + number.seq,
    reading_template.id,
    250000 + persona.persona_no * 10 + number.seq,
    1,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-04 09:00:00')
        WHEN 2 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-14 09:00:00')
        ELSE TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-24 09:00:00')
    END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-04 09:03:00')
        WHEN 2 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-14 09:03:00')
        ELSE TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-24 09:03:00')
    END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-04 09:12:00')
        WHEN 2 THEN TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-14 09:12:00')
        ELSE TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-24 09:12:00')
    END,
    'COMPLETED',
    JSON_OBJECT(
        'learningAssessment', CONCAT(
            persona.strength_area, '???쒖슜???쎄린?먯꽌 ',
            CASE
                WHEN number.seq = 1 THEN CONCAT(persona.weakness_area, ' 吏?먯씠 ?꾩슂?덉뒿?덈떎.')
                WHEN number.seq = 2 THEN '泥?湲곕줉蹂대떎 留앹꽕?꾩씠 以꾩뿀?듬땲??'
                ELSE '?뺥솗?꾩? ?쎄린 ?먮쫫???④퍡 ?덉젙?섏뿀?듬땲??'
            END
        ),
        'retryCount', GREATEST(0, 3 - number.seq - FLOOR(persona.base_accuracy / 800)),
        'questions', JSON_ARRAY(
            JSON_OBJECT(
                'questionNumber', 1,
                'question', '?쒖떆???깅쭚???쒖꽌?濡??쎌뼱 蹂댁꽭??',
                'isCorrect', TRUE,
                'selectedAnswer', '?꾩꽌愿 移쒓뎄 ?숆탳',
                'correctAnswer', '?꾩꽌愿 移쒓뎄 ?숆탳'
            ),
            JSON_OBJECT(
                'questionNumber', 2,
                'question', CONCAT(persona.weakness_area, '???뺤씤?섎뒗 臾몄옣???쎌뼱 蹂댁꽭??'),
                'isCorrect', number.seq > 1 OR persona.base_accuracy >= 750,
                'selectedAnswer', CASE
                    WHEN number.seq > 1 OR persona.base_accuracy >= 750
                    THEN '移쒓뎄? ?④퍡 ?꾩꽌愿??媛묐땲??'
                    ELSE '移쒓뎄? ?꾩꽌愿??媛먮땲??'
                END,
                'correctAnswer', '移쒓뎄? ?④퍡 ?꾩꽌愿??媛묐땲??'
            ),
            JSON_OBJECT(
                'questionNumber', 3,
                'question', '臾몄옣???살쓣 吏㏐쾶 留먰빐 蹂댁꽭??',
                'isCorrect', TRUE,
                'selectedAnswer', '移쒓뎄? ?꾩꽌愿??媛???댁슜?낅땲??',
                'correctAnswer', '移쒓뎄? ?꾩꽌愿??媛???댁슜?낅땲??'
            )
        )
    ),
    LEAST(980, GREATEST(400, ROUND(
        persona.base_accuracy + (number.seq - 1) * persona.trend_delta,
        2
    )))
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
JOIN demo_reading_templates reading_template
  ON reading_template.template_no =
     1 + MOD(persona.persona_no + number.seq - 2, @demo_reading_template_count)
WHERE NOT EXISTS (
    SELECT 1 FROM trainings existing
    WHERE existing.id = 251000 + persona.persona_no * 10 + number.seq
);

INSERT INTO training_datas (id, train_id, generated_data, created_at)
SELECT
    252000 + persona.persona_no * 10 + number.seq,
    251000 + persona.persona_no * 10 + number.seq,
    JSON_OBJECT(
        'version', 2,
        'trainingType', template.name,
        'personaFocus', persona.weakness_area,
        'questions', JSON_ARRAY(
            JSON_OBJECT(
                'questionId', CONCAT('trend-', persona.persona_no, '-', number.seq, '-1'),
                'questionNo', 1,
                'problem', JSON_OBJECT('targetText', '?꾩꽌愿 移쒓뎄 ?숆탳'),
                'answer', JSON_OBJECT('correctText', '?꾩꽌愿 移쒓뎄 ?숆탳')
            ),
            JSON_OBJECT(
                'questionId', CONCAT('trend-', persona.persona_no, '-', number.seq, '-2'),
                'questionNo', 2,
                'problem', JSON_OBJECT('targetText', '移쒓뎄? ?④퍡 ?꾩꽌愿??媛묐땲??'),
                'answer', JSON_OBJECT('correctText', '移쒓뎄? ?④퍡 ?꾩꽌愿??媛묐땲??')
            )
        )
    ),
    training.created_at
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
JOIN trainings training ON training.id = 251000 + persona.persona_no * 10 + number.seq
JOIN training_templates template ON template.id = training.training_template_id
WHERE NOT EXISTS (
    SELECT 1 FROM training_datas existing
    WHERE existing.id = 252000 + persona.persona_no * 10 + number.seq
);

INSERT INTO word_attempt_logs
    (id, student_id, word_id, story_line_id, training_id, test_id, use_location,
     surface_text, has_audio_data, fixation_duration_ms, fixation_count,
     gaze_start_offset_ms, gaze_end_offset_ms, is_skipped, regression_count,
     pronunciation_accuracy_score, speech_start_offset_ms, speech_end_offset_ms,
     is_correct, created_at, total_score, question_no, target_index, token_index, is_final)
SELECT
    300000 + persona.persona_no * 100 + trend_no.seq * 10 + token_no.seq,
    persona.student_id,
    word.id,
    NULL,
    251000 + persona.persona_no * 10 + trend_no.seq,
    NULL,
    'TRAINING',
    word.content,
    TRUE,
    GREATEST(
        260,
        1150 - persona.reading_speed * 6 - (trend_no.seq - 1) * persona.trend_delta * 2
            + token_no.seq * 18
    ),
    CASE
        WHEN persona.weakness_area LIKE '%?섏씫湲?' AND token_no.seq <= 2 THEN 3
        WHEN persona.weakness_area LIKE '%?꾨씫%' AND token_no.seq = 3 THEN 1
        ELSE 1 + MOD(token_no.seq + persona.persona_no, 2)
    END,
    FLOOR(
        (token_no.seq - 1) * 60000 /
        GREATEST(25, persona.reading_speed + (trend_no.seq - 1) * 5)
    ),
    FLOOR(
        (token_no.seq - 0.25) * 60000 /
        GREATEST(25, persona.reading_speed + (trend_no.seq - 1) * 5)
    ),
    FALSE,
    CASE
        WHEN token_no.seq <= 2 AND persona.base_accuracy < 650 THEN 2
        WHEN persona.past_gaze_failure AND trend_no.seq = 1 AND token_no.seq = 3 THEN 1
        ELSE 0
    END,
    LEAST(980, GREATEST(
        420,
        persona.base_accuracy + (trend_no.seq - 1) * persona.trend_delta
            + token_no.seq * 8 - 35
    )),
    FLOOR(
        (token_no.seq - 1) * 60000 /
        GREATEST(25, persona.reading_speed + (trend_no.seq - 1) * 5)
    ),
    FLOOR(
        (token_no.seq - 0.25) * 60000 /
        GREATEST(25, persona.reading_speed + (trend_no.seq - 1) * 5)
    ),
    NOT (
        token_no.seq = 2
        AND trend_no.seq = 1
        AND persona.base_accuracy < 700
    ),
    TIMESTAMPADD(
        SECOND,
        token_no.seq * 20,
        training.started_at
    ),
    LEAST(980, GREATEST(
        400,
        persona.base_accuracy + (trend_no.seq - 1) * persona.trend_delta
            + token_no.seq * 7 - 30
    )),
    CEIL(token_no.seq / 2),
    MOD(token_no.seq - 1, 2),
    token_no.seq - 1,
    TRUE
FROM demo_personas persona
JOIN demo_trend_numbers trend_no ON trend_no.seq <= 3
JOIN demo_token_numbers token_no ON token_no.seq <= 6
JOIN trainings training ON training.id = 251000 + persona.persona_no * 10 + trend_no.seq
JOIN words word ON word.id = 10000 + MOD(persona.persona_no + token_no.seq - 1, 10) + 1
WHERE NOT EXISTS (
    SELECT 1 FROM word_attempt_logs existing
    WHERE existing.id =
        300000 + persona.persona_no * 100 + trend_no.seq * 10 + token_no.seq
);

INSERT INTO gaze_sessions
    (id, student_id, test_id, training_id, story_id, content_type, started_at,
     ended_at, data, status, calibration_status, created_at)
SELECT
    260000 + persona.persona_no * 10 + number.seq,
    persona.student_id,
    NULL,
    251000 + persona.persona_no * 10 + number.seq,
    NULL,
    'TRAINING',
    training.started_at,
    training.finished_at,
    JSON_ARRAY(
        JSON_OBJECT('timestampMs', 0, 'x', 0.22 + persona.persona_no * 0.01, 'y', 0.39),
        JSON_OBJECT('timestampMs', 240, 'x', 0.41, 'y', 0.42),
        JSON_OBJECT('timestampMs', 480, 'x', 0.57, 'y', 0.44)
    ),
    CASE
        WHEN number.seq = 1 AND persona.past_gaze_failure THEN 'FAILED'
        ELSE 'COMPLETED'
    END,
    CASE
        WHEN number.seq = 1 AND persona.past_gaze_failure THEN 'FAILED'
        ELSE 'SUCCESS'
    END,
    training.started_at
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
JOIN trainings training ON training.id = 251000 + persona.persona_no * 10 + number.seq
WHERE NOT EXISTS (
    SELECT 1 FROM gaze_sessions existing
    WHERE existing.id = 260000 + persona.persona_no * 10 + number.seq
);


-- 援먯닔?먭? ?대뼡 ?꾨즺 ?덈젴??癒쇱? ?댁뼱???쒖꽑 遺꾩꽍 ?덉떆瑜??뺤씤?????덇쾶 ?꾨씫 ?몄뀡??蹂닿컯?쒕떎.
INSERT INTO gaze_sessions
    (id, student_id, test_id, training_id, story_id, content_type, started_at,
     ended_at, data, status, calibration_status, created_at)
SELECT
    400000 + training.id,
    persona.student_id,
    NULL,
    training.id,
    NULL,
    'TRAINING',
    training.started_at,
    training.finished_at,
    JSON_ARRAY(
        JSON_OBJECT('timestampMs', 0, 'x', 0.18 + persona.persona_no * 0.01, 'y', 0.38),
        JSON_OBJECT('timestampMs', 260, 'x', 0.39, 'y', 0.41),
        JSON_OBJECT('timestampMs', 520, 'x', 0.61, 'y', 0.43)
    ),
    'COMPLETED',
    'SUCCESS',
    training.started_at
FROM demo_personas persona
JOIN daily_curriculums curriculum ON curriculum.student_id = persona.student_id
JOIN trainings training ON training.daily_curriculum_id = curriculum.id
WHERE training.status = 'COMPLETED'
  AND training.started_at IS NOT NULL
  AND training.finished_at IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM gaze_sessions existing
      WHERE existing.training_id = training.id
        AND existing.content_type = 'TRAINING'
  )
  AND NOT EXISTS (
      SELECT 1 FROM gaze_sessions existing
      WHERE existing.id = 400000 + training.id
  );


-- DB ?뺤닔 ????⑥쐞(0~1000)瑜??붾㈃ 諛깅텇??0~100)濡??섎せ ?ｌ? 湲곗〈 ?곕え ?됱쓣 蹂듦뎄?쒕떎.
UPDATE trainings training
JOIN daily_curriculums curriculum ON curriculum.id = training.daily_curriculum_id
JOIN demo_personas persona ON persona.student_id = curriculum.student_id
SET training.accuracy = ROUND(training.accuracy * 10)
WHERE training.accuracy BETWEEN 0 AND 100;

INSERT INTO test_curriculums (id, student_id, status, created_at, completed_at)
SELECT
    140000 + persona.persona_no,
    persona.student_id,
    'COMPLETED',
    TIMESTAMPADD(DAY, persona.persona_no, '2026-04-01 10:00:00'),
    TIMESTAMPADD(DAY, persona.persona_no, '2026-07-28 11:30:00')
FROM demo_personas persona
WHERE NOT EXISTS (
    SELECT 1 FROM test_curriculums existing
    WHERE existing.id = 140000 + persona.persona_no
);

INSERT INTO tests
    (id, test_curriculum_id, training_template_id, status, result, accuracy,
     created_at, started_at, finished_at, sequence_no)
SELECT
    141000 + persona.persona_no * 10 + number.seq,
    140000 + persona.persona_no,
    1 + MOD(persona.persona_no * 5 + number.seq - 1, 34),
    'COMPLETED',
    JSON_OBJECT(
        'overallScore', LEAST(98, GREATEST(45,
            ROUND((persona.base_accuracy + (number.seq - 1) * persona.trend_delta) / 10))),
        'changeFromPrevious', CASE
            WHEN number.seq = 1 THEN NULL
            ELSE ROUND(persona.trend_delta / 10, 1)
        END,
        'strengthAreas', JSON_ARRAY(persona.strength_area),
        'improvementAreas', JSON_ARRAY(persona.weakness_area),
        'recommendedCourse', CONCAT(persona.weakness_area, ' 吏묒쨷 ?덈젴'),
        'nextTestRecommendation', CONCAT(
            '2二???', persona.weakness_area, ' ?곸뿭??媛숈? ?쒖씠???ш??щ? 沅뚯옣?⑸땲??'
        ),
        'areaScores', JSON_ARRAY(
            JSON_OBJECT(
                'area', persona.strength_area,
                'score', LEAST(98, ROUND(
                    (persona.base_accuracy + 90 + (number.seq - 1) * persona.trend_delta) / 10,
                    1
                ))
            ),
            JSON_OBJECT(
                'area', persona.weakness_area,
                'score', LEAST(95, GREATEST(35, ROUND(
                    (persona.base_accuracy - 110 + (number.seq - 1) * persona.trend_delta) / 10,
                    1
                )))
            ),
            JSON_OBJECT(
                'area', '?쎄린 ?좎갹??,
                'score', LEAST(98, GREATEST(35, persona.reading_speed + number.seq * 3))
            )
        ),
        'readingTimeSeconds', GREATEST(55, 190 - persona.reading_speed - number.seq * 8),
        'solvingTimeSeconds', GREATEST(90, 260 - persona.reading_speed - number.seq * 10),
        'gazeDepartureCount', GREATEST(0, 5 - number.seq),
        'questions', JSON_ARRAY(
            JSON_OBJECT('questionNumber', 1, 'question', '?쒖떆???깅쭚???뚮━ ?댁뼱 ?쎌뼱 蹂댁꽭??',
                        'isCorrect', TRUE, 'selectedAnswer', '?꾩꽌愿', 'correctAnswer', '?꾩꽌愿'),
            JSON_OBJECT('questionNumber', 2, 'question', CONCAT(persona.weakness_area, '???뺤씤?섎뒗 臾몄옣???쎌뼱 蹂댁꽭??'),
                        'isCorrect', number.seq > 1, 'selectedAnswer',
                        CASE WHEN number.seq > 1 THEN '移쒓뎄? ?④퍡 湲몄쓣 李얠븯?듬땲??' ELSE '移쒓뎄? 湲몄쓣 ?껋뿀?듬땲??' END,
                        'correctAnswer', '移쒓뎄? ?④퍡 湲몄쓣 李얠븯?듬땲??'),
            JSON_OBJECT('questionNumber', 3, 'question', '?댁빞湲곗쓽 以묒떖 ?댁슜??留먰빐 蹂댁꽭??',
                        'isCorrect', TRUE, 'selectedAnswer', '?쒕줈 ?꾩? 臾몄젣瑜??닿껐?덉뒿?덈떎.',
                        'correctAnswer', '?쒕줈 ?꾩? 臾몄젣瑜??닿껐?덉뒿?덈떎.')
        )
    ),
    LEAST(98, GREATEST(45,
        ROUND((persona.base_accuracy + (number.seq - 1) * persona.trend_delta) / 10))),
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-04-05 10:00:00')
        WHEN 2 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-10 10:00:00')
        ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 09:00:00')
    END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-04-05 10:05:00')
        WHEN 2 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-10 10:05:00')
        ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 09:05:00')
    END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-04-05 10:13:00')
        WHEN 2 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-10 10:13:00')
        ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 09:13:00')
    END,
    number.seq
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
WHERE NOT EXISTS (
    SELECT 1 FROM tests existing
    WHERE existing.id = 141000 + persona.persona_no * 10 + number.seq
);

UPDATE tests test
JOIN demo_personas persona
JOIN demo_numbers number
  ON number.seq <= 3
 AND test.id = 141000 + persona.persona_no * 10 + number.seq
SET test.result = JSON_SET(
        COALESCE(test.result, JSON_OBJECT()),
        '$.overallScore', LEAST(98, GREATEST(45, ROUND(
            (persona.base_accuracy + (number.seq - 1) * persona.trend_delta) / 10
        ))),
        '$.changeFromPrevious', CASE
            WHEN number.seq = 1 THEN NULL
            ELSE ROUND(persona.trend_delta / 10, 1)
        END,
        '$.strengthAreas', JSON_ARRAY(persona.strength_area),
        '$.improvementAreas', JSON_ARRAY(persona.weakness_area),
        '$.recommendedCourse', CONCAT(persona.weakness_area, ' 吏묒쨷 ?덈젴'),
        '$.nextTestRecommendation', CONCAT(
            '2二???', persona.weakness_area, ' ?곸뿭??媛숈? ?쒖씠???ш??щ? 沅뚯옣?⑸땲??'
        ),
        '$.areaScores', JSON_ARRAY(
            JSON_OBJECT(
                'area', persona.strength_area,
                'score', LEAST(98, ROUND(
                    (persona.base_accuracy + 90 + (number.seq - 1) * persona.trend_delta) / 10,
                    1
                ))
            ),
            JSON_OBJECT(
                'area', persona.weakness_area,
                'score', LEAST(95, GREATEST(35, ROUND(
                    (persona.base_accuracy - 110 + (number.seq - 1) * persona.trend_delta) / 10,
                    1
                )))
            ),
            JSON_OBJECT(
                'area', '?쎄린 ?좎갹??,
                'score', LEAST(98, GREATEST(35, persona.reading_speed + number.seq * 3))
            )
        )
    );

INSERT INTO test_datas (id, test_id, generated_data, created_at)
SELECT
    145000 + persona.persona_no * 10 + number.seq,
    141000 + persona.persona_no * 10 + number.seq,
    JSON_OBJECT(
        'schemaVersion', 2,
        'personaFocus', persona.weakness_area,
        'questions', JSON_ARRAY(
            JSON_OBJECT('questionNo', 1,
                        'type', 'SENTENCE_READING',
                        'requiredInputs', JSON_ARRAY('VOICE'),
                        'content', JSON_OBJECT('tokens', JSON_ARRAY('?꾩꽌愿?먯꽌', '梨낆쓣', '?쎌뿀?듬땲??')),
                        'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '?꾩꽌愿?먯꽌 梨낆쓣 ?쎌뿀?듬땲??')),
                        'answer', JSON_OBJECT('expectedText', '?꾩꽌愿?먯꽌 梨낆쓣 ?쎌뿀?듬땲??')),
            JSON_OBJECT('questionNo', 2,
                        'type', 'SENTENCE_READING',
                        'requiredInputs', JSON_ARRAY('VOICE'),
                        'content', JSON_OBJECT('tokens', JSON_ARRAY('移쒓뎄?', '?④퍡', '湲몄쓣', '李얠븯?듬땲??')),
                        'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '移쒓뎄? ?④퍡 湲몄쓣 李얠븯?듬땲??')),
                        'answer', JSON_OBJECT('expectedText', '移쒓뎄? ?④퍡 湲몄쓣 李얠븯?듬땲??')),
            JSON_OBJECT('questionNo', 3,
                        'type', 'SENTENCE_READING',
                        'requiredInputs', JSON_ARRAY('VOICE'),
                        'content', JSON_OBJECT('tokens', JSON_ARRAY('?쒕줈', '?꾩?', '臾몄젣瑜?, '?닿껐?덉뒿?덈떎.')),
                        'analysisTargets', JSON_ARRAY(JSON_OBJECT('text', '?쒕줈 ?꾩? 臾몄젣瑜??닿껐?덉뒿?덈떎.')),
                        'answer', JSON_OBJECT('expectedText', '?쒕줈 ?꾩? 臾몄젣瑜??닿껐?덉뒿?덈떎.'))
        )
    ),
    test.created_at
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
JOIN tests test ON test.id = 141000 + persona.persona_no * 10 + number.seq
WHERE NOT EXISTS (
    SELECT 1 FROM test_datas existing
    WHERE existing.id = 145000 + persona.persona_no * 10 + number.seq
);

INSERT INTO gaze_sessions
    (id, student_id, test_id, training_id, story_id, content_type, started_at,
     ended_at, data, status, calibration_status, created_at)
SELECT
    150000 + persona.persona_no * 10 + number.seq,
    persona.student_id,
    CASE WHEN number.seq <= 2 THEN
        141000 + persona.persona_no * 10 + CASE number.seq WHEN 1 THEN 1 ELSE 3 END
    END,
    CASE WHEN number.seq = 3 THEN 130000 + persona.persona_no * 100 + 8 END,
    NULL,
    CASE WHEN number.seq <= 2 THEN 'TEST' ELSE 'TRAINING' END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-04-05 10:05:00')
        WHEN 2 THEN TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 09:05:00')
        ELSE TIMESTAMPADD(DAY, persona.persona_no, '2026-07-08 09:05:00')
    END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-04-05 10:13:00')
        WHEN 2 THEN TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 09:13:00')
        ELSE TIMESTAMPADD(DAY, persona.persona_no, '2026-07-08 09:12:00')
    END,
    JSON_ARRAY(
        JSON_OBJECT('timestampMs', 0, 'x', 0.24 + persona.persona_no * 0.01, 'y', 0.38),
        JSON_OBJECT('timestampMs', 200, 'x', 0.40, 'y', 0.41),
        JSON_OBJECT('timestampMs', 400, 'x', 0.54, 'y', 0.43)
    ),
    CASE WHEN number.seq = 3 AND persona.past_gaze_failure THEN 'FAILED' ELSE 'COMPLETED' END,
    CASE WHEN number.seq = 3 AND persona.past_gaze_failure THEN 'FAILED' ELSE 'SUCCESS' END,
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-04-05 10:05:00')
        WHEN 2 THEN TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 09:05:00')
        ELSE TIMESTAMPADD(DAY, persona.persona_no, '2026-07-08 09:05:00')
    END
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 3
WHERE NOT EXISTS (
    SELECT 1 FROM gaze_sessions existing
    WHERE existing.id = 150000 + persona.persona_no * 10 + number.seq
);


INSERT INTO student_feature_profiles
    (id, student_id, reading_features_id, accuracy_rate, avg_pronunciation_scor,
     pronunciation_error_rate, avg_fixation_duration_ms, avg_fixation_count,
     avg_regression_count, skip_rate, avg_reading_time_ms, weakness_score,
     confidence, evidence_count, last_evidence_at, analyzed_at)
SELECT
    160000 + persona.persona_no * 10 + number.seq,
    persona.student_id,
    feature.id,
    LEAST(0.9800, GREATEST(0.4000,
        (persona.base_accuracy + (number.seq - 2) * persona.trend_delta) / 1000)),
    LEAST(980, GREATEST(420,
        persona.base_accuracy + number.seq * 18 - CASE WHEN number.seq = 1 THEN 90 ELSE 0 END)),
    GREATEST(0.02, (1000 - persona.base_accuracy + number.seq * 12) / 20),
    GREATEST(380, 980 - persona.reading_speed * 4 + number.seq * 45),
    1.10 + number.seq * 0.35,
    0.30 + number.seq * 0.28,
    LEAST(0.35, GREATEST(0.01, (1000 - persona.base_accuracy) / 1800 + number.seq * 0.01)),
    GREATEST(650, 2100 - persona.reading_speed * 11 + number.seq * 80),
    LEAST(950, GREATEST(120,
        1000 - persona.base_accuracy + CASE WHEN number.seq = 1 THEN 180 ELSE number.seq * 25 END)),
    0.7600 + number.seq * 0.045,
    8 + persona.persona_no + number.seq * 3,
    TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 16:00:00'),
    TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 17:00:00')
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 4
JOIN demo_features feature
  ON feature.feature_no = 1 + MOD(persona.persona_no * 7 + number.seq * 5 - 1,
      @demo_feature_count)
WHERE NOT EXISTS (
    SELECT 1 FROM student_feature_profiles existing
    WHERE existing.id = 160000 + persona.persona_no * 10 + number.seq
);

INSERT INTO reports
    (id, student_id, start_date, end_date, snapshot_data, teacher_memo, created_at)
SELECT
    170000 + persona.persona_no * 10 + number.seq,
    persona.student_id,
    CASE number.seq WHEN 1 THEN '2026-04-01 00:00:00' ELSE '2026-07-01 00:00:00' END,
    CASE number.seq WHEN 1 THEN '2026-05-31 23:59:59' ELSE '2026-07-29 23:59:59' END,
    JSON_OBJECT(
        'learningDays', CASE number.seq WHEN 1 THEN 5 ELSE 8 END,
        'totalTrainingTimeMinutes', CASE number.seq WHEN 1 THEN 58 ELSE 92 END,
        'completedTrainingCount', CASE number.seq WHEN 1 THEN 4 ELSE 8 END,
        'averageAccuracy', ROUND((persona.base_accuracy + (number.seq - 1) * persona.trend_delta) / 10, 1),
        'averageReadingSpeed', persona.reading_speed + (number.seq - 1) * 6,
        'readingSpeedUnit', 'CPM',
        'growthHistory', JSON_ARRAY(
            JSON_OBJECT('date', CASE number.seq WHEN 1 THEN '2026-04-12' ELSE '2026-07-10' END,
                        'accuracy', ROUND((persona.base_accuracy - 25) / 10, 1),
                        'readingSpeed', persona.reading_speed - 4,
                        'pronunciationScore', ROUND((persona.base_accuracy - 10) / 10, 1)),
            JSON_OBJECT('date', CASE number.seq WHEN 1 THEN '2026-05-20' ELSE '2026-07-28' END,
                        'accuracy', ROUND((persona.base_accuracy + (number.seq - 1) * persona.trend_delta) / 10, 1),
                        'readingSpeed', persona.reading_speed + (number.seq - 1) * 6,
                        'pronunciationScore', ROUND((persona.base_accuracy + persona.trend_delta) / 10, 1))
        ),
        'areaAchievements', JSON_ARRAY(
            JSON_OBJECT('area', persona.strength_area,
                        'achievement', LEAST(98, ROUND((persona.base_accuracy + 90) / 10, 1))),
            JSON_OBJECT('area', persona.weakness_area,
                        'achievement', GREATEST(40, ROUND((persona.base_accuracy - 80 + number.seq * persona.trend_delta) / 10, 1))),
            JSON_OBJECT('area', '臾몄옣 ?좎갹??,
                        'achievement', ROUND((persona.base_accuracy + persona.trend_delta / 2) / 10, 1))
        ),
        'frequentlyIncorrectWords', JSON_ARRAY(
            JSON_OBJECT('wordId', 10003, 'wordName', '苑껊강',
                        'attemptCount', 4, 'incorrectCount', CASE WHEN persona.base_accuracy < 700 THEN 3 ELSE 1 END,
                        'incorrectRate', CASE WHEN persona.base_accuracy < 700 THEN 75.00 ELSE 25.00 END),
            JSON_OBJECT('wordId', 10005, 'wordName', '援?Ъ',
                        'attemptCount', 3, 'incorrectCount', CASE WHEN persona.weakness_area LIKE '%鍮꾩쓬??' THEN 2 ELSE 1 END,
                        'incorrectRate', CASE WHEN persona.weakness_area LIKE '%鍮꾩쓬??' THEN 66.67 ELSE 33.33 END)
        ),
        'improvedPatterns', JSON_ARRAY(persona.strength_area, CONCAT(persona.weakness_area, ' ?ъ떆??)),
        'persistentDifficultyPatterns', JSON_ARRAY(persona.weakness_area),
        'gazeAnalysis', JSON_OBJECT(
            'gazeAnalysisResultId', 155000 + persona.persona_no * 10 + 2,
            'totalDwellTime', GREATEST(22000, 68000 - persona.reading_speed * 260 - 2 * persona.trend_delta * 80),
            'dwellCount', GREATEST(38, 104 - persona.reading_speed / 2 - 12),
            'regressionCount', GREATEST(1, 9 - persona.trend_delta / 20),
            'averageFixationTime', GREATEST(380, 820 - persona.trend_delta * 2)
        ),
        'gazeTrend', JSON_OBJECT(
            'generatedAt', CASE number.seq WHEN 1 THEN '2026-05-31T18:00:00' ELSE '2026-07-29T18:00:00' END,
            'training', JSON_OBJECT(
                'status', CASE WHEN persona.past_gaze_failure THEN 'FAILED' ELSE 'AVAILABLE' END,
                'comparisonAvailable', FALSE,
                'points', JSON_ARRAY(),
                'changes', NULL,
                'descriptions', JSON_ARRAY(
                    CASE WHEN persona.past_gaze_failure
                         THEN '怨쇨굅 蹂댁젙 ?ㅽ뙣 ?몄뀡? 異붿씠?먯꽌 ?쒖쇅?덉뒿?덈떎.'
                         ELSE '?덈젴 ?쒖꽑? ?덉젙 踰붿쐞?먯꽌 ?섏쭛?섏뿀?듬땲??' END),
                'failedSessionCount', CASE WHEN persona.past_gaze_failure THEN 1 ELSE 0 END
            ),
            'test', JSON_OBJECT(
                'status', 'AVAILABLE',
                'comparisonAvailable', TRUE,
                'points', JSON_ARRAY(
                    JSON_OBJECT(
                        'gazeAnalysisResultId', 155000 + persona.persona_no * 10 + 1,
                        'gazeSessionId', 150000 + persona.persona_no * 10 + 1,
                        'sourceType', 'TEST',
                        'sourceId', 141000 + persona.persona_no * 10 + 1,
                        'analyzedAt', DATE_FORMAT(
                            TIMESTAMPADD(DAY, persona.persona_no, '2026-04-05 10:14:00'),
                            '%Y-%m-%dT%H:%i:%s'),
                        'totalVisitedDurationMs', GREATEST(22000, 68000 - persona.reading_speed * 260 - persona.trend_delta * 80),
                        'totalVisitedCount', GREATEST(38, 104 - persona.reading_speed / 2 - 6),
                        'reverseReadCount', GREATEST(1, 12 - persona.trend_delta / 20),
                        'avgVisitedDurationMs', GREATEST(380, 820 - persona.trend_delta)
                    ),
                    JSON_OBJECT(
                        'gazeAnalysisResultId', 155000 + persona.persona_no * 10 + 2,
                        'gazeSessionId', 150000 + persona.persona_no * 10 + 2,
                        'sourceType', 'TEST',
                        'sourceId', 141000 + persona.persona_no * 10 + 3,
                        'analyzedAt', DATE_FORMAT(
                            TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-28 09:14:00'),
                            '%Y-%m-%dT%H:%i:%s'),
                        'totalVisitedDurationMs', GREATEST(22000, 68000 - persona.reading_speed * 260 - 2 * persona.trend_delta * 80),
                        'totalVisitedCount', GREATEST(38, 104 - persona.reading_speed / 2 - 12),
                        'reverseReadCount', GREATEST(1, 9 - persona.trend_delta / 20),
                        'avgVisitedDurationMs', GREATEST(380, 820 - persona.trend_delta * 2)
                    )
                ),
                'changes', JSON_OBJECT(
                    'totalVisitedDurationMs', JSON_OBJECT(
                        'first', GREATEST(22000, 68000 - persona.reading_speed * 260 - persona.trend_delta * 80),
                        'latest', GREATEST(22000, 68000 - persona.reading_speed * 260 - 2 * persona.trend_delta * 80),
                        'delta', -persona.trend_delta * 80),
                    'totalVisitedCount', JSON_OBJECT('first', 98, 'latest', 86, 'delta', -12),
                    'reverseReadCount', JSON_OBJECT('first', 10, 'latest', 6, 'delta', -4),
                    'avgVisitedDurationMs', JSON_OBJECT(
                        'first', GREATEST(380, 820 - persona.trend_delta),
                        'latest', GREATEST(380, 820 - persona.trend_delta * 2),
                        'delta', -persona.trend_delta)
                ),
                'descriptions', JSON_ARRAY(
                    CONCAT(persona.weakness_area, ' 愿???쒖꽑 吏?쒓? ?댁쟾 寃?щ낫???덉젙?섏뿀?듬땲??')),
                'failedSessionCount', CASE WHEN persona.past_gaze_failure THEN 1 ELSE 0 END
            )
        )
    ),
    CONCAT('[', persona.persona_title, '] ', persona.strength_area,
           '? 媛뺤젏?대ŉ ', persona.weakness_area, '???ㅼ쓬 吏??紐⑺몴濡?沅뚯옣?⑸땲??'),
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(HOUR, persona.persona_no, '2026-05-31 18:00:00')
        ELSE TIMESTAMPADD(MINUTE, persona.persona_no, '2026-07-29 18:00:00')
    END
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 2
WHERE NOT EXISTS (
    SELECT 1 FROM reports existing
    WHERE existing.id = 170000 + persona.persona_no * 10 + number.seq
);

UPDATE reports report
JOIN demo_personas persona ON report.student_id = persona.student_id
SET report.teacher_memo = CONCAT(
        '[湲곌컙 愿李? ', persona.strength_area, '? ?덉젙?곸쑝濡??쒖슜?덉뒿?덈떎. ',
        persona.weakness_area, '?먯꽌???⑥꽌 ?쒓났 ?щ????곕씪 ?섑뻾 李⑥씠媛 ?섑??ъ뒿?덈떎. ',
        '[?ㅼ쓬 吏?? ?꾩옱 ?쒖씠?꾨? ?좎??섎ŉ 媛숈? ?좏삎??吏㏐쾶 諛섎났?섍퀬, 2二????낅┰ ?섑뻾??鍮꾧탳?⑸땲??'
    ),
    report.snapshot_data = JSON_SET(
        report.snapshot_data,
        '$.gazeTrend.training',
        JSON_OBJECT(
            'status', 'AVAILABLE',
            'comparisonAvailable', TRUE,
            'points', CASE
                WHEN persona.past_gaze_failure THEN JSON_ARRAY(
                    JSON_OBJECT(
                        'gazeAnalysisResultId', 261000 + persona.persona_no * 10 + 2,
                        'gazeSessionId', 260000 + persona.persona_no * 10 + 2,
                        'sourceType', 'TRAINING',
                        'sourceId', 251000 + persona.persona_no * 10 + 2,
                        'analyzedAt', DATE_FORMAT(
                            TIMESTAMPADD(MINUTE, persona.persona_no + 13, '2026-07-14 09:00:00'),
                            '%Y-%m-%dT%H:%i:%s'
                        ),
                        'totalVisitedDurationMs', GREATEST(
                            18000,
                            61000 - persona.reading_speed * 250 - 2 * persona.trend_delta * 75
                        ),
                        'totalVisitedCount', GREATEST(30, 96 - persona.reading_speed / 2 - 10),
                        'reverseReadCount', GREATEST(
                            0,
                            8 + CASE WHEN persona.weakness_area LIKE '%?섏씫湲?' THEN 4 ELSE 0 END
                        ),
                        'avgVisitedDurationMs', GREATEST(300, 780 - persona.trend_delta * 2)
                    ),
                    JSON_OBJECT(
                        'gazeAnalysisResultId', 261000 + persona.persona_no * 10 + 3,
                        'gazeSessionId', 260000 + persona.persona_no * 10 + 3,
                        'sourceType', 'TRAINING',
                        'sourceId', 251000 + persona.persona_no * 10 + 3,
                        'analyzedAt', DATE_FORMAT(
                            TIMESTAMPADD(MINUTE, persona.persona_no + 13, '2026-07-24 09:00:00'),
                            '%Y-%m-%dT%H:%i:%s'
                        ),
                        'totalVisitedDurationMs', GREATEST(
                            18000,
                            61000 - persona.reading_speed * 250 - 3 * persona.trend_delta * 75
                        ),
                        'totalVisitedCount', GREATEST(30, 96 - persona.reading_speed / 2 - 15),
                        'reverseReadCount', GREATEST(
                            0,
                            6 + CASE WHEN persona.weakness_area LIKE '%?섏씫湲?' THEN 4 ELSE 0 END
                        ),
                        'avgVisitedDurationMs', GREATEST(300, 780 - persona.trend_delta * 3)
                    )
                )
                ELSE JSON_ARRAY(
                    JSON_OBJECT(
                        'gazeAnalysisResultId', 261000 + persona.persona_no * 10 + 1,
                        'gazeSessionId', 260000 + persona.persona_no * 10 + 1,
                        'sourceType', 'TRAINING',
                        'sourceId', 251000 + persona.persona_no * 10 + 1,
                        'analyzedAt', DATE_FORMAT(
                            TIMESTAMPADD(MINUTE, persona.persona_no + 13, '2026-07-04 09:00:00'),
                            '%Y-%m-%dT%H:%i:%s'
                        ),
                        'totalVisitedDurationMs', GREATEST(
                            18000,
                            61000 - persona.reading_speed * 250 - persona.trend_delta * 75
                        ),
                        'totalVisitedCount', GREATEST(30, 96 - persona.reading_speed / 2 - 5),
                        'reverseReadCount', GREATEST(
                            0,
                            10 + CASE WHEN persona.weakness_area LIKE '%?섏씫湲?' THEN 4 ELSE 0 END
                        ),
                        'avgVisitedDurationMs', GREATEST(300, 780 - persona.trend_delta)
                    ),
                    JSON_OBJECT(
                        'gazeAnalysisResultId', 261000 + persona.persona_no * 10 + 2,
                        'gazeSessionId', 260000 + persona.persona_no * 10 + 2,
                        'sourceType', 'TRAINING',
                        'sourceId', 251000 + persona.persona_no * 10 + 2,
                        'analyzedAt', DATE_FORMAT(
                            TIMESTAMPADD(MINUTE, persona.persona_no + 13, '2026-07-14 09:00:00'),
                            '%Y-%m-%dT%H:%i:%s'
                        ),
                        'totalVisitedDurationMs', GREATEST(
                            18000,
                            61000 - persona.reading_speed * 250 - 2 * persona.trend_delta * 75
                        ),
                        'totalVisitedCount', GREATEST(30, 96 - persona.reading_speed / 2 - 10),
                        'reverseReadCount', GREATEST(
                            0,
                            8 + CASE WHEN persona.weakness_area LIKE '%?섏씫湲?' THEN 4 ELSE 0 END
                        ),
                        'avgVisitedDurationMs', GREATEST(300, 780 - persona.trend_delta * 2)
                    ),
                    JSON_OBJECT(
                        'gazeAnalysisResultId', 261000 + persona.persona_no * 10 + 3,
                        'gazeSessionId', 260000 + persona.persona_no * 10 + 3,
                        'sourceType', 'TRAINING',
                        'sourceId', 251000 + persona.persona_no * 10 + 3,
                        'analyzedAt', DATE_FORMAT(
                            TIMESTAMPADD(MINUTE, persona.persona_no + 13, '2026-07-24 09:00:00'),
                            '%Y-%m-%dT%H:%i:%s'
                        ),
                        'totalVisitedDurationMs', GREATEST(
                            18000,
                            61000 - persona.reading_speed * 250 - 3 * persona.trend_delta * 75
                        ),
                        'totalVisitedCount', GREATEST(30, 96 - persona.reading_speed / 2 - 15),
                        'reverseReadCount', GREATEST(
                            0,
                            6 + CASE WHEN persona.weakness_area LIKE '%?섏씫湲?' THEN 4 ELSE 0 END
                        ),
                        'avgVisitedDurationMs', GREATEST(300, 780 - persona.trend_delta * 3)
                    )
                )
            END,
            'changes', JSON_OBJECT(
                'totalVisitedDurationMs', JSON_OBJECT(
                    'first', GREATEST(
                        18000,
                        61000 - persona.reading_speed * 250
                            - CASE WHEN persona.past_gaze_failure THEN 2 ELSE 1 END
                              * persona.trend_delta * 75
                    ),
                    'latest', GREATEST(
                        18000,
                        61000 - persona.reading_speed * 250 - 3 * persona.trend_delta * 75
                    ),
                    'delta', -(
                        3 - CASE WHEN persona.past_gaze_failure THEN 2 ELSE 1 END
                    ) * persona.trend_delta * 75
                ),
                'totalVisitedCount', JSON_OBJECT(
                    'first', GREATEST(
                        30,
                        96 - persona.reading_speed / 2
                            - CASE WHEN persona.past_gaze_failure THEN 10 ELSE 5 END
                    ),
                    'latest', GREATEST(30, 96 - persona.reading_speed / 2 - 15),
                    'delta', CASE WHEN persona.past_gaze_failure THEN -5 ELSE -10 END
                ),
                'reverseReadCount', JSON_OBJECT(
                    'first', CASE WHEN persona.past_gaze_failure THEN 8 ELSE 10 END,
                    'latest', 6,
                    'delta', CASE WHEN persona.past_gaze_failure THEN -2 ELSE -4 END
                ),
                'avgVisitedDurationMs', JSON_OBJECT(
                    'first', GREATEST(
                        300,
                        780 - persona.trend_delta
                            * CASE WHEN persona.past_gaze_failure THEN 2 ELSE 1 END
                    ),
                    'latest', GREATEST(300, 780 - persona.trend_delta * 3),
                    'delta', -persona.trend_delta
                        * (3 - CASE WHEN persona.past_gaze_failure THEN 2 ELSE 1 END)
                )
            ),
            'descriptions', JSON_ARRAY(
                CONCAT(
                    persona.weakness_area,
                    ' 吏?????덈젴 以?癒몃Т由꾧낵 ?섏씫湲?吏?쒓? ?꾨쭔?섍쾶 媛먯냼?덉뒿?덈떎.'
                ),
                CASE
                    WHEN persona.past_gaze_failure
                    THEN '泥??뚭린 蹂댁젙 ?ㅽ뙣??鍮꾧탳?먯꽌 ?쒖쇅?섍퀬 ?댄썑 ?깃났 ?몄뀡留?諛섏쁺?덉뒿?덈떎.'
                    ELSE '???뚭린 紐⑤몢 蹂댁젙???깃났??媛숈? 議곌굔??異붿씠瑜?鍮꾧탳?덉뒿?덈떎.'
                END
            ),
            'failedSessionCount', CASE WHEN persona.past_gaze_failure THEN 1 ELSE 0 END
        )
    )
WHERE report.id BETWEEN 170000 AND 170999;

INSERT INTO stories (id, student_id, story_template_id, created_at, status, progress)
SELECT
    180000 + persona.persona_no * 10 + number.seq,
    persona.student_id,
    1 + MOD(persona.persona_no + number.seq, 2),
    CASE number.seq
        WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-15 15:00:00')
        ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-27 15:00:00')
    END,
    CASE number.seq WHEN 1 THEN 'COMPLETED' ELSE persona.story_status END,
    CASE number.seq WHEN 1 THEN 100 ELSE persona.story_progress END
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 2
WHERE NOT EXISTS (
    SELECT 1 FROM stories existing
    WHERE existing.id = 180000 + persona.persona_no * 10 + number.seq
);

INSERT INTO story_scenes (scene_id, story_id, image_url, sequence_no, created_at)
SELECT
    181000 + persona.persona_no * 100 + story_no.seq * 10 + scene_no.seq,
    180000 + persona.persona_no * 10 + story_no.seq,
    NULL,
    scene_no.seq,
    TIMESTAMPADD(MINUTE, scene_no.seq * 5,
        CASE story_no.seq
            WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-15 15:00:00')
            ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-27 15:00:00')
        END)
FROM demo_personas persona
JOIN demo_story_numbers story_no ON story_no.seq <= 2
JOIN demo_scene_numbers scene_no ON scene_no.seq <= 2
WHERE NOT EXISTS (
    SELECT 1 FROM story_scenes existing
    WHERE existing.scene_id =
        181000 + persona.persona_no * 100 + story_no.seq * 10 + scene_no.seq
);

INSERT INTO story_lines
    (id, scene_id, has_choices, content, sequence_no, created_at, read_at)
SELECT
    182000 + persona.persona_no * 1000 + story_no.seq * 100 + scene_no.seq * 10 + line_no.seq,
    181000 + persona.persona_no * 100 + story_no.seq * 10 + scene_no.seq,
    line_no.seq = 2,
    CASE line_no.seq
        WHEN 1 THEN CONCAT(persona.persona_title, '媛 ?뀁냽 ?꾩꽌愿?먯꽌 鍮쏅굹??吏?꾨? 諛쒓껄?덉뒿?덈떎.')
        ELSE CONCAT('吏?꾩뿉??', persona.strength_area, '???쒖슜?댁빞 ?대━??湲몄씠 洹몃젮???덉뿀?듬땲??')
    END,
    line_no.seq,
    TIMESTAMPADD(MINUTE, scene_no.seq * 5 + line_no.seq,
        CASE story_no.seq
            WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-15 15:00:00')
            ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-27 15:00:00')
        END),
    CASE
        WHEN story_no.seq = 1 OR persona.story_progress >= scene_no.seq * 40
        THEN TIMESTAMPADD(MINUTE, scene_no.seq * 5 + line_no.seq + 1,
            CASE story_no.seq
                WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-15 15:00:00')
                ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-27 15:00:00')
            END)
    END
FROM demo_personas persona
JOIN demo_story_numbers story_no ON story_no.seq <= 2
JOIN demo_scene_numbers scene_no ON scene_no.seq <= 2
JOIN demo_line_numbers line_no ON line_no.seq <= 2
WHERE NOT EXISTS (
    SELECT 1 FROM story_lines existing
    WHERE existing.id =
        182000 + persona.persona_no * 1000 + story_no.seq * 100 + scene_no.seq * 10 + line_no.seq
);

INSERT INTO story_choices (id, story_line_id, content, created_at)
SELECT
    195000 + persona.persona_no * 100 + story_no.seq * 10 + scene_no.seq,
    182000 + persona.persona_no * 1000 + story_no.seq * 100 + scene_no.seq * 10 + 2,
    CONCAT(persona.weakness_area, '??泥쒖쿇???뺤씤?섎ŉ ?ㅼ쓬 湲몃줈 媛꾨떎.'),
    TIMESTAMPADD(MINUTE, scene_no.seq * 5 + 3,
        CASE story_no.seq
            WHEN 1 THEN TIMESTAMPADD(DAY, persona.persona_no, '2026-06-15 15:00:00')
            ELSE TIMESTAMPADD(HOUR, persona.persona_no, '2026-07-27 15:00:00')
        END)
FROM demo_personas persona
JOIN demo_story_numbers story_no ON story_no.seq <= 2
JOIN demo_scene_numbers scene_no ON scene_no.seq <= 2
WHERE NOT EXISTS (
    SELECT 1 FROM story_choices existing
    WHERE existing.id = 195000 + persona.persona_no * 100 + story_no.seq * 10 + scene_no.seq
);

INSERT INTO word_attempt_logs
    (id, student_id, word_id, story_line_id, training_id, test_id, use_location,
     surface_text, has_audio_data, fixation_duration_ms, fixation_count,
     gaze_start_offset_ms, gaze_end_offset_ms, is_skipped, regression_count,
     pronunciation_accuracy_score, speech_start_offset_ms, speech_end_offset_ms,
     is_correct, created_at, total_score, question_no, target_index, token_index, is_final)
SELECT
    200000 + persona.persona_no * 100 + number.seq,
    persona.student_id,
    10000 + MOD(persona.persona_no + number.seq - 1, 10) + 1,
    NULL,
    130000 + persona.persona_no * 100 + 8,
    NULL,
    'TRAINING',
    word.content,
    TRUE,
    GREATEST(480, 1500 - persona.reading_speed * 8 + number.seq * 55),
    1 + MOD(number.seq + persona.persona_no, 4),
    (number.seq - 1) * 900,
    number.seq * 900,
    FALSE,
    CASE WHEN number.seq <= 2 AND persona.base_accuracy < 700 THEN 2 ELSE 0 END,
    LEAST(980, GREATEST(420,
        persona.base_accuracy + number.seq * 20 - CASE WHEN number.seq = 1 THEN 100 ELSE 0 END)),
    (number.seq - 1) * 850,
    number.seq * 850,
    CASE WHEN number.seq <= 2 AND persona.base_accuracy < 700 THEN FALSE ELSE TRUE END,
    TIMESTAMPADD(MINUTE, number.seq,
        TIMESTAMPADD(DAY, persona.persona_no, '2026-07-08 09:05:00')),
    LEAST(980, GREATEST(400, persona.base_accuracy + number.seq * 15)),
    CEIL(number.seq / 2),
    MOD(number.seq - 1, 2),
    MOD(number.seq - 1, 3),
    TRUE
FROM demo_personas persona
JOIN demo_numbers number ON number.seq <= 6
JOIN words word ON word.id = 10000 + MOD(persona.persona_no + number.seq - 1, 10) + 1
WHERE NOT EXISTS (
    SELECT 1 FROM word_attempt_logs existing
    WHERE existing.id = 200000 + persona.persona_no * 100 + number.seq
);

DROP TEMPORARY TABLE demo_reading_templates;
DROP TEMPORARY TABLE demo_features;
DROP TEMPORARY TABLE demo_token_numbers;
DROP TEMPORARY TABLE demo_trend_numbers;
DROP TEMPORARY TABLE demo_line_numbers;
DROP TEMPORARY TABLE demo_scene_numbers;
DROP TEMPORARY TABLE demo_story_numbers;
DROP TEMPORARY TABLE demo_numbers;
DROP TEMPORARY TABLE demo_personas;
