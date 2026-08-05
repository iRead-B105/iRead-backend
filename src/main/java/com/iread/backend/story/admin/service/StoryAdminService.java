package com.iread.backend.story.admin.service;

import com.iread.backend.exception.ResourceNotFoundException;
import com.iread.backend.exception.ConflictException;
import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.req.GenerateImageRequest;
import com.iread.backend.ai.dto.res.GeneratedStoryBranchOption;
import com.iread.backend.ai.dto.res.GeneratedStoryBranchPrompt;
import com.iread.backend.global.storage.FileStorage;
import com.iread.backend.global.storage.LoadedFile;
import com.iread.backend.story.admin.domain.StoryPageEditAuditEntity;
import com.iread.backend.story.admin.dto.req.StoryPageUpdateRequest;
import com.iread.backend.story.admin.dto.res.StoryPageEditResponse;
import com.iread.backend.story.admin.repository.StoryPageEditAuditRepository;
import com.iread.backend.gaze.domain.GazeAnalysisResultEntity;
import com.iread.backend.gaze.domain.GazeContentType;
import com.iread.backend.gaze.domain.GazeSessionEntity;
import com.iread.backend.gaze.domain.GazeSessionStatus;
import com.iread.backend.gaze.app.service.GazeDataStorage;
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
import com.iread.backend.story.repository.StorySceneRepository;
import com.iread.backend.story.repository.StoryTemplateRepository;
import com.iread.backend.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

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
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryAdminService {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?。？！])\\s*");

    private final StudentRepository studentRepository;
    private final StoryRepository storyRepository;
    private final StorySceneRepository storySceneRepository;
    private final StoryTemplateRepository storyTemplateRepository;
    private final StoryLineRepository storyLineRepository;
    private final StoryChoiceRepository storyChoiceRepository;
    private final GazeSessionRepository gazeSessionRepository;
    private final GazeAnalysisResultRepository gazeAnalysisResultRepository;
    private final GazeDataStorage gazeDataStorage;
    private final StoryGazeWordAnalysisService storyGazeWordAnalysisService;
    private final StoryLineContentService storyLineContentService;
    private final ObjectMapper objectMapper;
    private final StoryPageEditAuditRepository storyPageEditAuditRepository;
    private final AiClient aiClient;
    private final FileStorage fileStorage;

    public LoadedFile getStoryImage(
            Long teacherId,
            Long studentId,
            Long storyId,
            String fileName
    ) {
        validateStudentOwner(teacherId, studentId);
        findVisibleStory(studentId, storyId);
        if (fileName == null || !fileName.matches("[0-9a-f-]{36}\\.(png|jpg|jpeg)")) {
            throw new IllegalArgumentException("올바르지 않은 이미지 파일 이름입니다.");
        }
        if (!storySceneRepository.existsByStoryIdAndImageUrlEndingWith(storyId, "/" + fileName)) {
            throw new ResourceNotFoundException("이야기 이미지를 찾을 수 없습니다.");
        }
        return fileStorage.load(fileName);
    }

    @Transactional
    public StoryPageEditResponse updateUnreadPage(
            Long teacherId, Long studentId, Long storyId, Long storyLineId,
            StoryPageUpdateRequest request
    ) {
        StoryLineEntity line = editableLine(teacherId, studentId, storyId, storyLineId, request.revision());
        if (request.subtitle() == null && request.body() == null && request.choices() == null) {
            throw new IllegalArgumentException("수정할 소제목, 본문 또는 선택지가 필요합니다.");
        }
        String before = editSnapshot(line);
        if (request.body() != null) {
            validateThreeSentenceBody(request.body());
            line.updateContent(storyLineContentService.buildContent(request.body().strip()));
        }
        if (request.subtitle() != null || request.choices() != null) {
            if (!line.isRequiresBranchInput()) {
                throw new IllegalArgumentException("분기 페이지에서만 소제목과 선택지를 수정할 수 있습니다.");
            }
            GeneratedStoryBranchPrompt current = branchPromptOf(line);
            String subtitle = request.subtitle() == null ? current.subtitle() : request.subtitle().strip();
            List<String> labels = request.choices() == null
                    ? current.options().stream().map(GeneratedStoryBranchOption::label).toList()
                    : request.choices().stream().map(String::strip).toList();
            validateBranch(subtitle, labels);
            line.updateBranchPrompt(writeJson(new GeneratedStoryBranchPrompt(
                    subtitle,
                    List.of(
                            new GeneratedStoryBranchOption(1, labels.get(0)),
                            new GeneratedStoryBranchOption(2, labels.get(1)),
                            new GeneratedStoryBranchOption(3, labels.get(2))
                    )
            )));
        }
        line.incrementRevision();
        storyLineRepository.saveAndFlush(line);
        saveAudit(line, teacherId, "CONTENT", before, editSnapshot(line));
        return toEditResponse(line);
    }

    @Transactional
    public StoryPageEditResponse uploadUnreadPageImage(
            Long teacherId, Long studentId, Long storyId, Long storyLineId,
            Long revision, MultipartFile image
    ) {
        StoryLineEntity line = editableLine(teacherId, studentId, storyId, storyLineId, revision);
        String before = editSnapshot(line);
        var stored = fileStorage.store(image);
        line.getScene().updateImageUrl(stored.url());
        line.incrementRevision();
        storyLineRepository.saveAndFlush(line);
        saveAudit(line, teacherId, "IMAGE_UPLOAD", before, editSnapshot(line));
        return toEditResponse(line);
    }

    @Transactional
    public StoryPageEditResponse regenerateUnreadPageImage(
            Long teacherId, Long studentId, Long storyId, Long storyLineId, Long revision
    ) {
        StoryLineEntity line = editableLine(teacherId, studentId, storyId, storyLineId, revision);
        String before = editSnapshot(line);
        String requestId = "teacher-story-image-" + storyLineId + "-" + revision;
        var generated = aiClient.generateImage(new GenerateImageRequest(
                requestId,
                storyLineContentService.textOf(line),
                line.getStory().getStoryTemplate().getId()
        ));
        line.getScene().updateImageUrl(generated.imageUrl());
        line.incrementRevision();
        storyLineRepository.saveAndFlush(line);
        saveAudit(line, teacherId, "IMAGE_REGENERATE", before, editSnapshot(line));
        return toEditResponse(line);
    }

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
            GeneratedStoryBranchPrompt generatedBranch = line.isRequiresBranchInput()
                    ? branchPromptOf(line)
                    : null;
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
                    branchRecord,
                    line.getRevision() == null ? 0L : line.getRevision(),
                    line.getReadAt() == null,
                    generatedBranch == null ? null : generatedBranch.subtitle(),
                    generatedBranch == null ? List.of() : generatedBranch.options().stream()
                            .map(GeneratedStoryBranchOption::label).toList()
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
        // 페이지마다 세션이 끝날 수 있으므로 최신 세션이 아니라, 분석까지 완료된
        // 모든 페이지 세션을 모아야 이전 페이지 리플레이가 사라지지 않는다.
        List<GazeAnalysisResultEntity> results = context.analysisBySession().values().stream()
                .filter(result -> result.getGazeSession().getStory().getId().equals(storyId))
                .sorted(Comparator.comparing(GazeAnalysisResultEntity::getCreatedAt).reversed())
                .toList();
        if (results.isEmpty()) {
            throw new ResourceNotFoundException("A story gaze analysis result was not found.");
        }
        GazeAnalysisResultEntity result = results.getFirst();
        GazeSessionEntity session = result.getGazeSession();

        List<StoryLineEntity> lines = context.linesByStory().getOrDefault(storyId, List.of());
        Map<Long, Integer> pageByLine = new LinkedHashMap<>();
        Map<Long, StoryLineEntity> lineById = new LinkedHashMap<>();
        for (int index = 0; index < lines.size(); index++) {
            pageByLine.put(lines.get(index).getId(), index + 1);
            lineById.put(lines.get(index).getId(), lines.get(index));
        }

        List<GazeSessionEntity> completedSessions = results.stream()
                .map(GazeAnalysisResultEntity::getGazeSession)
                .toList();
        List<JsonNode> storedReplayPayloads = loadStoryReplayPayloads(completedSessions);
        List<StoryGazeWordAnalysisService.Page> analysisPages = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            StoryLineEntity line = lines.get(index);
            analysisPages.add(new StoryGazeWordAnalysisService.Page(
                    line.getId(),
                    index + 1,
                    storyLineContentService.textOf(line)
            ));
        }
        StoryGazeWordAnalysisService.Analysis wordAnalysis =
                storyGazeWordAnalysisService.analyze(analysisPages, storedReplayPayloads);

        Map<Long, StoryGazeAnalysisResponse.PageMetric> metrics = new LinkedHashMap<>();
        for (GazeAnalysisResultEntity pageResult : results) {
            JsonNode regressionNodes = readArray(pageResult.getRegressions());
            for (JsonNode metric : readArray(pageResult.getSentenceMetrics())) {
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
        }

        return new StoryGazeAnalysisResponse(
                session.getId(),
                result.getId(),
                session.getCalibrationStatus(),
                offset(session.getStartedAt()),
                offset(session.getEndedAt()),
                results.stream().mapToInt(item -> item.getTotalVisitedDuration()).sum(),
                results.stream().mapToInt(item -> item.getTotalVisitedCount()).sum(),
                results.stream().mapToInt(item -> item.getReverseReadCount()).sum(),
                totalAverageFixationTime(results),
                List.copyOf(metrics.values()),
                wordAnalysis.wordMetrics(),
                storyReplay(storedReplayPayloads, wordAnalysis.events()),
                StoryGazeAnalysisResponse.AnalysisMeta.storyGazeWordV1()
        );
    }

    private StoryLineEntity editableLine(
            Long teacherId, Long studentId, Long storyId, Long storyLineId, Long revision
    ) {
        validateStudentOwner(teacherId, studentId);
        findVisibleStory(studentId, storyId);
        StoryLineEntity line = storyLineRepository.findByIdAndStoryIdForUpdate(storyLineId, storyId)
                .orElseThrow(() -> new ResourceNotFoundException("Story page was not found."));
        if (line.getReadAt() != null) {
            throw new ConflictException("이미 읽은 이야기 페이지는 수정할 수 없습니다.");
        }
        long currentRevision = line.getRevision() == null ? 0L : line.getRevision();
        if (revision == null || revision != currentRevision) {
            throw new ConflictException("이야기 페이지가 다른 요청에서 먼저 수정되었습니다.");
        }
        return line;
    }

    private void validateThreeSentenceBody(String body) {
        String text = body.strip();
        List<String> sentences = List.of(SENTENCE_END.split(text)).stream()
                .map(String::strip)
                .filter(value -> !value.isBlank())
                .toList();
        if (sentences.size() != 3) {
            throw new IllegalArgumentException("이야기 본문은 정확히 3문장이어야 합니다.");
        }
        for (String sentence : sentences) {
            long syllables = sentence.chars().filter(value -> value >= '가' && value <= '힣').count();
            if (syllables < 10 || syllables > 22) {
                throw new IllegalArgumentException("각 문장은 한글 10~22음절이어야 합니다.");
            }
        }
    }

    private void validateBranch(String subtitle, List<String> labels) {
        if (subtitle.isBlank() || subtitle.length() > 40) {
            throw new IllegalArgumentException("분기 소제목은 1~40자여야 합니다.");
        }
        if (labels.size() != 3 || labels.stream().anyMatch(String::isBlank)
                || labels.stream().distinct().count() != 3) {
            throw new IllegalArgumentException("서로 다른 분기 선택지 3개가 필요합니다.");
        }
    }

    private GeneratedStoryBranchPrompt branchPromptOf(StoryLineEntity line) {
        if (line.getBranchPrompt() == null || line.getBranchPrompt().isBlank()) {
            String text = storyLineContentService.textOf(line);
            return new GeneratedStoryBranchPrompt(
                    text.substring(0, Math.min(text.length(), 40)),
                    List.of()
            );
        }
        try {
            return objectMapper.readValue(line.getBranchPrompt(), GeneratedStoryBranchPrompt.class);
        } catch (Exception exception) {
            throw new IllegalStateException("저장된 분기 선택지를 읽을 수 없습니다.", exception);
        }
    }

    private StoryPageEditResponse toEditResponse(StoryLineEntity line) {
        GeneratedStoryBranchPrompt branch = line.isRequiresBranchInput() ? branchPromptOf(line) : null;
        return new StoryPageEditResponse(
                line.getId(),
                line.getRevision() == null ? 0L : line.getRevision(),
                branch == null ? null : branch.subtitle(),
                storyLineContentService.textOf(line),
                branch == null ? List.of() : branch.options().stream()
                        .map(GeneratedStoryBranchOption::label).toList(),
                line.getScene().getImageUrl(),
                line.getReadAt() == null
        );
    }

    private String editSnapshot(StoryLineEntity line) {
        return writeJson(Map.of(
                "body", storyLineContentService.textOf(line),
                "branchPrompt", Objects.toString(line.getBranchPrompt(), ""),
                "imageUrl", Objects.toString(line.getScene().getImageUrl(), ""),
                "revision", line.getRevision() == null ? 0L : line.getRevision()
        ));
    }

    private void saveAudit(StoryLineEntity line, Long teacherId, String editType,
                           String before, String after) {
        storyPageEditAuditRepository.save(new StoryPageEditAuditEntity(
                line.getId(), teacherId, editType, before, after
        ));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("이야기 수정 이력을 JSON으로 만들 수 없습니다.", exception);
        }
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
        String chapterTitle = lines.stream()
                .filter(StoryLineEntity::isRequiresBranchInput)
                .map(this::branchPromptOf)
                .map(GeneratedStoryBranchPrompt::subtitle)
                .filter(subtitle -> !subtitle.isBlank())
                .reduce((first, second) -> second)
                .orElse(null);
        return new StoryHistoryResponse.StorySummary(
                story.getId(),
                story.getStoryTemplate().getId(),
                story.getStoryTemplate().getTitle(),
                chapterTitle,
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
        // 페이지마다 별도 세션을 만들기 때문에, 페이지를 벗어나는 순간 샘플이 없는
        // 신규 세션이 FAILED가 되더라도 기존에 완성된 페이지 분석을 가리면 안 된다.
        // 교사 화면은 한 이야기의 완료된 페이지 리플레이를 우선 표시한다.
        boolean hasCompletedPageAnalysis = context.analysisBySession().values().stream()
                .anyMatch(result -> result.getGazeSession().getStory().getId().equals(storyId));
        if (hasCompletedPageAnalysis) {
            return StoryHistoryResponse.GazeAnalysisStatus.AVAILABLE;
        }
        if (session.getStatus() == GazeSessionStatus.FAILED) {
            return StoryHistoryResponse.GazeAnalysisStatus.FAILED;
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

    private Integer totalAverageFixationTime(List<GazeAnalysisResultEntity> results) {
        int count = results.stream().mapToInt(item -> item.getTotalVisitedCount()).sum();
        if (count == 0) {
            return null;
        }
        return results.stream().mapToInt(item -> item.getTotalVisitedDuration()).sum() / count;
    }

    private List<JsonNode> loadStoryReplayPayloads(List<GazeSessionEntity> sessions) {
        List<JsonNode> payloads = new ArrayList<>();
        for (GazeSessionEntity session : sessions) {
            if (session.getDataUrl() == null || session.getDataUrl().isBlank()) {
                continue;
            }
            JsonNode stored = readNullable(gazeDataStorage.load(session.getDataUrl()));
            if (stored != null && stored.isObject()) {
                payloads.add(stored);
            }
        }
        return List.copyOf(payloads);
    }

    private JsonNode storyReplay(
            List<JsonNode> storedPayloads,
            List<StoryGazeAnalysisResponse.ReplayEvent> events
    ) {
        ObjectNode replay = objectMapper.createObjectNode();
        ArrayNode words = objectMapper.createArrayNode();
        ArrayNode samples = objectMapper.createArrayNode();
        for (JsonNode stored : storedPayloads) {
            JsonNode data = unwrapRawData(stored);
            if (data == null || !data.isObject()) {
                continue;
            }
            JsonNode sourceWords = data.path("replayWords").isArray()
                    ? data.path("replayWords")
                    : data.path("words");
            if (sourceWords.isArray()) {
                sourceWords.forEach(word -> words.add(sanitizeReplayWord(word)));
            }
            JsonNode sourceSamples = data.path("samples");
            if (!sourceSamples.isArray()) {
                continue;
            }
            for (JsonNode sourceSample : sourceSamples) {
                if (!sourceSample.isObject()) {
                    continue;
                }
                ObjectNode sample = sanitizeReplaySample(sourceSample);
                if (!sample.hasNonNull("questionNumber") && sample.hasNonNull("pageNo")) {
                    sample.set("questionNumber", sample.path("pageNo").deepCopy());
                }
                samples.add(sample);
            }
        }
        if (words.isEmpty() && samples.isEmpty() && events.isEmpty()) {
            return null;
        }
        replay.set("words", words);
        replay.set("samples", samples);
        replay.set("events", objectMapper.valueToTree(events));
        return replay;
    }

    private JsonNode unwrapRawData(JsonNode stored) {
        JsonNode current = stored;
        for (int depth = 0;
             depth < 4 && current != null && current.path("rawData").isObject();
             depth++) {
            current = current.path("rawData");
        }
        return current;
    }

    private ObjectNode sanitizeReplayWord(JsonNode source) {
        return copyFields(source, List.of(
                "questionNo",
                "storyLineId",
                "targetIndex",
                "tokenIndex",
                "text",
                "dwellMs",
                "visitCount",
                "skipped",
                "regressionCount",
                "firstSeenMs",
                "lastSeenMs"
        ));
    }

    private ObjectNode sanitizeReplaySample(JsonNode source) {
        return copyFields(source, List.of(
                "questionNumber",
                "pageNo",
                "storyLineId",
                "targetIndex",
                "tokenIndex",
                "text",
                "capturedAtMs"
        ));
    }

    private ObjectNode copyFields(JsonNode source, List<String> fields) {
        ObjectNode target = objectMapper.createObjectNode();
        for (String field : fields) {
            if (source.has(field)) {
                target.set(field, source.path(field).deepCopy());
            }
        }
        return target;
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
