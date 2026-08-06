package com.iread.backend.exception;

/**
 * 같은 분기의 다음 장면이 이미 생성되고 있을 때 던진다.
 *
 * <p>다른 충돌과 달리 아이에게는 오류가 아니라 "이야기를 만들고 있어요"로 보여야 하므로
 * ConflictException 과 코드를 구분한다.
 */
public class StoryBranchGeneratingException extends RuntimeException {

    public StoryBranchGeneratingException(String message) {
        super(message);
    }
}
