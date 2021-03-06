package no.dcat.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import no.dcat.factory.RegistrationFactory;
import no.dcat.model.Catalog;
import no.dcat.model.Dataset;
import no.dcat.service.CatalogRepository;
import no.dcat.service.DatasetRepository;
import no.dcat.datastore.domain.dcat.smoke.TestCompleteCatalog;
import no.dcat.shared.testcategories.UnitTest;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.BeanUtils;
import org.springframework.http.HttpEntity;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by dask on 21.04.2017.
 */

@Category(UnitTest.class)
public class DatasetControllerTest {

    private DatasetController datasetController;

    @Mock
    private DatasetRepository mockDatasetRepository;

    @Mock
    private CatalogRepository mockCatalogRepository;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        String catalogId = "1234";
        Catalog catalog = new Catalog();
        catalog.setId(catalogId);
        when(mockCatalogRepository.findOne(anyString())).thenReturn(catalog);


        datasetController = new DatasetController(mockDatasetRepository, mockCatalogRepository);

    }

    @Test
    public void createDatasetOK() throws Throwable {
        String catalogId = "1234";

        Dataset copy = RegistrationFactory.createDataset(catalogId);
        Map<String, String> title = new HashMap<>();
        title.put("nb", "test");
        copy.setTitle(title);

        when(mockDatasetRepository.save((Dataset) anyObject())).thenReturn(copy);

        HttpEntity<Dataset> actualEntity = datasetController.saveDataset(catalogId, copy);

        Dataset actual = actualEntity.getBody();
        assertThat(actual.getCatalogId(), is(catalogId));

    }

    @Test
    public void serializationOK() throws Throwable {
        no.dcat.shared.Catalog catalog = TestCompleteCatalog.getCompleteCatalog();

        no.dcat.shared.Dataset completeDataset = catalog.getDataset().get(0);

        Dataset expected = new Dataset();
        expected.setId("1");
        BeanUtils.copyProperties(completeDataset, expected);

        Gson gson = new Gson();

        String json = gson.toJson(expected);

        Dataset actual = new GsonBuilder().create().fromJson(json, Dataset.class);

        assertThat(actual, is(expected));
        assertThat(actual.getReferences(), is (expected.getReferences()));

    }
}



