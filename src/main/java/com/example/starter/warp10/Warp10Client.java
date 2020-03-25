package com.example.starter.warp10;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class Warp10Client {

	private Warp10Token token;

	private static String API_URL = "http://127.0.0.1:8080/api/v0/";

	private Warp10Client() {
		try {
			token = new Warp10Token();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Warp10Client INSTANCE = new Warp10Client();

	public static Warp10Client getInstance() {
		return INSTANCE;
	}

	/**
	 * Load dat to warp10
	 * 
	 * @param vertx
	 * @param data  : GTS data
	 * @return
	 */
	public Future<Void> loadDataToWarp(Vertx vertx, String data) {
		Promise<Void> promise = Promise.promise();
		String url = API_URL + "update";

		WebClient client = WebClient.create(vertx);
		client.getAbs(url).putHeader("X-Warp10-Token", token.getWriteToken()).sendBuffer(Buffer.buffer(data), ar -> {
			if (ar.succeeded()) {
				// Obtain response
				if (ar.result().statusCode() != 200) {
					promise.fail("Warp10 return code " + ar.result().statusCode());
				}
			} else {
				System.out.println("Something went in loadDataToWarp : " + ar.cause().getMessage());
				promise.fail(ar.cause());
			}
			promise.complete();
		});

		return promise.future();

	}

	public Future<Void> DeleteAllGtsOfClass(Vertx vertx, String className) {
		Promise<Void> promise = Promise.promise();
		String url;
		url = API_URL + "delete?deleteall&selector=" + className + "{}";

		WebClient client = WebClient.create(vertx);
		client.getAbs(url).putHeader("X-Warp10-Token", token.getWriteToken()).send(ar -> {
			if (ar.succeeded()) {
				// Obtain response
				if (ar.result().statusCode() != 200) {
					promise.fail("Warp10 return code " + ar.result().statusCode());
					System.out.println("DeleteAllGtsOfClass warp10 returns code  : " + ar.result().statusCode());
				} else {
					promise.complete();
				}
			} else {
				System.out.println("Something went in DeleteAllGtsOfClass : " + ar.cause().getMessage());
				promise.fail(ar.cause());
			}
		});

		return promise.future();
	}

}
