基于应用网关的API调用帮助库
================================

### 一、使用方法

#### 1、MAVEN工程配置

> 在pom中加入私仓配置：

```
  <repositories>
    <repository>
      <id>nexus</id>
      <name>Nexus</name>
      <url>https://api.hngytobacco.com/nexus/content/groups/public/</url>
      <releases>
        <enabled>true</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </repository>
  </repositories>
  <pluginRepositories>
    <pluginRepository>
      <id>nexus</id>
      <name>Nexus</name>
      <url>https://api.hngytobacco.com/nexus/content/groups/public/</url>
      <releases>
        <enabled>true</enabled>
      </releases>
      <snapshots>
        <enabled>true</enabled>
      </snapshots>
    </pluginRepository>
  </pluginRepositories>
```

> 在依赖中加入下属配置：

```
<dependency>
  <groupId>api.gateway</groupId>
  <artifactId>apihelper</artifactId>
  <version>1.3.0</version>
  <type>pom</type>
</dependency>
```

#### 2、直接引用

下载[apihelper-1.3.0-all.jar](https://api.hngytobacco.com/nexus/service/local/repo_groups/public/content/api/gateway/apihelper/1.3.0/apihelper-1.3.0-all.jar
)文件并放到classpath中。

### 二、基本功能

供第三方应用通过网关调用API时进行安全认证时使用。

提供pClient复用链接实现（需要HttpClient4.5.2以上版本）。

提供Retrofit2注解式RestAPI客户端实现（依赖OKHttpClient3.10版以上）。

通过网关调用的一些通用工具工具类（需要Spring支持）。

### 三、RSA工具类

参见`gateway.api.RSAUtils`类，用于基本的RSA密钥加载操作。

#### 1、PublicKey parsePublicKey(String pub)

加载PKCS8格式的RSA公钥。

#### 2、PublicKey parsePublicKeyFromPEM(String pub)

加载PEM格式的RSA公钥。

#### 3、PrivateKey parsePrivateKey(String priv)

加载PKCS8格式的RSA私钥。

#### 4、PrivateKey parsePrivateKeyFromPEM(String priv)

加载PEM格式的RSA私钥。

### 四、创建JWT令牌

##### 1、加载RSA私钥

使用RSAUtils工具加载RSA私钥:

 - PEM格式

```java
RSAKey rsakey = (RSAKey)gateway.RSAUtils.parsePrivateKey(" PEM of your rsa privatekey")
```

 - PKCS8格式
 
```java
RSAKey rsakey = (RSAKey)gateway.RSAUtils.parsePrivateKey("PKCS8 of your rsa privatekey");
```
 
##### 2、创建JWT上下文
 
```java
JwtContext jwt_context = gateway.JwtContext.create("your apptoken", rsakey, 1200);
```
 
##### 3、创建JWT令牌

``` 
JwtToken jwt_token = jwt_context.createJwtToken();
```

 - 获得`Authorization`头需要的字符串
 
```
jwt_token.toAuthorizationString();
```
 - 获得`jwt`请求参数需要的字符串
 
```
jwt_token.toParameterString();
```
 
 - 判断令牌是否过期

```
jwt_token.isExpired();
```

注：如果令牌已过期请使用上下文重新创建。

### 五、使用复用的HttpClient连接

参见HttpClientHelper的requestExecute方法。 

### 六、使用Retrofit2实现服务客户端

Retrofit2大大简化了基于REST接口的客户端开发过程，因此建议使用此方式调用通过应用网关提供的API。

> 构造在请求头中加入JWT令牌认证的OkHttpClient对象：

```

  OkHttpClient client = new OkHttpClient.Builder().addInterceptor(
      new OkHttpClientJwtInterceptor(jwtContext)).build();

```

_注：jwtContext请提前创建_

> 然后构造Retrofit2服务实例：

```

Retrofit retrofit = new Retrofit.Builder().baseUrl(gatweayURLPrefix)
          .addCallAdapterFactory(SynchCallAdapterFactory.create())
          .addConverterFactory(GsonConverterFactory.create(JsonUtils.getGson())).client(client).build();

```

> 准备好已注解的客户端接口类：

```

import gateway.api.Return; 
import gateway.api.NotExceptException;

public interface TestService {

  @GET("/test/interface")
  public Return<Map<String, Object>> test() throws NotExceptException;
  
}

```

> 然后通过Retrofit2实例获取委派实现：

```
TestService proxy_impl = retrofit.create(TestService.class);
proxy_impl.test();
```

其他更多的Retrofit2资料请参见[Retrofit2官网](http://square.github.io/retrofit/)

### 七、服务网关通用的工具类

参见`gateway.api.ViaGatewayUtlis`类实现。





 
