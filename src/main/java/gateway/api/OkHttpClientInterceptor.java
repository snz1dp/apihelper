package gateway.api;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Response;

public class OkHttpClientInterceptor implements Interceptor {
	
	protected okhttp3.Request.Builder newRequestBuilder(Chain chain) {
		okhttp3.Request.Builder new_req_builder = chain.request().newBuilder();
		if (ViaGatewayUtlis.isRequestViaWeb()) {
			if (ViaGatewayUtlis.isAppRequestViaGateway()) {
				ViaGatewayUtlis.copyRequestChainForHttpClient(new_req_builder);
			} else {
				ViaGatewayUtlis.firstRequestChainForHttpClient(new_req_builder);
			}
		}
		return new_req_builder;
	}

	@Override
	public Response intercept(Chain chain) throws IOException {
		return chain.proceed(newRequestBuilder(chain).build());
	}

}
