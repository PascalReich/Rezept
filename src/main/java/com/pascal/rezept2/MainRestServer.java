package com.pascal.rezept2;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.CookieSameSite;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerHandler;
import io.vertx.ext.web.handler.ResponseTimeHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.sstore.SessionStore;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;


import java.io.*;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MainRestServer extends AbstractVerticle {
  private final Map<String, JsonObject> products = new HashMap<>();
  private final SQLBridge sqlBridge = new SQLBridge();

  private final RecipeRequestHandler recipeRequestHandler = new RecipeRequestHandler(sqlBridge);
  private final UserRequestHandler userRequestHandler = new UserRequestHandler(sqlBridge);

  private final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(vertx);

  @Override
  public void start() {

    // Set up SQLBridge
    try {
      sqlBridge.connectionToDerby();
    } catch (SQLException sqlException) {
      System.out.println("Failed to Connect to Database");
      System.out.println(sqlException.getMessage());
      this.getVertx().close();
      System.exit(1);
    }

    setUpInitialData();
    configureThymeleafEngine(engine);

    Router mainRouter = Router.router(vertx);

    configureSessionHandler(mainRouter); // add session handler

    mainRouter.route().handler(LoggerHandler.create()); // enable automatic request logging
    mainRouter.route().handler(BodyHandler.create()); //enable reading request body
    mainRouter.route().handler(ResponseTimeHandler.create()); //enable response time reporting

    mainRouter.route().handler(ctx -> {
      Session session = ctx.session();
      if (session.get("begin") != null) {
        System.out.println((String) session.get("begin"));
      } else {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        Date date = new Date();
        session.put("begin", formatter.format(date));
        System.out.println("Started new Session");
      }
      ctx.next();
    });

    setUpAPIRouter(mainRouter);

    mainRouter.get("/home").handler(this::serveHomepage);

    mainRouter.get("/").handler(ctx -> ctx.reroute("/home"));

    mainRouter.route().failureHandler(failureRoutingContext -> {

      int statusCode = failureRoutingContext.statusCode();

      System.out.println(failureRoutingContext.failure().getMessage());
      failureRoutingContext.failure().printStackTrace();

      // Status code will be 500 for the RuntimeException
      // or 403 for the other failure
      HttpServerResponse response = failureRoutingContext.response();
      response.setStatusCode(statusCode).end("Sorry! Not today");
    });

    vertx.createHttpServer().requestHandler(mainRouter).listen(8080);

  }

  private void configureSessionHandler(Router router) {
    SessionStore store = LocalSessionStore.create(vertx);

    SessionHandler sessionHandler = SessionHandler.create(store);

    sessionHandler.setCookieSameSite(CookieSameSite.STRICT);

    router.route()
      .handler(sessionHandler);

    // Session session = ctx.session();
    //  session.put("foo", "bar");

  }

  private void configureThymeleafEngine(ThymeleafTemplateEngine engine) {
    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("/");
    templateResolver.setSuffix(".html");
    engine.getThymeleafTemplateEngine().setTemplateResolver(templateResolver);
  }

  private void setUpAPIRouter(Router mainRouter) {
    Router APIRouter = Router.router(vertx);

    // Set up /API/recipes REST endpoints
    APIRouter.get("/recipes/:recipeID").handler(recipeRequestHandler::getRecipe);
    APIRouter.put("/recipes/:recipeID").handler(this::handleAddProduct);
    APIRouter.get("/recipes").handler(recipeRequestHandler::listRecipes);

    // Set up /API/users REST endpoints
    APIRouter.get("/users/:userID").handler(userRequestHandler::getUser);
    APIRouter.put("/users/:userID").handler(this::handleAddProduct);
    APIRouter.get("/users").handler(userRequestHandler::listUsers);

    APIRouter.get("/users/me");
    APIRouter.post("/users/login"); //TODO implement

    mainRouter.route("/API/*").subRouter(APIRouter);
  }

  private void serveHomepage(RoutingContext routingContext) {
    JsonObject data = null;
    try {
      data = new JsonObject()
        .put("name", "Vert.x Web")
        .put("recipes", sqlBridge.getRecipesAndAuthors());
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }


    engine.render(data, "templates/main", res -> {
      if (res.succeeded()) {
        routingContext.response().end(res.result());
      } else {
        System.out.println(res.cause().getMessage());
        res.cause().printStackTrace();
        routingContext.fail(res.cause());
      }
    });
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

  private static class RecipeRequestHandler {
    private final SQLBridge sqlBridge;

    RecipeRequestHandler(SQLBridge s) {
      sqlBridge = s;
    }
    public void listRecipes (RoutingContext routingContext){
      StringBuilder response = new StringBuilder();
      List<Recipe> recipes;
      try {
        recipes = sqlBridge.getRecipes();
      } catch (SQLException sqlException) {
        routingContext.fail(sqlException);
        return;
      }
      for (Recipe recipe: recipes) {
        response.append(recipe.toString());
        response.append("\n");
      }
      routingContext.response().end(response.toString());
    }

    public void listRecipesAndAuthors (RoutingContext routingContext){
      StringBuilder response = new StringBuilder();
      List<RecipeWithAuthor> recipes;
      try {
        recipes = sqlBridge.getRecipesAndAuthors();
      } catch (SQLException sqlException) {
        routingContext.fail(sqlException);
        return;
      }
      for (RecipeWithAuthor recipe: recipes) {
        response.append(recipe.toString());
        response.append("\n");
      }
      routingContext.response().end(response.toString());
    }

    public void getRecipe(RoutingContext routingContext) {
      String recipeID = routingContext.request().getParam("recipeID");

      //StringBuilder response = new StringBuilder();
      List<Recipe> recipe;
      try {
        recipe = sqlBridge.getRecipes("*", "WHERE ID=" + recipeID);
      } catch (SQLException sqlException) {
        routingContext.fail(sqlException);
        return;
      }

      if (recipe.isEmpty()) {
        routingContext.fail(404);
        return;
      }

      routingContext.response().end(recipe.get(0).toString());
    }
  }

  private static class UserRequestHandler {
    private final SQLBridge sqlBridge;

    UserRequestHandler(SQLBridge s) {
      sqlBridge = s;
    }

    public void listUsers(RoutingContext routingContext) {
      StringBuilder response = new StringBuilder();
      List<User> users;
      try {
        users = sqlBridge.getUsers();
      } catch (SQLException sqlException) {
        routingContext.fail(sqlException);
        return;
      }
      for (User user: users) {
        response.append(user.toString());
        response.append("\n");
      }
      routingContext.response().end(response.toString());
    }

    public void getUser(RoutingContext routingContext) {
      String userID = routingContext.request().getParam("userID");

      //StringBuilder response = new StringBuilder();
      List<User> users;
      try {
        users = sqlBridge.getUsers("*", "WHERE ID=" + userID);
      } catch (SQLException sqlException) {
        routingContext.fail(sqlException);
        return;
      }

      if (users.isEmpty()) {
        routingContext.fail(404);
        return;
      }

      routingContext.response().end(users.get(0).toString());
    }

  }
}

