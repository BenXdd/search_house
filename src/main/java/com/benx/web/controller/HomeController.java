package com.benx.web.controller;

import com.benx.base.ApiResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class HomeController {

    @GetMapping(value = {"/","/index"})
    public String index(Model model){
        //model.addAttribute("name","456");
        //thymeleaf默认会给它加一个前缀(template,在classpath下寻找)
        return "index";
    }

    @GetMapping("/get")
    @ResponseBody
    public ApiResponse get(){
        return ApiResponse.ofMessage(200,"成功了!");
    }

    @GetMapping("/403")
    public  String f1(){
        return "403";
    }

    @GetMapping("/404")
    public  String f2(){
        return "404";
    }

    @GetMapping("/500")
    public  String f3(){
        return "500";
    }

    @GetMapping("/logout/page")
    public  String f4(){
        return "logout";
    }
}
