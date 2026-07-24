package com.iread.backend.story.app.service;

import com.iread.backend.ai.client.AiClient;
import com.iread.backend.ai.dto.req.*;
import com.iread.backend.ai.dto.res.GenerateStoryResponse;
import com.iread.backend.ai.dto.res.GeneratedStoryLine;
import com.iread.backend.ai.exception.AiClientException;
import com.iread.backend.story.app.dto.req.StoryChoiceRequest;
import com.iread.backend.story.app.dto.res.*;
import com.iread.backend.story.domain.*;
import com.iread.backend.story.repository.*;
import com.iread.backend.student.domain.StudentEntity;
import com.iread.backend.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoryService {

    private static final int STORY_SCHEMA_VERSION = 1;

    private final StudentRepository studentRepository;
    private final StoryTemplateRepository storyTemplateRepository;
    private final StoryRepository storyRepository;
    private final StoryLineRepository storyLineRepository;
    private final AiClient aiClient;

    public StoryShelfResponse getStoryShelf(Long teacherId, Long studentId) {
        validateStudentOwner(teacherId, studentId);

        List<StoryShelfResponse.StoryItem> stories = storyRepository
                .findAllByStudentIdAndStatusNotOrderByCreatedAtDesc(studentId, StoryStatus.DELETED)
                .stream()
                .map(story -> new StoryShelfResponse.StoryItem(
                        story.getId(),
                        studentId,
                        story.getStoryTemplate().getId(),
                        story.getCreatedAt(),
                        story.getStatus()
                ))
                .toList();

        List<StoryShelfResponse.StoryTemplateItem> templates = storyTemplateRepository.findAllByOrderByIdAsc()
                .stream()
                .map(template -> new StoryShelfResponse.StoryTemplateItem(template.getId(), template.getTitle()))
                .toList();

        return new StoryShelfResponse(stories, templates);
    }

    public StoryTemplateResponse getStoryTemplate(Long teacherId, Long studentId, Long storyTemplateId) {
        validateStudentOwner(teacherId, studentId);
        StoryTemplateEntity template = findTemplate(storyTemplateId);
        return new StoryTemplateResponse(template.getId(), template.getTitle(), template.getContent());
    }

    public StoryResumeResponse resumeStory(Long teacherId, Long studentId, Long storyId) {
        StoryEntity story = findOwnedStory(teacherId, studentId, storyId);

        Optional<StoryLineEntity> resumeLine = storyLineRepository
                .findFirstByStoryIdAndReadAtIsNullOrderBySequenceNoAsc(storyId);
        if (resumeLine.isEmpty() && story.isInProgress()) {
            resumeLine = storyLineRepository.findFirstByStoryIdOrderBySequenceNoDesc(storyId)
                    .filter(StoryLineEntity::isRequiresBranchInput);
        }

        return new StoryResumeResponse(story.getId(), story.getStatus(), resumeLine.map(this::toLineResponse).orElse(null));
    }

    @Transactional
    public StoryLineResponse getStoryLine(Long teacherId, Long studentId, Long storyId, Long storyLineId) {
        StoryEntity story = findOwnedStory(teacherId, studentId, storyId);
        StoryLineEntity line = findLine(story.getId(), storyLineId);
        line.markRead(LocalDateTime.now());
        return toLineResponse(line);
    }

    public List<StoryLineResponse> getStoryLines(Long teacherId, Long studentId, Long storyId) {
        StoryEntity story = findOwnedStory(teacherId, studentId, storyId);
        return storyLineRepository.findAllByStoryIdOrderBySequenceNoAsc(story.getId())
                .stream()
                .map(this::toLineResponse)
                .toList();
    }

    @Transactional
    public StorySessionResponse startStory(Long teacherId, Long studentId, Long storyTemplateId) {
        StudentEntity student = findStudentOwner(teacherId, studentId);
        StoryTemplateEntity template = findTemplate(storyTemplateId);
        StoryEntity story = storyRepository.saveAndFlush(new StoryEntity(student, template));

        String requestId = UUID.randomUUID().toString();
        GenerateStoryResponse generated = aiClient.generateStory(new GenerateStoryRequest(
                requestId,
                story.getId(),
                student.getId(),
                STORY_SCHEMA_VERSION,
                story.getProgress(),
                toTemplateData(template)
        ));

        appendGeneratedLines(story, null, 1, generated);
        updateProgress(story, generated);

        return new StorySessionResponse(
                story.getId(), student.getId(), template.getId(), story.getCreatedAt(), story.getStatus()
        );
    }

    @Transactional
    public StoryChoiceResponse chooseStoryDirection(Long teacherId, Long studentId, Long storyId,
                                                    Long storyLineId, StoryChoiceRequest request) {
        StoryEntity story = findOwnedStory(teacherId, studentId, storyId);
        if (!story.isInProgress()) {
            throw new IllegalArgumentException("진행 중인 스토리에서만 분기 입력을 제출할 수 있습니다.");
        }

        StoryLineEntity selectedLine = findLine(story.getId(), storyLineId);
        StoryLineEntity lastLine = storyLineRepository.findFirstByStoryIdOrderBySequenceNoDesc(story.getId())
                .orElseThrow(() -> new IllegalArgumentException("스토리 대사를 찾을 수 없습니다."));
        if (!Objects.equals(selectedLine.getId(), lastLine.getId())) {
            throw new IllegalArgumentException("현재 마지막 분기 장면에만 답할 수 있습니다.");
        }
        if (!selectedLine.isRequiresBranchInput()) {
            throw new IllegalArgumentException("분기 입력이 필요한 장면이 아닙니다.");
        }
        if (selectedLine.getReadAt() == null) {
            throw new IllegalArgumentException("장면을 읽은 후 선택지를 제출할 수 있습니다.");
        }
        List<StoryLineEntity> historyLines = storyLineRepository
                .findAllByStoryIdOrderBySequenceNoAsc(story.getId());

        String requestId = UUID.randomUUID().toString();
        GenerateStoryResponse generated = aiClient.continueStory(new ContinueStoryRequest(
                requestId,
                story.getId(),
                studentId,
                STORY_SCHEMA_VERSION,
                story.getProgress(),
                toTemplateData(story.getStoryTemplate()),
                selectedLine.getId(),
                request.content(),
                historyLines.stream().map(this::toHistoryLine).toList()
        ));

        List<StoryLineEntity> generatedLines = appendGeneratedLines(
                story, selectedLine, selectedLine.getSequenceNo() + 1, generated
        );
        updateProgress(story, generated);

        return new StoryChoiceResponse(
                selectedLine.getId(),
                story.getStatus(),
                generatedLines.stream().map(this::toLineResponse).toList()
        );
    }

    private List<StoryLineEntity> appendGeneratedLines(StoryEntity story, StoryLineEntity previousLine,
                                                       int startSequence, GenerateStoryResponse response) {
        validateGeneratedSegment(response);
        List<StoryLineEntity> lines = new ArrayList<>();
        StoryLineEntity previous = previousLine;
        for (int index = 0; index < response.lines().size(); index++) {
            GeneratedStoryLine generated = response.lines().get(index);
            StoryLineEntity line = new StoryLineEntity(
                    previous,
                    story,
                    null,
                    generated.requiresBranchInput(),
                    generated.content(),
                    startSequence + index
            );
            lines.add(line);
            previous = line;
        }
        return storyLineRepository.saveAllAndFlush(lines);
    }

    private void validateGeneratedSegment(GenerateStoryResponse response) {
        if (response.lines().isEmpty()) {
            throw new AiClientException("AI 서버가 생성한 스토리 대사가 없습니다.");
        }
        for (int index = 0; index < response.lines().size(); index++) {
            GeneratedStoryLine line = response.lines().get(index);
            if (line.content() == null || line.content().isBlank()) {
                throw new AiClientException("AI 서버가 빈 스토리 대사를 반환했습니다.");
            }
            if (index < response.lines().size() - 1 && line.requiresBranchInput()) {
                throw new AiClientException("AI 서버 응답의 분기 입력은 생성 구간 마지막에만 올 수 있습니다.");
            }
        }

        boolean lastRequiresBranchInput = response.lines().getLast().requiresBranchInput();
        if (response.completed() == lastRequiresBranchInput) {
            throw new AiClientException("AI 서버 응답의 완료 상태와 마지막 분기 입력 상태가 일치하지 않습니다.");
        }
    }

    private void updateProgress(StoryEntity story, GenerateStoryResponse response) {
        if (response.nextProgress() < story.getProgress() || response.nextProgress() > 100) {
            throw new AiClientException("AI 서버 응답의 nextProgress가 유효하지 않습니다.");
        }
        if (response.completed() != (response.nextProgress() == 100)) {
            throw new AiClientException("AI 서버 응답의 완료 상태와 진행률이 일치하지 않습니다.");
        }
        story.updateProgress(response.nextProgress());
    }

    private StoryTemplateData toTemplateData(StoryTemplateEntity template) {
        return new StoryTemplateData(template.getId(), template.getTitle(), template.getContent());
    }

    private StoryHistoryLine toHistoryLine(StoryLineEntity line) {
        return new StoryHistoryLine(
                line.getId(),
                line.getContent(),
                line.isRequiresBranchInput()
        );
    }

    private StoryLineResponse toLineResponse(StoryLineEntity line) {
        return new StoryLineResponse(
                line.getId(),
                line.getPreviousStoryLine() == null ? null : line.getPreviousStoryLine().getId(),
                line.getStory().getId(),
                line.getImageUrl(),
                line.isRequiresBranchInput(),
                line.getContent(),
                line.getSequenceNo(),
                line.getCreatedAt(),
                line.getReadAt()
        );
    }

    private StoryTemplateEntity findTemplate(Long storyTemplateId) {
        return storyTemplateRepository.findById(storyTemplateId)
                .orElseThrow(() -> new IllegalArgumentException("스토리 템플릿을 찾을 수 없습니다."));
    }

    private StoryEntity findOwnedStory(Long teacherId, Long studentId, Long storyId) {
        validateStudentOwner(teacherId, studentId);
        return storyRepository.findByIdAndStudentId(storyId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("스토리를 찾을 수 없습니다."));
    }

    private StoryLineEntity findLine(Long storyId, Long storyLineId) {
        return storyLineRepository.findByIdAndStoryId(storyLineId, storyId)
                .orElseThrow(() -> new IllegalArgumentException("스토리 대사를 찾을 수 없습니다."));
    }

    private StudentEntity findStudentOwner(Long teacherId, Long studentId) {
        return studentRepository.findByIdAndTeacherId(studentId, teacherId)
                .orElseThrow(() -> new IllegalArgumentException("학생을 찾을 수 없습니다."));
    }

    private void validateStudentOwner(Long teacherId, Long studentId) {
        findStudentOwner(teacherId, studentId);
    }
}
