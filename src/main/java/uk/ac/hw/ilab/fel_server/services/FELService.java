package uk.ac.hw.ilab.fel_server.services;

import com.google.common.collect.Multimap;
import com.yahoo.semsearch.fastlinking.FastEntityLinker;
import com.yahoo.semsearch.fastlinking.hash.AbstractEntityHash;
import com.yahoo.semsearch.fastlinking.hash.QuasiSuccinctEntityHash;
import com.yahoo.semsearch.fastlinking.view.EmptyContext;
import com.yahoo.semsearch.fastlinking.view.EntityScore;
import it.unimi.dsi.fastutil.io.BinIO;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.hw.ilab.fel_server.model.EntityAnnotation;
import uk.ac.hw.ilab.fel_server.model.EntityLink;
import uk.ac.hw.ilab.fel_server.model.KnowledgeBase;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class FELService {

    private final WikidataSPARQLClient wikidataSPARQLClient;
    @Value("${fel_server.hash_filename}")
    private String hashFilename;
    private FastEntityLinker fel;
    private AbstractEntityHash hash;

    public FELService(@Autowired WikidataSPARQLClient wikidataSPARQLClient) {
        this.wikidataSPARQLClient = wikidataSPARQLClient;
    }

    @PostConstruct
    public void init() throws IOException, ClassNotFoundException {
        this.hash = (QuasiSuccinctEntityHash) BinIO.loadObject(hashFilename);
        this.fel = new FastEntityLinker(hash, new EmptyContext());
    }

    public List<EntityAnnotation> getAnnotations(String text) {
        Multimap<Pair<Integer, Integer>, EntityScore> results = fel.getResults(text);
        return processAnnotations(results);
    }


    private List<EntityAnnotation> processAnnotations(Multimap<Pair<Integer, Integer>, EntityScore> annotations) {
        List<EntityAnnotation> refinedAnnotations = new ArrayList<>();
        for (Pair<Integer, Integer> offset : annotations.keys()) {
            for (EntityScore score : annotations.get(offset)) {
                EntityAnnotation annotation = new EntityAnnotation();
                annotation.setSpan(offset);
                annotation.setEntity(hash.getEntityName(score.entity.id).toString());
                annotation.setScore(score.score);
                try {
                    annotation.setEntityLink(new EntityLink(KnowledgeBase.WIKIDATA, wikidataSPARQLClient.getWikidataURI(annotation.getEntity())));
                } catch (UnsupportedEncodingException e) {
                    System.err.println("Skipping Wikidata annotation due to invalid Wikipedia URL encoding!");
                }
                refinedAnnotations.add(annotation);

            }
        }

        Collections.sort(refinedAnnotations);

        return refinedAnnotations;
    }
}
