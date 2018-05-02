package gateway.api;

import java.security.Security;
import java.security.interfaces.RSAKey;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

//JWT上下文
public class JwtContext {

	//添加BouncyCastle的RSA扩展提供器
	static {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private RSAKey jwtRsaKey;
	
	private String appToken;
	
	private int tokenLiveSconds = 1200;
	
	JwtContext(String appToken, int tokenLiveSconds, RSAKey jwtRsaKey) {
		super();
		this.appToken = appToken;
		this.tokenLiveSconds = tokenLiveSconds;
		this.jwtRsaKey = jwtRsaKey;
	}

	public String getAppToken() {
		return appToken;
	}

	public int getTokenLiveSconds() {
		return tokenLiveSconds;
	}
	
	public void setTokenLiveSconds(int tokenLiveSconds) {
		this.tokenLiveSconds = tokenLiveSconds;
	}

	@SuppressWarnings("deprecation")
	public JwtToken createJwtToken() {
		Algorithm jwt_algorithm;
		jwt_algorithm = Algorithm.RSA256(jwtRsaKey);
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, tokenLiveSconds);
		String jwt_token = JWT.create()
				.withIssuer(getAppToken()) //设置APP令牌
				.withExpiresAt(calendar.getTime()) //设置多少秒后过期，保证令牌安全。
				.sign(jwt_algorithm);
		JwtToken rtoken = new JwtToken(jwt_token, new Date(), calendar.getTime());		
		return rtoken;
	}
	
	//创建JWT上下文
	public static JwtContext create(String app_token, RSAKey rsa_key, int token_live_seconds) {
		JwtContext w  = new JwtContext(app_token, token_live_seconds, rsa_key);
		return w;
	}
	
}
