package com.example.starter;

import java.util.TreeSet;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class OmsAccess {
	
	TreeSet<String> countryNames = new TreeSet<String>();

	public TreeSet<String> getCountryNames() {
		return countryNames;
	}


	private OmsAccess() {
	}

	private static OmsAccess INSTANCE = new OmsAccess();

	public static OmsAccess getInstance() {
		return INSTANCE;
	}
// historique pour 1 pays
//	curl 'https://services.arcgis.com/5T5nSi527N4F7luB/arcgis/rest/services/Historic_adm0_v3/FeatureServer/0/query?f=json&where=ADM0_NAME%3D%27France%27&outFields=*&orderByFields=DateOfDataEntry%20asc&resultOffset=0&resultRecordCount=20000&cacheHint=true'
	
	
//	curl 'https://services.arcgis.com/5T5nSi527N4F7luB/arcgis/rest/services/Cases_by_country_Plg_V3/FeatureServer/0/query?f=json&where=1%3D1&returnGeometry=false&outFields=*&orderByFields=cum_conf%20desc&resultOffset=0&resultRecordCount=2000&cacheHint=true'  --compressed
	public Future<Void> initCountry(Vertx vertx) {
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
						JsonArray jObjArr = response.bodyAsJsonObject().getJsonArray("features");
						jObjArr.forEach(obj -> {
							String name=((JsonObject)obj).getJsonObject("attributes").getString("ADM0_NAME");
							System.out.println("Country " + name);
							countryNames.add(name);
						});
						promise.complete();
					} else {
						System.out.println("Something went wrong " + ar.cause().getMessage());
						promise.fail(ar.cause());
					}
				});
		return promise.future();

	}
}
