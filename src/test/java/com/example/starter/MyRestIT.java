package com.example.starter;

import com.jayway.restassured.RestAssured;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


public class MyRestIT {

  @BeforeAll
  public static void configureRestAssured() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = Integer.getInteger("http.port", 8080);
  }

  @AfterAll
  public static void unconfigureRestAssured() {
    RestAssured.reset();
  }
  
  @Test
  public void checkThatWeCanRetrieveIndividualProduct() {
    // Get the list of bottles, ensure it's a success and extract the first id.
    final int id = RestAssured.get("/api/whiskies").then()
        .assertThat()
        .statusCode(200)
        .extract()
        .jsonPath().getInt("find { it.name=='Bowmore 15 Years Laimrig' }.id");
    // Now get the individual resource and check the content
    RestAssured.get("/api/whiskies/" + id).then()
        .assertThat()
        .statusCode(200)
        .body("name",Matchers.equalTo("Bowmore 15 Years Laimrig"))
        .body("origin", Matchers.equalTo("Scotland, Islay"))
        .body("id", Matchers.equalTo(id));
  }
}