
### 功能说明

供第三方应用通过网关调用API时进行安全认证时使用。

HttpClient复用链接实现（需要HttpClient4.5.2以上版本）

#### RSA工具类

参见`gateway.RSAUtils`类

#### 创建JWT令牌

##### 加载RSA私钥

使用RSAUtils工具加载RSA私钥:

 - PEM格式

```java
RSAKey rsakey = (RSAKey)gateway.RSAUtils.parsePrivateKey(" PEM of your rsa privatekey")
```

 - PKCS8格式
 
```java
RSAKey rsakey = (RSAKey)gateway.RSAUtils.parsePrivateKey("PKCS8 of your rsa privatekey");
```
 
 ##### 创建JWT上下文
 
```java
JwtContext jwt_context = gateway.JwtContext.create("your apptoken", rsakey, 1200);
```
 
 ##### 创建JWT令牌

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

### 使用复用的HttpClient连接

参见HttpClientHelper的requestExecute方法。 

### 资源下载



 
