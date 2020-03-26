package oms;

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
import oms.warp10.Warp10Client;

public class OmsAccess {

	Set<String> countryNames = Collections.synchronizedSet(new TreeSet<String>());

	static final String countriesNameFilePath = "data/oms/countriesName.json";
	static final String dataRootPath = "data/oms/";

	/**
	 * @return the set of counties
	 */
	public Set<String> getCountryNames() {
		return countryNames;
	}

	/**
	 * prvate constructor
	 */
	private OmsAccess() {
	}

	/**
	 * The instance of OmsAccess
	 */
	private static OmsAccess INSTANCE = new OmsAccess();

	public static OmsAccess getInstance() {
		return INSTANCE;
	}

//	curl 'https://services.arcgis.com/5T5nSi527N4F7luB/arcgis/rest/services/Cases_by_country_Plg_V3/FeatureServer/0/query?f=json&where=1%3D1&returnGeometry=false&outFields=*&orderByFields=cum_conf%20desc&resultOffset=0&resultRecordCount=2000&cacheHint=true'  --compressed
	/**
	 * Download json file from who containing all the country names
	 * 
	 * @param vertx
	 * @return
	 */
	public Future<Void> downloadCountries(Vertx vertx) {
		Promise<Void> promise = Promise.promise();

		countryNames.clear();

		WebClient client = WebClient.create(vertx);
		client.getAbs(
				"https://services.arcgis.com/5T5nSi527N4F7luB/arcgis/rest/services/Cases_by_country_Plg_V3/FeatureServer/0/query?f=json&where=1%3D1&returnGeometry=false&outFields=*&orderByFields=cum_conf%20desc&resultOffset=0&resultRecordCount=2000&cacheHint=true")
				.send(ar -> {
					if (ar.succeeded()) {
						// Obtain response
						HttpResponse<Buffer> response = ar.result();

						System.out.println("Received response with status code" + response.statusCode());
						Path path = Paths.get(countriesNameFilePath);
						try {
							Files.write(path, response.bodyAsBuffer().getBytes());
						} catch (IOException e) {
							promise.fail(e);
							e.printStackTrace();
						}
						promise.complete();
					} else {
						System.out.println("Something went wrong " + ar.cause().getMessage());
						promise.fail(ar.cause());
					}
				});
		return promise.future();
	}

	/**
	 * Read the local json file containing all country name and load it in
	 * countryNames
	 * 
	 * @param vertx
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public void readCountry() throws FileNotFoundException, IOException {

		String jsonStr = IOUtils.toString(new FileReader(countriesNameFilePath));
		JsonObject jsonObj = new JsonObject(jsonStr);
		countryNames.clear();

		JsonArray jObjArr = jsonObj.getJsonArray("features");
		jObjArr.forEach(obj -> {
			String name = ((JsonObject) obj).getJsonObject("attributes").getString("ADM0_NAME");
			if (!name.contains("(") && !name.contains("[")&& !name.contains("'")) { // les donnees oms sont pourrie si le nom contient un (
				System.out.println("Country " + name);
				countryNames.add(name);
			}
		});
	}

// historique pour 1 pays
//	curl 'https://services.arcgis.com/5T5nSi527N4F7luB/arcgis/rest/services/Historic_adm0_v3/FeatureServer/0/query?f=json&where=ADM0_NAME%3D%27France%27&outFields=*&orderByFields=DateOfDataEntry%20asc&resultOffset=0&resultRecordCount=20000&cacheHint=true'
	/**
	 * download json containing all data of 1 country
	 * 
	 * @param vertx
	 * @param countryName the name of the country
	 * @return
	 */
	public Future<Void> downloadData(Vertx vertx, String countryName) {
		Promise<Void> promise = Promise.promise();

		String url;
		try {
			url = "https://services.arcgis.com/5T5nSi527N4F7luB/arcgis/rest/services/Historic_adm0_v3/FeatureServer/0/query?f=json&where=ADM0_NAME%3D%27"
					+ URLEncoder.encode(countryName, "UTF-8")
					+ "%27&outFields=*&orderByFields=DateOfDataEntry%20asc&resultOffset=0&resultRecordCount=20000&cacheHint=true";
		} catch (UnsupportedEncodingException e) {
			promise.fail(e);
			e.printStackTrace();
			promise.complete();
			return promise.future();
		}

		WebClient client = WebClient.create(vertx);
		client.getAbs(url).send(ar -> {
			if (ar.succeeded()) {
				// Obtain response
				HttpResponse<Buffer> response = ar.result();

				System.out.println("Received response for : " + countryName);
				Path path = Paths.get(dataRootPath, countryName + ".json");
				try {
					Files.write(path, response.bodyAsBuffer().getBytes());
				} catch (IOException e) {
					promise.fail(e);
					e.printStackTrace();
				}
				promise.complete();
			} else {
				System.out.println("Something went wrong for " + countryName + " : " + ar.cause().getMessage());
				promise.fail(ar.cause());
			}
		});
		return promise.future();
	}

