package com.pascal.rezept2;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.HashMap;
import java.util.Map;

public class MainRestServer extends AbstractVerticle {
  private final Map<String, JsonObject> products = new HashMap<>();

  @Override
  public void start() {

    setUpInitialData();

    Router mainRouter = Router.router(vertx);

    mainRouter.route().handler(BodyHandler.create()); //enable reading request body

    setUpAPIRouter(mainRouter);

    mainRouter.get("/products/:productID").handler(this::handleGetProduct);
    mainRouter.put("/products/:productID").handler(this::handleAddProduct);
    mainRouter.get("/products").handler(this::handleListProducts);

    vertx.createHttpServer().requestHandler(mainRouter).listen(8080);
  }

  private void setUpAPIRouter(Router mainRouter) {
    Router APIRouter = Router.router(vertx);

    // Set up /API/recipes REST endpoints
    APIRouter.get("/recipes/:productID").handler(this::handleGetProduct);
    APIRouter.put("/recipes/:productID").handler(this::handleAddProduct);
    APIRouter.get("/recipes").handler(this::handleListProducts);

    mainRouter.route("/API/*").subRouter(APIRouter);
  }

  private void handleGetProduct(RoutingContext routingContext) {
    String productID = routingContext.request().getParam("productID");
    HttpServerResponse response = routingContext.response();
    if (productID == null) {
      sendError(400, response);
    } else {
      JsonObject product = products.get(productID);
      if (product == null) {
        sendError(404, response);
      } else {
        response.putHeader("content-type", "application/json").end(product.encodePrettily());
      }
    }
  }

  private void handleAddProduct(RoutingContext routingContext) {
    String productID = routingContext.request().getParam("productID");
    HttpServerResponse response = routingContext.response();
    if (productID == null) {
      sendError(400, response);
    } else {
      JsonObject product = routingContext.getBodyAsJson();
      if (product == null) {
        sendError(400, response);
      } else {
        products.put(productID, product);
        response.end();
      }
    }
  }

  private void handleListProducts(RoutingContext routingContext) {
    JsonArray arr = new JsonArray();
    products.forEach((k, v) -> arr.add(v));
    routingContext.response().putHeader("content-type", "application/json").end(arr.encodePrettily());
  }

  private void sendError(int statusCode, HttpServerResponse response) {
    response.setStatusCode(statusCode).end();
  }

  private void setUpInitialData() {
    addProduct(new JsonObject().put("id", "prod3568").put("name", "Egg Whisk").put("price", 3.99).put("weight", 150));
    addProduct(new JsonObject().put("id", "prod7340").put("name", "Tea Cosy").put("price", 5.99).put("weight", 100));
    addProduct(new JsonObject().put("id", "prod8643").put("name", "Spatula").put("price", 1.00).put("weight", 80));
  }

  private void addProduct(JsonObject product) {
    products.put(product.getString("id"), product);
  }
}

