package com.example.starter;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {
	static DeploymentOptions options;

	@BeforeAll
	static void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
		try {
			options = new DeploymentOptions().setConfig(
					new JsonObject(new String(Files.readAllBytes(Paths.get("src/main/conf/starter-conf.json")))));
			vertx.deployVerticle(new MainVerticle(), options, testContext.succeeding(id -> testContext.completeNow()));
		} catch (IOException e) {
			testContext.failNow(new Exception("Unable to read config"));
			testContext.completeNow();
			e.printStackTrace();
		}
	}

	@AfterAll
	static public void tearDown(Vertx vertx, VertxTestContext testContext) {
		vertx.close(testContext.succeeding(id -> testContext.completeNow()));
	}

	@Test
	void verticle_deployed(Vertx vertx, VertxTestContext testContext) throws Throwable {
		testContext.completeNow();
	}

	@Test
	public void testMyApplication(Vertx vertx, VertxTestContext testContext) {

		WebClient client = WebClient.create(vertx);
		client.get(options.getConfig().getInteger("http.port"), "localhost", "/").send(ar -> {
			if (ar.succeeded()) {
				HttpResponse<Buffer> response = ar.result();
				if (response.statusCode() != 200) {
					testContext.failNow(null);
				}
				String expected = "<h1>Hello from Vert.x!</h1>";
				if (!response.bodyAsString().equals(expected)) {
					testContext.failNow(new Exception(
							"bad body received [" + response.bodyAsString() + "] instead of [" + expected + "]"));
				}
			} else {
				testContext.failNow(null);
			}
			testContext.completeNow();
		});
	}

	@Test
	public void testIndexPage(Vertx vertx, VertxTestContext testContext) {

		WebClient client = WebClient.create(vertx);
		client.get(options.getConfig().getInteger("http.port"), "localhost", "/assets/index.html").send(ar -> {
			if (ar.succeeded()) {
				HttpResponse<Buffer> response = ar.result();
				testContext.verify(()->{
					assertEquals(200,response.statusCode());
					assertTrue(response.headers().get("content-type").contains("text/html"));
					assertTrue(response.bodyAsBuffer().getString(0, 8000).contains("<title>My Whisky Collection</title>"));
				});
			} else {
				testContext.failNow(null);
			}
			testContext.completeNow();
		});
	}

	@Test
	public void testAddWhiky(Vertx vertx, VertxTestContext testContext) {

		Whisky newBottle = new Whisky("Jameson", "Ireland");
		WebClient client = WebClient.create(vertx);
		client.post(options.getConfig().getInteger("http.port"), "localhost", "/api/whiskies").sendJson(newBottle,
				ar -> {
					if (ar.succeeded()) {
						HttpResponse<Buffer> response = ar.result();
						testContext.verify(() -> {
							assertEquals(201, response.statusCode());
							assertTrue(response.headers().get("content-type").contains("application/json"));
							Whisky whiskyRep = response.bodyAsJson(Whisky.class);
							assertEquals(newBottle.getName(), whiskyRep.getName());
							assertEquals(newBottle.getOrigin(), whiskyRep.getOrigin());
						});
					} else {
						testContext.failNow(new Exception("Request failed"));
					}
					testContext.completeNow();
				});
	}
}
