package br.com.conectabyte.profissu.properties;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Profissu {
    private Jwt jwt = new Jwt();
    private Token token = new Token();
    private String url;
    private List<String> allowedOrigins;
}