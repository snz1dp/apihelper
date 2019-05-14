package gateway.api;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * OkHttp请求拦截器
 * @author neeker
 *
 */
public class OkHttpClientInterceptor implements Interceptor {

	private String userAgent = Version.NAME + "/" + Version.STRING;
	
	public OkHttpClientInterceptor(String user_agent) {
		if (user_agent != null && user_agent.length() > 0) {
			this.userAgent = user_agent;
		}
	}
	
	protected okhttp3.Request.Builder newRequestBuilder(Chain chain) {
		okhttp3.Request.Builder new_req_builder = chain.request().newBuilder();
		ViaGatewayUtils.initRequestChainForHttpClient(new_req_builder);
		new_req_builder.removeHeader("User-Agent").addHeader("User-Agent", userAgent);
		return new_req_builder;
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		return chain.proceed(newRequestBuilder(chain).build());
	}

}
