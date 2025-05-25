package br.com.conectabyte.profissu.properties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Spring {
  private Application application = new Application();
}
