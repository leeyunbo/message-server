package com.readtimeout.core.domain.model;

/**
 * PublishStatus Enum
 *
 * 메시지 발행 결과 상태를 표현하는 열거형.
 * 문자열 하드코딩 대신 타입 안전성을 제공.
 */
public enum PublishStatus {
    OK("ok"),
    ERROR("error");

    private final String value;

    PublishStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
