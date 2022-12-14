package gateway.api;

import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpMessage;

/**
 * 通过网关的工具类
 * @author neeker
 *
 */
public abstract class ViaGatewayUtils {
	
	private static final Log Log = LogFactory.getLog(ViaGatewayUtils.class);

	/**
	 * 获取客户端真实IP
	 * @return
	 */
	public static String getClientRealIp() {
		return getClientRealIp(getHttpServletRequest());
	}
	
	public static String getClientRealIp(HttpServletRequest request) {
		if (request == null) return null;
		
		// 获得真实IP
		String realIp = request.getRemoteAddr();
		String clientIp = request.getHeader("X-Forwarded-For");
		if (!StringUtils.isEmpty(clientIp)) {
			realIp = clientIp;
		} else {
			clientIp = request.getHeader("X-Real-IP");
			if (!StringUtils.isEmpty(clientIp)) {
				realIp = clientIp;
			}
		}
		return StringUtils.split(realIp, ",")[0];
	}	
	
	/**
	 * 获取用户浏览器UserAgent
	 * @return
	 */
	public static String getClientUserAgent() {
		HttpServletRequest request = getHttpServletRequest();
		if (request == null) return null;
		return request.getHeader("User-Agent");
	}

	public static String getClientUserAgent(HttpServletRequest request) {
		if (request == null) return null;
		return request.getHeader("User-Agent");
	}

	/**
	 * 分析QueryString格式的字符串为Properties对象，不进行UrlDecode
	 * @param data
	 * @return
	 */
	public static Properties parseQueryStringParameters(String data) {
		return parseQueryStringParameters(data, false);
	}
	
	@Deprecated
	public static HttpServletRequest GetHttpServletRequest() {
		return getHttpServletRequest();
	}
	
	/**
	 * 获得通过网关的请求根
	 * @return
	 */
	public static String getRequestRootViaGateway() {
		return getRequestRootViaGateway(getHttpServletRequest());
	}
	
	public static String getRequestRootViaGateway(HttpServletRequest request) {
		String proto = getGatewayProto(request);
		StringBuffer sbf = new StringBuffer(proto);
		sbf.append("://").append(getGatewayHost(request));
		int port = getGatewayPort(request);
		if (StringUtils.equalsIgnoreCase(proto, "http") &&  port != 80 || 
				StringUtils.equalsIgnoreCase(proto, "https") && port != 443) {
			sbf.append(":").append(port);
		}
		return sbf.toString();
	}
	
	/**
	 * 获取协议头
	 * @param request
	 * @return
	 */
	public static String getGatewayProto() {
		return getGatewayProto(getHttpServletRequest());
	}
	
	/**
	 * 获取协议头
	 * @param request
	 * @return
	 */
	public static String getGatewayProto(HttpServletRequest request) {
		if (isRequestViaGateway(request))
			return request.getHeader("x-forwarded-proto");
		return request.getScheme();
	}
	
	/**
	 * 获取请求上下文URI（不含主机名与端口信息）
	 * @return
	 */
	public static String getRequestURIViaGateway() {
		return getRequestURIViaGateway(getHttpServletRequest());
	}
	
	/**
	 * 获取请求上下文URI（不含主机名与端口信息）
	 * @return
	 */
	public static String getRequestURIViaGateway(HttpServletRequest request) {
		if (isRequestViaGateway(request))
			return request.getHeader("x-source-uri");
		return request.getRequestURI();
	}
	
	/**
	 * 获取请求URL（完整路径）
	 * @return
	 */
	public static String getRequestURLViaGateway() {
		return getRequestURIViaGateway(getHttpServletRequest());
	}
	
	/**
	 * 获取请求URL（完整路径）
	 * @return
	 */
	public static String getRequestURLViaGateway(HttpServletRequest request) {
		String proto = getGatewayProto(request);
		StringBuffer sbf = new StringBuffer(proto);
		sbf.append("://").append(getGatewayHost(request));
		int port = getGatewayPort(request);
		if (StringUtils.equalsIgnoreCase(proto, "http") &&  port != 80 || 
				StringUtils.equalsIgnoreCase(proto, "https") && port != 443) {
			sbf.append(":").append(port);
		}
		sbf.append(getRequestURIViaGateway(request));
		return sbf.toString();
	}
		
	/**
	 * 获取网关端口
	 * @return
	 */
	public static int getGatewayPort() {
		return getGatewayPort(getHttpServletRequest());
	}
	
	/**
	 * 获取网关端口
	 * @return
	 */
	public static int getGatewayPort(HttpServletRequest request) {
		if (isRequestViaGateway(request)) {
			String port_val = request.getHeader("x-forwarded-port");
			if (StringUtils.isEmpty(port_val)) return request.getServerPort();
			try {
				return Integer.parseInt(port_val);
			} catch(Throwable e) {
				request.getServerPort();
			}
		}
		return request.getServerPort();
	}
	
