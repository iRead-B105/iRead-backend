package com.iread.backend.training.curriculum;

import com.iread.backend.readingfeature.domain.ReadingFeatureCategory;
import com.iread.backend.readingfeature.domain.ReadingFeatureEntity;
import com.iread.backend.readingfeature.domain.ReadingFeatureScope;
import com.iread.backend.readingfeature.domain.StudentFeatureProfileEntity;
import com.iread.backend.readingfeature.repository.StudentFeatureProfileRepository;
import com.iread.backend.training.domain.CurriculumUnitEntity;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import com.iread.backend.training.repository.TrainingTemplateRepository;
import com.iread.backend.student.repository.StudentRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PersonalizedCurriculumPlannerTest {

    @Test
    void selectsThreeDirectOneExtensionAndOneFluencyTraining() {
        DailyCurriculumRepository curricula = mock(DailyCurriculumRepository.class);
        TrainingTemplateRepository templates = mock(TrainingTemplateRepository.class);
        StudentFeatureProfileRepository profiles =
                mock(StudentFeatureProfileRepository.class);
        StudentRepository students = mock(StudentRepository.class);
        PersonalizedCurriculumPlanner planner = new PersonalizedCurriculumPlanner(
                curricula, templates, profiles, students, JsonMapper.builder().build()
        );

        ReadingFeatureEntity feature = new ReadingFeatureEntity(
                1L, null, "GRAPHEME.CODA.SIMPLE.ㄱ", "받침 ㄱ",
                ReadingFeatureCategory.GRAPHEME, ReadingFeatureScope.CHARACTER
        );
        StudentFeatureProfileEntity profile = new StudentFeatureProfileEntity(
                1L, mock(com.iread.backend.student.domain.StudentEntity.class),
                feature, new BigDecimal("0.9000")
        );
        profile.updateMetrics(
                new BigDecimal("0.3000"), 500, new BigDecimal("0.70"),
                1200, new BigDecimal("3.00"), new BigDecimal("2.00"),
                BigDecimal.ZERO, 2500, 800, new BigDecimal("0.9000"),
                10, null, null
        );

        List<TrainingTemplateEntity> catalog = List.of(
                template(1L, 1, 1, true),
                template(2L, 1, 2, true),
                template(3L, 1, 3, true),
                template(4L, 4, 1, false),
                template(5L, 8, 1, false),
                template(6L, 1, 4, false)
        );
        when(templates.findAllByOrderByCurriculumUnitSequenceNoAscSequenceNoAsc())
                .thenReturn(catalog);
        when(profiles.findAllByStudentIdOrderByWeaknessScoreDesc(15L))
                .thenReturn(List.of(profile));

        List<Long> selected = planner.selectTemplates(15L).stream()
                .map(TrainingTemplateEntity::getId)
                .toList();

        assertThat(selected).containsExactly(1L, 2L, 3L, 4L, 5L);
    }

    private TrainingTemplateEntity template(
            Long id,
            int unitSequence,
            int sequence,
            boolean compatible
    ) {
        CurriculumUnitEntity unit = mock(CurriculumUnitEntity.class);
        when(unit.getSequenceNo()).thenReturn(unitSequence);
        TrainingTemplateEntity template = mock(TrainingTemplateEntity.class);
        when(template.getId()).thenReturn(id);
        when(template.getCurriculumUnit()).thenReturn(unit);
        when(template.getSequenceNo()).thenReturn(sequence);
        when(template.getPrompt()).thenReturn(compatible
                ? """
                  {"supportedFeatureCategories":["GRAPHEME"],
                   "supportedScopes":["CHARACTER"]}
                  """
                : """
                  {"supportedFeatureCategories":["SENTENCE"],
                   "supportedScopes":["SENTENCE"]}
                  """);
        return template;
    }
}
