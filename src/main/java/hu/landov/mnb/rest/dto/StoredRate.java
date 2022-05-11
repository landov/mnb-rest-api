package hu.landov.mnb.rest.dto;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class StoredRate {

    @Id
    private String id;
    private Integer unit;
    private Double rate;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getUnit() {
        return unit;
    }

    public void setUnit(Integer unit) {
        this.unit = unit;
    }

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    @Override
    public String toString() {
        return "StoredRate{" +
                "id='" + id + '\'' +
                ", unit=" + unit +
                ", rate=" + rate +
                '}';
    }
}
