package gateway.api.retrofit;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import gateway.api.RetrofitUtils;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Path;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ByteConverterFactoryTest.Application.class, properties = {
})
public class ByteConverterFactoryTest {
	
	private static final org.apache.commons.logging.Log Log = org.apache.commons.logging.LogFactory.getLog(ByteConverterFactoryTest.class);

	@Autowired
	private TestInterface testInterface;
	
	@Test
	public void testByteConverterFactory() throws IOException {
		byte [] bytes = testInterface.getProcessDefinitionImage("process:5:35008");
		
		FileOutputStream fs = null;
		try {
			fs = new FileOutputStream(new File("d:\\test.png"));
			IOUtils.write(bytes, fs);
		} finally {
			IOUtils.closeQuietly(fs);
		}
		Log.info(testInterface.getProcessDefinitionImage("process:5:35008"));
	}

	@Test
	public void testResponseBody() throws IOException {
		ResponseBody body = testInterface.getProcessDefinitionBpmnXML("process:5:35008");
		Log.info(body.string());
	}

	@SpringBootApplication
	@Configuration
	@Configurable
	public static class Application {
		
		@Bean
		public Retrofit retrofit() {
			return RetrofitUtils.createRetrofit("http://localhost:8888/activiti");
		}
		
		@Bean
		public TestInterface testInterface(Retrofit retrofit) {
			return retrofit.create(TestInterface.class);
		}
		
	}
	
	public interface TestInterface {
		
		@GET("api/repository/process-definitions/{proc_id}/image")
		byte[] getProcessDefinitionImage(
				@Path("proc_id")String proc_id);
		
		@GET("api/repository/process-definitions/{proc_id}/resourcedata")
		ResponseBody getProcessDefinitionBpmnXML(
				@Path("proc_id")String proc_id);

	}

}
