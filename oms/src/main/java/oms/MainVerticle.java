package oms;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class MainVerticle extends AbstractVerticle {

	
	OmsAccess oms = OmsAccess.getInstance();


	@Override
	public void start(Promise<Void> startPromise) throws Exception {

		
		int port = config().getInteger("http.port", 8888);
		// Create a router object.
		Router router = Router.router(vertx);
		
		router.get("/data/download").handler(this::downloadOmsData); // download data from oms
		router.get("/data/warp10/load").handler(this::loadwarp10); // load data to warp10
		router.get("/data/warp10/reset").handler(this::resetwarp10); // reset all data in warp10


		// Create the HTTP server and pass the "accept" method to the request handler.
		vertx.createHttpServer().requestHandler(router::accept).listen(
				// Retrieve the port from the configuration,
				// default to 8080.
				port, result -> {
					if (result.succeeded()) {
						startPromise.complete();
						System.out.println("server started on " + port);
					} else {
						startPromise.fail(result.cause());
					}
				});

	}

	/**
	 * Dowload data for oms and save it locally
	 * @param routingContext
	 */
	private void downloadOmsData(RoutingContext routingContext) {
		Future<Void> fut = oms.downloadall(vertx);
		
		fut.setHandler(ar -> {
			if (ar.succeeded()) {
				routingContext.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
				.end(Json.encodePrettily(oms.countryNames));
			}else {
				routingContext.response().setStatusCode(500).end();
			}
		});
		
	}

	
	/**
	 * Load saved data to warp10
	 * @param routingContext
	 */
	private void loadwarp10(RoutingContext routingContext) {
		Future<Void> fut = oms.loadAllDataToWarp(vertx);
		
		fut.setHandler(ar -> {
			if (ar.succeeded()) {
				routingContext.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
				.end(Json.encodePrettily(oms.countryNames));
			}else {
				routingContext.response().setStatusCode(500).end();
			}
		});
		
	}

	/**
	 * Reset warp10 data
	 * @param routingContext
	 */
	private void resetwarp10(RoutingContext routingContext) {
		Future<Void> fut = oms.resetWarp(vertx);
		
		fut.setHandler(ar -> {
			if (ar.succeeded()) {
				routingContext.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
				.end("{}");
			}else {
				routingContext.response().setStatusCode(500).end();
			}
		});
		
	}


}
