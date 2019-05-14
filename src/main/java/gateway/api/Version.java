package gateway.api;

public class Version {
	
	public static final String NAME = "ApiHelper";

  // 产品主版本号
  public static final int MAJOR = 1;

  // 产品子版本号
  public static final int MINOR = 3;

  // 产品修订版本号
  public static final int REVSION = 5;

  // 产品编译版本号
  public static final int BUILD = 514;
  
  // 产品版本字符串
  public static final String STRING = String.format("%d.%d.%d.%d", MAJOR, MINOR, REVSION, BUILD);

}
