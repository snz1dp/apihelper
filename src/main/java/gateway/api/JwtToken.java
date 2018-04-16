package gateway.api;

import java.io.Serializable;
import java.util.Date;

//JWT安全令牌
public class JwtToken implements Serializable {

	private static final long serialVersionUID = 2545258825711201640L;
	
	private String jwtToken;
	
	private Date createTime;
	
	private Date expireTime;
	
	JwtToken(String jwtToken, Date createTime, Date expireTime) {
		super();
		this.jwtToken = jwtToken;
		this.createTime = createTime;
		this.expireTime = expireTime;
	}

	//是否已过期
	boolean isExpired() {
		return !expireTime.after(new Date());
	}

	//获得JWT令牌
	public String getJwtToken() {
		return jwtToken;
	}
	
	//获得查询参数的字符串
	public String toParameterString() {
		return "jwt=" + getJwtToken();
	}
	
	//获得认证头字符串
	public String toAuthorizationString() {
		return "Bearer " + getJwtToken();
	}

	//创建时间
	public Date getCreateTime() {
		return createTime;
	}

	//超时时间
	public Date getExpireTime() {
		return expireTime;
	}
	
	@Override
	public String toString() {
		return "JWTToken [jwtToken=" + jwtToken + ", createTime=" + createTime + ", expireTime=" + expireTime + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((jwtToken == null) ? 0 : jwtToken.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JwtToken other = (JwtToken) obj;
		if (jwtToken == null) {
			if (other.jwtToken != null)
				return false;
		} else if (!jwtToken.equals(other.jwtToken))
			return false;
		return true;
	}
	
	
}
