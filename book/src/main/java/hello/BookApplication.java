package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@SpringBootApplication
public class BookApplication {

    @RequestMapping(value = "/available")
    public String available() {
        return "available books";
    }

    @RequestMapping(value = "/checked-out")
    public String checkedOut() {
        return "checked out books";
    }

    public static void main(String[] args) {
        SpringApplication.run(BookApplication.class, args);
    }
}