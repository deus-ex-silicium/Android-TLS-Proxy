package com.nibiru.evil_ap.proxy;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by Nibiru on 2016-12-23.
 */

final class InterceptorRequest implements Interceptor{
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        if (originalRequest.body() == null) {
            return chain.proceed(originalRequest);
        }

        Request editedReq = originalRequest.newBuilder()
                .removeHeader("Upgrade-Insecure-Requests")
                .removeHeader("Strict-Transport-Security")
                .removeHeader("User-Agent")
                .method(originalRequest.method(), originalRequest.body())
                .build();

        return chain.proceed(editedReq);
    }
}
