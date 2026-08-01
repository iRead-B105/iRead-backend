package com.iread.backend.story.admin.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.gaze.domain.GazeAnalysisResultEntity;
import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionEntity;
import com.iread.backend.gaze.domain.GazeSessionStatus;
import com.iread.backend.gaze.repository.GazeAnalysisResultRepository;
import com.iread.backend.gaze.repository.GazeSessionRepository;
import com.iread.backend.story.admin.dto.res.StoryGazeAnalysisResponse;
import com.iread.backend.story.admin.dto.res.StoryHistoryDetailResponse;
import com.iread.backend.story.admin.dto.res.StoryHistoryResponse;
import com.iread.backend.story.analysis.StoryLineContentService;
import com.iread.backend.story.domain.StoryChoiceEntity;
import com.iread.backend.story.domain.StoryEntity;
import com.iread.backend.story.domain.StoryLineEntity;
import com.iread.backend.story.domain.StoryStatus;
import com.iread.backend.story.repository.StoryChoiceRepository;
import com.iread.backend.story.repository.StoryLineRepository;
import com.iread.backend.story.repository.StoryRepository;
import com.iread.backend.story.repository.StoryTemplateRepository;
import com.iread.backend.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryAdminService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final StudentRepository studentRepository;
    private final StoryRepository storyRepository;
    private final StoryTemplateRepository storyTemplateRepository;
    private final StoryLineRepository storyLineRepository;
    private final StoryChoiceRepository storyChoiceRepository;
    private final GazeSessionRepository gazeSessionRepository;
    private final GazeAnalysisResultRepository gazeAnalysisResultRepository;
    private final StoryLineContentService storyLineContentService;
    private final ObjectMapper objectMapper;

    public StoryHistoryResponse getStoryHistory(
            Long teacherId,
            Long studentId,
            LocalDate from,
            LocalDate to,
            Long storyTemplateId,
            int page,
            int size
    ) {
        validateRequest(from, to, page, size);
        validateStudentOwner(teacherId, studentId);

        List<StoryEntity> stories = storyRepository
                .findAllByStudentIdAndStatusNotOrderByCreatedAtDesc(studentId, StoryStatus.DELETED);
        StoryContext context = loadContext(studentId, stories);
        List<StoryHistoryResponse.StorySummary> filtered = stories.stream()
                .map(story -> toSummary(story, context))
                .filter(summary -> storyTemplateId == null
                        || storyTemplateId.equals(summary.storyTemplateId()))
                .filter(summary -> inRange(summary.activityAt(), from, to))
                .sorted(Comparator
                        .comparing(StoryHistoryResponse.StorySummary::activityAt)
                        .thenComparing(StoryHistoryResponse.StorySummary::storyId)
                        .reversed())
                .toList();

        int fromIndex = (int) Math.min((long) page * size, filtered.size());
        int toIndex = (int) Math.min((long) fromIndex + size, filtered.size());
        int totalPages = filtered.isEmpty() ? 0 : (filtered.size() + size - 1) / size;

        List<StoryHistoryResponse.StoryTemplateItem> templates = storyTemplateRepository
                .findAllByOrderByIdAsc().stream()
                .map(template -> new StoryHistoryResponse.StoryTemplateItem(
                        template.getId(), template.getTitle(), template.getImageUrl()
                ))
                .toList();
        return new StoryHistoryResponse(
                templates,
                filtered.subList(fromIndex, toIndex),
                page,
                size,
                filtered.size(),
                totalPages
        );
    }

    public StoryHistoryDetailResponse getStoryHistoryDetail(
            Long teacherId,
            Long studentId,
            Long storyId
    ) {
        validateStudentOwner(teacherId, studentId);
        StoryEntity story = findVisibleStory(studentId, storyId);
        StoryContext context = loadContext(studentId, List.of(story));
        List<StoryLineEntity> lines = context.linesByStory().getOrDefault(storyId, List.of());
        Map<Long, StoryChoiceEntity> choices = loadChoices(lines);

        List<StoryHistoryDetailResponse.StoryPage> pages = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            StoryLineEntity line = lines.get(index);
            StoryChoiceEntity choice = choices.get(line.getId());
            String imageUrl = line.getScene().getImageUrl();
            StoryHistoryDetailResponse.ImageGenerationStatus imageStatus =
                    imageUrl == null || imageUrl.isBlank()
                            ? StoryHistoryDetailResponse.ImageGenerationStatus.NOT_REQUESTED
                            : StoryHistoryDetailResponse.ImageGenerationStatus.AVAILABLE;
            StoryHistoryDetailResponse.BranchRecord branchRecord = choice == null
                    ? null
                    : new StoryHistoryDetailResponse.BranchRecord(
                            choice.getId(),
                            storyLineContentService.textOf(line),
                            choice.getContent(),
                            offset(choice.getCreatedAt())
                    );
            pages.add(new StoryHistoryDetailResponse.StoryPage(
                    index + 1,
                    line.getId(),
                    line.getScene().getId(),
                    line.getScene().getSequenceNo(),
                    line.getSequenceNo(),
                    imageStatus == StoryHistoryDetailResponse.ImageGenerationStatus.AVAILABLE
                            ? imageUrl : null,
                    "center",
                    imageStatus,
                    List.of(storyLineContentService.textOf(line)),
                    line.isRequiresBranchInput(),
                    offset(line.getReadAt()),
                    branchRecord
            ));
        }
        return new StoryHistoryDetailResponse(
                toSummary(story, context), pages, pages.size()
        );
    }

    public StoryGazeAnalysisResponse getStoryGazeAnalysis(
            Long teacherId,
            Long studentId,
            Long storyId
    ) {
        validateStudentOwner(teacherId, studentId);
        StoryEntity story = findVisibleStory(studentId, storyId);
        StoryContext context = loadContext(studentId, List.of(story));
        GazeSessionEntity session = context.latestCompletedSessionByStory().get(storyId);
        if (session == null || session.getStatus() != GazeSessionStatus.COMPLETED) {
            throw new ResourceNotFoundException("A completed story gaze session was not found.");
        }
        GazeAnalysisResultEntity result = context.analysisBySession().get(session.getId());
        if (result == null) {
            throw new ResourceNotFoundException("A story gaze analysis result was not found.");
        }

        List<StoryLineEntity> lines = context.linesByStory().getOrDefault(storyId, List.of());
        Map<Long, Integer> pageByLine = new LinkedHashMap<>();
        Map<Long, StoryLineEntity> lineById = new LinkedHashMap<>();
        for (int index = 0; index < lines.size(); index++) {
            pageByLine.put(lines.get(index).getId(), index + 1);
            lineById.put(lines.get(index).getId(), lines.get(index));
        }

        JsonNode regressionNodes = readArray(result.getRegressions());
        Map<Long, StoryGazeAnalysisResponse.PageMetric> metrics = new LinkedHashMap<>();
        for (JsonNode metric : readArray(result.getSentenceMetrics())) {
            Long lineId = nullableLong(metric, "storyLineId");
            Integer pageNo = pageByLine.get(lineId);
            if (lineId == null || pageNo == null || metrics.containsKey(lineId)) {
                continue;
            }
            StoryLineEntity line = lineById.get(lineId);
            List<StoryGazeAnalysisResponse.Regression> regressions = pageRegressions(
                    regressionNodes, pageNo, metric.path("sequenceNo").asInt(pageNo)
            );
            int dwellDuration = metric.path("dwellDurationMs").asInt(0);
            int fixationCount = metric.path("fixationCount").asInt(0);
            metrics.put(lineId, new StoryGazeAnalysisResponse.PageMetric(
                    lineId,
                    pageNo,
                    metric.path("surfaceText").asText(line.getContent()),
                    dwellDuration,
                    fixationCount,
                    metric.path("regressionCount").asInt(regressions.size()),
                    fixationCount == 0 ? null : dwellDuration / fixationCount,
                    metric.path("firstGazeOffsetMs").asInt(0),
                    metric.path("lastGazeOffsetMs").asInt(0),
                    regressions
            ));
        }

        return new StoryGazeAnalysisResponse(
                session.getId(),
                result.getId(),
                session.getCalibrationStatus(),
                offset(session.getStartedAt()),
                offset(session.getEndedAt()),
                result.getTotalVisitedDuration(),
                result.getTotalVisitedCount(),
                result.getReverseReadCount(),
                result.getAvgVisitedDuration(),
                List.copyOf(metrics.values()),
                readNullable(result.getAnalysisMeta())
        );
    }

    private StoryContext loadContext(Long studentId, List<StoryEntity> stories) {
        if (stories.isEmpty()) {
            return StoryContext.empty();
        }
        List<Long> storyIds = stories.stream().map(StoryEntity::getId).toList();
        Map<Long, List<StoryLineEntity>> linesByStory = storyLineRepository
                .findAllByStoryIdInOrderBySequenceNoAsc(storyIds).stream()
                .collect(Collectors.groupingBy(
                        line -> line.getScene().getStory().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<GazeSessionEntity> sessions = gazeSessionRepository
                .findAllByStudentIdAndContentTypeAndStoryIdInOrderByCreatedAtDescIdDesc(
                        studentId, GazeContentType.STORY, storyIds
                );
        Map<Long, GazeSessionEntity> latestSessionByStory = new LinkedHashMap<>();
        sessions.forEach(session -> latestSessionByStory.putIfAbsent(
                session.getStory().getId(), session
        ));
        Map<Long, GazeSessionEntity> latestCompletedSessionByStory = new LinkedHashMap<>();
        sessions.stream()
                .filter(session -> session.getStatus() == GazeSessionStatus.COMPLETED)
                .forEach(session -> latestCompletedSessionByStory.putIfAbsent(
                        session.getStory().getId(), session));
        Map<Long, GazeAnalysisResultEntity> analysisBySession = sessions.isEmpty()
                ? Map.of()
                : gazeAnalysisResultRepository.findAllByGazeSessionIdIn(
                                sessions.stream().map(GazeSessionEntity::getId).toList()
                        ).stream()
                        .collect(Collectors.toMap(
                                result -> result.getGazeSession().getId(),
                                Function.identity(),
                                (first, ignored) -> first
                        ));
        return new StoryContext(linesByStory, latestSessionByStory,
                latestCompletedSessionByStory, analysisBySession);
    }

    private Map<Long, StoryChoiceEntity> loadChoices(List<StoryLineEntity> lines) {
        if (lines.isEmpty()) {
            return Map.of();
        }
        return storyChoiceRepository.findAllByStoryLineIdIn(
                        lines.stream().map(StoryLineEntity::getId).toList()
                ).stream()
                .collect(Collectors.toMap(
                        choice -> choice.getStoryLine().getId(),
                        Function.identity()
                ));
    }

    private StoryHistoryResponse.StorySummary toSummary(
            StoryEntity story,
            StoryContext context
    ) {
        List<StoryLineEntity> lines = context.linesByStory()
                .getOrDefault(story.getId(), List.of());
        List<LocalDateTime> readTimes = lines.stream()
                .map(StoryLineEntity::getReadAt)
                .filter(Objects::nonNull)
                .toList();
        int readCount = readTimes.size();
        int totalCount = lines.size();
        LocalDateTime lastReadAt = readTimes.stream().max(LocalDateTime::compareTo).orElse(null);
        LocalDateTime activityAt = lastReadAt == null
                ? story.getCreatedAt()
                : max(story.getCreatedAt(), lastReadAt);
        LocalDateTime completedAt = totalCount > 0 && readCount == totalCount
                ? lastReadAt : null;
        int progress = totalCount == 0
                ? 0
                : (int) Math.round((double) readCount * 100 / totalCount);
        StoryHistoryResponse.ReadingStatus readingStatus = readCount == 0
                ? StoryHistoryResponse.ReadingStatus.NOT_STARTED
                : readCount == totalCount
                ? StoryHistoryResponse.ReadingStatus.COMPLETED
                : StoryHistoryResponse.ReadingStatus.IN_PROGRESS;
        return new StoryHistoryResponse.StorySummary(
                story.getId(),
                story.getStoryTemplate().getId(),
                story.getStoryTemplate().getTitle(),
                story.getStoryTemplate().getImageUrl(),
                story.getStatus(),
                story.getProgress(),
                offset(story.getCreatedAt()),
                offset(lastReadAt),
                offset(completedAt),
                offset(activityAt),
                readCount,
                totalCount,
                progress,
                readingStatus,
                gazeStatus(story.getId(), context)
        );
    }

    private StoryHistoryResponse.GazeAnalysisStatus gazeStatus(
            Long storyId,
            StoryContext context
    ) {
        GazeSessionEntity session = context.latestSessionByStory().get(storyId);
        if (session == null) {
            return StoryHistoryResponse.GazeAnalysisStatus.NOT_COLLECTED;
        }
        if (session.getStatus() == GazeSessionStatus.FAILED) {
            return StoryHistoryResponse.GazeAnalysisStatus.FAILED;
        }
        if (session.getStatus() == GazeSessionStatus.COMPLETED
                && context.analysisBySession().containsKey(session.getId())) {
            return StoryHistoryResponse.GazeAnalysisStatus.AVAILABLE;
        }
        return StoryHistoryResponse.GazeAnalysisStatus.RUNNING;
    }

    private List<StoryGazeAnalysisResponse.Regression> pageRegressions(
            JsonNode nodes,
            int pageNo,
            int sequenceNo
    ) {
        List<StoryGazeAnalysisResponse.Regression> result = new ArrayList<>();
        for (JsonNode node : nodes) {
            int fromTarget = node.path("fromTargetIndex").asInt(-1);
            int toTarget = node.path("toTargetIndex").asInt(-2);
            boolean samePage = fromTarget == toTarget
                    && (fromTarget == pageNo - 1 || fromTarget == sequenceNo - 1);
            if (samePage) {
                result.add(new StoryGazeAnalysisResponse.Regression(
                        node.path("fromTokenIndex").asInt(0),
                        node.path("toTokenIndex").asInt(0),
                        node.path("offsetMs").asInt(0)
                ));
            }
        }
        return List.copyOf(result);
    }

    private StoryEntity findVisibleStory(Long studentId, Long storyId) {
        StoryEntity story = storyRepository.findByIdAndStudentId(storyId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Story was not found."));
        if (story.getStatus() == StoryStatus.DELETED) {
            throw new ResourceNotFoundException("Story was not found.");
        }
        return story;
    }

    private void validateStudentOwner(Long teacherId, Long studentId) {
        studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Student was not found."));
    }

    private void validateRequest(LocalDate from, LocalDate to, int page, int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must not be later than to.");
        }
        if (page < 0) {
            throw new IllegalArgumentException("page must be zero or greater.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100.");
        }
    }

    private boolean inRange(OffsetDateTime activityAt, LocalDate from, LocalDate to) {
        LocalDate activityDate = activityAt.atZoneSameInstant(SEOUL).toLocalDate();
        return (from == null || !activityDate.isBefore(from))
                && (to == null || !activityDate.isAfter(to));
    }

    private LocalDateTime max(LocalDateTime left, LocalDateTime right) {
        return left.isAfter(right) ? left : right;
    }

    private OffsetDateTime offset(LocalDateTime value) {
        return value == null ? null : value.atZone(SEOUL).toOffsetDateTime();
    }

    private JsonNode readArray(String value) {
        JsonNode node = readNullable(value);
        return node != null && node.isArray() ? node : objectMapper.createArrayNode();
    }

    private JsonNode readNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        } catch (Exception exception) {
            throw new IllegalStateException("Stored gaze analysis JSON could not be parsed.", exception);
        }
    }

    private Long nullableLong(JsonNode node, String field) {
        return node.hasNonNull(field) ? node.path(field).asLong() : null;
    }

    private record StoryContext(
            Map<Long, List<StoryLineEntity>> linesByStory,
            Map<Long, GazeSessionEntity> latestSessionByStory,
            Map<Long, GazeSessionEntity> latestCompletedSessionByStory,
            Map<Long, GazeAnalysisResultEntity> analysisBySession
    ) {
        private static StoryContext empty() {
            return new StoryContext(Map.of(), Map.of(), Map.of(), Map.of());
        }
    }
}
