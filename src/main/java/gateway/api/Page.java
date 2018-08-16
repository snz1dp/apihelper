package gateway.api;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public class Page<T> implements Serializable {

	private static final long serialVersionUID = -1670466635388646632L;

	public long total;
	
	public long offset;
	
	public List<T> data;
	
	@SuppressWarnings("unchecked")
	public Page() {
		this.total = 0;
		this.offset = 0;
		this.data = Collections.EMPTY_LIST;
	}

	public Page(List<T> data, int offset, int total) {
		this.data = data;
		this.offset = offset;
		this.total = total;
	}
	
}
