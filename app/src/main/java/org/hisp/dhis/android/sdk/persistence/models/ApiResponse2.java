package org.hisp.dhis.android.sdk.persistence.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;



import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiResponse2 {

    @JsonProperty("responseType")
    private String responseType;

    @JsonProperty("status")
    private ImportSummary2.Status status;

    @JsonProperty("importCount")
    private ImportCount2 importCount;

    @JsonProperty("importSummaries")
    private List<ImportSummary2> importSummaries;

    public ApiResponse2() {
        // explicit empty constructor
    }

    public String getResponseType() {
        return responseType;
    }

    public ImportSummary2.Status getStatus() {
        return status;
    }

    public ImportCount2 getImportCount() {
        return importCount;
    }

    public List<ImportSummary2> getImportSummaries() {
        return importSummaries;
    }
}
