package org.hisp.dhis.android.sdk.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Created by erling on 04.04.16.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportSummary2 {

    @JsonProperty("status")
    private Status status;

    @JsonProperty("description")
    private String description;

    @JsonProperty("importCount")
    private ImportCount2 importCount;

    @JsonProperty("reference")
    private String reference;

    @JsonProperty("href")
    private String href;

    @JsonProperty("conflicts")
    private List<Conflict2> conflicts;


    public ImportSummary2() {
        // explicit empty constructor
    }

    public Status getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public ImportCount2 getImportCount() {
        return importCount;
    }

    public String getReference() {
        return reference;
    }

    public String getHref() {
        return href;
    }

    public List<Conflict2> getConflicts() {
        return conflicts;
    }

    public enum Status {
        SUCCESS, OK, ERROR
    }
}
