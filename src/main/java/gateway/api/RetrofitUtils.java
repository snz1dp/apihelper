package gateway.api;

import java.security.interfaces.RSAKey;

import org.apache.commons.lang3.StringUtils;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public abstract class RetrofitUtils {

	private static final org.apache.commons.logging.Log Log = org.apache.commons.logging.LogFactory.getLog(RetrofitUtils.class);

	/**
	 * 不使用JWT创建Retrofit
	 * @return {@link Retrofit}
	 */
	public static Retrofit createRetrofit(String apiprefix) {
		return createRetrofit(null, null, null);
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
			String jwtPrivateKey) {
		return createRetrofit(apiprefix, jwtAppToken, jwtPrivateKey, 1800);
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
		JwtContext jwt_context = null;
		if (StringUtils.isNoneBlank(jwtAppToken) &&
				StringUtils.isNoneBlank(jwtPrivateKey)) {
			jwt_context = JwtContext.create(jwtAppToken, 
					(RSAKey)RSAUtils.parsePrivateKeyFromPEM(jwtPrivateKey),
					jwtTokenLiveSeconds);
		} else {
			Log.warn("未设置请求的JWT令牌参数！");
		}
		
		String admin_url = apiprefix;
		if (!StringUtils.endsWith(admin_url, "/"))
			admin_url += "/";
		
		OkHttpClient.Builder client_builder = new OkHttpClient.Builder();
		if (jwt_context != null)
			client_builder.addInterceptor(new OkHttpClientJwtInterceptor(jwt_context));
		return new Retrofit.Builder().baseUrl(admin_url)
				.addCallAdapterFactory(SynchCallAdapterFactory.create())
				.addConverterFactory(GsonConverterFactory.create(
						JsonUtils.getGson())).client(client_builder.build()).build();
	}
		
	
}
