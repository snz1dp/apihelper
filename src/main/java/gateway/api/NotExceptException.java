package gateway.api;

public class NotExceptException extends RuntimeException {
	
	public static final int CLIENT_START_ERROR = 5000;

	private static final long serialVersionUID = -1823306261700764444L;
	
	private int code;

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
	
	public NotExceptException(int code, String message, Throwable e) {
		super(message, e);
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
	
	@Override
	public String getMessage() {
		return super.getMessage();
	}

}
