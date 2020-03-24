package com.example.starter;

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

public class OmsAccess {

	Set<String> countryNames = Collections.synchronizedSet(new TreeSet<String>());

	static final String countriesNameFilePath = "data/oms/countriesName.json";
	static final String dataRootPath = "data/oms/";

	public Set<String> getCountryNames() {
		return countryNames;
	}

	private OmsAccess() {
	}

	private static OmsAccess INSTANCE = new OmsAccess();

	public static OmsAccess getInstance() {
		return INSTANCE;
	}

//	curl 'https://services.arcgis.com/5T5nSi527N4F7luB/arcgis/rest/services/Cases_by_country_Plg_V3/FeatureServer/0/query?f=json&where=1%3D1&returnGeometry=false&outFields=*&orderByFields=cum_conf%20desc&resultOffset=0&resultRecordCount=2000&cacheHint=true'  --compressed
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

	public void readCountry(Vertx vertx) throws FileNotFoundException, IOException {

		String jsonStr = IOUtils.toString(new FileReader(countriesNameFilePath));
		JsonObject jsonObj = new JsonObject(jsonStr);
		countryNames.clear();

		JsonArray jObjArr = jsonObj.getJsonArray("features");
		jObjArr.forEach(obj -> {
			String name = ((JsonObject) obj).getJsonObject("attributes").getString("ADM0_NAME");
			System.out.println("Country " + name);
			countryNames.add(name);
		});
	}

// historique pour 1 pays
//	curl 'https://services.arcgis.com/5T5nSi527N4F7luB/arcgis/rest/services/Historic_adm0_v3/FeatureServer/0/query?f=json&where=ADM0_NAME%3D%27France%27&outFields=*&orderByFields=DateOfDataEntry%20asc&resultOffset=0&resultRecordCount=20000&cacheHint=true'
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

	public Future<Void> downloadall(Vertx vertx) {
		Promise<Void> promise = Promise.promise();

		Future<Void> fut = downloadCountries(vertx);

		fut.setHandler(ar -> {
			if (ar.succeeded()) {
				try {
					readCountry(vertx);
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

}
