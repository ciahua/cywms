package com.chenyang.cywms.data.api

import com.chenyang.cywms.data.model.ApiResult
import com.chenyang.cywms.data.model.LoginRequest
import com.chenyang.cywms.data.model.LoginResult
import com.chenyang.cywms.data.prefs.SessionPrefs
import com.chenyang.cywms.data.prefs.SessionSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

interface AuthApi {
    @POST("jeecg-boot/sys/mLogin")
    suspend fun login(@Body body: LoginRequest): ApiResult<LoginResult>

    /** 轻量连通性探测：能拿到 HTTP 响应即视为网络可达（401/404 也算通） */
    @GET("jeecg-boot/sys/mLogin")
    suspend fun pingLoginPath(): retrofit2.Response<Unit>
}

class DynamicBaseUrlInterceptor(
    private val baseUrlRef: AtomicReference<String>,
    private val ngrokRef: AtomicReference<Boolean>
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val base = baseUrlRef.get().trimEnd('/') + "/"
        val baseHttp = base.toHttpUrlOrNull()
            ?: return chain.proceed(original)

        val path = original.url.encodedPath.removePrefix("/")
        val query = original.url.encodedQuery
        val newUrlBuilder = baseHttp.newBuilder()
            .addPathSegments(path)
        if (!query.isNullOrEmpty()) {
            newUrlBuilder.encodedQuery(query)
        }

        val builder = original.newBuilder().url(newUrlBuilder.build())
        if (ngrokRef.get()) {
            builder.header("ngrok-skip-browser-warning", "true")
        }
        return chain.proceed(builder.build())
    }
}

class TokenInterceptor(
    private val tokenRef: AtomicReference<String>
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenRef.get()
        val req = if (token.isNotBlank()) {
            chain.request().newBuilder()
                .header("X-Access-Token", token)
                .header("Content-Type", "application/json")
                .build()
        } else {
            chain.request().newBuilder()
                .header("Content-Type", "application/json")
                .build()
        }
        return chain.proceed(req)
    }
}

class ApiClient(private val prefs: SessionPrefs) {
    private val mutex = Mutex()
    private val baseUrlRef = AtomicReference("${SessionPrefs.DEFAULT_PROXY_URL}/")
    private val ngrokRef = AtomicReference(false)
    private val tokenRef = AtomicReference("")

    @Volatile
    private var retrofit: Retrofit? = null

    @Volatile
    private var authApi: AuthApi? = null

    @Volatile
    private var updateApi: UpdateApi? = null

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(DynamicBaseUrlInterceptor(baseUrlRef, ngrokRef))
        .addInterceptor(TokenInterceptor(tokenRef))
        .addInterceptor(logging)
        .build()

    suspend fun applySession(snapshot: SessionSnapshot) {
        mutex.withLock {
            baseUrlRef.set(snapshot.resolveBaseUrl().trimEnd('/') + "/")
            ngrokRef.set(snapshot.isNgrokProxy())
            tokenRef.set(snapshot.token)
            // Retrofit baseUrl 仅为占位；实际由 DynamicBaseUrlInterceptor 改写
            if (retrofit == null) {
                retrofit = Retrofit.Builder()
                    .baseUrl("http://127.0.0.1/")
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                authApi = retrofit!!.create(AuthApi::class.java)
                updateApi = retrofit!!.create(UpdateApi::class.java)
            }
        }
    }

    suspend fun authApi(): AuthApi {
        applySession(prefs.current())
        return authApi!!
    }

    suspend fun updateApi(): UpdateApi {
        applySession(prefs.current())
        return updateApi!!
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            applySession(prefs.current())
            val base = baseUrlRef.get().trimEnd('/')
            val url = "$base/jeecg-boot/sys/mLogin".toHttpUrlOrNull()
                ?: error("无效的服务器地址")
            val request = okhttp3.Request.Builder()
                .url(url)
                .get()
                .apply {
                    if (ngrokRef.get()) header("ngrok-skip-browser-warning", "true")
                }
                .build()
            client.newCall(request).execute().use { resp ->
                "HTTP ${resp.code} · $base"
            }
        }
    }
}