	/**
	 * 分析QueryString格式的字符串为Properties对象
	 * @param data
	 * @param val_url_decode
	 * @return
	 */
	public static Properties parseQueryStringParameters(String data, boolean val_url_decode) {
		String[] paramters = StringUtils.split(data, "&");
		Properties props = new Properties();
		for (String paramter : paramters) {
			if (StringUtils.isEmpty(paramter))
				continue;
			int paramter_equals_pos = paramter.indexOf('=');
			if (paramter_equals_pos < 2)
				continue;
			String name = paramter.substring(0, paramter_equals_pos);
			String value = paramter.substring(paramter_equals_pos + 1);
			if (val_url_decode) {
				try {
					value = URLDecoder.decode(value, "UTF-8");
				} catch (Throwable e) {
					if (Log.isDebugEnabled()) {
						Log.debug("URLDecoder.decode error: " + e.getMessage(), e);
					}
				}
			}
			props.setProperty(name, value);
		}
		return props;
	}
	
	/**
	 * 获得请求页面ID
	 * @return
	 */
	public static String getRequestPageId() {
		HttpServletRequest request = getHttpServletRequest();
		if (request == null) return null;
		String v = request.getRequestURI();
		int sp_lst = v.lastIndexOf('/');
		if (sp_lst >= 0) {
			v = v.substring(sp_lst + 1);
			if (v.endsWith(".jsp") || v.endsWith(".zul") || v.endsWith(".html")) {
				v = v.substring(0, v.length() - 4);
			}
		}
		return v;
	}
	
	/**
	 * 获得当前的Servlet请求对象
	 * @return
	 */
	public static HttpServletRequest getHttpServletRequest() {
		try {
			Class<?> request_context_holder_clazz = Class.forName("org.springframework.web.context.request.RequestContextHolder");
			Class<?> sa_clazz = Class.forName("org.springframework.web.context.request.ServletRequestAttributes");
			Method get_request_attributes_method = request_context_holder_clazz.getMethod("getRequestAttributes");
			Method get_request_method = sa_clazz.getMethod("getRequest");
			Object sa = get_request_attributes_method.invoke(null);
			if (sa == null) return null;
			return (HttpServletRequest)get_request_method.invoke(sa);
		} catch (Throwable e) {
			return null;
		}
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
		return StringUtils.isNoneBlank(getRequestIdViaGateway());
	}
	
	public static boolean isRequestViaGateway(HttpServletRequest request) {
		return StringUtils.isNoneBlank(getRequestIdViaGateway(request));
	}
	
	/**
	 * 获取网关发过来的请求ID
	 * @return
	 */
	public static String getRequestIdViaGateway() {
		return getRequestIdViaGateway(getHttpServletRequest());
	}
	
	/**
	 * 获取网关发过来的请求ID
	 * @return
	 */
	public static String getRequestIdViaGateway(HttpServletRequest request) {
		if (request == null) return null;
		return request.getHeader("x-request-id");
	}
	
	/**
	 * 获取调用链上层ID
	 * @return
	 */
	public static String getTraceParentIdViaGateway() {
		return getTraceParentIdViaGateway(getHttpServletRequest());
	}	

	/**
	 * 获取调用链上层ID
	 * @return
	 */
	public static String getTraceParentIdViaGateway(HttpServletRequest request) {
		if (request == null) return null;
		return request.getHeader("x-trace-parent");
	}	
	
	/**
	 * 获取调用链
	 * @return
	 */
	public static String getTraceChainIdViaGateway() {
		return getTraceChainIdViaGateway(getHttpServletRequest());
	}
	
	/**
	 * 获取调用链
	 * @return
	 */
	public static String getTraceChainIdViaGateway(HttpServletRequest request) {
		if (request == null) return null;
		return request.getHeader("x-app-sticky");
	}
		
	/**
	 * 为httpclient设置调用链入口参数，验证模式是前端接口调用其它接口时使用
	 * @param http_msg
	 */
	public static void firstRequestChainForHttpClient(HttpMessage http_msg) {
		if (!isRequestViaGateway()) return;
		http_msg.addHeader("x-trace-parent", getRequestIdViaGateway());
		http_msg.addHeader("x-app-sticky", getRequestIdViaGateway());
	}
	
	public static void firstRequestChainForHttpClient(okhttp3.Request.Builder http_msg) {
		if (!isRequestViaGateway()) return;
		http_msg.addHeader("x-trace-parent", getRequestIdViaGateway());
		http_msg.addHeader("x-app-sticky", getRequestIdViaGateway());
	}
	
