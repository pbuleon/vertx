package googleLocation;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import warp10.client.Warp10Client;

public class GoogleLocationAccess {

	/**
	 * prvate constructor
	 */
	private GoogleLocationAccess() {
	}

	/**
	 * The instance of OmsAccess
	 */
	private static GoogleLocationAccess INSTANCE = new GoogleLocationAccess();

	public static GoogleLocationAccess getInstance() {
		return INSTANCE;
	}

	/**
	 * return gts data from google locations 1469849960727/48.79349466:-3.4716841/
	 * oms.NewCase{people=Patrice} 1598
	 * 
	 * @param countryName
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public String getGtsData(String jsonData, String peopleName) {
		System.out.println("getGtsData");
		JsonArray jObjArr = new JsonObject(jsonData).getJsonArray("locations");
		StringBuilder res = new StringBuilder();
		jObjArr.forEach(obj -> {
//			JsonObject obj = jObjArr.getJsonObject(i);
			int accuracy = ((JsonObject) obj).getInteger("accuracy");
			float lon = (((JsonObject) obj).getLong("longitudeE7").floatValue()) / 10000000;
			float lat = (((JsonObject) obj).getLong("latitudeE7").floatValue()) / 10000000;
			System.out.println("avant timestampMs");
			String Time = ((JsonObject) obj).getString("timestampMs");
//			System.out
//					.println("" + Time + "/" + lat + ":" + lon + "/ google_loc{people=" + peopleName + "} " + accuracy);
			res.append(Time).append("000/").append(lat).append(':').append(lon).append("/ google_loc{people=")
					.append(peopleName).append("} ").append(accuracy).append("\n");
		});
		return res.toString();
	}

	/**
	 * Load oms data of 1 country to warp10
	 * 
	 * @param vertx
	 * @param countryName
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public Future<Void> loadDataToWarp10(Vertx vertx, String jsonData, String peopleName) {
		Promise<Void> promise = Promise.promise();

		Future<Void> fut = Warp10Client.getInstance().loadDataToWarp(vertx, getGtsData(jsonData, peopleName)); // pas
																												// terrible
																												// de
		// tous mettre
		// dans une
		// variable,
		// faudrait un
		// stream

		fut.setHandler(ar -> {
			if (ar.failed()) {
				promise.fail("loadDataToWarp failed");
			} else {
				System.out.println(peopleName + " loaded in warp10");
			}
			promise.complete();
		});

		return promise.future();

	}

	/***
	 * reset all oms data
	 * 
	 * @param vertx
	 * @return
	 */
	public Future<Void> resetWarp(Vertx vertx) {
		Promise<Void> promise = Promise.promise();

		Future<Void> fut = Warp10Client.getInstance().DeleteAllGtsOfClass(vertx, "google_loc");
		System.out.println("DeleteAllGtsOfClass requested for : " + "google_loc");
		fut.setHandler(ar2 -> {
			promise.complete();
		});

		return promise.future();
	}

}
