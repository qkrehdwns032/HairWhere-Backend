package hairwhere.hairwhere.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("test")
public class TestController {

    @GetMapping("/api/hello")
    public String hello() {
        return "안녕하세요! 현재 시간은 " + LocalDateTime.now().toString() + " 입니다.";
    }
}
