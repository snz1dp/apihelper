package gateway.api;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页对象
 * @author neeker
 *
 * @param <T>
 */
public class Page<T> implements Serializable {

	private static final long serialVersionUID = -1670466635388646632L;

	/**
	 * 总量
	 */
	public long total;
	
	/**
	 * 当前起始索引
	 */
	public long offset;
	
	/**
	 * 当前分页数据列表
	 */
	public List<T> data;
	
	@SuppressWarnings("unchecked")
	public Page() {
		this.total = 0;
		this.offset = 0;
		this.data = Collections.EMPTY_LIST;
	}

	public Page(List<T> data, long offset, long total) {
		this.data = data;
		this.offset = offset;
		this.total = total;
	}
	
}
