package gateway.api;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HeaderElement;
import org.apache.http.HeaderElementIterator;
import org.apache.http.HttpClientConnection;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.ConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.DefaultClientConnectionReuseStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.IdleConnectionEvictor;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeaderElementIterator;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

/**
 * 优化HttpClient请求帮助类<br/>
 * 使用示例：参见main函数
 * 
 * @author neeker 2018-02-04
 */
@SuppressWarnings("deprecation")
public abstract class HttpClientHelper {

	private static final Log Log = LogFactory.getLog(HttpClientHelper.class);

	// 缺省HttpClient连接配置参数
	static HttpClientConnectManagerProperty DEFAULT_HCCM_PROPERTIES = new HttpClientConnectManagerProperty();

	private static HttpClientConnectionManager _defaultSingletonConnectionManager;

	private static HttpClient _defaultSingletonHttpClient;

	static {
		createDefaultSingletonHttpClient();
	}
	
	//获取缺省的配置
	public static HttpClientConnectManagerProperty getDefaultConnectManagerProperty() {
		return DEFAULT_HCCM_PROPERTIES;
	}

	// 创建缺省的单例HttpClient
	static void createDefaultSingletonHttpClient() {
		synchronized (HttpClientHelper.class) {
			if (_defaultSingletonHttpClient != null)
				return;
			HttpClientConnectManagerProperty prop = DEFAULT_HCCM_PROPERTIES;
			ConnectionKeepAliveStrategy keep_alive_strategy = createConnectionKeepAliveStrategy(prop);
			HttpRequestRetryHandler request_retry_handler = createHttpRequestRetryHandler(prop);
			_defaultSingletonConnectionManager = new SingletonHttpClientConnectionManager(createHttpClientConnectionManager(prop));
			_defaultSingletonHttpClient = new SingletonHttpClient(createHttpClient(prop, _defaultSingletonConnectionManager, keep_alive_strategy, request_retry_handler));
		}
	}
	
	//重新创建
	static void reCreateDefaultSingletonHttpClient() {
		destorySingletonHttpClientObjects();
		createDefaultSingletonHttpClient();
	}
	
	// 获得缺省的HttpClient连接管理器
	public static HttpClientConnectionManager getSingletonConnectionManager() {
		return _defaultSingletonConnectionManager;
	}

	// 获得缺省的HttpClient
	public static HttpClient getSingletonHttpClient() {
		return _defaultSingletonHttpClient;
	}

	// 停止缺省的单例连接管理器并关闭所有连接
	// !!!只能在应用停止时使用 !!!
	// !!!只能在应用停止时使用 !!!
	// !!!只能在应用停止时使用 !!!
	public static void destorySingletonHttpClientObjects() {
		synchronized(HttpClientHelper.class) {
			if (_defaultSingletonHttpClient != null) {
				if (_defaultSingletonHttpClient instanceof SingletonHttpClient) {
					HttpClientUtils.closeQuietly(((SingletonHttpClient) _defaultSingletonHttpClient).impl);
				} else {
					HttpClientUtils.closeQuietly(_defaultSingletonHttpClient);
				}
				_defaultSingletonHttpClient = null;
			}
			if (_defaultSingletonConnectionManager != null) {
				if (_defaultSingletonConnectionManager instanceof SingletonHttpClientConnectionManager) {
					(((SingletonHttpClientConnectionManager) _defaultSingletonConnectionManager).impl).shutdown();
				} else {
					_defaultSingletonConnectionManager.shutdown();
				}
				_defaultSingletonConnectionManager = null;
			}
		}
	}
	
	//执行不需要返回的请求
	public static void requestExecute(HttpUriRequest request, HttpContext context) throws Exception {
		requestExecute(request, context, RESPONSE_VOID_EXTRACTOR);
	}
	
	public static void requestExecute(HttpUriRequest request) throws Exception {
		requestExecute(request, HttpClientContext.create(), RESPONSE_VOID_EXTRACTOR);
	}
	
	//执行并返回字符串（默认UTF-8）
	public static String requestExecuteAsString(HttpUriRequest request, HttpContext context, Charset charset) throws Exception {
		return requestExecute(request, context, getHttpResponseStringExtractorByCharset(charset));
	}
	
