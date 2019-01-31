package util;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Map;

/**
 * Interface to offer JSON serialization/deserialization methods.
 * 
 * @author aminfaez
 *
 */
public interface JsonSerializable {

	public JSONObject toJson();

	public void fromJson(Map<String, Object> data);

	public default String toJsonString() {
		return toJson().toJSONString();
	}

	@SuppressWarnings("unchecked")
	public default void fromJsonString(String jsonString) {
		try {
			fromJson((JSONObject) new JSONParser().parse(jsonString));
		} catch (ParseException e) {
			throw new RuntimeException("Error parsing json String: " + jsonString);
		}
	}
}
