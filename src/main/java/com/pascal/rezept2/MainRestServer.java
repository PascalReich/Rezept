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


import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MainRestServer extends AbstractVerticle {
  private final Map<String, JsonObject> products = new HashMap<>();
  private final SQLBridge sqlBridge = SQLBridge.getInstance();
  private final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(vertx);

  private final RecipeRequestHandler recipeRequestHandler = new RecipeRequestHandler(sqlBridge, engine);
  private final UserRequestHandler userRequestHandler = new UserRequestHandler(sqlBridge, engine);


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

    //configureThymeleafEngine(engine);
    TemplateRenderer.getInstance().setVertexInstance(vertx);


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
      if (session.get("User") != null) {
        System.out.printf("Authenticated as %s \n", ((User) session.get("User")).getUsername());
      }
      ctx.next();
    });

    setUpAPIRouter(mainRouter);

    mainRouter.get("/home").handler(this::serveHomepage);
    mainRouter.get("/login").handler(userRequestHandler::getLoginPage);
    mainRouter.get("/recipes").handler(recipeRequestHandler::renderRecipes);


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
    APIRouter.put("/recipes/new").handler(this::handleAddProduct);
    APIRouter.get("/recipes").handler(recipeRequestHandler::listRecipes);

    // Set up /API/users REST endpoints
    APIRouter.get("/users/me").handler(userRequestHandler::displayCurrentUser);
    //APIRouter.get("users/me/update");

    APIRouter.get("/users/:userID").handler(userRequestHandler::getUser);
    APIRouter.put("/users/new").handler(userRequestHandler::createUser);
    APIRouter.get("/users").handler(userRequestHandler::listUsers);
    APIRouter.patch("/users/:userID").handler(userRequestHandler::updateUser);




    APIRouter.post("/users/login").handler(userRequestHandler::loginUser); //TODO implement


    mainRouter.route("/API/*").subRouter(APIRouter);
  }

  private void serveHomepage(RoutingContext routingContext) {
    JsonObject data = null;
    try {
      data = new JsonObject()
        .put("name", "Vert.x Web")
        .put("recipes", sqlBridge.getRecipesAndAuthors());
      User user = routingContext.session().get("User");
      if (user != null) {
        data.put("username", user.getUsername());
      } else {
        data.put("username", "there");
      }
    } catch (SQLException e) {
      throw new RuntimeException(e);
    }

    TemplateRenderer.getInstance().renderTemplate(routingContext, "templates/main", data);

  }

  private void handleAddProduct(RoutingContext routingContext) {

  }

  private void sendError(int statusCode, HttpServerResponse response) {
    response.setStatusCode(statusCode).end();
  }

  private static class RecipeRequestHandler {
    private final SQLBridge sqlBridge;
    private final ThymeleafTemplateEngine engine;

    RecipeRequestHandler(SQLBridge s, ThymeleafTemplateEngine e) {
      sqlBridge = s;
      engine = e;
    }

    public void renderRecipes(RoutingContext routingContext) {
      try {
        TemplateRenderer.getInstance().renderTemplate(
          routingContext,
          "templates/viewRecipes",
          new JsonObject()
            .put("recipes", sqlBridge.getRecipesAndAuthors()));
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
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
    private final ThymeleafTemplateEngine engine;

    UserRequestHandler(SQLBridge s, ThymeleafTemplateEngine e) {
      sqlBridge = s;
      engine = e;
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
      displayUser(routingContext, userID);
    }

    private void initExistingUser(String username, String password) {
      try {
        User user = sqlBridge.getUsers("*", "WHERE USERNAME='" + username+"'").get(0);
        byte[] salt = getNewSalt();

        String passHash = getEncryptedPassword(password, salt);
        user.updatePassword(passHash, Base64.getEncoder().encodeToString(salt));
        sqlBridge.updateUser(user);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    public void createUser(RoutingContext routingContext) {

    }

    public void loginUser(RoutingContext routingContext) {
      JsonObject jsonObject = routingContext.getBodyAsJson();

      String passHash;
      User user;

      try {
        user = sqlBridge.getUsers("*", "WHERE USERNAME='" + jsonObject.getString("username")+ "'").get(0);
      } catch (SQLException e) {
        throw new RuntimeException(e);
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new RuntimeException(e); // TODO change
      }

      //initExistingUser(jsonObject.getString("username"), jsonObject.getString("password"));


      try {
        passHash = getEncryptedPassword(jsonObject.getString("password"), Base64.getDecoder().decode(user.getPasswordSalt()));
        System.out.println(passHash);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      if (user.comparePassword(passHash)) {
        Session session = routingContext.session();
        session.put("User", user);
        routingContext.response()
          .putHeader("Content-Type", "application/json")
          .end((new JsonObject().put("auth", "true")).toString());
      } else {
        routingContext.response()
          .putHeader("Content-Type", "application/json")
          .setStatusCode(401)
          .end((new JsonObject().put("auth", "true")).toString());
      }

      //initExistingUser(jsonObject.getString("username"), jsonObject.getString("password"));
      // System.out.println(jsonObject);
    }

    public void displayCurrentUser(RoutingContext routingContext) {

      String userID = String.valueOf(((User) routingContext.session().get("User")).getUserID());

      displayUser(routingContext, userID);
    }

    private void displayUser(RoutingContext routingContext, String userID) {
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

    public void updateUser(RoutingContext routingContext) {
      String userID = routingContext.request().getParam("userID");

      JsonObject jsonObject = routingContext.getBodyAsJson();

      //System.out.println(jsonObject);

      if (jsonObject.containsKey("password")) {

        try {
          byte[] salt = getNewSalt();
          String password = getEncryptedPassword(jsonObject.getString("password"), salt);

          // TODO get user from USERS MAP

        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    }

    // Returns salt byte array
    public byte[] getNewSalt() throws NoSuchAlgorithmException {
      // Don't use Random!
      SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
      // NIST recommends minimum 4 bytes. We use 8.
      byte[] salt = new byte[8];
      random.nextBytes(salt);
      return salt;
    }

    public String getEncryptedPassword(String password, byte[] saltBytes) throws Exception {
      String algorithm = "PBKDF2WithHmacSHA1";
      int derivedKeyLength = 160; // for SHA1
      int iterations = 20000; // NIST specifies 10000

      KeySpec spec = new PBEKeySpec(password.toCharArray(), saltBytes, iterations, derivedKeyLength);
      SecretKeyFactory f = SecretKeyFactory.getInstance(algorithm);

      byte[] encBytes = f.generateSecret(spec).getEncoded();
      return Base64.getEncoder().encodeToString(encBytes);
    }

    public void getLoginPage(RoutingContext routingContext) {
      TemplateRenderer.getInstance().renderTemplate(routingContext, "templates/login");
    }

    public void getUserUpdatePage(RoutingContext routingContext) {
      TemplateRenderer.getInstance().renderTemplate(routingContext, "templates/updateUser");
    }
  }
}