	public static String requestExecuteAsString(HttpUriRequest request, HttpContext context, String charset) throws Exception {
		return requestExecute(request, context, getHttpResponseStringExtractorByCharset(charset));
	}
	
	public static String requestExecuteAsString(HttpUriRequest request, Charset charset) throws Exception {
		return requestExecute(request, HttpClientContext.create(), getHttpResponseStringExtractorByCharset(charset));
	}
	
	public static String requestExecuteAsString(HttpUriRequest request, String charset) throws Exception {
		return requestExecute(request, HttpClientContext.create(), getHttpResponseStringExtractorByCharset(charset));
	}
	
	public static String requestExecuteAsString(HttpUriRequest request, HttpContext context) throws Exception {
		return requestExecute(request, context, RESPONSE_STRING_EXTRACTOR);
	}
	
	public static String requestExecuteAsString(HttpUriRequest request) throws Exception {
		return requestExecute(request, HttpClientContext.create(), RESPONSE_STRING_EXTRACTOR);
	}
	
	//执行并返回字节
	public static byte[] requestExecuteAsBytes(HttpUriRequest request, HttpContext context) throws Exception {
		return requestExecute(request, context, RESPONSE_BYTES_EXTRACTOR);
	}
	
	public static byte[] requestExecuteAsBytes(HttpUriRequest request) throws Exception {
		return requestExecute(request, HttpClientContext.create(), RESPONSE_BYTES_EXTRACTOR);
	}
	
	//执行并返回GSON对象
	public static JsonObject requestExecuteAsJson(HttpUriRequest request, HttpContext context) throws Exception {
		request.setHeader(HttpHeaders.ACCEPT, "application/json");
		return requestExecute(request, context, RESPONSE_GSON_EXTRACTOR);
	}
	
	public static JsonObject requestExecuteAsJson(HttpUriRequest request) throws Exception {
		request.setHeader(HttpHeaders.ACCEPT, "application/json");
		return requestExecute(request, HttpClientContext.create(), RESPONSE_GSON_EXTRACTOR);
	}
	
	//执行并返回对象
	public static <T> T requestExecuteAsObject(HttpUriRequest request, HttpContext context, Class<T> clazz) throws Exception {
		request.setHeader(HttpHeaders.ACCEPT, "application/json");
		return requestExecute(request, context, createHttpResponseObjectExtractor(clazz, false));
	}
	
	public static <T> T requestExecuteAsObject(HttpUriRequest request, Class<T> clazz) throws Exception {
		request.setHeader(HttpHeaders.ACCEPT, "application/json");
		return requestExecute(request, HttpClientContext.create(), createHttpResponseObjectExtractor(clazz, false));
	}
	
	public static <T> T requestExecuteAsObject(HttpUriRequest request, HttpContext context, Class<T> clazz, boolean evelope_response) throws Exception {
		request.setHeader(HttpHeaders.ACCEPT, "application/json");
		return requestExecute(request, context, createHttpResponseObjectExtractor(clazz, evelope_response));
	}
	
	public static <T> T requestExecuteAsObject(HttpUriRequest request, Class<T> clazz, boolean evelope_response) throws Exception {
		request.setHeader(HttpHeaders.ACCEPT, "application/json");
		return requestExecute(request, HttpClientContext.create(), createHttpResponseObjectExtractor(clazz, evelope_response));
	}
	
	
	// 使用缺省的HttpClient执行请求并提取应答内容
	public static <T> T requestExecute(HttpUriRequest request, HttpResponseExtractor<T> response_extracter) throws Exception {
		return requestExecute(getSingletonHttpClient(), request, HttpClientContext.create(), response_extracter);
	}

	// 使用缺省的HttpClient执行请求并提取应答内容
	public static <T> T requestExecute(HttpUriRequest request, HttpContext context, HttpResponseExtractor<T> response_extracter) throws Exception {
		return requestExecute(getSingletonHttpClient(), request, context, response_extracter);
	}

