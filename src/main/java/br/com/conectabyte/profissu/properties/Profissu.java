package br.com.conectabyte.profissu.properties;

import java.util.List;

import lombok.Data;

@Data
public class Profissu {
    private Jwt jwt = new Jwt();
    private String url;
    private List<String> allowedOrigins;
}