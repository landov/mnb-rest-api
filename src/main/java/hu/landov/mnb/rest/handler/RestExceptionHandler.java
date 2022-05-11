package hu.landov.mnb.rest.handler;

import hu.landov.mnb.rest.dto.error.ErrorDetail;
import hu.landov.mnb.rest.exception.InvalidRequestException;
import hu.landov.mnb.rest.exception.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@ControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<?> handleInvalidDateException(
            InvalidRequestException invalidRequestException,
            HttpServletRequest request
    ) {
        ErrorDetail errorDetail = buildErrorDetail(
                HttpStatus.BAD_REQUEST,
                "Malformed Request",
                invalidRequestException,
                request.getServletPath()
        );
        return new ResponseEntity<>(errorDetail, null, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> handleResourceNotFoundException(
            ResourceNotFoundException resourceNotFoundException,
            HttpServletRequest request
    ) {
        ErrorDetail errorDetail = buildErrorDetail(
                HttpStatus.NOT_FOUND,
                "Resource Not Found",
                resourceNotFoundException,
                request.getServletPath()
        );
        return new ResponseEntity<>(errorDetail, null, HttpStatus.NOT_FOUND);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers,
            HttpStatus status, WebRequest request) {
        ErrorDetail errorDetail = buildErrorDetail(status,"Message Not Readable",ex,request.getContextPath());
        return handleExceptionInternal(ex, errorDetail, headers, status, request);
    }

    private ErrorDetail buildErrorDetail(
            HttpStatus status,
            String title,
            Exception exception,
            String path
    ) {
        ErrorDetail errorDetail = new ErrorDetail();
        errorDetail.setTimeStamp((new Date().getTime()));
        errorDetail.setStatus(status.value());
        errorDetail.setTitle(title);
        errorDetail.setPath(path);
        errorDetail.setDetail(exception.getMessage());
        //TODO is it safe to include?
        errorDetail.setDeveloperMessage(exception.getClass().getName());
        return errorDetail;
    }
}
