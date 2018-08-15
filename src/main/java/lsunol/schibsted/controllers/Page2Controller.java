package lsunol.schibsted.controllers;

import lsunol.schibsted.controllers.annotations.HttpProduces;

public class Page2Controller extends AuthenticatedController {

    @Override
    public String getRequestMapping() {
        return "/page2";
    }

    @Override
    String getRequiredRole() {
        return "PAGE_2";
    }

    @HttpProduces(name = "text/html")
    public final String doGet() {
        return "page2";
    }
}
