package oms.warp10;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import io.vertx.core.json.JsonArray;

public class Warp10Token {
	
	static final String tokenFileName = "data/warp10/token_oms.json";
	private String readToken;
	private String writeToken;

	public String getReadToken() {
		return readToken;
	}

	public String getWriteToken() {
		return writeToken;
	}

	public Warp10Token() throws FileNotFoundException, IOException {
		String jsonStr = IOUtils.toString(new FileReader(tokenFileName));
		JsonArray jsonArr = new JsonArray(jsonStr);
		if (jsonArr.size() == 0) {
			throw new IOException("bad token file");
		}
		readToken = jsonArr.getJsonObject(0).getJsonObject("READ").getString("token");
		writeToken = jsonArr.getJsonObject(0).getJsonObject("WRITE").getString("token");


	}

}
