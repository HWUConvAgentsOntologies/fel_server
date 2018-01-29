package uk.ac.hw.ilab.fel_server.services;

import com.google.common.collect.Multimap;
import com.yahoo.semsearch.fastlinking.FastEntityLinker;
import com.yahoo.semsearch.fastlinking.hash.AbstractEntityHash;
import com.yahoo.semsearch.fastlinking.hash.QuasiSuccinctEntityHash;
import com.yahoo.semsearch.fastlinking.view.EmptyContext;
import com.yahoo.semsearch.fastlinking.view.EntityScore;
import it.unimi.dsi.fastutil.io.BinIO;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.hw.ilab.fel_server.model.EntityAnnotation;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class FELService {

    @Value("${fel_server.hash_filename}")
    private String hashFilename;
    private FastEntityLinker fel;
    private AbstractEntityHash hash;

    @PostConstruct
    public void init() throws IOException, ClassNotFoundException {
        this.hash = (QuasiSuccinctEntityHash) BinIO.loadObject(hashFilename);
        this.fel = new FastEntityLinker(hash, new EmptyContext());
    }

    public List<EntityAnnotation> getAnnotations(String text) {
        Multimap<Pair<Integer, Integer>, EntityScore> results = fel.getResults(text);
        return processAnnotations(results, text);
    }


    private List<EntityAnnotation> processAnnotations(Multimap<Pair<Integer, Integer>, EntityScore> annotations, String text) {
        List<EntityAnnotation> refinedAnnotations = new ArrayList<>();
        for (Pair<Integer, Integer> offset : annotations.keys()) {
            for (EntityScore score : annotations.get(offset)) {
                EntityAnnotation annotation = new EntityAnnotation();
                annotation.setSpan(offset);
                annotation.setEntity(hash.getEntityName(score.entity.id).toString());
                annotation.setScore(score.score);
                refinedAnnotations.add(annotation);

            }
        }

        Collections.sort(refinedAnnotations);

        return refinedAnnotations;
    }
}
