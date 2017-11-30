package no.dcat.controller;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import no.dcat.model.Catalog;
import no.dcat.model.Dataset;
import no.dcat.service.CatalogRepository;
import no.dcat.service.DatasetRepository;
import no.dcat.datastore.domain.dcat.smoke.TestCompleteCatalog;
import org.hamcrest.Matchers;
import org.junit.Assert;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by bjg on 01.03.2017.
 */
@ActiveProfiles(value = "unit-integration")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
public class DatasetControllerIT {
    private static Logger logger = LoggerFactory.getLogger(DatasetControllerIT.class);

    @Autowired
    private TestRestTemplate authorizedRestTemplate;

    private TestRestTemplate unathorizedRestTemplate = new TestRestTemplate();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CatalogRepository catalogRepository;

    @Autowired
    private DatasetRepository datasetRepository;

    private HttpHeaders headers = new HttpHeaders();

    @Before
    public void before() {
        catalogRepository.deleteAll();
        datasetRepository.deleteAll();
    }

    @Test
    @WithUserDetails("03096000854")
    public void postCatalogAndDatasetFollowedByGetRequestShouldWork() throws Exception {
        Catalog catalog = new Catalog();
        String catalogId = "910244132";
        catalog.setId(catalogId);
        mockMvc
                .perform(
                        MockMvcRequestBuilders
                                .post("/catalogs")
                                .content(asJsonString(catalog))
                                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andExpect(content().string("{\"id\":\"910244132\",\"uri\":\"http://brreg.no/catalogs/910244132\",\"title\":{},\"description\":{},\"publisher\":{\"uri\":\"http://data.brreg.no/enhetsregisteret/enhet/910244132\",\"id\":\"910244132\",\"name\":\"RAMSUND OG ROGNAN REVISJON\"}}"))
                .andExpect(status().isOk());


        Dataset dataset = new Dataset();

        Map<String, String> languageTitle = new HashMap<>();
        languageTitle.put("nb", "Test-tittel");
        dataset.setTitle(languageTitle);

        Map<String, String> languangeDescription = new HashMap<>();
        languangeDescription.put("nb", "test");
        dataset.setDescription(languangeDescription);


        String datasetResponseJson = mockMvc
                .perform(
                        MockMvcRequestBuilders
                                .post("/catalogs/" + catalogId + "/datasets/")
                                .content(asJsonString(dataset))
                                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(MockMvcResultMatchers.jsonPath("$.catalogId").value(catalogId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();


        mockMvc
                .perform(MockMvcRequestBuilders.get("/catalogs/" + catalogId + "/datasets/" + new Gson().fromJson(datasetResponseJson, Dataset.class).getId()))
                .andExpect(status().isOk());


    }


    @Test
    public void unauthorizedPostOfDatasetShouldRedirectToLoginPage() throws Exception {


        Dataset dataset = new Dataset("101");

        Map<String, String> languageTitle = new HashMap<>();
        languageTitle.put("nb", "Test-tittel");
        dataset.setTitle(languageTitle);

        Map<String, String> languangeDescription = new HashMap<>();
        languangeDescription.put("nb", "test");
        dataset.setDescription(languangeDescription);

        dataset.setCatalogId("910244132");


        mockMvc
                .perform(
                        MockMvcRequestBuilders
                                .post("/catalogs/" + dataset.getCatalogId() + "/datasets/")
                                .content(asJsonString(dataset))
                                .contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(print())
                .andExpect(status().is3xxRedirection());

    }


    @Test
    @WithUserDetails("03096000854")
    public void deleteUnknownDatasetShouldResultIn404() throws Exception {

        mockMvc
                .perform(MockMvcRequestBuilders.delete("/catalogs/910244132/datasets/123"))
                .andDo(print())
                .andExpect(status().isNotFound());


    }

    @Test
    public void deleteDatasetWithoutUserShouldGiveRedirectToLogin() throws Exception {
        mockMvc
                .perform(MockMvcRequestBuilders.delete("/catalogs/910244132/datasets/123"))
                .andDo(print())
                .andExpect(status().is3xxRedirection());
    }


//  @Test
//  public void createADatasetAndTryToDeleteItWithAnotherUser(){
//        //@TODO
//  }

//
//    @Test
//    @WithUserDetails("03096000854")
//    public void deleteDatasetSuccess() throws Exception {
//        Catalog catalog = new Catalog();
//        String catalogId = "910244132";
//        catalog.setId(catalogId);
//        Catalog catResult = authorizedRestTemplate
//                .postForObject("/catalogs/", catalogId, Catalog.class);
//
//        String datasetId = "101";
//        Dataset dataset = new Dataset(datasetId);
//
//        Map<String, String> languageTitle = new HashMap<>();
//        languageTitle.put("nb","Test-tittel");
//        dataset.setTitle(languageTitle);
//
//        Map<String, String> languangeDescription = new HashMap<>();
//        languangeDescription.put("nb","test");
//        dataset.setDescription(languangeDescription);
//
//        dataset.setCatalogId(catalogId);
//
//        Dataset result = authorizedRestTemplate
//                .postForObject("/catalogs/" + catalogId + "/datasets/", dataset, Dataset.class);
//
//        datasetId = result.getId();
//
//        HttpEntity<String> request = new HttpEntity<String>(headers);
//
//        String datasetResUrl = "/catalogs/" + catalogId + "/datasets/" + result.getId();
//        ResponseEntity<String> deleteResponse = authorizedRestTemplate.exchange(datasetResUrl, HttpMethod.DELETE, request, String.class);
//
//        assertThat(deleteResponse.getStatusCode(), is(HttpStatus.OK));
//
//        //Check that dataset is actually gone...
//        ResponseEntity<String> getResponse = authorizedRestTemplate.exchange(datasetResUrl, HttpMethod.GET, request, String.class);
//        assertThat(getResponse.getStatusCode(), is(HttpStatus.NOT_FOUND));
//
//    }

//    @Test
//    @WithUserDetails("03096000854")
//    public void createDatasetInUnknownCatalogFails() throws Throwable {
//        Dataset ds = new Dataset();
//        ds.setId("444");
//        String url = "/catalogs/" + "XX" + "/datasets/";
//
//        HttpEntity<Dataset> request = new HttpEntity<>(ds, headers);
//
//        ResponseEntity<Dataset> result = authorizedRestTemplate.exchange(url, HttpMethod.POST, request, Dataset.class);
//
//        //TODO: Returnerer 302 found. Hvorfor?
//        assertThat(result.getStatusCode(), is(HttpStatus.FORBIDDEN));
//    }

    public static String asJsonString(Object obj) {

        return new Gson().toJson(obj);

    }

    @Test
    public void repositoryCanSaveCompleteDataset() throws Throwable {
        no.dcat.shared.Catalog catalog = TestCompleteCatalog.getCompleteCatalog();

        no.dcat.shared.Dataset completeDataset = catalog.getDataset().get(0);

        Dataset expected = new Dataset();
        expected.setId("1");
        BeanUtils.copyProperties(completeDataset, expected);

        String id = expected.getId();

        datasetRepository.save(expected);

        Dataset actual = datasetRepository.findOne(id);

        Assert.assertThat(actual, Matchers.is(expected));
        Assert.assertThat(actual.getReferences(), Matchers.is(expected.getReferences()));
    }

    final String datasetWRefs =
            "{\"id\":\"cb84a1ef-c502-4802-8cbb-02827a51874c\",\"uri\":\"http://brreg.no/catalogs/910244132/datasets/cb84a1ef-c502-4802-8cbb-02827a51874c\",\"title\":{},\"description\":{},\"objective\":{},\"publisher\":{\"uri\":\"http://data.brreg.no/enhetsregisteret/enhet/910244132.json\",\"id\":\"910244132\",\"name\":\"RAMSUND OG ROGNAN REVISJON\"},\"accessRights\":{\"uri\":\"http://publications.europa.eu/resource/authority/access-right/PUBLIC\",\"prefLabel\":{}},\"provenance\":{\"uri\":\"\",\"prefLabel\":{\"nb\":\"\"}},\"accrualPeriodicity\":{\"uri\":\"\",\"prefLabel\":{\"no\":\"\"}},\"type\":\"\",\"catalogId\":\"910244132\",\"_lastModified\":\"2017-10-25T13:59:31.562+0000\",\"registrationStatus\":\"DRAFT\",\"issued\":null,\"modified\":null,\"contactPoint\":[{\"email\":\"\",\"organizationUnit\":\"\",\"hasURL\":\"\",\"hasTelephone\":\"\"}],\"keyword\":[],\"language\":[],\"landingPage\":[],\"theme\":[],\"distribution\":[{\"id\":\"507715\",\"uri\":\"\",\"title\":{\"nb\":\"\"},\"description\":{\"nb\":\"\"},\"downloadURL\":[],\"accessURL\":[],\"license\":{\"uri\":\"\",\"prefLabel\":{\"nb\":\"\"}},\"conformsTo\":[{\"uri\":\"\",\"prefLabel\":{\"nb\":\"\"}}],\"page\":[],\"format\":[],\"type\":\"API\",\"conformsToUri\":\"\"}],\"sample\":[{\"id\":\"993127\",\"uri\":\"\",\"title\":{\"nb\":\"\"},\"description\":{\"nb\":\"\"},\"downloadURL\":[],\"accessURL\":[],\"license\":{\"uri\":\"\",\"prefLabel\":{\"nb\":\"\"}},\"conformsTo\":[{\"uri\":\"\",\"prefLabel\":{\"nb\":\"\"}}],\"page\":[],\"format\":[],\"type\":\"API\",\"conformsToPrefLabel\":\"\",\"conformsToUri\":\"\"}],\"temporal\":[{}],\"spatial\":[],\"accessRightsComment\":[],\"legalBasisForRestriction\":[],\"legalBasisForProcessing\":[],\"legalBasisForAccess\":[],\"identifier\":[],\"subject\":[],\"conformsTo\":[],\"informationModel\":[{\"uri\":\"\",\"prefLabel\":{\"nb\":\"d\"},\"extraType\":null}],\"references\":[{\"referenceType\":{\"uri\":\"http://purl.org/dc/source\",\"code\":\"x\",\"prefLabel\":{\"nb\":\"Er avledet fra\"}},\"source\":{\"uri\":\"cb84a1ef-c502-4802-8cbb-02827a51874c\",\"prefLabel\":{\"nb\":\"x\"}}}]}";

    @Test
    public void canParseDatasetWithJackson() throws Throwable {
        ObjectMapper mapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        Dataset expected = mapper.readValue(datasetWRefs, Dataset.class);

        Assert.assertThat(expected.getReferences().size(), Matchers.is(1));

        datasetRepository.save(expected);

        Dataset actual = datasetRepository.findOne(expected.getId());

        Assert.assertThat(actual, Matchers.is(expected));

        logger.info(expected.getReferences().toString());

    }

}
