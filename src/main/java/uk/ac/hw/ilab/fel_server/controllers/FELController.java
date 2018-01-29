package uk.ac.hw.ilab.fel_server.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.hw.ilab.fel_server.exceptions.InvalidFELRequest;
import uk.ac.hw.ilab.fel_server.services.FELService;

@RestController
public class FELController {
    private final FELService felService;

    public FELController(@Autowired FELService felService) {
        this.felService = felService;
    }

    @RequestMapping(method = RequestMethod.POST)
    public Object annotate(@RequestBody String text) {
        if (text == null) {
            throw new InvalidFELRequest("[FEL-server]: text is null");
        }

        return felService.getAnnotations(text);
    }
}
