package no.dcat.portal.query;

import org.elasticsearch.action.ListenableActionFuture;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Class for testing SimpleQueryService.
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleQueryServiceMochitoTest {

    SimpleQueryService sqs;
    Client client;

    @Before
    public void setUp() {
        sqs = new SimpleQueryService();
        client = mock(Client.class);
        populateMock();
        sqs.client = client;

    }

    /**
     * Tests that elasicsearch is called with the correct set of Parameters.
     */
    @Test
    public void testElasticSearchIsCalledWithCorrectParameters() {
        ResponseEntity<String> actual =  sqs.search("query", 1, 10, "tema.nb", "ascending");

        verify(client.prepareSearch("dcat").setTypes("dataset").setQuery(any(QueryBuilder.class)).setFrom(1).setSize(10)).addSort("tema.nb.raw", SortOrder.ASC);
        assertThat(actual.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    }

    /**
     * Tests that elasicsearch is called with the correct set of Parameters. Check default direction.
     */
    @Test
    public void testElasticSearchIsCalledWithCorrectParametersDefaultDirection() {
        ResponseEntity<String> actual =  sqs.search("query", 1, 10, "", "");

        verify(client.prepareSearch("dcat").setTypes("dataset").setQuery(any(QueryBuilder.class)).setFrom(1)).setSize(10);
        verify(client.prepareSearch("dcat").setTypes("dataset").setQuery(any(QueryBuilder.class)).setFrom(1).setSize(10), never()).addSort("", SortOrder.DESC);
        assertThat(actual.getStatusCodeValue()).isEqualTo(HttpStatus.OK.value());
    }

    /**
     * Inputparameter validation. Minus from value shall throw http-error bad request.
     */
    @Test
    public void return400IfFromIsBelowZero() {
        SimpleQueryService sqs = new SimpleQueryService();
        ResponseEntity<String> actual =  sqs.search("", -10, 1000, "", "");

        assertThat(actual.getStatusCodeValue()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    /**
     * Inputparameter validation. Over 100 size shall throw http-error bad request.
     */
    @Test
    public void return400IfSizeIsLargerThan100() {
        SimpleQueryService sqs = new SimpleQueryService();
        ResponseEntity<String> actual =  sqs.search("", 10, 101, "", "");

        assertThat(actual.getStatusCodeValue()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    private void populateMock() {
        SearchHit[] hits = null;

        SearchHits searchHits = mock(SearchHits.class);
        when(searchHits.getHits()).thenReturn(hits);

        SearchResponse response = mock(SearchResponse.class);
        when(response.getHits()).thenReturn(searchHits);

        ListenableActionFuture<SearchResponse> action = mock(ListenableActionFuture.class);
        when(action.actionGet()).thenReturn(response);

        SearchRequestBuilder builder = mock(SearchRequestBuilder.class);
        when(builder.setTypes("dataset")).thenReturn(builder);
        when(builder.setQuery(any(QueryBuilder.class))).thenReturn(builder);
        when(builder.setFrom(anyInt())).thenReturn(builder);
        when(builder.setSize(anyInt())).thenReturn(builder);
        when(builder.addSort(anyString(), any(SortOrder.class))).thenReturn(builder);
        when(builder.execute()).thenReturn(action);

        when(client.prepareSearch("dcat")).thenReturn(builder);
    }
}