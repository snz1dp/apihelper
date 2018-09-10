package gateway.api;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

public abstract class JsonUtils {
	
	public static final String JsonDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS";

	private static Gson gson = new GsonBuilder()
			.enableComplexMapKeySerialization()
			.setDateFormat(JsonDateFormat)
      .create();
	
	public static Gson getGson() {
		return gson;
	}
	
	public static <T> String toJson(T object) {
		return getGson().toJson(object);
	}
	
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
