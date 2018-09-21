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
	
	protected okhttp3.Request.Builder newRequestBuilder(Chain chain) {
		okhttp3.Request.Builder new_req_builder = chain.request().newBuilder();
		ViaGatewayUtils.initRequestChainForHttpClient(new_req_builder);
		return new_req_builder;
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		return chain.proceed(newRequestBuilder(chain).build());
	}

}
