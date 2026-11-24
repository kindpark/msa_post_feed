package com.example.login.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * API 에러의 단일 목록. 응답은 항상 { "code", "message", "status" } 형식으로 나간다.
 * code 는 프론트엔드가 분기할 때 쓰는 값이라 enum 이름과 같게 맞춘다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 BAD REQUEST
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "입력값이 올바르지 않습니다."),
    INVALID_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "INVALID_CURRENT_PASSWORD", "현재 비밀번호가 일치하지 않습니다."),
    SELF_FOLLOW(HttpStatus.BAD_REQUEST, "SELF_FOLLOW", "자기 자신을 팔로우할 수 없습니다."),
    TOO_MANY_IDS(HttpStatus.BAD_REQUEST, "TOO_MANY_IDS", "한 번에 최대 100명까지 조회할 수 있습니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "INVALID_NICKNAME", "닉네임은 공백 없이 1~50자여야 합니다."),

    INVALID_IMAGE(HttpStatus.BAD_REQUEST, "INVALID_IMAGE", "PNG, JPG, GIF, WEBP 이미지 파일만 올릴 수 있습니다."),

    // 401 UNAUTHORIZED
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 일치하지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요하거나 인증 자격 증명이 유효하지 않습니다."),

    // 403 FORBIDDEN: 로그인은 했지만 이 자원에 대한 권한이 없음 (예: 남의 게시글 수정·삭제)
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "이 작업을 수행할 권한이 없습니다."),

    // 404 NOT FOUND
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "존재하지 않는 회원입니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "존재하지 않는 게시글입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 경로를 찾을 수 없습니다."),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMAGE_NOT_FOUND", "존재하지 않는 이미지입니다."),
   // 405
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),

    // 409 CONFLICT
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 가입된 이메일입니다."),
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "이미 존재하는 데이터입니다."),
    DUPLICATE_HANDLE(HttpStatus.CONFLICT, "DUPLICATE_HANDLE", "이미 존재하는 핸들입니다."),
    LOCK_CONFLICT(HttpStatus.CONFLICT, "LOCK_CONFLICT", "다른 요청과 충돌했습니다. 잠시 후 다시 시도해 주세요."),

    // 413
    FILE_TOO_LARGE(HttpStatus.valueOf(413), "FILE_TOO_LARGE", "이미지는 5MB 이하만 올릴 수 있습니다."),

    // 500 INTERNAL SERVER ERROR
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),

    // X 연동 (API_SPEC §14.3)
    X_NOT_CONNECTED(HttpStatus.CONFLICT, "X_NOT_CONNECTED", "X 계정이 연결되어 있지 않습니다."),
    X_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "X_TOKEN_EXPIRED", "X 인증이 만료되었습니다. 다시 연결해 주세요."),
    X_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "X_UNAVAILABLE", "X 서비스를 일시적으로 이용할 수 없습니다."),
    X_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "X_RATE_LIMITED", "오늘 X 호출 한도를 초과했습니다."),

    // 503
    UPSTREAM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "UPSTREAM_UNAVAILABLE", "연결된 서비스가 일시적으로 응답할수 없는 상태입니다.");
    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
