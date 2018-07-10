package gateway.api;

public class NotFoundException extends NotExceptException {

	private static final long serialVersionUID = -5993454083919681765L;

	public NotFoundException(String message) {
		super(404, message);
	}
	
}
