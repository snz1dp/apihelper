package gateway.api;

import java.security.Security;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;
import java.util.Date;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

/**
 * JWT上下文
 * @author neeker
 *
 */
public class JwtContext {

	//添加BouncyCastle的RSA扩展提供器
	static {
		if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	private String appToken;
	
	private int tokenLiveSconds = 1200;

	private Algorithm algorithm;
	
	JwtContext(String appToken, int tokenLiveSconds, Algorithm algorithm) {
		super();
		this.appToken = appToken;
		this.tokenLiveSconds = tokenLiveSconds;
		this.algorithm = algorithm;
	}

	/**
	 * 获取应用凭据
	 * @return
	 */
	public String getAppToken() {
		return appToken;
	}

	/**
	 * 获取令牌存活时间（秒）
	 * @return
	 */
	public int getTokenLiveSconds() {
		return tokenLiveSconds;
	}
	
	/**
	 * 设置令牌存活时间（秒）
	 * @param tokenLiveSconds 秒
	 */
	public void setTokenLiveSconds(int tokenLiveSconds) {
		this.tokenLiveSconds = tokenLiveSconds;
	}

	/**
	 * 创建JWT令牌
	 * @return
	 */
	public JwtToken createJwtToken() {
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.SECOND, tokenLiveSconds);
		String jwt_token = JWT.create()
				.withIssuer(getAppToken()) //设置APP令牌
				.withExpiresAt(calendar.getTime()) //设置多少秒后过期，保证令牌安全。
				.sign(this.algorithm);
		JwtToken rtoken = new JwtToken(jwt_token, new Date(), calendar.getTime());		
		return rtoken;
	}

	/**
	 * 创建JWT上下文
	 * @param app_token 应用凭据
	 * @param rsa_key RSA私钥
	 * @param token_live_seconds 令牌存活时间
	 * @return
	 */
	public static JwtContext create(String app_token, RSAKey rsa_key, int token_live_seconds) {
		RSAPublicKey public_key = rsa_key instanceof RSAPublicKey ? (RSAPublicKey)rsa_key : null;
		RSAPrivateKey private_key = rsa_key instanceof RSAPrivateKey ? (RSAPrivateKey)rsa_key : null;
		JwtContext w  = new JwtContext(
			app_token, token_live_seconds,
			Algorithm.RSA256(public_key, private_key)
		);
		return w;
	}

	public static JwtContext create(String app_token, Algorithm algorithm, int token_live_seconds) {
		JwtContext w = new JwtContext(app_token, token_live_seconds, algorithm);
		return w;
	}

}
