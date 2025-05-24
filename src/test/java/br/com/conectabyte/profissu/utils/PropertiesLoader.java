package br.com.conectabyte.profissu.utils;

import java.io.InputStream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import br.com.conectabyte.profissu.properties.ProfissuProperties;

public class PropertiesLoader {
  public ProfissuProperties loadProperties() throws Exception {
    final var objectMapper = new ObjectMapper(new YAMLFactory());
    var profissuProperties = new ProfissuProperties();
    var profissuPropertiesTest = new ProfissuProperties();

    objectMapper.findAndRegisterModules();

    try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.yaml")) {
      profissuProperties = objectMapper.readValue(input, ProfissuProperties.class);
    }

    try (InputStream input = getClass().getClassLoader().getResourceAsStream("application-test.yaml")) {
      profissuPropertiesTest = objectMapper.readValue(input, ProfissuProperties.class);
    }

    profissuPropertiesTest.setSpring(profissuProperties.getSpring());
    profissuPropertiesTest.getProfissu().setJwt(profissuProperties.getProfissu().getJwt());

    return profissuPropertiesTest;
  }
}
