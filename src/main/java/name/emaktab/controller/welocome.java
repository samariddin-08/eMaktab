package name.emaktab.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class welocome {

    @PostMapping
    public String welocome(){
        return "Welcome to eMaktab ";
    }
}
