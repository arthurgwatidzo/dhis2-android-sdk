package org.hisp.dhis.android.sdk.persistence.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Created by erling on 04.04.16.
 */
public class Conflict2 {


    @JsonProperty("object")
    private String object;

    @JsonProperty("value")
    private String value;

    public Conflict2() {
        // explicit empty constructor
    }

    public String getObject() {
        return object;
    }

    public String getValue() {
        return value;
    }
}
