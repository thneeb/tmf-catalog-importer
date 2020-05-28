package de.neebs;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class Mapping {
    private String type;
    private String connection;
    private String query;
    private String request;
    private Map<String, Object> mapping;
    private Map<String, Object> behaviour;
}
