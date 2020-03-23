package com.example.starter;

import java.util.LinkedHashMap;
import java.util.Map;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class MainVerticle extends AbstractVerticle {

	// Store our product
	private Map<Integer, Whisky> products = new LinkedHashMap<>();

	// Create some product
	private void createSomeData() {
		Whisky bowmore = new Whisky("Bowmore 15 Years Laimrig", "Scotland, Islay");
		products.put(bowmore.getId(), bowmore);
		Whisky talisker = new Whisky("Talisker 57° North", "Scotland, Island");
		products.put(talisker.getId(), talisker);
	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {


		
		int port = config().getInteger("http.port", 8888);
		// Create a router object.
		Router router = Router.router(vertx);

		// Bind "/" to our hello message - so we are still compatible.
		router.route("/").handler(routingContext -> {
			HttpServerResponse response = routingContext.response();
			response.putHeader("content-type", "text/html").end("<h1>Hello from Vert.x!</h1>");
		});

		// Serve static resources from the /assets directory
		router.route("/assets/*").handler(StaticHandler.create("assets"));

		router.get("/api/whiskies").handler(this::getAll);

		router.route("/api/whiskies*").handler(BodyHandler.create()); // pour lire le body
		router.post("/api/whiskies").handler(this::addOne);
		router.delete("/api/whiskies/:id").handler(this::deleteOne);
		router.put("/api/whiskies/:id").handler(this::updateOne);
	    router.get("/api/whiskies/:id").handler(this::getOne);


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

	private void getAll(RoutingContext routingContext) {
		routingContext.response().putHeader("content-type", "application/json; charset=utf-8")
				.end(Json.encodePrettily(products.values()));
	}

	private void addOne(RoutingContext routingContext) {
		final Whisky whisky = Json.decodeValue(routingContext.getBodyAsString(), Whisky.class);
		products.put(whisky.getId(), whisky);
		routingContext.response().setStatusCode(201).putHeader("content-type", "application/json; charset=utf-8")
				.end(Json.encodePrettily(whisky));
	}

	private void deleteOne(RoutingContext routingContext) {
		String id = routingContext.request().getParam("id");
		if (id == null) {
			routingContext.response().setStatusCode(400).end();
		} else {
			Integer idAsInteger = Integer.valueOf(id);
			products.remove(idAsInteger);
		}
		routingContext.response().setStatusCode(204).end();
	}

	private void updateOne(RoutingContext routingContext) {
	    final String id = routingContext.request().getParam("id");
	    JsonObject json = routingContext.getBodyAsJson();
	    if (id == null || json == null) {
	      routingContext.response().setStatusCode(400).end();
	    } else {
	      final Integer idAsInteger = Integer.valueOf(id);
	      Whisky whisky = products.get(idAsInteger);
	      if (whisky == null) {
	        routingContext.response().setStatusCode(404).end();
	      } else {
	        whisky.setName(json.getString("name"));
	        whisky.setOrigin(json.getString("origin"));
	        routingContext.response()
	            .putHeader("content-type", "application/json; charset=utf-8")
	            .end(Json.encodePrettily(whisky));
	      }
	    }
	}
	  private void getOne(RoutingContext routingContext) {
		    final String id = routingContext.request().getParam("id");
		    if (id == null) {
		      routingContext.response().setStatusCode(400).end();
		    } else {
		      final Integer idAsInteger = Integer.valueOf(id);
		      Whisky whisky = products.get(idAsInteger);
		      if (whisky == null) {
		        routingContext.response().setStatusCode(404).end();
		      } else {
		        routingContext.response()
		            .putHeader("content-type", "application/json; charset=utf-8")
		            .end(Json.encodePrettily(whisky));
		      }
		    }
		  }


}
