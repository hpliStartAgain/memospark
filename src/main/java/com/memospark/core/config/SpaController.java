package com.memospark.core.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all non-API, non-asset requests to the React SPA entry point
 * so that React Router can handle client-side navigation.
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
            "/",
            "/login",
            "/dashboard/**",
            "/targets/**",
            "/decks/**",
            "/review/**",
            "/practice/**",
            "/notebook/**",
            "/stats/**",
            "/settings/**"
    })
    public String forwardToSpa() {
        return "forward:/index.html";
    }
}
