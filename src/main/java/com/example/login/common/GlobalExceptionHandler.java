package com.example.login.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 컨트롤러에서 던져진 예외를 { code, message, status } 형식의 응답으로 바꾼다.
 * 스택 트레이스 같은 내부 정보는 응답에 넣지 않고 서버 로그에만 남긴다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 서비스에서 의도적으로 던진 예외 (로그인 실패, 중복 이메일 등) */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
        return toResponse(ErrorResponse.of(e.getErrorCode()));
    }
    // 경로 값 문자로 받았을때 에러 처리
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e){
        return toResponse(ErrorResponse.of(ErrorCode.VALIDATION_ERROR,
                e.getName() + ": 형식이 올바르지 않습니다."));
    }
    /** @Valid 검증 실패: 첫 번째로 틀린 필드를 메시지에 담는다. 예) "email: 올바른 형식의 이메일 주소여야 합니다" */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        FieldError field = e.getBindingResult().getFieldError();
        String message = field == null
                ? ErrorCode.VALIDATION_ERROR.getMessage()
                : field.getField() + ": " + field.getDefaultMessage();
        return toResponse(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, message));
    }

    /** 요청 본문이 JSON 이 아니거나 형식이 깨진 경우 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return toResponse(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "요청 본문 형식이 올바르지 않습니다."));
    }

    /** 파일 크기 제한 초과 (spring.servlet.multipart.max-file-size / max-request-size) */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException e) {
        return toResponse(ErrorResponse.of(ErrorCode.FILE_TOO_LARGE));
    }

    /** 필수 파일·파라미터가 없거나 multipart 요청이 아닌 경우 */
    @ExceptionHandler({MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            MultipartException.class})
    public ResponseEntity<ErrorResponse> handleMissingPart(Exception e) {
        return toResponse(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "요청에 필요한 파일이나 값이 없습니다."));
    }

    /** DB 제약 위반 (예: 동시에 같은 이메일로 가입해 unique 제약에 걸린 경우) */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException e) {
        log.warn("DB 제약 위반: {}", e.getMostSpecificCause().getMessage());
        return toResponse(ErrorResponse.of(ErrorCode.DUPLICATE_RESOURCE));
    }

    /** 존재하지 않는 경로 */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
        return toResponse(ErrorResponse.of(ErrorCode.NOT_FOUND));
    }

    /** 그 밖의 예상하지 못한 예외: 원인은 로그에만 남기고 응답에는 일반 메시지만 보낸다 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리되지 않은 예외", e);
        return toResponse(ErrorResponse.of(ErrorCode.INTERNAL_ERROR));
    }
    /** 데드락 처리 **/
    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleLockingFailure(PessimisticLockingFailureException e) {
        log.warn("데드락 감지, 원인 : {}", e.getMostSpecificCause().getMessage());
        return toResponse(ErrorResponse.of(ErrorCode.LOCK_CONFLICT));
    }

    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e){
        log.warn("지원하지 않는 HTTP 메서드 요청, 요청 메서드 : {}, 허용 메서드 : {}", e.getMethod(), e.getSupportedMethods());
        return toResponse(ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED));
    }

    private ResponseEntity<ErrorResponse> toResponse(ErrorResponse body) {
        return ResponseEntity.status(body.status()).body(body);
    }

}
