package lsunol.schibsted.controllers;

import lsunol.schibsted.controllers.annotations.HttpProduces;

public class Page1Controller extends AuthenticatedController {

    @Override
    public String getRequestMapping() {
        return "/page1";
    }

    @Override
    String getRequiredRole() {
        return "PAGE_1";
    }

    @HttpProduces(name = "text/html")
    public final String doGet() {
        return "page1";
    }
}
