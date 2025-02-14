package pods.project.walletservice.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @RequestMapping
    public String hello(){
        return "Hello From Wallet Service!";
    }
    @RequestMapping("/check")
    public String what(){
        return "I am Wallet-Service running at port 8081";
    }
}