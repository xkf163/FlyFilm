package com.fly.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author:xukangfeng
 * @Description
 * @Date : Create in 10:26 2019/8/22
 */

@Controller
@RequestMapping(value = "/film")
public class FilmController {

    @GetMapping(value = "/all")
    public String filmAll(HttpServletRequest request) {
        request.setAttribute("dataUrl", "api/film/all");
        return "views/film/list";
    }
}