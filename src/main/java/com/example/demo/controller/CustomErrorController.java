package com.example.demo.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

@Controller
public class CustomErrorController implements ErrorController {

    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    @GetMapping(path = "/error/unauthenticated")
    public String getUnAuthenticatedPage() {
        return "Errors/unAuthenticated";
    }

    @ResponseStatus(value = HttpStatus.FORBIDDEN)
    @GetMapping(path = "/error/unauthorised")
    public String unAuthorisedException() {
        return "Errors/unAuthorised";
    }

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);

        if (status != null) {
            int statusCode = Integer.parseInt(status.toString());

            if (statusCode == 404) {
                return "Errors/pageNotFound";
            } else if (statusCode == 500) {
                return "Errors/internalServerError";
            } else if (statusCode == 401) {
                return "Errors/unAuthenticated";
            } else if (statusCode == 403) {
                return "Errors/unAuthorised";
            }
        }
        return "Errors/internalServerError";
    }

}
