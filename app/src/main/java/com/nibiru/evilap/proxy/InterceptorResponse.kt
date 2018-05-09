package com.nibiru.evilap.proxy

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import okhttp3.ResponseBody




class InterceptorResponse : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        val orgBody = originalResponse.body()!!
        val newBody = ResponseBody.create(orgBody.contentType(), orgBody.bytes())
        originalResponse.close()
        return originalResponse.newBuilder()
                .removeHeader("Transfer-Encoding")
                .header("Content-Length", newBody.contentLength().toString())
                .body(newBody)
                .build()
    }
}