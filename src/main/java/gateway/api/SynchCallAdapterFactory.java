package gateway.api;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

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

		final Type responseType = returnType;
		return new CallAdapter<Object, Object>() {
			
			@Override
			public Type responseType() {
				return responseType;
			}

			@Override
			public Object adapt(Call<Object> call) {
				try {
					Response<Object> resp = call.execute();
					if (responseType == Void.class) {
						if (resp.code() != 200) {
							return new NotExceptException(resp.code(), resp.message());
						}
						return null;
					}

					if (responseType instanceof Result && resp.code() != 200) {
						Result ret = (Result) resp.body();
						if (ret == null) {
							ret = new Result(resp.code(), resp.message());
						}
						throw new NotExceptException(ret);
					}
					
					if (resp.code() != 200) {
						throw new NotExceptException(resp.code(), resp.message());
					}
					
					return resp.body();
				} catch (IOException e) {
					throw new NotExceptException(NotExceptException.CLIENT_START_ERROR, e.getMessage(), e);
				}
			}
			
		};
		
	}

}
