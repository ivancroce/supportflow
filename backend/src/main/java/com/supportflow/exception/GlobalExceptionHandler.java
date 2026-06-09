package com.supportflow.exception;

import com.supportflow.escalation.EscalationNotConfiguredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(EscalationNotConfiguredException.class)
    ProblemDetail handleEscalationNotConfigured(EscalationNotConfiguredException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setProperty("missingTargets", ex.getMissingTargets());
        return problem;
    }
}
