package gateway.api;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.google.gson.internal.bind.util.ISO8601Utils;


public class JsonDateTypeAdapter implements JsonSerializer<Date>, JsonDeserializer<Date> {

  private final DateFormat localFormat;
  
  private final DateFormat fixedFormat1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
  private final DateFormat fixedFormat2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  private final DateFormat fixedFormat3 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  private final DateFormat fixedFormat4 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
  private final DateFormat fixedFormat5 = new SimpleDateFormat("yyyy-MM-dd");

  public JsonDateTypeAdapter(String datePattern) {
    this(new SimpleDateFormat(datePattern));
  }


  JsonDateTypeAdapter(DateFormat localFormat) {
    this.localFormat = localFormat;
  }

  // These methods need to be synchronized since JDK DateFormat classes are not thread-safe
  // See issue 162
  @Override
  public JsonElement serialize(Date src, Type typeOfSrc, JsonSerializationContext context) {
    synchronized (localFormat) {
      String dateFormatAsString = localFormat.format(src);
      return new JsonPrimitive(dateFormatAsString);
    }
  }

  @Override
  public Date deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
      throws JsonParseException {
    if (!(json instanceof JsonPrimitive)) {
      throw new JsonParseException("The date should be a string value");
    }
    Date date = deserializeToDate(json);
    if (typeOfT == Date.class) {
      return date;
    } else if (typeOfT == Timestamp.class) {
      return new Timestamp(date.getTime());
    } else if (typeOfT == java.sql.Date.class) {
      return new java.sql.Date(date.getTime());
    } else {
      throw new IllegalArgumentException(getClass() + " cannot deserialize to " + typeOfT);
    }
  }

  private Date deserializeToDate(JsonElement json) {
    synchronized (localFormat) {
      try {
      	return localFormat.parse(json.getAsString());
      } catch (ParseException ignored) {}
      try {
      	return fixedFormat1.parse(json.getAsString());
      } catch (ParseException ignored) {}
      try {
      	return fixedFormat2.parse(json.getAsString());
      } catch (ParseException ignored) {}
      try {
      	return fixedFormat3.parse(json.getAsString());
      } catch (ParseException ignored) {}
      try {
      	return fixedFormat4.parse(json.getAsString());
      } catch (ParseException ignored) {}
      try {
      	return fixedFormat5.parse(json.getAsString());
      } catch (ParseException ignored) {}
      try {
        return ISO8601Utils.parse(json.getAsString(), new ParsePosition(0));
      } catch (ParseException e) {
        throw new JsonSyntaxException(json.getAsString(), e);
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(JsonDateTypeAdapter.class.getSimpleName());
    sb.append('(').append(localFormat.getClass().getSimpleName()).append(')');
    return sb.toString();
  }

}
