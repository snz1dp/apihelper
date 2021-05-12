package gateway.api;

import okhttp3.Request.Builder;

/**
 * OkHttp客户JWT拦截器
 * @author neeker
 *
 */
public class OkHttpClientJwtInterceptor extends OkHttpClientInterceptor {
	
	private JwtContext jwtContext;
	
	public OkHttpClientJwtInterceptor(JwtContext jwtContext) {
		super(null);
		this.jwtContext = jwtContext;
	}

	public OkHttpClientJwtInterceptor(String user_agent, JwtContext jwtContext) {
		super(user_agent);
		this.jwtContext = jwtContext;
	}
	
	@Override
	protected Builder newRequestBuilder(Chain chain) {
		Builder builder = super.newRequestBuilder(chain);
		builder.addHeader("Authorization", jwtContext.createJwtToken().toAuthorizationString());
		return builder;
	}

}
