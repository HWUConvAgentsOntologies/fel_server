package uk.ac.hw.ilab.fel_server.services;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.yahoo.semsearch.fastlinking.FastEntityLinker;
import com.yahoo.semsearch.fastlinking.hash.AbstractEntityHash;
import com.yahoo.semsearch.fastlinking.hash.QuasiSuccinctEntityHash;
import com.yahoo.semsearch.fastlinking.view.EmptyContext;
import com.yahoo.semsearch.fastlinking.view.EntityScore;
import com.yahoo.semsearch.fastlinking.view.Span;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import it.unimi.dsi.fastutil.io.BinIO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.ac.hw.ilab.fel_server.model.EntityAnnotation;
import uk.ac.hw.ilab.fel_server.model.EntityLink;
import uk.ac.hw.ilab.fel_server.model.KnowledgeBase;
import uk.ac.hw.ilab.fel_server.model.LinkerRequest;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FELService {
    private final StanfordNLPService nlpService;
    private final WikidataSPARQLClient wikidataSPARQLClient;
    @Value("${fel_server.hash_filename}")
    private String hashFilename;
    @Value("${fel_server.candidate_per_spot}")
    private Integer candidatePerSpot;
    private FastEntityLinker fel;
    private AbstractEntityHash hash;

    public FELService(@Autowired WikidataSPARQLClient wikidataSPARQLClient,
                      @Autowired StanfordNLPService nlpService) {
        this.wikidataSPARQLClient = wikidataSPARQLClient;
        this.nlpService = nlpService;
    }

    @PostConstruct
    public void init() throws IOException, ClassNotFoundException {
        this.hash = (QuasiSuccinctEntityHash) BinIO.loadObject(hashFilename);
        this.fel = new FastEntityLinker(hash, new EmptyContext());
    }

    public Set<EntityAnnotation> getAnnotations(LinkerRequest request) {
        Annotation annotatedText = this.nlpService.annotate(request.getText());
        Multimap<Span, EntityAnnotation> annotations = processAnnotations(
                fel.getResults(request.getText(), candidatePerSpot),
                annotatedText
        );

        if (annotations == null)
            return null;

        List<String> types = request.getTypes();

        // resolve context: additional type filters can be derived from it
        if (request.getContext() != null)
            types.addAll(applyContextFilter(annotations, request.getContext()));

        if (types != null)
            annotations = applyTypesFilter(annotations, request.getTypes());

        return selectBestCandidate(annotations);
    }

    private Multimap<Span, EntityAnnotation> applyTypesFilter(Multimap<Span, EntityAnnotation> annotations, List<String> types) {
        Multimap<Span, EntityAnnotation> refinedAnnotations = HashMultimap.create();

        for (Span s : annotations.keySet()) {
            refinedAnnotations.putAll(
                    s,
                    annotations.get(s).stream()
                            .filter(e -> e.getEntityLink().getTypes().stream().anyMatch(types::contains))
                            .collect(Collectors.toSet()));
        }

        return refinedAnnotations;
    }

    private Set<EntityAnnotation> selectBestCandidate(Multimap<Span, EntityAnnotation> annotations) {
        Set<EntityAnnotation> refinedAnnotations = new HashSet<>();

        for (Span span : annotations.keySet()) {
            Collection<EntityAnnotation> currSpanAnnotations = annotations.get(span);

            if (currSpanAnnotations.size() == 1) {
                refinedAnnotations.add(currSpanAnnotations.iterator().next());
            } else {
                refinedAnnotations.add(currSpanAnnotations.stream().max(Comparator.comparingDouble(EntityAnnotation::getScore)).get());
            }
        }

        Double meanScore = refinedAnnotations.stream().collect(Collectors.averagingDouble(EntityAnnotation::getScore));

        return refinedAnnotations.stream().filter(e -> e.getScore() >= meanScore).collect(Collectors.toSet());
    }

    //TODO: retrieve related concept types according to the current annotations and context
    private List<String> applyContextFilter(Multimap<Span, EntityAnnotation> annotations, Multimap<String, String> context) {
        return null;
    }


    private Multimap<Span, EntityAnnotation> processAnnotations(Multimap<Span, EntityScore> annotations,
                                                                Annotation annotatedText) {
        Multimap<Span, EntityAnnotation> refinedAnnotations = HashMultimap.create();

        for (Span span : annotations.keySet()) {

            if (this.skipAnnotation(span, annotatedText)) {
                continue;
            }

            Set<EntityAnnotation> currEntityAnnotations = new HashSet<>();

            for (EntityScore score : annotations.get(span)) {
                EntityAnnotation annotation = new EntityAnnotation();
                annotation.setSpan(span);
                annotation.setEntity(hash.getEntityName(score.entity.id).toString());
                annotation.setScore(score.score);
                try {
                    EntityLink link = new EntityLink(KnowledgeBase.WIKIDATA, wikidataSPARQLClient.getWikidataURI(annotation.getEntity()));
                    annotation.setEntityLink(link);
                    link.setTypes(wikidataSPARQLClient.getEntityTypes(link.getIdentifier()));
                } catch (UnsupportedEncodingException e) {
                    System.err.println(String.format(
                            "Skipping Wikidata annotation %s due to invalid Wikipedia URL encoding!",
                            annotation.getEntity()
                    ));

                }
                currEntityAnnotations.add(annotation);

            }

            refinedAnnotations.putAll(span, currEntityAnnotations);
        }

        return refinedAnnotations;
    }

    private boolean skipAnnotation(Span span, Annotation annotatedText) {
        List<CoreLabel> tokens = annotatedText.get(CoreAnnotations.TokensAnnotation.class);
        int beginPos = span.getStartOffset(),
                endPos = span.getEndOffset();
        CoreLabel firstToken = tokens.get(beginPos);

        String posTag = firstToken.get(CoreAnnotations.PartOfSpeechAnnotation.class);

        if (endPos - beginPos == 0) {
            if (!posTag.equals("NN") &&
                    !posTag.equals("NNS") &&
                    !posTag.equals("NNPS") &&
                    !posTag.equals("NNP") &&
                    !posTag.contains("VB")) {
                // skip the current entity: it is not a noun phrase or a simple noun or a verb
                return true;
            }
        }

        return false;

    }
}
