package com.example.starter;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

	@BeforeAll
	static void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
		vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
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
		client.get(8888, "localhost", "/").send(ar -> {
			if (ar.succeeded()) {
				HttpResponse<Buffer> response = ar.result();
				if (response.statusCode() != 200) {
					testContext.failNow(null);
				}
				String expected = "Hello from Vert.x!";
				if (!response.bodyAsString().equals(expected)) {
					testContext.failNow(new Exception("bad body received [" + response.bodyAsString() + "] instead of [" + expected + "]"));
				}
			} else {
				testContext.failNow(null);
			}
			testContext.completeNow();
		});
	}
}
