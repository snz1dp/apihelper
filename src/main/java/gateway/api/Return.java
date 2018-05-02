package gateway.api;

/**
 * API分装返回值
 * @author neeker
 *
 * @param <T>
 */
public class Return<T> extends Result {

	private static final long serialVersionUID = -6422710904853351605L;
	
	public T data;
	
	@Override
	public String toString() {
		return JsonUtils.toJson(this);
	}
	
}
