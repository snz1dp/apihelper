package gateway.api;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpMessage;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class ViaGatewayUtlis {

	
	public static final String GatewayTraceIdHeader = "x-app-sticky";
	
	public static final String GatewayTraceParentIdHeader = "x-trace-parent";

	/**
	 * 获得当前的Servlet请求对象
	 * @return
	 */
	public static HttpServletRequest getHttpServletRequest() {
		ServletRequestAttributes sa = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		if (sa == null) return null;
		return sa.getRequest();
	}

	
	/**
	 * 判断是否来自WEB请求
	 * @return
	 */
	public static boolean isRequestViaWeb() {
		HttpServletRequest request = getHttpServletRequest();
		return request != null;
	}
	
	/**
	 * 判断是否从网关过来的
	 * @return
	 */
	public static boolean isRequestViaGateway() {
		return getRequestIdViaGateway() != null && getRequestIdViaGateway().length() > 0;
	}
	
	/**
	 * 获取网关发过来的请求ID
	 * @return
	 */
	public static String getRequestIdViaGateway() {
		HttpServletRequest request = getHttpServletRequest();
		if (request == null) return null;
		return request.getHeader("x-request-id");
	}
	
	/**
	 * 获取调用链上层ID
	 * @return
	 */
	public static String getTraceParentIdViaGateway() {
		HttpServletRequest request = getHttpServletRequest();
		if (request == null) return null;
		return request.getHeader(GatewayTraceParentIdHeader);
	}	

	/**
	 * 获取调用链
	 * @return
	 */
	public static String getTraceChainIdViaGateway() {
		HttpServletRequest request = getHttpServletRequest();
		if (request == null) return null;
		return request.getHeader(GatewayTraceIdHeader);
	}
	
	/**
	 * 为httpclient设置调用链入口参数，验证模式是前端接口调用其它接口时使用
	 * @param http_msg
	 */
	public static void firstRequestChainForHttpClient(HttpMessage http_msg) {
		if (!isRequestViaGateway()) return;
		http_msg.addHeader(GatewayTraceParentIdHeader, getRequestIdViaGateway());
		http_msg.addHeader(GatewayTraceIdHeader, getRequestIdViaGateway());
	}
	
	/**
	 * 为httpclient复制调用链参数，验证模式是后台接口调用其它后台接口时使用
	 * @param http_msg
	 */
	public static void copyRequestChainForHttpClient(HttpMessage http_msg) {
		if (!isRequestViaGateway()) return;
		http_msg.addHeader(GatewayTraceParentIdHeader, getRequestIdViaGateway());
		http_msg.addHeader(GatewayTraceIdHeader, getTraceChainIdViaGateway());
	} 

	/**
	 * 获取当前API在服务端注册的ID
	 * @return
	 */
	public static String getGatweayApiId() {
		HttpServletRequest request = getHttpServletRequest();
		if (request == null) return null;
		return request.getHeader("x-api");
	}
	
	/**
	 * 获取网关主机名
	 * @return
	 */
	public static String getGatewayHost() {
		HttpServletRequest request = getHttpServletRequest();
		if (request == null) return null;
		return request.getHeader("x-host-override");
	}
	
	/**
	 * 获取网关ID
	 * @return
	 */
	public static String getGatewayId() {
		HttpServletRequest request = getHttpServletRequest();
		if (request == null) return null;
		return request.getHeader("x-gateway-id");
	}
	
	/**
	 * 获取请求应用ID
	 * @return
	 */
	public static String getRequestAppIdViaGateway() {
		HttpServletRequest request = getHttpServletRequest();
		if (request == null) return null;
		return request.getHeader("x-app-id");
	}
	
	/**
	 * 判断是否应用通过网关访问
	 * @return
	 */
	public static boolean isAppRequestViaGateway() {
		return (getRequestUsernameViaGateway() == null || getRequestUsernameViaGateway().length() == 0) 
				&& getRequestAppIdViaGateway() != null && getRequestAppIdViaGateway().length() > 0;
	}
	
	/**
	 * 判断是否通过用户前端访问
	 * @return
	 */
	public static boolean isUserRequestViaGateway() {
		return !isAppRequestViaGateway();
	}
	
	/**
	 * 获取请求用户ID
	 * @return
	 */
	public static String getRequestUserIdViaGateway() {
		HttpServletRequest request = getHttpServletRequest();
		if (request == null) return null;
		String userid = request.getHeader("x-credential-userid");
		return userid;
	}
	
	/**
	 * 获取请求用户名
	 * @return
	 */
	public static String getRequestUsernameViaGateway() {
		HttpServletRequest request = getHttpServletRequest();
		if (request == null) return null;
		String username = request.getHeader("iv-user");
		if (username == null || username.length() == 0) {
			username = request.getHeader("x-credential-username");
		}
		return username;
	}
	
	
}
