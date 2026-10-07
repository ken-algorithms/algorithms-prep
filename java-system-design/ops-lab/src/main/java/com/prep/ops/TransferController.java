package com.prep.ops;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TransferController {

    private final TransferService service;

    public TransferController(TransferService service) {
        this.service = service;
    }

    @PostMapping("/transfers/bad")
    public String bad(@RequestParam long from, @RequestParam long to, @RequestParam long amount) {
        return service.transferBad(from, to, amount);
    }

    @PostMapping("/transfers/good")
    public String good(@RequestParam long from, @RequestParam long to, @RequestParam long amount) {
        return service.transferGood(from, to, amount);
    }
}
