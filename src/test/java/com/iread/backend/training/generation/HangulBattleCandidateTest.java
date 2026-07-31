package com.iread.backend.training.generation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import com.iread.backend.training.analysis.HangulSyllable;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HangulBattleCandidateTest {

    private static final int TILE_MINIMUM = 8;

    private final DeterministicTrainingCandidateProvider provider =
            new DeterministicTrainingCandidateProvider(new JsonMapper());

    @ParameterizedTest
    @EnumSource(
            value = TrainingType.class,
            names = {"HANGUL_BATTLE_BASIC", "HANGUL_BATTLE_FINAL", "HANGUL_BATTLE_DOUBLE_FINAL"}
    )
    void 자모를_순서대로_합치면_라운드의_낱말이_된다(TrainingType type) {
        for (JsonNode candidate : generate(type)) {
            JsonNode rounds = candidate.path("rounds");
            JsonNode answerOrders = candidate.path("answerOrders");
            assertThat(rounds).hasSameSizeAs(answerOrders);

            for (int index = 0; index < rounds.size(); index++) {
                String word = rounds.path(index).path("word").asText();
                assertThat(texts(answerOrders.path(index))).isEqualTo(decompose(word));
            }
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = TrainingType.class,
            names = {"HANGUL_BATTLE_BASIC", "HANGUL_BATTLE_FINAL", "HANGUL_BATTLE_DOUBLE_FINAL"}
    )
    void 타일에_정답_자모를_모두_담고_혼동용_자모로_채운다(TrainingType type) {
        for (JsonNode candidate : generate(type)) {
            JsonNode rounds = candidate.path("rounds");
            for (int index = 0; index < rounds.size(); index++) {
                List<String> tiles = texts(rounds.path(index).path("tiles"));
                List<String> answer = texts(candidate.path("answerOrders").path(index));
                assertThat(tiles).hasSizeGreaterThanOrEqualTo(TILE_MINIMUM);
                // 정답 자모는 중복 개수까지 타일에 있어야 조립할 수 있다.
                List<String> remaining = new ArrayList<>(tiles);
                assertThat(answer).allSatisfy(value ->
                        assertThat(remaining.remove(value)).isTrue());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(
            value = TrainingType.class,
            names = {"HANGUL_BATTLE_BASIC", "HANGUL_BATTLE_FINAL", "HANGUL_BATTLE_DOUBLE_FINAL"}
    )
    void 라운드가_진행될수록_상대가_빨라진다(TrainingType type) {
        for (JsonNode candidate : generate(type)) {
            JsonNode rounds = candidate.path("rounds");
            int previous = Integer.MAX_VALUE;
            for (JsonNode round : rounds) {
                int duration = round.path("opponentDurationMs").asInt();
                assertThat(duration).isPositive().isLessThan(previous);
                previous = duration;
            }
        }
    }

    @Test
    void 상대는_난이도별로_고정한다() {
        assertThat(opponent(TrainingType.HANGUL_BATTLE_BASIC)).isEqualTo("RABBIT");
        assertThat(opponent(TrainingType.HANGUL_BATTLE_FINAL)).isEqualTo("TURTLE");
        assertThat(opponent(TrainingType.HANGUL_BATTLE_DOUBLE_FINAL)).isEqualTo("ANT");
    }

    private String opponent(TrainingType type) {
        return generate(type).get(0).path("opponent").asText();
    }

    private List<JsonNode> generate(TrainingType type) {
        JsonNode data = provider.generate(new TrainingCandidateRequest(
                "battle-" + type,
                2,
                type,
                5,
                2,
                List.of(),
                List.of(),
                "",
                new JsonMapper().createObjectNode()
        )).data();
        List<JsonNode> candidates = new ArrayList<>();
        data.forEach(candidates::add);
        assertThat(candidates).hasSize(5);
        return candidates;
    }

    private List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(node -> values.add(node.asText()));
        return values;
    }

    private List<String> decompose(String word) {
        List<String> jamo = new ArrayList<>();
        for (char letter : word.toCharArray()) {
            HangulSyllable syllable = HangulSyllable.decompose(letter);
            jamo.add(syllable.onset());
            jamo.add(syllable.vowel());
            if (syllable.coda() != null) {
                jamo.add(syllable.coda());
            }
        }
        return jamo;
    }
}
