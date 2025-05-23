package br.com.conectabyte.profissu.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@Component
@ConfigurationProperties
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProfissuProperties {
    private Spring spring = new Spring();
    private Profissu profissu = new Profissu();
}
