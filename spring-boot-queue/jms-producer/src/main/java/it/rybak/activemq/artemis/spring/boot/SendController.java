package it.rybak.activemq.artemis.spring.boot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.jms.ObjectMessage;

@RestController
@RequestMapping("/api/v1")
public class SendController {
    @Autowired
    JmsTemplate jmsTemplate;

    @GetMapping("/send/{message}")
    public String send(@PathVariable("message") String message) {
        jmsTemplate.send("exampleQueue", session -> {
            ObjectMessage object = session.createObjectMessage(message);
            return object;
        });
        return message;
    }
}
