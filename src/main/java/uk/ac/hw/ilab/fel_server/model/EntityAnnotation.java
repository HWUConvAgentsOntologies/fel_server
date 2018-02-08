package uk.ac.hw.ilab.fel_server.model;

import com.yahoo.semsearch.fastlinking.view.Span;

import java.util.Objects;

public class EntityAnnotation implements Comparable<EntityAnnotation> {
    private EntityLink entityLink;
    private Span span;
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

    public Span getSpan() {
        return span;
    }

    public void setSpan(Span span) {
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
        if (o.entityLink.equals(this.entityLink)) {
            return 0;
        }

        return -Double.compare(this.score, o.score);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityAnnotation that = (EntityAnnotation) o;
        return Objects.equals(entity, that.entity);
    }

    @Override
    public int hashCode() {

        return Objects.hash(entity);
    }

    @Override
    public String toString() {
        return "EntityAnnotation{" +
                "entityLink=" + entityLink +
                ", span=" + span +
                ", entity='" + entity + '\'' +
                ", score=" + score +
                '}';
    }
}
