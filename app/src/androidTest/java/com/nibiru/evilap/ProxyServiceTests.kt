package com.nibiru.evilap

import android.content.Intent
import android.graphics.BitmapFactory
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ServiceTestRule
import android.support.test.runner.AndroidJUnit4
import com.nibiru.evilap.proxy.ProxyService
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.Matchers.contains
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class ProxyServiceTests {
    @Rule @JvmField
    val mServiceRule = ServiceTestRule()
    private lateinit var client: OkHttpClient

    @Before
    fun init(){
        mServiceRule.startService(Intent(InstrumentationRegistry.getTargetContext(), ProxyService::class.java))
        client = OkHttpClient.Builder()
                .connectTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(0, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.SECONDS)
                .followRedirects(false)
                .proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(InetAddress.getByName("127.0.0.1"), 1337)))
                .build()
    }

    @Test
    fun GET_html() {
        val request = Request.Builder()
                .url("http://example.com")
                //.url("http://www.icanhazip.com")
                //.url("https://publicobject.com/helloworld.txt")
                .build()
       client.newCall(request).execute().use { res ->
            Assert.assertTrue(res.isSuccessful)
            val b = res.body()?.string()
            Assert.assertNotNull(b)
            Assert.assertThat(b, containsString("<title>Example Domain</title>"))
            res.close()
        }
    }
    @Test
    fun GET_png() {
        val request = Request.Builder()
                .url("http://httpbin.org/image/png")
                .build()
        client.newCall(request).execute().use { res ->
            Assert.assertTrue(res.isSuccessful)
            val b = res.body()?.bytes()
            Assert.assertNotNull(b)
            Assert.assertEquals(b!!.size, 8090)
            val opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = true
            val bitmap = BitmapFactory.decodeByteArray(b,0,b.size,opts)
            if (opts.outWidth != -1 && opts.outHeight != -1) {
                Assert.assertEquals(opts.outWidth, 100)
                Assert.assertEquals(opts.outHeight, 100)
            } else {
                Assert.fail("Could not decode PNG")
            }
            res.close()
        }
    }
    @Test
    fun GET_jpeg() {
        val request = Request.Builder()
                .url("http://httpbin.org/image/jpeg")
                .build()
        client.newCall(request).execute().use { res ->
            Assert.assertTrue(res.isSuccessful)
            val b = res.body()?.bytes()
            Assert.assertNotNull(b)
            Assert.assertEquals(b!!.size, 35588)
            val opts = BitmapFactory.Options()
            opts.inJustDecodeBounds = true
            val bitmap = BitmapFactory.decodeByteArray(b,0,b.size,opts)
            if (opts.outWidth != -1 && opts.outHeight != -1) {
                Assert.assertEquals(opts.outWidth, 239)
                Assert.assertEquals(opts.outHeight, 178)
            } else {
                Assert.fail("Could not decode JPEG")
            }
            res.close()
        }
    }
    @Test
    fun GET_redirect() {
        val request = Request.Builder()
                .url("http://httpbin.org/redirect-to?url=http%3A%2F%2Fexample.com%2F")
                .build()
        client.newCall(request).execute().use { res ->
            Assert.assertEquals(res.code(), 302)
        }
    }
    @Test
    fun POST_string() {
        val mime = MediaType.parse("text/x-markdown; charset=utf-8")
        val body = RequestBody.create(mime, "Test Body")
        val request = Request.Builder()
                .url("http://httpbin.org/post")
                .post(body)
                .build()
        client.newCall(request).execute().use { res ->
            Assert.assertTrue(res.isSuccessful)
            val b = res.body()?.string()
            Assert.assertNotNull(b)
            Assert.assertThat(b, containsString("Test Body"))
            res.close()
        }
    }
    @Test
    fun POST_binary() {
        val mime = MediaType.parse("application/octet-stream")
        val testPayload = byteArrayOf( 0x01, 0x00, 0x03, 0x0a, 0x04, 0x0d, 0x05)
        val body = RequestBody.create(mime, testPayload)
        val request = Request.Builder()
                .url("http://httpbin.org/post")
                .post(body)
                .build()
        client.newCall(request).execute().use { res ->
            Assert.assertTrue(res.isSuccessful)
            val b = res.body()?.string()
            Assert.assertNotNull(b)
            Assert.assertThat(b, containsString("\"data\":\"\\u0001\\u0000\\u0003\\n\\u0004\\r\\u0005\""))
            res.close()
        }
    }
    @Test
    fun HEAD() {
        val request = Request.Builder()
                .url("http://httpbin.org/anything")
                .head()
                .build()
        client.newCall(request).execute().use { res ->
            Assert.assertTrue(res.isSuccessful)
            val b = res.body()?.string()
            Assert.assertNotNull(b)
            Assert.assertThat(b, containsString(""))
            val headers = res.headers()
            Assert.assertTrue( headers.size() > 0 )
            res.close()
        }
    }
    @Test
    fun PUT_binary() {
        val mime = MediaType.parse("application/octet-stream")
        val testPayload = byteArrayOf( 0x01, 0x00, 0x03, 0x0a, 0x04, 0x0d, 0x05)
        val body = RequestBody.create(mime, testPayload)
        val request = Request.Builder()
                .url("http://httpbin.org/anything")
                .put(body)
                .build()
        client.newCall(request).execute().use { res ->
            Assert.assertTrue(res.isSuccessful)
            val b = res.body()?.string()
            Assert.assertNotNull(b)
            Assert.assertThat(b, containsString("\"data\":\"\\u0001\\u0000\\u0003\\n\\u0004\\r\\u0005\""))
            Assert.assertThat(b, containsString("\"method\":\"PUT\""))
            res.close()
        }
    }
    @Test
    fun DELETE_binary_and_url_args() {
        val mime = MediaType.parse("application/octet-stream")
        val testPayload = byteArrayOf( 0x01, 0x00, 0x03, 0x0a, 0x04, 0x0d, 0x05)
        val body = RequestBody.create(mime, testPayload)
        val request = Request.Builder()
                .url("http://httpbin.org/anything?file=test_file_to_delete")
                .delete(body)
                .build()
        client.newCall(request).execute().use { res ->
            Assert.assertTrue(res.isSuccessful)
            val b = res.body()?.string()
            Assert.assertNotNull(b)
            Assert.assertThat(b, containsString("\"data\":\"\\u0001\\u0000\\u0003\\n\\u0004\\r\\u0005\""))
            Assert.assertThat(b, containsString("\"method\":\"DELETE\""))
            Assert.assertThat(b, containsString("\"args\":{\"file\":\"test_file_to_delete\"}"))
            res.close()
        }
    }
}
