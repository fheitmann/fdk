package no.dcat.harvester.crawler.client;

import com.google.gson.Gson;
import no.dcat.harvester.crawler.exception.DataThemesNotLoadedException;
import no.dcat.shared.SkosCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RetrieveCodes {

    private static final Logger logger = LoggerFactory.getLogger(RetrieveCodes.class);


    public static Map<String, Map<String, SkosCode>> getAllCodes() throws DataThemesNotLoadedException {
        Map<String, Map<String, SkosCode>> allCodes = new HashMap<>();

        RestTemplate restTemplate = new RestTemplate();

        List<String> codeTypes = restTemplate.getForEntity("http://themes:8080/codes", List.class).getBody();

        for (String codeType : codeTypes) {
            Map<String, SkosCode> codes = new HashMap<>();


            List<SkosCode> body = restTemplate.exchange("http://themes:8080/codes/" + codeType, HttpMethod.GET, null, new ParameterizedTypeReference<List<SkosCode>>() {}).getBody();


            body.forEach(code -> codes.put(code.getUri(), code));


            allCodes.put(codeType, codes);
        }


        String s = new Gson().toJson(allCodes);

        logger.info(s);

        return allCodes;
    }
}
