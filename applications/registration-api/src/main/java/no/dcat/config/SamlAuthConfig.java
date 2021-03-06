package no.dcat.config;

import com.github.ulisesbocchio.spring.boot.security.saml.annotation.EnableSAMLSSO;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile({"prod", "st1"})
@Configuration
@EnableSAMLSSO
public class SamlAuthConfig {
}