	/**
	 * 为httpclient复制调用链参数，验证模式是后台接口调用其它后台接口时使用
	 * @param http_msg
	 */
	public static void copyRequestChainForHttpClient(HttpMessage http_msg) {
		if (!isRequestViaGateway()) return;
		http_msg.addHeader("x-trace-parent", getRequestIdViaGateway());
		http_msg.addHeader("x-app-sticky", getTraceChainIdViaGateway());
	}
	
	public static void initRequestChainForHttpClient(HttpMessage http_msg) {
		if (isRequestViaWeb()) {
			firstRequestChainForHttpClient(http_msg);
		} else {
			copyRequestChainForHttpClient(http_msg);
		}
	}
	
	public static void initRequestChainForHttpClient(okhttp3.Request.Builder http_msg) {
		if (isRequestViaWeb()) {
			firstRequestChainForHttpClient(http_msg);
		} else {
			copyRequestChainForHttpClient(http_msg);
		}
	}
	
	public static void copyRequestChainForHttpClient(okhttp3.Request.Builder http_msg) {
		if (!isRequestViaGateway()) return;
		http_msg.addHeader("x-trace-parent", getRequestIdViaGateway());
		http_msg.addHeader("x-app-sticky", getTraceChainIdViaGateway());
	}
	
	/**
	 * 获取当前API在服务端注册的ID
	 * @return
	 */
	public static String getApiIdViaGateway() {
		return getApiIdViaGateway(getHttpServletRequest());
	}
	
	/**
	 * 获取当前API在服务端注册的ID
	 * @return
	 */
	public static String getApiIdViaGateway(HttpServletRequest request) {
		if (request == null) return null;
		return request.getHeader("x-api");
	}	
	
	/**
	 * 获取网关主机名
	 * @return
	 */
	public static String getGatewayHost() {
		return getGatewayHost(getHttpServletRequest());
	}
	
	public static String getGatewayHost(HttpServletRequest request) {
		if (isRequestViaGateway(request)) {
			String hostname = request.getHeader("x-host-override");
			if (StringUtils.isNotEmpty(hostname)) return hostname;
			hostname = request.getHeader("x-forwarded-host");
			if (StringUtils.isNotEmpty(hostname)) return hostname;
		}
		return request.getServerName();
	}
	
	/**
	 * 获取网关ID
	 * @return
	 */
	public static String getGatewayId() {
		return getGatewayId(getHttpServletRequest());
	}
	
	/**
	 * 获取网关ID
	 * @return
	 */
	public static String getGatewayId(HttpServletRequest request) {
		if (request == null) return null;
		return request.getHeader("x-gateway-id");
	}
	
	/**
	 * 获取请求应用ID
	 * @return
	 */
	public static String getRequestAppIdViaGateway() {
		return getRequestAppIdViaGateway(getHttpServletRequest());
	}
	
	/**
	 * 获取请求应用ID
	 * @return
	 */
	public static String getRequestAppIdViaGateway(HttpServletRequest request) {
		if (request == null) return null;
		return request.getHeader("x-app-id");
	}
	
	/**
	 * 判断是否应用通过网关访问
	 * @return
	 */
	public static boolean isAppRequestViaGateway() {
		return StringUtils.isBlank(getRequestUsernameViaGateway()) 
				&& StringUtils.isNoneBlank(getRequestAppIdViaGateway());
	}
	
	/**
	 * 判断是否应用通过网关访问
	 * @return
	 */
	public static boolean isAppRequestViaGateway(HttpServletRequest request) {
		return StringUtils.isBlank(getRequestUsernameViaGateway(request)) 
				&& StringUtils.isNoneBlank(getRequestAppIdViaGateway(request));
	}	
	
	/**
	 * 判断是否通过用户前端访问
	 * @return
	 */
	public static boolean isUserRequestViaGateway() {
		return !isAppRequestViaGateway();
	}
	
	/**
	 * 判断是否通过用户前端访问
	 * @return
	 */
	public static boolean isUserRequestViaGateway(HttpServletRequest request) {
		return !isAppRequestViaGateway(request);
	}	
	
	/**
	 * 获取请求用户ID
	 * @return
	 */
	public static String getRequestUserIdViaGateway() {
		return getRequestUserIdViaGateway(getHttpServletRequest());
	}
	
	/**
	 * 获取请求用户ID
	 * @return
	 */
	public static String getRequestUserIdViaGateway(HttpServletRequest request) {
		if (request == null) return null;
		String userid = request.getHeader("x-credential-userid");
		return userid;
	}
		
