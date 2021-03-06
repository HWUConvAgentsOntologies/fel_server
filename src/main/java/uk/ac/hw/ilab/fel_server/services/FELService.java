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
import uk.ac.hw.ilab.fel_server.model.properties.WikidataProperties;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class FELService {
    private static final Logger logger = Logger.getLogger(FELService.class.getName());
    private final StanfordNLPService nlpService;
    private final WikidataSPARQLClient wikidataSPARQLClient;
    private List<String> entityProperties;
    @Value("${fel_server.hash_filename}")
    private String hashFilename;
    @Value("#{new Integer('${fel_server.candidate_per_spot}')}")
    private Integer candidatePerSpot;
    @Value("#{new Double('${fel_server.annotation_score}')}")
    private Double ANNOTATION_SCORE;
    @Value("#{new Double('${fel_server.candidate_score}')}")
    private Double CANDIDATE_SCORE;
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
        initPropertyList();
    }

    private void initPropertyList() {
        entityProperties = new ArrayList<>();
        entityProperties.add(WikidataProperties.ENTITY_TYPE);
        entityProperties.add(WikidataProperties.OCCUPATION);
    }

    public Multimap<Span, EntityAnnotation> getAnnotations(LinkerRequest request) {
        Annotation annotatedText = this.nlpService.annotate(request.getText());
        // use the threshold values specified and override the default ones
        Double annotationScore = request.getAnnotationScore(), candidateScore = request.getCandidateScore();

        if (annotationScore == null) {
            annotationScore = ANNOTATION_SCORE;
        }

        if (candidateScore == null) {
            candidateScore = CANDIDATE_SCORE;
        }

        Multimap<Span, EntityAnnotation> annotations = processAnnotations(
                fel.getResults(request.getText(), candidatePerSpot),
                annotatedText,
                annotationScore,
                candidateScore
        );

        logger.info(String.format("Retrieved annotations from FEL service for request %s", request));
        logger.info(annotations.toString());

        Multimap<String, String> properties = request.getProperties(),
                profanityFilter = request.getProfanity();

        // resolve context: additional type filters can be derived from it
        if (request.getContext() != null)
            properties.putAll(applyContextFilter(annotations, request.getContext()));

        if (properties != null)
            annotations = applyPropertiesFilter(annotations, properties, false);

        annotations = filterSubspans(annotations);

        if (profanityFilter != null)
            // reversed=true -> discard all the annotations that matches the profanity filter
            annotations = applyPropertiesFilter(annotations, profanityFilter, true);

        return annotations;
    }

    private Multimap<Span, EntityAnnotation> applyPropertiesFilter(
            Multimap<Span, EntityAnnotation> annotations,
            Multimap<String, String> propertiesFilter,
            boolean reversed) {
        Multimap<Span, EntityAnnotation> refinedAnnotations = HashMultimap.create();

        for (Span s : annotations.keySet()) {
            if (reversed) {
                refinedAnnotations.putAll(
                        s,
                        annotations.get(s).stream()
                                .filter(e -> !satisfiesPropertyFilter(e, propertiesFilter))
                                .collect(Collectors.toSet()));
            } else {
                refinedAnnotations.putAll(
                        s,
                        annotations.get(s).stream()
                                .filter(e -> satisfiesPropertyFilter(e, propertiesFilter))
                                .collect(Collectors.toSet()));
            }
        }

        return refinedAnnotations;
    }

    private boolean satisfiesPropertyFilter(EntityAnnotation entity, Multimap<String, String> propertiesFilter) {
        boolean okFilter = false;
        EntityLink link = entity.getEntityLink();
        int i = 0;

        // The entity automatically passes the filter
        if (propertiesFilter.isEmpty())
            return true;

        // checks if all the properties are satisfied
        for (String prop : propertiesFilter.keySet()) {
            List<String> filterProp = new ArrayList<>(propertiesFilter.get(prop));
            if (!filterProp.isEmpty() && !link.getValuesForProperty(prop).isEmpty()) {
                filterProp.retainAll(link.getValuesForProperty(prop));
                if (i == 0) {
                    okFilter = !filterProp.isEmpty();
                } else {
                    okFilter = okFilter || !filterProp.isEmpty();
                }
                i++;
            }

        }

        return okFilter;

    }

    private boolean containSubString(Span entitySpan, Set<Span> checkedSpans) {
        for (Span span : checkedSpans) {
            Set<String> spanStrings = new HashSet<>(Arrays.asList(span.span.split(" "))),
                    entityStrings = new HashSet<>(Arrays.asList(entitySpan.span.split(" ")));

            if (setOverlap(entityStrings, spanStrings)) {
                return true;
            }
        }

        return false;
    }

    private boolean setOverlap(Set<String> set1, Set<String> set2) {
        if (set1.size() < set2.size()) {
            for (String s1 : set1) {
                for (String s2 : set2) {
                    if (s1.equals(s2))
                        return true;
                }
            }
        } else {
            for (String s2 : set2) {
                for (String s1 : set1) {
                    if (s1.equals(s2))
                        return true;
                }
            }
        }

        return false;
    }

    private Multimap<Span, EntityAnnotation> filterSubspans(Multimap<Span, EntityAnnotation> annotations) {
        if (annotations.keySet().size() == 1)
            return annotations;

        Multimap<Span, EntityAnnotation> refinedAnnotations = HashMultimap.create();
        Set<Span> checkedSpans = new HashSet<>();

        int i = 0, j;
        for (Span s1 : annotations.keySet()) {
            Span selectedSpan = null;
            j = 0;
            if (!checkedSpans.contains(s1) || !containSubString(s1, checkedSpans)) {
                for (Span s2 : annotations.keySet()) {
                    if (j > i) {
                        if (s1.getStartOffset() == s2.getStartOffset() &&
                                s1.getEndOffset() < s2.getEndOffset() ||
                                s1.getStartOffset() > s2.getStartOffset() &&
                                        s1.getEndOffset() == s2.getEndOffset()) {
                            Collection<EntityAnnotation> e1List = annotations.get(s1),
                                    e2List = annotations.get(s2);
                            if (e1List.size() == 1 && e2List.size() == 1) {
                                EntityAnnotation e1 = e1List.iterator().next(),
                                        e2 = e2List.iterator().next();

                                // just one entity annotation, select the one with the highest score
                                if (e1.getScore() > e2.getScore()) {
                                    selectedSpan = s1;
                                } else {
                                    selectedSpan = s2;
                                }

                                checkedSpans.add(s1);
                                checkedSpans.add(s2);

                            } else {
                                // Take the span which has the candidate with the maximum score
                                Double score1 = e1List.stream()
                                        .max(Comparator.comparingDouble(EntityAnnotation::getScore))
                                        .map(EntityAnnotation::getScore).get(),
                                        score2 = e2List.stream()
                                                .max(Comparator.comparingDouble(EntityAnnotation::getScore))
                                                .map(EntityAnnotation::getScore).get();

                                if (score1 > score2) {
                                    selectedSpan = s1;
                                    if (refinedAnnotations.containsKey(s2)) {
                                        refinedAnnotations.removeAll(s2);
                                    }
                                } else {
                                    selectedSpan = s2;
                                    if (refinedAnnotations.containsKey(s1)) {
                                        refinedAnnotations.removeAll(s1);
                                    }
                                }
                                checkedSpans.add(s1);
                                checkedSpans.add(s2);
                            }
                        }
                    }
                    j++;
                }

                if (selectedSpan != null) {
                    refinedAnnotations.putAll(selectedSpan, annotations.get(selectedSpan));
                } else {
                    if (!checkedSpans.contains(s1) && !containSubString(s1, checkedSpans)) {
                        refinedAnnotations.putAll(s1, annotations.get(s1));
                    }
                }
                i++;
            }
        }

        return refinedAnnotations;
    }

    private Set<EntityAnnotation> filterSubspans(Set<EntityAnnotation> annotations) {
        Set<EntityAnnotation> refinedAnnotations = new HashSet<>();

        int i = 0, j = 0;
        for (EntityAnnotation a1 : annotations) {
            EntityAnnotation selectedAnnotation = a1;

            for (EntityAnnotation a2 : annotations) {
                if (j > i) {
                    if (a1.getSpan().getStartOffset() == a2.getSpan().getStartOffset() &&
                            a1.getSpan().getEndOffset() < a2.getSpan().getEndOffset()) {
                        selectedAnnotation = a2;
                    }
                }
                j++;
            }

            refinedAnnotations.add(selectedAnnotation);

            i++;
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

        // filter out subspans if there are span of text that cover a bigger part of the text
        refinedAnnotations = filterSubspans(refinedAnnotations);

        Double meanScore = refinedAnnotations.stream().collect(Collectors.averagingDouble(EntityAnnotation::getScore));

        return refinedAnnotations.stream().filter(e -> e.getScore() >= meanScore).collect(Collectors.toSet());
    }

    //TODO: retrieve related concept types according to the current annotations and context
    private Multimap<String, String> applyContextFilter(Multimap<Span, EntityAnnotation> annotations, Multimap<String, String> context) {
        return null;
    }


    private Multimap<Span, EntityAnnotation> processAnnotations(Multimap<Span, EntityScore> annotations,
                                                                Annotation annotatedText,
                                                                Double annotationScore,
                                                                Double candidateScore) {
        Multimap<Span, EntityAnnotation> refinedAnnotations = HashMultimap.create();

        for (Span span : annotations.keySet()) {

            if (this.skipAnnotation(span, annotatedText)) {
                continue;
            }

            // if the maximum for the current annotation is lower than the threshold skip it
            if (annotations.get(span).stream().max(Comparator.comparingDouble(i -> i.score)).get().score <= annotationScore)
                continue;

            Set<EntityAnnotation> currEntityAnnotations = new HashSet<>();

            for (EntityScore score : annotations.get(span)) {
                if (score.score <= candidateScore)
                    continue;

                EntityAnnotation annotation = new EntityAnnotation();
                annotation.setSpan(span);
                annotation.setEntity(hash.getEntityName(score.entity.id).toString());
                annotation.setScore(score.score);
                try {
                    String entityIdentifier = wikidataSPARQLClient.getWikidataURI(annotation.getEntity());

                    if (entityIdentifier != null && !isDisambiguationPage(entityIdentifier)) {
                        EntityLink link = new EntityLink(KnowledgeBase.WIKIDATA, entityIdentifier);
                        annotation.setEntityLink(link);

                        Multimap<String, String> properties = wikidataSPARQLClient.getEntityProps(
                                link.getIdentifier(),
                                entityProperties
                        );
                        link.setProperties(properties);
                        currEntityAnnotations.add(annotation);
                    } else {
                        System.err.println(String.format("No valid identifier for entity %s", score.entity.id));
                    }
                } catch (UnsupportedEncodingException e) {
                    System.err.println(String.format(
                            "Skipping Wikidata annotation %s due to invalid Wikipedia URL encoding!",
                            annotation.getEntity()
                    ));

                }

            }

            refinedAnnotations.putAll(span, currEntityAnnotations);
        }

        return refinedAnnotations;
    }

    private boolean isDisambiguationPage(String entityIdentifier) {
        return wikidataSPARQLClient.isDisambiguationPage(entityIdentifier);
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
