package com.back.common.utils;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class Translator {

    private static ResourceBundleMessageSource messageSource;

    public Translator(ResourceBundleMessageSource messageSource) {
        Translator.messageSource = messageSource;
    }

    public static String toLocale(String msgCode) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource != null ? messageSource.getMessage(msgCode, null, locale) : msgCode;
    }
    
    public static String toLocale(String msgCode, Object[] args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource != null ? messageSource.getMessage(msgCode, args, locale) : msgCode;
    }

    public static String toLocale(String msgCode, String defaultMessage) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource != null ? messageSource.getMessage(msgCode, null, defaultMessage, locale) : defaultMessage;
    }
}
