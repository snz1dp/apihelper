package gateway.api;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;

import org.apache.commons.lang3.reflect.TypeUtils;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import retrofit2.Response;
import retrofit2.Call;
import retrofit2.CallAdapter;
import retrofit2.Retrofit;

public class SynchCallAdapterFactory extends CallAdapter.Factory {

	public static CallAdapter.Factory create() {
		return new SynchCallAdapterFactory();
	}

	@Override
	public CallAdapter<?, ?> get(Type returnType, Annotation[] annotations, Retrofit retrofit) {
		if (getRawType(returnType) == Call.class) {
			return null;
		}
		
		boolean is_envelope_resp = false; 
		for (Annotation ann : annotations) {
			if (ann != null && ann instanceof EnvelopeResponse) {
				is_envelope_resp = true;
				break;
			}
		}
		
		if (is_envelope_resp) {
			if (returnType instanceof Result) throw new IllegalStateException("Return type error when use @EnvelopeResponse!");
			return new EnvelopeCallAdapter(returnType);
		}
				
		return is_envelope_resp ? null : 
			new ObjectCallAdapter(returnType); 
	}
	
	private static class EnvelopeCallAdapter implements CallAdapter<Object, Object> {
		
		private Type responseType;
		
		public EnvelopeCallAdapter(Type responseType) {
			this.responseType = responseType;
		}
		
		@Override
		public Type responseType() {
			return EnvelopeReturn.class;
		}

		@Override
		public Object adapt(Call<Object> call) {
			try {
				Response<Object> resp = call.execute();
				EnvelopeReturn ersp = (EnvelopeReturn)resp.body();
				if (!(resp.code() == 200 || resp.code() == 201) || ersp == null || ersp.code != 0) {
					throw new NotExceptException(
							ersp == null ? resp.code() : ersp.code, 
							ersp == null ? resp.message() : ersp.message, 
							ersp == null ? new Date() : ersp.timestamp, 
							ersp == null ? null : ersp.exception, 
							ersp == null ? null : ersp.path);
				}
				if (ersp.data == null) return ersp.data;
				
				if (TypeUtils.isArrayType(responseType)) {
					return JsonUtils.fromJson(ersp.data, TypeToken.getArray(TypeUtils.getArrayComponentType(responseType)).getType());
				} else if (responseType instanceof ParameterizedType) {
					Type rawType = TypeUtils.getRawType(responseType, null);
					Object val = JsonUtils.fromJson(ersp.data, 
							TypeToken.getParameterized(rawType, new ArrayList<Type>(
									TypeUtils.getTypeArguments((ParameterizedType)responseType
											).values()).toArray(new Type[0])).getType());
					return val;
				} else if (responseType instanceof Class) {
					return JsonUtils.fromJson(ersp.data, TypeToken.get((Class<?>)responseType).getType());
				}
				return JsonUtils.fromJson(ersp.data, TypeToken.get(responseType).getType());
			} catch (IOException e) {
				throw new NotExceptException(NotExceptException.CLIENT_START_ERROR, e.getMessage(), e);
			}
		}
		
	}
	
	private static class EnvelopeReturn {
		
		public int code;
		
		public String message;
		
		public JsonElement data;
		
		public Date timestamp;
		
		public String exception;
		
		public String path;
		
	}
	
	private static class ObjectCallAdapter implements CallAdapter<Object, Object> {
		
		private Type responseType;
		
		public ObjectCallAdapter(Type responseType) {
			this.responseType = responseType;
		}
		
		@Override
		public Type responseType() {
			return responseType;
		}

		@Override
		public Object adapt(Call<Object> call) {
			try {
				Response<Object> resp = call.execute();
				if (responseType instanceof Result) {
					return resp.body(); 
				} else if (responseType == Void.class) {
					if (!(resp.code() == 200 || resp.code() == 201)) {
						return new NotExceptException(resp.code(), resp.message());
					}
					return null;
				} else if (!(resp.code() == 200 || resp.code() == 201)) {
					throw new NotExceptException(resp.code(), resp.message());
				}
				return resp.body();
			} catch (IOException e) {
				if (responseType instanceof Result) {
					return new Result(NotExceptException.CLIENT_START_ERROR, e.getMessage()); 
				}
				throw new NotExceptException(NotExceptException.CLIENT_START_ERROR, e.getMessage(), e);
			}
		}
		
	};

}
