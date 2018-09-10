package gateway.api;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Date;
import java.text.SimpleDateFormat;

import org.apache.commons.lang3.reflect.TypeUtils;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

@SuppressWarnings({"rawtypes", "unchecked"})
public class GsonConverterFactory  extends Converter.Factory {
  /**
   * Create an instance using a default {@link Gson} instance for conversion. Encoding to JSON and
   * decoding from JSON (when no charset is specified by a header) will use UTF-8.
   */
  public static GsonConverterFactory create() {
    return create(JsonUtils.getGson(), JsonUtils.JsonDateFormat);
  }

  /**
   * Create an instance using {@code gson} for conversion. Encoding to JSON and
   * decoding from JSON (when no charset is specified by a header) will use UTF-8.
   */
  public static GsonConverterFactory create(Gson gson, String json_date_format) {
    if (gson == null) throw new NullPointerException("gson == null");
    return new GsonConverterFactory(gson, json_date_format);
  }

  private final Gson gson;
  
  private final DateToStringConverter dateToStringConverter;

  private GsonConverterFactory(Gson gson, String json_date_format) {
    this.gson = gson;
    this.dateToStringConverter = new DateToStringConverter(json_date_format);
  }

	@Override
  public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
      Retrofit retrofit) {
    TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
    return new GsonResponseBodyConverter(gson, adapter);
  }

	@Override
  public Converter<?, RequestBody> requestBodyConverter(Type type,
      Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
    TypeAdapter<?> adapter = gson.getAdapter(TypeToken.get(type));
    return new GsonRequestBodyConverter(gson, adapter);
  }

	@Override
	public Converter<?, String> stringConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
		if (TypeUtils.isAssignable(type, Date.class)) {
			return this.dateToStringConverter;
		} else { 
			return super.stringConverter(type, annotations, retrofit);
		}
	}
	
  public static final class DateToStringConverter implements Converter<Date, String> {
  	
    public static final DateToStringConverter INSTANCE = new DateToStringConverter();
    
    private String json_date_format = JsonUtils.JsonDateFormat;
    
    private DateToStringConverter() {}
    
    public DateToStringConverter(String json_date_format) {
			this.json_date_format = json_date_format;
		}

		@Override public String convert(Date value) {
    	if (value == null) return null;
      return new SimpleDateFormat(json_date_format).format(value);
    }
		
  }
  
}