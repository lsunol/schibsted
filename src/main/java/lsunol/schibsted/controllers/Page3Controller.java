package lsunol.schibsted.controllers;

import lsunol.schibsted.controllers.annotations.HttpProduces;

public class Page3Controller extends AuthenticatedController {

    @Override
    public String getRequestMapping() {
        return "/page3";
    }

    @Override
    String getRequiredRole() {
        return "PAGE_3";
    }

    @HttpProduces(name = "text/html")
    public final String doGet() {
        return "page3";
    }
}
