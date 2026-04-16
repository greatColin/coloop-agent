package com.colin.code.core.interceptor;

import java.util.Optional;

public interface InputInterceptor {
    Optional<String> intercept(String userMessage);
}
