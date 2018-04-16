package gateway.examples;

import java.io.IOException;
import java.security.interfaces.RSAKey;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;

import gateway.api.JwtContext;
import gateway.api.JwtToken;
import gateway.api.RSAUtils;


public class Example implements HttpRequestInterceptor {

	// 消费测试用KEY
	private static final String TestAppToken = "e368d50ba0cd43429539f5efe13f9056";

	// 消费测试应用RSA私钥（PEM格式）
	private static final String TestPrivatePem = "-----BEGIN RSA PRIVATE KEY-----\nMIICWwIBAAKBgQDF2+yc7Wj8DFPnY9Lae4v3CncyEIarkymvG03i3Rvvb/UyaDsH\n/gvSoYrifornnW31Cd7wF8N4E4rIvGO8farrrtuFTYH+igAeaxgouinyJVDyK5AM\nEO73IiF5Tcf5m0eoUIlom1P1MbwHrJnIwwPTkjRHrSuqFR/b7nVFMNCWGwIDAQAB\nAoGANCRUA7sRGrNI/UayT3+VkCIC7X+rbdXXe10PtoSckwoHLSSIwf9yMC0AQ9Yj\nVwyG6LeUN+ObUK6duW7kPc1EWGbkn/jG2bXuEaQTg0SWJkY8C8D6W5904D3c22ZG\nJxwpH6l9vawghCdd44VkTgoBNOZ1I76hft32ikygEpKyFuECQQD1vnsjcYVRSgOY\nVPR0ah3SX3iqEkra3prb4+UIrggU1T8hBducMDCfHksPmIPGxc7rAs1oNMMK6ApC\npLHW/V4rAkEAzh3XlXiYN/0B3/PJkEVWiMYiOvqQ/wkHG6OdFl9d1Cw+Xnc3S3F+\nOgk7QLy9Dt7b4Zu4+NysjudBMYX4iG6f0QJAc4c7KKyDunWLPyAhVGFW58HOXlX/\nLuob72gyEmSOlAy0gvfYCJN3KDb7nrdarCXuYvmMS4MSdpwjxrTajnHKxQJAMGLv\nDibOTS529zUK13R/mQIyXPgfe8+JvKJPKUZgB4QPbCu+blaJVGSAZXUpSMlmgvME\nnF9pnu6I7nBN5PFbUQJAEYCGDUoVX1+YbHohCd+UPGkrZumZmaTpAArIjklialVF\nXwwmXOTbtx/dXSkVe+aPqmmnqfW3wHNvrlqBsCkJ2g==\n-----END RSA PRIVATE KEY-----";

	private JwtContext jwtContext;
	
	public Example() {
		loadJwtContext();
	}

	private void loadJwtContext() {
		jwtContext = JwtContext.create(TestAppToken, (RSAKey) RSAUtils.parsePrivateKeyFromPEM(TestPrivatePem), 120);
	}

	public HttpClient createHttpClient(boolean jwt_token_header) {
		HttpClientBuilder hcb = HttpClientBuilder.create();
		if (jwt_token_header) {
				hcb.addInterceptorFirst(this);
		}
		return hcb.build();
	}
	
	private JwtToken createJwtToken() {
		//生成的jwt令牌可多次使用，为安全起见请设置令牌过期时间。
		JwtToken jwt_token = jwtContext.createJwtToken(); 
		System.out.println("产生新的JWT安全令牌: \n" + jwt_token.getJwtToken() + "\n");		
		return jwt_token;
	}
	
	@Override
	public void process(HttpRequest request, HttpContext context) throws HttpException, IOException {
		request.addHeader(HttpHeaders.AUTHORIZATION,  createJwtToken().toAuthorizationString());
	}

	//通过认证头
	public void requestTestApiOverAuthorization() {
		HttpGet test_api = new HttpGet("http://192.168.1.29/gateway/admin");
		test_api.addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);
		try {
			HttpResponse response = createHttpClient(true).execute(test_api);
			System.out.println("测试接口返回: \n" + 
					IOUtils.toString(response.getEntity().getContent(), "UTF-8")
			);
			System.out.println("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//通过请求参数传递JWT安全令牌
	public void requestTestApiOverParameters() {
		HttpGet test_api = new HttpGet("http://192.168.1.29/gateway/admin?"
				+ createJwtToken().toParameterString());
		test_api.addHeader(HTTP.CONN_DIRECTIVE, HTTP.CONN_CLOSE);		
		try {
			HttpResponse response = createHttpClient(false).execute(test_api);
			System.out.println("测试接口返回: \n" + 
					IOUtils.toString(response.getEntity().getContent(), "UTF-8")
			);
			System.out.println("\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		Example example = new Example();
		example.requestTestApiOverAuthorization();
		example.requestTestApiOverParameters();
	}

}
