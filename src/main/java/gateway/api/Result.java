package gateway.api;

import java.io.Serializable;

/**
 * api简单返回值
 * @author neeker
 */
public class Result implements Serializable {

	private static final long serialVersionUID = 4869701503316052780L;
	
	public Result() {
	}
	
	public Result(int code, String message) {
		this.code = code;
		this.message = message;
	}

	public int code = 0;
	
	public String message = "调用成功!";
	
	@Override
	public String toString() {
		return JsonUtils.toJson(this);
	}
	
}
