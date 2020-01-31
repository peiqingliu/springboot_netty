package com.example.netty.controller;


import com.example.netty.server.ServerNetty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/api")
public class ApiController {

    @Autowired
    private ServerNetty serverNetty;

    @GetMapping("/start")
    public String startServer(){
        return "server start success";
    }
}
