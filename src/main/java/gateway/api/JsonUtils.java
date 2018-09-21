package gateway.api;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

/**
 * JSON工具类
 * @author neeker
 *
 */
public abstract class JsonUtils {
	
	public static final String JsonDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS";

	private static Gson gson = new GsonBuilder()
			.enableComplexMapKeySerialization()
			.setDateFormat(JsonDateFormat)
      .create();
	
	/**
	 * 获取全局GSON对象
	 * @return
	 */
	public static Gson getGson() {
		return gson;
	}
	
	/**
	 * 从对象转换为JSON
	 * @param object
	 * @return
	 */
	public static <T> String toJson(T object) {
		return getGson().toJson(object);
	}
	
	/**
	 * 从JSON中转换为对象
	 * @param json
	 * @param toClass
	 * @return
	 */
	public static <T> T fromJson(String json, Class<T> toClass) {
		return getGson().fromJson(json, toClass);
	}
	
	public static <T> T fromJson(String json, Type toClass) {
		return getGson().fromJson(json, toClass);
	}
	
	public static <T> T fromJson(InputStream reader, Class<T> toClass) {
		return getGson().fromJson(new InputStreamReader(reader), toClass);
	}
	
	public static <T> T fromJson(InputStream reader, Type toClass) {
		return getGson().fromJson(new InputStreamReader(reader), toClass);
	}
	
	public static <T> T fromJson(JsonElement el, Class<T> toClass) {
		return getGson().fromJson(el, toClass);
	}
	
	public static <T> T fromJson(JsonElement el, Type type) {
		return getGson().fromJson(el, type);
	}
	
}
