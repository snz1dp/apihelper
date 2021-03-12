package gateway.api;

import java.lang.reflect.Method;
import java.security.interfaces.RSAKey;
import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

/**
 * Retrofit工具类
 * @author neeker
 *
 */
public abstract class RetrofitUtils {

	private static final org.apache.commons.logging.Log Log = org.apache.commons.logging.LogFactory.getLog(RetrofitUtils.class);

	private static Method GetWebSpringContextMethod = null;
	
	private static Method GetEnvironmentMethod = null;
	
	private static Method GetPropertyMethod = null;
	
	static {
		try {
			GetWebSpringContextMethod = Class.forName("org.springframework.web.context.ContextLoader").getMethod("getCurrentWebApplicationContext");
			GetEnvironmentMethod = Class.forName("org.springframework.core.env.EnvironmentCapable").getMethod("getEnvironment");
			GetPropertyMethod = Class.forName("org.springframework.core.env.PropertyResolver").getMethod("getProperty", String.class);
		} catch (Throwable e) {
			GetWebSpringContextMethod = null;
			GetEnvironmentMethod = null;
			GetPropertyMethod = null;
			if (Log.isDebugEnabled()) {
				Log.debug("Not run on web application!");
			}
		}
	}
	
	private static String doGetApiHelperCustomUserAgent() {
		if (GetWebSpringContextMethod == null ||
				GetEnvironmentMethod == null ||
				GetPropertyMethod == null) return null;
		
		try {
			Object val = GetWebSpringContextMethod.invoke(null);
			val = GetEnvironmentMethod.invoke(val);
			return (String)GetPropertyMethod.invoke(val, "app.client.user-agent");
		} catch (Throwable e) {
			if (Log.isDebugEnabled()) {
				Log.debug("get web application context error: " + e.getMessage());
			}
		}
		return null;
	}
	
	/**
	 * 不使用JWT创建Retrofit
	 * @return {@link Retrofit}
	 */
	public static Retrofit createRetrofit(String apiprefix) {
		return createRetrofit(apiprefix, null, null);
	}

	public static Retrofit.Builder createRetrofitBuilder(String apiprefix) {
		return createRetrofitBuilder(apiprefix, null, null);
	}
	
	/**
	 * 创建Retrofit对象并返回（JWT令牌默认存活1800秒）
	 * @param apiprefix 第三方接口的URL前缀
	 * @param jwtAppToken 应用令牌(token)
	 * @param jwtPrivateKey 应用私钥(RSA)
	 * @param apiprefix
	 * @param jwtAppToken
	 * @param jwtPrivateKey
	 * @return {@link Retrofit}
	 */
	public static Retrofit createRetrofit(
			String apiprefix,
			String jwtAppToken,
			String jwtPrivateKey
	) {
		return createRetrofit(apiprefix, jwtAppToken, jwtPrivateKey, 1800);
	}

	public static Retrofit.Builder createRetrofitBuilder(
			String apiprefix,
			String jwtAppToken,
			String jwtPrivateKey
	) {
		return createRetrofitBuilder(apiprefix, jwtAppToken, jwtPrivateKey, 1800);
	}
	
	/**
	 * 创建Retrofit对象并返回
	 * @param apiprefix 第三方接口的URL前缀
	 * @param jwtAppToken 应用令牌(token)
	 * @param jwtPrivateKey 应用私钥(RSA)
	 * @param jwtTokenLiveSeconds 有效时间(秒)
	 * @return {@link Retrofit}
	 */
	public static Retrofit createRetrofit(
			String apiprefix,
			String jwtAppToken,
			String jwtPrivateKey,
			int jwtTokenLiveSeconds) {
		return createRetrofit(apiprefix, jwtAppToken, jwtPrivateKey, jwtTokenLiveSeconds, 
				JsonUtils.newGson(), JsonUtils.JsonDateFormat, 180, TimeUnit.SECONDS);
	}

	public static Retrofit.Builder createRetrofitBuilder(
			String apiprefix,
			String jwtAppToken,
			String jwtPrivateKey,
			int jwtTokenLiveSeconds) {
		return createRetrofitBuilder(apiprefix, jwtAppToken, jwtPrivateKey, jwtTokenLiveSeconds, 
				JsonUtils.newGson(), JsonUtils.JsonDateFormat, 180, TimeUnit.SECONDS);
	}

	/**
	 * 创建Retrofit对象并返回
	 * @param apiprefix
	 * @param jwtAppToken
	 * @param jwtPrivateKey
	 * @param jwtTokenLiveSeconds
	 * @param gson
	 * @param json_date_format
	 * @param timeout
	 * @param time_unit
	 * @return
	 */
	public static Retrofit createRetrofit(
			String apiprefix,
			String jwtAppToken,
			String jwtPrivateKey,
			int jwtTokenLiveSeconds,
			Gson gson,
			String json_date_format,
			long timeout, 
			TimeUnit time_unit) {
		return createRetrofitBuilder(
			apiprefix, jwtAppToken, jwtPrivateKey,
			jwtTokenLiveSeconds, gson, json_date_format,
			timeout, time_unit
		).build();
	}
		
	/**
	 * 创建Retrofit对象并返回
	 * @param apiprefix
	 * @param jwtAppToken
	 * @param jwtPrivateKey
	 * @param jwtTokenLiveSeconds
	 * @param gson
	 * @param json_date_format
	 * @param timeout
	 * @param time_unit
	 * @return
	 */
	public static Retrofit.Builder createRetrofitBuilder(
			String apiprefix,
			String jwtAppToken,
			String jwtPrivateKey,
			int jwtTokenLiveSeconds,
			Gson gson,
			String json_date_format,
			long timeout, 
			TimeUnit time_unit) {
		JwtContext jwt_context = null;
		if (StringUtils.isNoneBlank(jwtAppToken) &&
				StringUtils.isNoneBlank(jwtPrivateKey)) {
			jwt_context = JwtContext.create(jwtAppToken, 
					(RSAKey)RSAUtils.parsePrivateKeyFromPEM(jwtPrivateKey),
					jwtTokenLiveSeconds);
		} else {
			Log.warn(MessageFormat.format("未设置对{0}的JWT令牌参数！", apiprefix)); 
		}
		
		String admin_url = apiprefix;
		if (!StringUtils.endsWith(admin_url, "/"))
			admin_url += "/";
		
		OkHttpClient.Builder client_builder = new OkHttpClient.Builder();
		if (jwt_context != null)
			client_builder.addInterceptor(new OkHttpClientJwtInterceptor(doGetApiHelperCustomUserAgent(), jwt_context));
		else
			client_builder.addInterceptor(new OkHttpClientInterceptor(doGetApiHelperCustomUserAgent()));

		client_builder.connectTimeout(timeout, time_unit);
		client_builder.readTimeout(timeout, time_unit);
		client_builder.writeTimeout(timeout, time_unit);
		
		return new Retrofit.Builder().baseUrl(admin_url)
				.addCallAdapterFactory(SynchCallAdapterFactory.create())
				.addConverterFactory(BytesConverterFactory.create())
				.addConverterFactory(GsonConverterFactory.create(gson, json_date_format))
				.client(client_builder.build());
	}
	
}
