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
		
		boolean is_page_resp = false; 
		for (Annotation ann : annotations) {
			if (ann != null && ann instanceof OnlyhPageList) {
				is_page_resp = true;
				break;
			}
		}
		
		if (is_envelope_resp) {
			if (returnType instanceof Result) throw new IllegalStateException("Return type error when use @EnvelopeResponse!");
			return new EnvelopeCallAdapter(returnType, is_page_resp);
		}
				
		return new ObjectCallAdapter(returnType, is_page_resp); 
	}
	
	private static class EnvelopeCallAdapter implements CallAdapter<Object, Object> {
		
		private Type responseType;
		
		private boolean is_page_resp = false;
		
		public EnvelopeCallAdapter(Type responseType, boolean is_page_resp) {
			this.responseType = responseType;
			this.is_page_resp = is_page_resp; 
			if (this.is_page_resp && responseType instanceof ParameterizedType) {
				this.responseType = TypeUtils.parameterize(Page.class, 
						TypeUtils.getTypeArguments((ParameterizedType) responseType).entrySet().iterator().next().getValue());
			}
		}
		
		@Override
		public Type responseType() {
			return EnvelopeReturn.class;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object adapt(Call<Object> call) {
			try {
				Response<Object> resp = call.execute();
				EnvelopeReturn ersp = (EnvelopeReturn)resp.body();
				if (!(resp.code() == 200 || resp.code() == 201) || ersp == null || ersp.code != 0) {
					if (resp.code() == 404 || ersp != null && ersp.code == 404) {
						throw new NotFoundException(ersp == null ? resp.message() : ersp.message);
					}
					if (ersp == null) {
						ersp = JsonUtils.fromJson(resp.errorBody().byteStream(), EnvelopeReturn.class);
					}
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
					if (val instanceof Page && is_page_resp)  {
						val = ((Page<Object>)(val)).data;
					}
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
		
		private boolean is_page_list = false;
		
		public ObjectCallAdapter(Type responseType, boolean is_page_list) {
			this.responseType = responseType;
			this.is_page_list = is_page_list;
		}
		
		@Override
		public Type responseType() {
			return responseType;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object adapt(Call<Object> call) {
			try {
				Response<Object> resp = call.execute();
				if (responseType instanceof Result) {
					return resp.body(); 
				} else if (responseType == Void.class) {
					if (!(resp.code() == 200 || resp.code() == 201)) {
						if (resp.code() == 404) throw new NotFoundException(resp.message());
						return new NotExceptException(resp.code(), resp.message());
					}
					return null;
				} else if (!(resp.code() == 200 || resp.code() == 201)) {
					if (resp.code() == 404) throw new NotFoundException(resp.message());
					throw new NotExceptException(resp.code(), resp.message());
				}
				Object val = resp.body();
				if (val instanceof Page && is_page_list) {
					val = ((Page<Object>)val).data;
				}
				return val;
			} catch (IOException e) {
				if (responseType instanceof Result) {
					return new Result(NotExceptException.CLIENT_START_ERROR, e.getMessage()); 
				}
				throw new NotExceptException(NotExceptException.CLIENT_START_ERROR, e.getMessage(), e);
			}
		}
		
	};

}
