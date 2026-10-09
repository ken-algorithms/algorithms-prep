package com.prep.kafka.model;

import java.io.Serializable;

public record EventItem(String key, Boolean value) implements Serializable {
    public EventItem {
        if (key == null) key = "";
    }
}
