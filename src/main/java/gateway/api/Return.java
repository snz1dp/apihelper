package gateway.api;

/**
 * 封装返回
 * @author neeker
 *
 * @param <T>
 */
public class Return<T> extends Result {

	private static final long serialVersionUID = -6422710904853351605L;
		
	public T data;
	
	@SuppressWarnings("rawtypes")
	private static final Return success = new Return();
	
	@Override
	public String toString() {
		return JsonUtils.toJson(this);
	}
	
	public static <T> Return<T> wrap(T obj) {
		Return<T> wval = new Return<T>();
		wval.data = obj;
		return wval;
	}
	
	public static <T> Return<T> wrap(int code, String msg, T obj) {
		Return<T> wval = new Return<T>();
		wval.code = code;
		wval.message = msg;
		wval.data = obj;
		return wval;
	}
	
	@SuppressWarnings("unchecked")
	public static <T> Return<T> success() {
		return success;
	}
	
	public static <T> Return<T> success(String msg) {
		Return<T> wval = new Return<T>();
		wval.message = msg;
		return wval;
	}
	
	public static <T> Return<T> error(int code) {
		Return<T> wval = new Return<T>();
		wval.code = code;
		wval.message = "调用出现错误！";
		return wval;
	}
	
	public static <T> Return<T> error(int code, String msg) {
		Return<T> wval = new Return<T>();
		wval.code = code;
		wval.message = msg;
		return wval;
	}
	
	public static <T> Return<T> error(int code, String msg, T obj) {
		Return<T> wval = new Return<T>();
		wval.code = code;
		wval.message = msg;
		wval.data = obj;
		return wval;
	}
	
}