	/**
	 * Download all data all countries (sequentially, due to my bandwith...)
	 * 
	 * @param vertx
	 * @return
	 */
	public Future<Void> downloadall(Vertx vertx) {
		Promise<Void> promise = Promise.promise();

		Future<Void> fut = downloadCountries(vertx);

		fut.setHandler(ar -> {
			if (ar.succeeded()) {
				try {
					readCountry();
				} catch (IOException e) {
					promise.fail(e);
					promise.complete();
				}
				// chaine les requetes les une apres les autres....
				Future<Void> fut2 = fut;
				for (String name : countryNames) {
					fut2 = fut2.compose((ok) -> {
						return downloadData(vertx, name);
					});
				}
				// le dernier met a jour le promise
				fut2.setHandler(ar2 -> {
					promise.complete();
				});
			} else {
				promise.fail(ar.cause());
				promise.complete();
			}
		});

		return promise.future();
	}

	/**
	 * return gts data for a country 1582502400000/2.55042646:46.56487403/
	 * oms.cum_conf{country=France} 12475 1582502400000/2.55042646:46.56487403/
	 * oms.NewCase{country=France} 1598
	 * 
	 * @param countryName
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public String getGtsData(String countryName) throws FileNotFoundException, IOException {
		String jsonStr = IOUtils.toString(new FileReader(dataRootPath + countryName + ".json"));
		JsonArray jObjArr = new JsonObject(jsonStr).getJsonArray("features");
		StringBuilder res = new StringBuilder();
		jObjArr.forEach(obj -> {
			JsonObject attributes = ((JsonObject) obj).getJsonObject("attributes");
			int cum_conf = attributes.getInteger("cum_conf");
			int NewCase = attributes.getInteger("NewCase");
			float lon = attributes.getFloat("CENTER_LON");
			float lat = attributes.getFloat("CENTER_LAT");
			long Time = attributes.getLong("DateOfDataEntry");
			res.append(Time*1000).append('/').append(lat).append(':').append(lon).append("/ oms.cum_conf{country=")
					.append(countryName).append("} ").append(cum_conf).append("\n");
			res.append(Time*1000).append('/').append(lat).append(':').append(lon).append("/ oms.NewCase{country=")
					.append(countryName).append("} ").append(NewCase).append("\n");
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
	public Future<Void> loadDataToWarp10(Vertx vertx, String countryName) throws FileNotFoundException, IOException {
		Promise<Void> promise = Promise.promise();

		Future<Void> fut = Warp10Client.getInstance().loadDataToWarp(vertx, getGtsData(countryName)); // pas terrible de
																										// tous mettre
																										// dans une
																										// variable,
																										// faudrait un
																										// stream

		fut.setHandler(ar -> {
			if (ar.failed()) {
				promise.fail("loadDataToWarp failed");
			} else {
				System.out.println(countryName + " loaded in warp10");
			}
			promise.complete();
		});

		return promise.future();

	}

	/**
	 * Download all data all countries (sequentially, due to my bandwith...)
	 * 
	 * @param vertx
	 * @return
	 */
	public Future<Void> loadAllDataToWarp(Vertx vertx) {
		Promise<Void> promise = Promise.promise();

		try {
			readCountry();
		} catch (IOException e) {
			promise.fail(e);
			promise.complete();
			return promise.future();
		}

		Future<Void> fut = null;
		for (String name : countryNames) {
			if (fut == null) {
				try {
					fut = loadDataToWarp10(vertx, name);
					System.out.println("loadDataToWarp10 requested for : " + name);
				} catch (IOException e) {
					e.printStackTrace();
					promise.fail(e);
					promise.complete();
					return promise.future();
				}
			} else {
				fut = fut.compose((ok) -> {
					try {
						System.out.println("loadDataToWarp10 requested for : " + name);
						return loadDataToWarp10(vertx, name);
					} catch (IOException e) {
						e.printStackTrace();
						promise.fail(e);
						promise.complete();
						return promise.future();
					}
				});

			}
		}
		fut.setHandler(ar2 -> {
			promise.complete();
		});

		return promise.future();
	}

	/***
	 * reset all oms data
	 * @param vertx
	 * @return
	 */
	public Future<Void> resetWarp(Vertx vertx) {
		Promise<Void> promise = Promise.promise();
		
		Future<Void> fut = Warp10Client.getInstance().DeleteAllGtsOfClass(vertx, "oms.cum_conf");
		System.out.println("DeleteAllGtsOfClass requested for : " + "oms.cum_conf");
		fut = fut.compose((ok)-> {
			System.out.println("DeleteAllGtsOfClass requested for : " + "oms.NewCase");
			return Warp10Client.getInstance().DeleteAllGtsOfClass(vertx, "oms.NewCase");
		});
		fut.setHandler(ar2 -> {
			promise.complete();
		});

		return promise.future();
	}

}
