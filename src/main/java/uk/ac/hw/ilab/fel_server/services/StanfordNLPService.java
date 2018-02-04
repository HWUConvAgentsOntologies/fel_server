package uk.ac.hw.ilab.fel_server.services;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import uk.ac.hw.ilab.fel_server.properties.StanfordProperties;

import javax.annotation.PostConstruct;
import java.util.Properties;

@Service
@EnableConfigurationProperties(StanfordProperties.class)
public class StanfordNLPService {
    private final Logger logger = Logger.getLogger(StanfordNLPService.class.getName());
    @Autowired
    private StanfordProperties properties;
    private StanfordCoreNLP pipeline;

    @PostConstruct
    public void init() {
        Properties props = this.properties.getProperties();
        logger.info("Loading Stanford CoreNLP service using the following properties: " + props.toString());
        pipeline = new StanfordCoreNLP(props);
        logger.info("Stanford CoreNLP service correctly initialized");
    }

    public Annotation annotate(String text) {
        return pipeline.process(text);
    }

}
