package googleLocation;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {

	
	GoogleLocationAccess googleLoc = GoogleLocationAccess.getInstance();

	@Override
	public void start(Promise<Void> startPromise) throws Exception {

		
		int port = config().getInteger("http.port", 8889);
		// Create a router object.
		Router router = Router.router(vertx);
		
		router.post("/data/warp10/load/:peopleName").handler(BodyHandler.create()); // read body
		router.post("/data/warp10/load/:peopleName").handler(this::loadwarp10); // load data to warp10
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
	 * Load saved data to warp10
	 * @param routingContext : 
	 */
	private void loadwarp10(RoutingContext routingContext) {
		String bod = routingContext.getBodyAsString();
		String peopleName = routingContext.request().getParam("peopleName");
		Future<Void> fut = googleLoc.loadDataToWarp10(vertx, bod, peopleName);
		
		fut.setHandler(ar -> {
			if (ar.succeeded()) {
				routingContext.response().setStatusCode(200).putHeader("content-type", "application/json; charset=utf-8")
				.end(Json.encodePrettily(peopleName));
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
		Future<Void> fut = googleLoc.resetWarp(vertx);
		
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
