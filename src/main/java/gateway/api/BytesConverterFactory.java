package gateway.api;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.apache.commons.lang3.reflect.TypeUtils;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

public class BytesConverterFactory extends Converter.Factory {

	private static final MediaType MEDIA_TYPE = MediaType.parse("application/octet-stream");

	public static Converter.Factory create() {
		return new BytesConverterFactory();
	}
	
	@Override
	public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations, Retrofit retrofit) {
		if (byte[].class.equals(type)) {
			return new Converter<ResponseBody, byte[]>() {
				@Override
				public byte[] convert(ResponseBody value) throws IOException {
					return value.bytes();
				}
			};
		}
		return super.responseBodyConverter(type, annotations, retrofit);
	}

	@Override
	public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations, Annotation[] methodAnnotations, Retrofit retrofit) {
		if (TypeUtils.isAssignable(byte[].class, type)) {
			return new Converter<byte[], RequestBody>() {
				@Override
				public RequestBody convert(byte[] value) throws IOException {
					return RequestBody.create(MEDIA_TYPE, value);
				}
			};
		}
		return super.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit);
	}

}
