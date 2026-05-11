package com.back.common.utils.pagevalidate;

import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class PageValidate{
    public static void validatePage(int page, int size){
        if(page < 1){
            throw new AppException(ErrorCode.INVALID_PAGE);
        }
        if(size < 0){
            throw new AppException(ErrorCode.INVALID_SIZE);
        }
    }
}
