package gateway.api;

import java.util.Date;

/**
 * 非预期异常
 * @author neeker
 *
 */
public class NotExceptException extends RuntimeException {
	
	public static final int CLIENT_START_ERROR = 5000;

	private static final long serialVersionUID = -1823306261700764444L;
	
	private int code;
	
	public Date timestamp;
	
	public String exception;
	
	public String path;

	public NotExceptException() {
		super("执行出错！");
		this.code = 999;
	}
	
	public NotExceptException(Result result) {
		super(result.message);
		this.code = result.code;
	}
	
	public NotExceptException(int code, String message) {
		super(message);
		this.code = code;
	}
	
	public NotExceptException(int code, String message, Date timestamp, String exception, String path) {
		super(message);
		this.code = code;
		this.timestamp = timestamp == null ? new Date() : timestamp;
		this.exception = exception;
		this.path = path;
	}
	
	public NotExceptException(int code, String message, Throwable e) {
		super(message, e);
		this.code = code;
	}
	
	/**
	 * 获得错误代码
	 * @return
	 */
	public int getCode() {
		return code;
	}
	
	/**
	 * 获得异常时间
	 * @return
	 */
	public Date getTimestamp() {
		return timestamp;
	}
	
	/**
	 * 获取错误类型
	 * @return
	 */
	public String getErrorType() {
		return exception;
	}
	
	/**
	 * 请求错误URI
	 * @return
	 */
	public String getApiURI() {
		return path;
	}
	
	@Override
	public String getMessage() {
		return super.getMessage();
	}

}
