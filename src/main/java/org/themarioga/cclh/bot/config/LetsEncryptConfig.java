package org.themarioga.cclh.bot.config;

import com.github.valb3r.letsencrypthelper.tomcat.TomcatWellKnownLetsEncryptChallengeEndpointConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;

@Profile("pro")
@Configuration
@Import(TomcatWellKnownLetsEncryptChallengeEndpointConfig.class)
public class LetsEncryptConfig {

}
