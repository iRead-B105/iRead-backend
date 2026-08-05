package com.iread.backend.training.curriculum;

import com.iread.backend.readingfeature.domain.StudentFeatureProfileEntity;
import com.iread.backend.readingfeature.repository.StudentFeatureProfileRepository;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import com.iread.backend.test.domain.TestCurriculumEntity;
import com.iread.backend.test.domain.TestStatus;
import com.iread.backend.test.repository.TestCurriculumRepository;
import com.iread.backend.training.domain.DailyCurriculumEntity;
import com.iread.backend.training.domain.DailyCurriculumStatus;
import com.iread.backend.training.domain.TrainingDataEntity;
import com.iread.backend.training.domain.TrainingEntity;
import com.iread.backend.training.domain.TrainingTemplateEntity;
import com.iread.backend.training.generation.PersonalizedTrainingGenerationService;
import com.iread.backend.training.generation.TrainingCatalogPolicy;
import com.iread.backend.training.repository.DailyCurriculumRepository;
import com.iread.backend.training.repository.TrainingDataRepository;
import com.iread.backend.training.repository.TrainingTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PersonalizedCurriculumPlanner {

    public static final int TRAINING_COUNT = 5;
    private static final int DIRECT_COUNT = 3;
    private static final int FLUENCY_UNIT_SEQUENCE = 8;

    private final DailyCurriculumRepository curriculumRepository;
    private final TrainingTemplateRepository templateRepository;
    private final StudentFeatureProfileRepository profileRepository;
    private final StudentRepository studentRepository;
    private final TestCurriculumRepository testCurriculumRepository;
    private final TrainingDataRepository trainingDataRepository;
    private final PersonalizedTrainingGenerationService generationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public DailyCurriculumEntity createNextIfAbsent(StudentEntity student) {
        studentRepository.findByIdForUpdate(student.getId())
                .orElseThrow(() -> new IllegalStateException("학생을 찾을 수 없습니다."));
        Optional<DailyCurriculumEntity> existing = curriculumRepository.findByStudentIdAndStatus(
                student.getId(), DailyCurriculumStatus.NOT_STARTED
        );
        if (existing.isPresent()) {
            return existing.get();
        }
        boolean firstCurriculum = !curriculumRepository.existsByStudentId(student.getId());
        DailyCurriculumEntity curriculum = curriculumRepository.saveAndFlush(
                new DailyCurriculumEntity(student, selectTemplates(student.getId()))
        );
        if (firstCurriculum) {
            seedFirstCurriculum(curriculum);
        }
        return curriculum;
    }

    @Transactional
    public DailyCurriculumEntity createRecommendedFromTestIfAbsent(
            StudentEntity student,
            Long testCurriculumId
    ) {
        studentRepository.findByIdForUpdate(student.getId())
                .orElseThrow(() -> new IllegalStateException("학생을 찾을 수 없습니다."));
        Optional<DailyCurriculumEntity> existing =
                curriculumRepository.findBySourceTestCurriculumId(testCurriculumId);
        if (existing.isPresent()) {
            return existing.get();
        }

        TestCurriculumEntity source = testCurriculumRepository
                .findByIdForUpdate(testCurriculumId)
                .orElseThrow(() -> new IllegalStateException("근거 검사를 찾을 수 없습니다."));
        if (!source.getStudent().getId().equals(student.getId())) {
            throw new IllegalStateException("근거 검사와 학습자가 일치하지 않습니다.");
        }
        if (!TestStatus.COMPLETED.name().equals(source.getStatus())) {
            throw new IllegalStateException("완료된 실력도전 검사만 추천 근거로 사용할 수 있습니다.");
        }
        if (curriculumRepository.findByStudentIdAndStatus(
                student.getId(), DailyCurriculumStatus.NOT_STARTED
        ).isPresent()) {
            throw new IllegalStateException(
                    "다른 출처의 시작 전 커리큘럼이 있어 검사 추천을 생성할 수 없습니다."
            );
        }
        boolean firstCurriculum = !curriculumRepository.existsByStudentId(student.getId());
        DailyCurriculumEntity curriculum = curriculumRepository.saveAndFlush(
                new DailyCurriculumEntity(
                        student,
                        selectTemplates(student.getId()),
                        source
                )
        );
        if (firstCurriculum) {
            seedFirstCurriculum(curriculum);
        }
        return curriculum;
    }

    /**
     * 학생의 첫 커리큘럼은 새벽 배치(AI 생성)를 기다리지 않고 즉시 학습할 수 있어야
     * 하므로, 미리 정의된 시드 데이터로 문항을 채우고 바로 시작 가능 상태로 만든다.
     */
    private void seedFirstCurriculum(DailyCurriculumEntity curriculum) {
        for (TrainingEntity training : curriculum.getTrainings()) {
            ObjectNode generated = generationService.generateSeed(training);
            generated.put("revision", 1);
            trainingDataRepository.save(
                    new TrainingDataEntity(training, writeJson(generated))
            );
            training.markReady();
        }
        curriculum.refreshReviewRequirement();
        trainingDataRepository.flush();
    }

    private String writeJson(ObjectNode value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("첫 커리큘럼 시드 JSON 저장에 실패했습니다.", exception);
        }
    }

    public List<TrainingTemplateEntity> selectTemplates(Long studentId) {
        List<TrainingTemplateEntity> catalog = templateRepository
                .findAllByOrderByCurriculumUnitSequenceNoAscSequenceNoAsc()
                .stream()
                .filter(TrainingCatalogPolicy::isSelectable)
                .toList();
        if (catalog.size() < TRAINING_COUNT) {
            throw new IllegalStateException("맞춤 커리큘럼을 편성할 훈련 템플릿이 부족합니다.");
        }
        List<StudentFeatureProfileEntity> profiles =
                profileRepository.findAllByStudentIdOrderByWeaknessScoreDesc(studentId);

        List<TrainingTemplateEntity> selected = new ArrayList<>();
        Set<Long> selectedIds = new HashSet<>();

        catalog.stream()
                .filter(template -> unitSequence(template) != FLUENCY_UNIT_SEQUENCE)
                .sorted(templateComparator(profiles))
                .filter(template -> compatibilityScore(template, profiles) > 0)
                .limit(DIRECT_COUNT)
                .forEach(template -> add(selected, selectedIds, template));
        fillFromCatalog(selected, selectedIds, catalog, DIRECT_COUNT, false);

        catalog.stream()
                .filter(template -> unitSequence(template) != FLUENCY_UNIT_SEQUENCE)
                .filter(template -> !selectedIds.contains(template.getId()))
                .sorted(extensionComparator(selected, profiles))
                .findFirst()
                .ifPresent(template -> add(selected, selectedIds, template));

        catalog.stream()
                .filter(template -> unitSequence(template) == FLUENCY_UNIT_SEQUENCE)
                .filter(template -> !selectedIds.contains(template.getId()))
                .sorted(templateComparator(profiles))
                .findFirst()
                .ifPresent(template -> add(selected, selectedIds, template));

        fillFromCatalog(selected, selectedIds, catalog, TRAINING_COUNT, true);
        if (selected.size() != TRAINING_COUNT) {
            throw new IllegalStateException("맞춤 커리큘럼 훈련 5개를 편성하지 못했습니다.");
        }
        return List.copyOf(selected);
    }

    private Comparator<TrainingTemplateEntity> templateComparator(
            List<StudentFeatureProfileEntity> profiles
    ) {
        return Comparator
                .comparingInt((TrainingTemplateEntity template) ->
                        compatibilityScore(template, profiles))
                .reversed()
                .thenComparingInt(this::unitSequence)
                .thenComparing(TrainingTemplateEntity::getSequenceNo)
                .thenComparing(TrainingTemplateEntity::getId);
    }

    private Comparator<TrainingTemplateEntity> extensionComparator(
            List<TrainingTemplateEntity> selected,
            List<StudentFeatureProfileEntity> profiles
    ) {
        int highestDirectUnit = selected.stream()
                .mapToInt(this::unitSequence)
                .max()
                .orElse(1);
        return Comparator
                .comparingInt((TrainingTemplateEntity template) ->
                        unitSequence(template) > highestDirectUnit ? 0 : 1)
                .thenComparing(templateComparator(profiles));
    }

    private int compatibilityScore(
            TrainingTemplateEntity template,
            List<StudentFeatureProfileEntity> profiles
    ) {
        JsonNode prompt = parsePrompt(template.getPrompt());
        Set<String> categories = strings(prompt.path("supportedFeatureCategories"));
        Set<String> scopes = strings(prompt.path("supportedScopes"));
        return profiles.stream()
                .filter(profile -> profile.getWeaknessScore() != null)
                .filter(profile -> categories.contains(profile.getReadingFeature().getCategory().name()))
                .filter(profile -> scopes.contains(profile.getReadingFeature().getScope().name()))
                .mapToInt(StudentFeatureProfileEntity::getWeaknessScore)
                .sum();
    }

    private void fillFromCatalog(
            List<TrainingTemplateEntity> selected,
            Set<Long> selectedIds,
            List<TrainingTemplateEntity> catalog,
            int targetSize,
            boolean includeFluency
    ) {
        catalog.stream()
                .filter(template -> includeFluency
                        || unitSequence(template) != FLUENCY_UNIT_SEQUENCE)
                .filter(template -> !selectedIds.contains(template.getId()))
                .limit(Math.max(0, targetSize - selected.size()))
                .forEach(template -> add(selected, selectedIds, template));
    }

    private void add(
            List<TrainingTemplateEntity> selected,
            Set<Long> selectedIds,
            TrainingTemplateEntity template
    ) {
        if (selected.size() < TRAINING_COUNT && selectedIds.add(template.getId())) {
            selected.add(template);
        }
    }

    private int unitSequence(TrainingTemplateEntity template) {
        Integer sequence = template.getCurriculumUnit().getSequenceNo();
        return sequence == null ? Integer.MAX_VALUE : sequence;
    }

    private JsonNode parsePrompt(String prompt) {
        try {
            return objectMapper.readTree(prompt);
        } catch (Exception exception) {
            throw new IllegalStateException("훈련 템플릿 prompt JSON이 올바르지 않습니다.", exception);
        }
    }

    private Set<String> strings(JsonNode array) {
        Set<String> result = new HashSet<>();
        if (array.isArray()) {
            array.forEach(value -> result.add(value.asText()));
        }
        return result;
    }
}