	// 使用缺省的HttpClient执行请求并提取应答内容
	public static <T> T requestExecute(HttpClient hc, HttpUriRequest request, HttpResponseExtractor<T> response_extracter) throws Exception {
		return requestExecute(hc, request, HttpClientContext.create(), response_extracter);
	}

	// 使用HttpClient执行请求并提取应答内容
	public static <T> T requestExecute(HttpClient hc, HttpUriRequest request, HttpContext context, HttpResponseExtractor<T> response_extracter) throws Exception {
		HttpClient httpclient = hc != null ? hc : getSingletonHttpClient();
		if (httpclient == null) {
			throw new IllegalStateException("无可用的执行器!");
		}
		if (request instanceof HttpRequestBase && ((HttpRequestBase) request).getConfig() == null) {
			// 配置不存在则使用缺省的
			((HttpRequestBase) request).setConfig(getDefaultRequestConfig());
		}
		HttpResponse response = null;
		try {
			try {
				response = httpclient.execute(request, context);
			} catch (IOException e) {
				throw new IOException("请求服务时出错: " + e.getMessage(), e);
			}
			return response_extracter.extract(response);
		} finally {
			HttpClientUtils.closeQuietly(response);
		}
	}

	// 缺省的连接保持策略实现类
	public static ConnectionKeepAliveStrategy createConnectionKeepAliveStrategy(final HttpClientConnectManagerProperty prop) {
		ConnectionKeepAliveStrategy ret = new ConnectionKeepAliveStrategy() {
			@Override
			public long getKeepAliveDuration(final HttpResponse response, final HttpContext context) {
				Args.notNull(response, "HTTP response");
				final HeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));
				while (it.hasNext()) {
					final HeaderElement he = it.nextElement();
					final String param = he.getName();
					final String value = he.getValue();
					if (value != null && param.equalsIgnoreCase("timeout")) {
						try {
							return Long.parseLong(value) * 1000;
						} catch (final NumberFormatException ignore) {
						}
					}
				}
				return prop.getTimeLive();
			}
		};
		return ret;
	}

	// 创建空闲连接主动移除对象
	public static IdleConnectionEvictor createIdleConnectionEvictor(HttpClientConnectionManager cm, long idle_check_inteval, long max_idle_time, TimeUnit time_unit) {
		return new IdleConnectionEvictor(cm, idle_check_inteval, time_unit, max_idle_time, time_unit);
	}

	// 缺省的连接管理器
	public static HttpClientConnectionManager createHttpClientConnectionManager(HttpClientConnectManagerProperty prop) {
		PoolingHttpClientConnectionManager pcm = new PoolingHttpClientConnectionManager(prop.getTimeLive(), TimeUnit.MILLISECONDS);
		pcm.setMaxTotal(prop.getMaxTotal());
		pcm.setDefaultMaxPerRoute(prop.getMaxPerRoute());
		IdleConnectionEvictor idle_conn_evictor = createIdleConnectionEvictor(pcm, prop.getIdleCheckInterval(), prop.getMaxIdleTime(), TimeUnit.MILLISECONDS);
		idle_conn_evictor.start();
		return new InnerHttpClientConnectionManager(pcm, idle_conn_evictor);
	}

	// 重试连接处理器
	public static HttpRequestRetryHandler createHttpRequestRetryHandler(final HttpClientConnectManagerProperty prop) {
		return new InnerHttpRequestRetryHandler(prop);
	}
	
	
	// 创建缺省配置的HttpClient编译器
	public static HttpClientBuilder createDefaultHttpClientBuilder() {
		return createHttpClientBuilder(getDefaultConnectManagerProperty(), 
				getSingletonConnectionManager(), 
				createConnectionKeepAliveStrategy(getDefaultConnectManagerProperty()), 
				createHttpRequestRetryHandler(getDefaultConnectManagerProperty()));
	}
	
	// 创建HttpClient编译器
	public static HttpClientBuilder createHttpClientBuilder(
			HttpClientConnectManagerProperty prop, 
			HttpClientConnectionManager httpClientConnectionManager,
			ConnectionKeepAliveStrategy connectionKeepAliveStrategy, 
			HttpRequestRetryHandler httpRequestRetryHandler) {
		HttpClientBuilder hb = HttpClients.custom().setConnectionManager(httpClientConnectionManager).setRetryHandler(httpRequestRetryHandler)
				.setConnectionManagerShared(true);
		if (!prop.isSslVerifyHost()) {
			hb.setSSLHostnameVerifier(new NoopHostnameVerifier());
		}
		hb.setKeepAliveStrategy(connectionKeepAliveStrategy);
		hb.setConnectionReuseStrategy(DefaultClientConnectionReuseStrategy.INSTANCE);

		if (prop.getProxyHost() != null && prop.getProxyHost().length() > 0 && prop.getProxyPort() > 0) {
			hb.setProxy(new HttpHost(prop.getProxyHost(), prop.getProxyPort()));
		}
		return hb;
	}

	// 创建HttpClient对象
	public static HttpClient createHttpClient(HttpClientConnectManagerProperty prop, HttpClientConnectionManager httpClientConnectionManager,
			ConnectionKeepAliveStrategy connectionKeepAliveStrategy, HttpRequestRetryHandler httpRequestRetryHandler) {
		HttpClientBuilder hb = HttpClients.custom().setConnectionManager(httpClientConnectionManager).setRetryHandler(httpRequestRetryHandler)
				.setConnectionManagerShared(true);
		if (!prop.isSslVerifyHost()) {
			hb.setSSLHostnameVerifier(new NoopHostnameVerifier());
		}
		hb.setKeepAliveStrategy(connectionKeepAliveStrategy);
		hb.setConnectionReuseStrategy(DefaultClientConnectionReuseStrategy.INSTANCE);

		if (prop.getProxyHost() != null && prop.getProxyHost().length() > 0 && prop.getProxyPort() > 0) {
			hb.setProxy(new HttpHost(prop.getProxyHost(), prop.getProxyPort()));
		}
		return hb.build();
	}

	// 获取缺省的
	public static RequestConfig createRequestConfig(HttpClientConnectManagerProperty prop) {
		return RequestConfig.custom().setConnectionRequestTimeout(prop.getRequestTimeout().intValue()).setConnectTimeout(prop.getConnectTimeout().intValue())
				.setSocketTimeout(prop.getSocketTimeout().intValue()).build();
	}
	
	// 获取缺省的请求配置
	public static RequestConfig getDefaultRequestConfig() {
		return createRequestConfig(getDefaultConnectManagerProperty());
	}

	// HttpClient连接管理器属性配置
	public static class HttpClientConnectManagerProperty implements Serializable {

		private static final long serialVersionUID = -6518068245615660705L;

		// 代理主机
		private String proxyHost;

		// 代理端口
		private int proxyPort;

		// 是否校验主机
		private boolean sslVerifyHost = true;

		// 重试次数
		private Integer maxRetry = 3;

		// 最大请求总数
		private Integer maxTotal = 1000;

		// 每个host最大请求数
		private Integer maxPerRoute = 100;

		// 毫秒（默认1天）
		private Long timeLive = 1 * 24 * 60 * 60 * 1000L;

		// 最大空闲时间
		private Long maxIdleTime = 5 * 60 * 60 * 1000L;

		// 空闲检查间隔
		private Long idleCheckInterval = 5000L;

		// 毫秒
		private Long requestTimeout = 6000L;

		private Long connectTimeout = 6000L;

		private Long SocketTimeout = 6000L;

		public Long getMaxIdleTime() {
			return maxIdleTime;
		}

		public void setMaxIdleTime(Long maxIdleTime) {
			this.maxIdleTime = maxIdleTime;
		}

		public Long getIdleCheckInterval() {
			return idleCheckInterval;
		}

		public void setIdleCheckInterval(Long idleCheckInterval) {
			this.idleCheckInterval = idleCheckInterval;
		}

		public String getProxyHost() {
			return proxyHost;
		}

		public void setProxyHost(String proxyHost) {
			this.proxyHost = proxyHost;
		}

		public int getProxyPort() {
			return proxyPort;
		}

		public void setProxyPort(int proxyPort) {
			this.proxyPort = proxyPort;
		}

		public boolean isSslVerifyHost() {
			return sslVerifyHost;
		}

		public void setSslVerifyHost(boolean sslVerifyHostName) {
			this.sslVerifyHost = sslVerifyHostName;
		}

		public Integer getMaxTotal() {
			return maxTotal;
		}

		public void setMaxTotal(Integer maxTotal) {
			this.maxTotal = maxTotal;
		}

		public Integer getMaxPerRoute() {
			return maxPerRoute;
		}

		public void setMaxPerRoute(Integer maxPerRoute) {
			this.maxPerRoute = maxPerRoute;
		}

		public Long getTimeLive() {
			return timeLive;
		}

		public void setTimeLive(Long timeLiveMS) {
			this.timeLive = timeLiveMS;
		}

		public Long getRequestTimeout() {
			return requestTimeout;
		}

		public void setRequestTimeout(Long requestTimeout) {
			this.requestTimeout = requestTimeout;
		}

		public Long getConnectTimeout() {
			return connectTimeout;
		}

		public void setConnectTimeout(Long connectTimeout) {
			this.connectTimeout = connectTimeout;
		}

		public Long getSocketTimeout() {
			return SocketTimeout;
		}

		public void setSocketTimeout(Long socketTimeout) {
			SocketTimeout = socketTimeout;
		}

		public Integer getRetry() {
			return maxRetry;
		}

		public void setRetry(Integer retry) {
			this.maxRetry = retry;
		}

		public Integer getMaxRetry() {
			return maxRetry;
		}

		public void setMaxRetry(Integer maxRetry) {
			this.maxRetry = maxRetry;
		}

	}

	// 单例HttpClient类，不能关闭
	private static class SingletonHttpClient implements HttpClient, Closeable {
		
		private HttpClient impl;
		
		private SingletonHttpClient(HttpClient impl) {
			if (impl instanceof SingletonHttpClient) {
				this.impl = ((SingletonHttpClient) impl).impl;
			} else {
				this.impl = impl;
			}
		}

		@Override
		public HttpParams getParams() {
			return impl.getParams();
		}

		@Override
		public ClientConnectionManager getConnectionManager() {
			return impl.getConnectionManager();
		}

		@Override
		public HttpResponse execute(HttpUriRequest request) throws IOException, ClientProtocolException {
			return impl.execute(request);
		}

		@Override
		public HttpResponse execute(HttpUriRequest request, HttpContext context) throws IOException, ClientProtocolException {
			return impl.execute(request, context);
		}

		@Override
		public HttpResponse execute(HttpHost target, HttpRequest request) throws IOException, ClientProtocolException {
			return impl.execute(target, request);
		}

		@Override
		public HttpResponse execute(HttpHost target, HttpRequest request, HttpContext context) throws IOException, ClientProtocolException {
			return impl.execute(target, request, context);
		}

		@Override
		public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
			return impl.execute(request, responseHandler);
		}

		@Override
		public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
			return impl.execute(request, responseHandler, context);
		}

		@Override
		public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler) throws IOException, ClientProtocolException {
			return impl.execute(target, request, responseHandler);
		}

		@Override
		public <T> T execute(HttpHost target, HttpRequest request, ResponseHandler<? extends T> responseHandler, HttpContext context) throws IOException, ClientProtocolException {
			return impl.execute(target, request, responseHandler, context);
		}

		@Override
		public void close() throws IOException {
		}
		
	}
	
	// 单例HttpClientConnectionManager实现类
	private static class SingletonHttpClientConnectionManager implements HttpClientConnectionManager, Closeable {

		private HttpClientConnectionManager impl;
		
		public SingletonHttpClientConnectionManager(HttpClientConnectionManager impl) {
			if (impl instanceof SingletonHttpClientConnectionManager) {
				impl = ((SingletonHttpClientConnectionManager) impl).impl;
			} else {
				this.impl = impl;
			}
		}

		@Override
		public void close() throws IOException {			
		}

		@Override
		public ConnectionRequest requestConnection(HttpRoute route, Object state) {
			return impl.requestConnection(route, state);
		}

		@Override
		public void releaseConnection(HttpClientConnection conn, Object newState, long validDuration, TimeUnit timeUnit) {
			impl.releaseConnection(conn, newState, validDuration, timeUnit);
		}

		@Override
		public void connect(HttpClientConnection conn, HttpRoute route, int connectTimeout, HttpContext context) throws IOException {
			impl.connect(conn, route, connectTimeout, context);
		}

		@Override
		public void upgrade(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
			impl.upgrade(conn, route, context);
		}

		@Override
		public void routeComplete(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
			impl.routeComplete(conn, route, context);
		}

		@Override
		public void closeIdleConnections(long idletime, TimeUnit tunit) {
			impl.closeIdleConnections(idletime, tunit);
		}

		@Override
		public void closeExpiredConnections() {
			impl.closeExpiredConnections();
		}

		@Override
		public void shutdown() {
		}
		
	}
	

	// 内部代理实现的HttpClient连接管理器
	private static class InnerHttpClientConnectionManager implements HttpClientConnectionManager, Closeable {

		private HttpClientConnectionManager pcm;

		private IdleConnectionEvictor idle_conn_evictor;

		private InnerHttpClientConnectionManager(HttpClientConnectionManager p, IdleConnectionEvictor conn_evictor) {
			this.pcm = p;
			this.idle_conn_evictor = conn_evictor;
		}

		@Override
		public void upgrade(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
			pcm.upgrade(conn, route, context);
		}

		@Override
		public void shutdown() {
			idle_conn_evictor.shutdown();
			pcm.shutdown();
		}

		@Override
		public void routeComplete(HttpClientConnection conn, HttpRoute route, HttpContext context) throws IOException {
			pcm.routeComplete(conn, route, context);
		}

		@Override
		public ConnectionRequest requestConnection(HttpRoute route, Object state) {
			return pcm.requestConnection(route, state);
		}

		@Override
		public void releaseConnection(HttpClientConnection conn, Object newState, long validDuration, TimeUnit timeUnit) {
			pcm.releaseConnection(conn, newState, validDuration, timeUnit);
		}

		@Override
		public void connect(HttpClientConnection conn, HttpRoute route, int connectTimeout, HttpContext context) throws IOException {
			pcm.connect(conn, route, connectTimeout, context);
		}

		@Override
		public void closeIdleConnections(long idletime, TimeUnit tunit) {
			pcm.closeIdleConnections(idletime, tunit);
		}

		@Override
		public void closeExpiredConnections() {
			pcm.closeExpiredConnections();
		}

		@Override
		public void close() throws IOException {
			shutdown();
		}
	};
	
	//内部的重试机制
	private static class InnerHttpRequestRetryHandler implements HttpRequestRetryHandler {
		
		private HttpClientConnectManagerProperty prop;
		
    private InnerHttpRequestRetryHandler(HttpClientConnectManagerProperty prop) {
			this.prop = prop;
		}

		public boolean retryRequest(IOException exception,
        int executionCount, HttpContext context) {
	    if (executionCount >= prop.getRetry()) {
	        return false;
	    }
	    if (exception instanceof NoHttpResponseException) {// 如果服务器丢掉了连接，那么就重试
	        return true;
	    }
	    if (exception instanceof SSLHandshakeException) {// 不要重试SSL握手异常
	        return false;
	    }
	    if (exception instanceof InterruptedIOException) {// 超时
	        return false;
	    }
	    if (exception instanceof UnknownHostException) {// 目标服务器不可达
	        return false;
	    }
	    if (exception instanceof ConnectTimeoutException) {// 连接被拒绝
	        return false;
	    }
	    if (exception instanceof SSLException) {// SSL握手异常
	        return false;
	    }
	    HttpClientContext clientContext = HttpClientContext.adapt(context);
	    HttpRequest request = clientContext.getRequest();
	    // 如果请求是幂等的，就再次尝试
	    return !(request instanceof HttpEntityEnclosingRequest);
		}
	};

	// 请求应答对象榨取器
	public static interface HttpResponseExtractor<T> {

		T extract(HttpResponse response) throws Exception;

	}
	
	// 应答缺省字符集
	public static final Charset HttpResponseDefaultCharset = Charset.forName("UTF-8");
	

	// 默认字符串提取
	public static final HttpResponseStringExtractor RESPONSE_STRING_EXTRACTOR = new HttpResponseStringExtractor();

	// 默认字节提取对象
	public static final HttpResponseBytesExtractor RESPONSE_BYTES_EXTRACTOR = new HttpResponseBytesExtractor();
	
	// 默认空提取对象
	public static final HttpResponseVoidExtractor RESPONSE_VOID_EXTRACTOR = new HttpResponseVoidExtractor();
	
	// 默认GSON提取对象
	public static final HttpResponseGsonExtractor RESPONSE_GSON_EXTRACTOR = new HttpResponseGsonExtractor();
	
	// 根据字符集获取字符提取器
	public static HttpResponseStringExtractor getHttpResponseStringExtractorByCharset(Charset charset) {
		return new HttpResponseStringExtractor(charset);
	}
	
	public static HttpResponseStringExtractor getHttpResponseStringExtractorByCharset(String charset) {
		return new HttpResponseStringExtractor(Charset.forName(charset));
	}

	// 字符串内容提取类
	public static class HttpResponseStringExtractor implements HttpResponseExtractor<String> {

		private Charset charset = HttpResponseDefaultCharset;
		
		private HttpResponseStringExtractor(Charset charset) {
			this.charset = charset;
		}

		private HttpResponseStringExtractor() {}

		@Override
		public String extract(HttpResponse response) throws Exception {
			StatusLine state = response.getStatusLine();
			if (!(state.getStatusCode() >= 200 && state.getStatusCode() < 400)) {
				throw new Exception(MessageFormat.format("服务端应答错误(CODE={0}: {1}", state.getStatusCode(), 
						EntityUtils.toString(response.getEntity(), HttpResponseDefaultCharset)));
			}

			return EntityUtils.toString(response.getEntity(), getResponseContentCharset(response, charset));
		}

	}

	// 二进制内容提取类
	public static class HttpResponseBytesExtractor implements HttpResponseExtractor<byte[]> {

		private HttpResponseBytesExtractor() {
		};

		@Override
		public byte[] extract(HttpResponse response) throws Exception {
			StatusLine state = response.getStatusLine();
			if (!(state.getStatusCode() >= 200 && state.getStatusCode() < 400)) {
				throw new Exception(MessageFormat.format("服务端应答错误(CODE={0}: {1}", state.getStatusCode(), 
						EntityUtils.toString(response.getEntity(), HttpResponseDefaultCharset)));
			}
			return EntityUtils.toByteArray(response.getEntity());
		}

	}
	
	//获取应答字符集对象（默认为UTF-8）
	public static Charset getResponseContentCharset(HttpResponse response) {
		return getResponseContentCharset(response, HttpResponseDefaultCharset);
	}
	
	//获取应答字符集对象
	public static Charset getResponseContentCharset(HttpResponse response, Charset default_charset) {
		if (response.getEntity().getContentEncoding() == null ||
				StringUtils.isBlank(response.getEntity().getContentEncoding().getValue())) {
			return default_charset == null ? Charset.forName("UTF-8") : default_charset;
		}
		try {
			return Charset.forName(response.getEntity().getContentEncoding().getValue());
		} catch(Throwable e) {
			if (Log.isDebugEnabled()) {
				Log.debug(e.getMessage(), e);
			}
			return default_charset == null ? Charset.forName("UTF-8") : default_charset;
		}
	}
	
	//空返回
	public static class HttpResponseVoidExtractor implements HttpResponseExtractor<Void> {

		private HttpResponseVoidExtractor() {
		};

		@Override
		public Void extract(HttpResponse response) throws Exception {
			StatusLine state = response.getStatusLine();
			if (!(state.getStatusCode() >= 200 && state.getStatusCode() < 400)) {
				throw new NotExceptException(state.getStatusCode(), 
						MessageFormat.format("服务端应答错误: {1}", state.getStatusCode(), 
								EntityUtils.toString(response.getEntity(), getResponseContentCharset(response))));
			}
			return null;
		}

	}
	
	//GSON返回
	public static class HttpResponseGsonExtractor implements HttpResponseExtractor<JsonObject> {
		
		private HttpResponseGsonExtractor() {
		}

		@Override
		public JsonObject extract(HttpResponse response) throws Exception {
			StatusLine state = response.getStatusLine();
			if (!(state.getStatusCode() >= 200 && state.getStatusCode() < 400)) {
				throw new NotExceptException(state.getStatusCode(), 
						MessageFormat.format("服务端应答错误: {1}", state.getStatusCode(), 
								EntityUtils.toString(response.getEntity(), getResponseContentCharset(response))));
			}
			return JsonUtils.fromJson(response.getEntity().getContent(), JsonObject.class);
		}
		
	}
	
	public static <T> HttpResponseObjectExtractor<T> createHttpResponseObjectExtractor(Class<T> clazz, boolean evelope_response) {
		return new HttpResponseObjectExtractor<T>(clazz, evelope_response);
	}
	
	public static class HttpResponseObjectExtractor<T> implements HttpResponseExtractor<T> {
		
		private Type object_clazz;
		
		private boolean evelope_response;
		
		public HttpResponseObjectExtractor(Type clazz, boolean evelope_response) {
			this.object_clazz = clazz;
			this.evelope_response = evelope_response;
		}

		@SuppressWarnings("unchecked")
		@Override
		public T extract(HttpResponse response) throws Exception {
			StatusLine state = response.getStatusLine();
			if (evelope_response) {
				JsonObject json_node = null;
				String json_error = null;
				try {
					json_node = JsonUtils.fromJson(response.getEntity().getContent(), JsonObject.class);
				} catch(Throwable e) {
					json_error = e.getMessage();
				}

				if (!(state.getStatusCode() >= 200 && state.getStatusCode() < 400) || 
						json_node == null || json_node.get("code") == null || json_node.get("code").getAsInt() != 0) {
					if (state.getStatusCode() == 404 || json_node != null && json_node.get("code") != null && json_node.get("code").getAsInt() == 404) {
						throw new NotFoundException(json_node.get("message") != null ? json_node.get("message").getAsString() : json_error);
					} else 
						throw new NotExceptException(
							json_node == null || json_node.get("code") == null ? state.getStatusCode() : json_node.get("code").getAsInt(), 
									json_node == null || json_node.get("message") == null ? json_error : json_node.get("message").getAsString(), 
											json_node == null || json_node.get("timestamp") == null ? new Date() : new SimpleDateFormat(JsonUtils.JsonDateFormat).parse(json_node.get("timestamp").getAsString()), 
													json_node == null || json_node.get("exception") == null ? null : json_node.get("exception").getAsString(), 
															json_node == null || json_node.get("path") == null ? null : json_node.get("path").getAsString());
				}
				
				if (!json_node.has("data")) return null;
				
				if (Objects.equals(object_clazz, Void.class)) return null;
				
				if (TypeUtils.isArrayType(object_clazz)) {
					return JsonUtils.fromJson(json_node.get("data"), TypeToken.getArray(TypeUtils.getArrayComponentType(object_clazz)).getType());
				} else if (object_clazz instanceof ParameterizedType) {
					Type rawType = TypeUtils.getRawType(object_clazz, null);
					Object val = JsonUtils.fromJson(json_node.get("data"), 
							TypeToken.getParameterized(rawType, new ArrayList<Type>(
									TypeUtils.getTypeArguments((ParameterizedType)object_clazz).values()).toArray(new Type[0])).getType());
					return (T) val;
				} else if (object_clazz instanceof Class) {
					return JsonUtils.fromJson(json_node.get("data"), TypeToken.get((Class<?>)object_clazz).getType());
				}
				return JsonUtils.fromJson(json_node.get("data"), TypeToken.get(object_clazz).getType());
				
			} else {
				if (!(state.getStatusCode() >= 200 && state.getStatusCode() < 400))
					throw new NotExceptException(state.getStatusCode(), 
							IOUtils.toString(response.getEntity().getContent(), 
									getResponseContentCharset(response)));
				return JsonUtils.fromJson(response.getEntity().getContent(), TypeToken.get(object_clazz).getType());
			}
		}
		
	}


}
