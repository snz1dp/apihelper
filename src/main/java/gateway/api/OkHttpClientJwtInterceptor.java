package gateway.api;

import okhttp3.Interceptor;
import okhttp3.Request.Builder;

public class OkHttpClientJwtInterceptor extends OkHttpClientInterceptor implements Interceptor {
	
	private JwtContext jwtContext;
	
	public OkHttpClientJwtInterceptor(JwtContext jwtContext) {
		this.jwtContext = jwtContext;
	}
	
	@Override
	protected Builder newRequestBuilder(Chain chain) {
		Builder builder = super.newRequestBuilder(chain);
		builder.addHeader("Authorization", jwtContext.createJwtToken().toAuthorizationString());
		return builder;
	}

}
