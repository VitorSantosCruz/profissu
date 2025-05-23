package br.com.conectabyte.profissu.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties
public class ProfissuProperties {
    private Spring spring = new Spring();
    private Profissu profissu = new Profissu();
}
