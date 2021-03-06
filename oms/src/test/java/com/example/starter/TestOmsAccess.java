package com.example.starter;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import oms.MainVerticle;
import oms.OmsAccess;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class TestOmsAccess {
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
	
//	@Test
//	public void testInitCountry(Vertx vertx, VertxTestContext testContext) {
//		OmsAccess omsAccess = OmsAccess.getInstance();
//		
//		Future<Void> fut = omsAccess.downloadCountries(vertx);
//		fut.setHandler(ar -> {
//			testContext.assertComplete(fut);
//			testContext.completeNow();
//		});
//
//	}

	@Test
	public void testLoadAll(Vertx vertx, VertxTestContext testContext) {
		OmsAccess omsAccess = OmsAccess.getInstance();
		
		try {
			Future<Void> fut = omsAccess.loadDataToWarp10(vertx,"France");
			fut.setHandler(ar->{
				if(ar.failed()) {
					testContext.failNow(ar.cause());
					testContext.completeNow();
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
			testContext.failNow(e);
			testContext.completeNow();
		}

	}


}