package com.ecommerce.application.util;

import com.ecommerce.application.advice.ExceptionParam;
import com.ecommerce.application.api.exception.ECOMErrorType;
import com.ecommerce.application.api.exception.EcommerceException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author AmirHossein ZamanZade
 * @since 4/25/2023
 */
@Component
@RequiredArgsConstructor
public class ExceptionHandlerUtil {

    private final MessageSource messageSource;

    public ExceptionParam generateExceptionParam(Throwable exception) {
        if (exception instanceof EcommerceException e) {
            return buildParam(e.getEcomErrorType(), e.getData(), e.getMessageArgs());
        }
        return buildParam(ECOMErrorType.GENERAL_ERROR, null, null);
    }

    private ExceptionParam buildParam(ECOMErrorType errorType, Map<String, Object> data,
                                      Object[] messageArgs) {
        var param = new ExceptionParam();
        param.setErrorCode(errorType.name());
        param.setMessage(resolveMessage(errorType, messageArgs));
        param.setErrorParams(data);
        return param;
    }

    private String resolveMessage(ECOMErrorType errorType, Object[] messageArgs) {
        try {
            return messageSource.getMessage(errorType.getMessageKey(), messageArgs,
                    LocaleContextHolder.getLocale());
        } catch (NoSuchMessageException e) {
            return errorType.getMessageKey();
        }
    }
}