	/**
	 * 获取请求用户名
	 * @return
	 */
	public static String getRequestUsernameViaGateway() {
		return getRequestUsernameViaGateway(getHttpServletRequest());
	}
	
	public static String getRequestUsernameViaGateway(HttpServletRequest request) {
		if (request == null) return null;
		String username = request.getHeader("iv-user");
		if (StringUtils.isBlank(username)) {
			username = request.getHeader("x-credential-username");
		}
		if (StringUtils.isNotBlank(username) && StringUtils.contains(username, "%")) {
			try {
				username = URLDecoder.decode(username, "UTF-8");
			} catch(Throwable e) {
				if (Log.isDebugEnabled()) {
					Log.debug("URLDecoder.decode error: " + e.getMessage(), e);
				}
			}
		}
		return username;
	}

	public static String getRequestDisplayNameViaGateway(HttpServletRequest request) {
		if (request == null) return null;
		String display_name = request.getHeader("x-user-displayname");
		if (StringUtils.isNotBlank(display_name) && StringUtils.contains(display_name, "%")) {
			try {
				display_name = URLDecoder.decode(display_name, "UTF-8");
			} catch(Throwable e) {
				if (Log.isDebugEnabled()) {
					Log.debug("URLDecoder.decode error: " + e.getMessage(), e);
				}
			}
		}
		return display_name;
	}

	public static String getRequestDisplayNameViaGateway() {
		return getRequestDisplayNameViaGateway(getHttpServletRequest());
	}

	/**
	 * 获得ACL分组代码
	 * @return
	 */
	@Deprecated
	public static String[] getRequestAclGroups() {
		return getRequestAclGroups(getHttpServletRequest());
	}
	
	
	/**
	 * 获得ACL分组代码
	 * @return
	 */
	@Deprecated
	public static String[] getRequestAclGroups(HttpServletRequest request) {
		if (request == null) return new String[0];
		String groups = request.getHeader("x-app-groups");
		String [] tmpret =  StringUtils.split(groups, ',');
		List<String> tmps = new LinkedList<String>();
		for (String tv : tmpret) {
			tmps.add(StringUtils.trim(tv));
		}
		return tmps.toArray(new String[0]);
	}
	
	/**
	 * 获得请求根地址
	 * @param request
	 * @return
	 */
	public static String getReqeustRoot(HttpServletRequest request) {
		String proto = request.getScheme();
		StringBuffer sbf = new StringBuffer(proto);
		sbf.append("://").append(request.getServerName());
		int port = request.getServerPort();
		if (StringUtils.equalsIgnoreCase(proto, "http") &&  port != 80 || 
				StringUtils.equalsIgnoreCase(proto, "https") && port != 443) {
			sbf.append(":").append(port);
		}
		return sbf.toString();
	}
	
	public static String getReqeustRoot() {
		return getReqeustRoot(getHttpServletRequest());
	}
	
	public static String getPublishURLViaGateway(String target_url) {
		return getPublishURLViaGateway(getHttpServletRequest(), target_url);
	}
	
	/**
	 * 获得通过网关的外部URL
	 * @param request
	 * @param target_url
	 * @return
	 */
	public static String getPublishURLViaGateway(HttpServletRequest request, String target_url) {
		if (isRequestViaGateway(request)) {
			if (target_url.startsWith("//")) {
				return getGatewayProto(request) + ":" + target_url;
			} else if (target_url.startsWith("https://") || target_url.startsWith("http://")) {
				return target_url;
			} else if (target_url.startsWith("/")) {
				return getRequestRootViaGateway(request) + target_url;
			}
			return getRequestRootViaGateway(request) + "/" + target_url;
		} else {
			if (target_url.startsWith("//")) {
				return request.getScheme() + ":" + target_url;
			} else if (target_url.startsWith("https://") || target_url.startsWith("http://")) {
				return target_url;
			} else if (target_url.startsWith("/")) {
				return getReqeustRoot(request)  + target_url;
			}
			return getReqeustRoot(request) + "/" + target_url;
		}
	}
	
	public static Map<String, Object> requestParametersToMap(HttpServletRequest request) {
		return requestParametersToMap(request, null);
	}

	public static Map<String, Object> requestParametersToMap(HttpServletRequest request, Set<String> exclude_names) {
		Map<String, Object> ext_properties = new LinkedHashMap<String, Object>();
		Enumeration<String> req_names = request.getParameterNames();
		while(req_names.hasMoreElements()) {
			String req_name = req_names.nextElement();
			if (exclude_names != null && exclude_names.contains(req_name)) continue;
			ext_properties.put(req_name, request.getParameter(req_name));
		}
		if (ext_properties.size() == 0) return null;
		return new HashMap<String, Object>(ext_properties);
	}

	
}
