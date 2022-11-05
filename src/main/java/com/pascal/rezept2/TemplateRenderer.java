package com.pascal.rezept2;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class TemplateRenderer {

  private static TemplateRenderer templateRenderer;
  private ThymeleafTemplateEngine thymeleafTemplateEngine;
  private Vertx vertx;

  private TemplateRenderer () {
  }

  /**
   * configure the TemplateRenderer Singleton with the app's vertx instance
   * @param vertx
   */
  public void setVertexInstance(Vertx vertx) {
    this.vertx = vertx;
    thymeleafTemplateEngine = ThymeleafTemplateEngine.create(vertx);
    configureThymeleafEngine(thymeleafTemplateEngine);
  }

  private void configureThymeleafEngine(ThymeleafTemplateEngine engine) {
    ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("/");
    templateResolver.setSuffix(".html");
    engine.getThymeleafTemplateEngine().setTemplateResolver(templateResolver);
  }

  /**
   *
   * @param routingContext Routing Context of response to end
   * @param template String path of template to render
   */
  public void renderTemplate(RoutingContext routingContext, String template) {
    renderTemplate(routingContext, template, new JsonObject());
  }

  /**
   *
   * @param routingContext Routing Context of response to end
   * @param template String path of template to render
   * @param data a JsonObject with data that the template expects.
   */
  public void renderTemplate(RoutingContext routingContext, String template, JsonObject data) {
    thymeleafTemplateEngine.render(data, template, res -> {
      if (res.succeeded()) {
        routingContext.response().end(res.result());
      } else {
        //System.out.println(res.cause().getMessage());
        //res.cause().printStackTrace();
        routingContext.fail(res.cause());
      }
    });
  }

  public static TemplateRenderer getInstance() {
    if (templateRenderer == null) {
      templateRenderer = new TemplateRenderer();
    }
    return templateRenderer;
  }
}
