package lsunol.schibsted.controllers;

import com.sun.net.httpserver.HttpExchange;
import lsunol.schibsted.controllers.annotations.HttpProduces;

import java.util.Map;

public class LogoutController extends AuthenticatedController {

    @Override
    public String getRequestMapping() {
        return "/logout";
    }

    @Override
    String getRequiredRole() {
        return null;
    }

    @HttpProduces(name = "text/html")
    public String doGet(HttpExchange httpExchange, Map<String, String> templateAttributes) {
        unsetSessionCookie(httpExchange);
        templateAttributes.remove("username");
        templateAttributes.remove("roles");
        return "logout";
    }
}
