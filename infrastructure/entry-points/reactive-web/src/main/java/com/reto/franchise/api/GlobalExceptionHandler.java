package com.reto.franchise.api;

import com.reto.franchise.model.exception.BranchAlreadyExistsException;
import com.reto.franchise.model.exception.BranchNotFoundException;
import com.reto.franchise.model.exception.BusinessException;
import com.reto.franchise.model.exception.InvalidDataException;
import com.reto.franchise.model.exception.ProductAlreadyExistsException;
import com.reto.franchise.model.exception.ProductNotFoundException;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Order(-2)
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {
    private static final String INTERNAL_ERROR_MESSAGE = "Ha ocurrido un error inesperado";

    public GlobalExceptionHandler(ErrorAttributes errorAttributes,
                                  ApplicationContext applicationContext,
                                  ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, new WebProperties.Resources(), applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable ex = getError(request);
        HttpStatus status = resolveStatus(ex);
        String message = resolveMessage(ex, status);

        Map<String, Object> body = new HashMap<>();
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body);
    }

    private HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof InvalidDataException) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ex instanceof BranchAlreadyExistsException || ex instanceof ProductAlreadyExistsException) {
            return HttpStatus.CONFLICT;
        }
        if (ex instanceof BusinessException || ex instanceof BranchNotFoundException || ex instanceof ProductNotFoundException) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(Throwable ex, HttpStatus status) {
        return status == HttpStatus.INTERNAL_SERVER_ERROR
                ? INTERNAL_ERROR_MESSAGE
                : Optional.ofNullable(ex.getMessage())
                .filter(message -> !message.isBlank())
                .orElse(status.getReasonPhrase());
    }
}