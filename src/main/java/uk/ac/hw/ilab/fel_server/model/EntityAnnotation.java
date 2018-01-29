package uk.ac.hw.ilab.fel_server.model;

import org.apache.commons.lang3.tuple.Pair;

public class EntityAnnotation implements Comparable<EntityAnnotation> {
    private EntityLink entityLink;
    private Pair<Integer, Integer> span;
    private String entity;
    private double score;


    public EntityAnnotation() {
    }

    public EntityLink getEntityLink() {
        return entityLink;
    }

    public void setEntityLink(EntityLink entityLink) {
        this.entityLink = entityLink;
    }

    public Pair<Integer, Integer> getSpan() {
        return span;
    }

    public void setSpan(Pair<Integer, Integer> span) {
        this.span = span;
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }


    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    @Override
    public int compareTo(EntityAnnotation o) {
        if (o.getEntity().equals(this.getEntity())) {
            return 0;
        }

        return -Double.compare(this.score, o.score);
    }
}
