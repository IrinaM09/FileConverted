package com.lib2life.api.rest;

import com.alibaba.fastjson.JSON;
import com.lib2life.api.model.Book;
import com.lib2life.api.util.Utilities;
import org.elasticsearch.action.admin.cluster.health.ClusterHealthResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.Strings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.transport.client.PreBuiltTransportClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import static org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder;

@Path("/")
public class ElasticsearchService {

    /**
     * Function that saves a book in Elasticsearch by indexing it
     */
    @Path("esSave")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response elasticsearchSave() {
        TransportAddress address;
        TransportClient client;
        Settings settings;

        try {
            address = new TransportAddress(InetAddress.getByName("127.0.0.1"), 9200);
            settings = Settings
                    .builder()
                    .put("cluster.name", "lib2life")
                    .put("client.transport.sniff", true)
                    .build();

            /* Initiate Transport Client */
            client = new PreBuiltTransportClient(settings)
                    .addTransportAddress(address);

            /* Verify it cluster is healthy */
            ClusterHealthResponse clusterResponse = client
                    .admin()
                    .cluster()
                    .prepareHealth()
                    .setWaitForGreenStatus()
                    .setTimeout(TimeValue.timeValueSeconds(5))
                    .execute()
                    .actionGet();

            if (clusterResponse.isTimedOut()) {
                Utilities.LOGGER.log(Level.WARNING,
                        "[ElasticsearchService] The cluster is unhealthy: " + clusterResponse.getStatus());
            }

            //TODO: content: html to json content
            String content = "";
            String bookName = "";

            XContentBuilder builder = jsonBuilder()
                    .startObject()
                    .field("name", bookName)
                    .field("content", content)
                    .endObject();

            String json = Strings.toString(builder);

            /* Index given file (index /doc type/ id) */
            IndexResponse indexResponse = client
                    .prepareIndex("books", "doc_", bookName)
                    .setSource(json, XContentType.JSON)
                    .get();

            Utilities.LOGGER.log(Level.INFO, "[ElasticsearchService] Index Response: " + indexResponse);

        } catch (Exception e) {
            e.printStackTrace();

            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("[ElasticsearchService] esSave - Server Error")
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity("[ElasticsearchService] esSave - OK")
                .build();
    }

    /**
     * Function that searches for an indexed book in Elasticsearch
     */
    @Path("esSearch")
    public Response elasticsearchSearch() {
        TransportAddress transportAddress;
        Client transportClient;
        Settings transportSettings;

        try {
            transportAddress = new TransportAddress(InetAddress.getLocalHost(), 9300);
            transportSettings = Settings
                    .builder()
                    .put("client.transport.sniff", true)
                    .put("cluster.name", "elasticsearch")
                    .build();

            /* Initiate Transport Client */
            transportClient = new PreBuiltTransportClient(transportSettings)
                    .addTransportAddress(transportAddress);

            /* Execute search in ES cluster */
            SearchResponse response = transportClient
                    .prepareSearch()
                    .execute()
                    .actionGet();

            List<SearchHit> searchHits = Arrays.asList(response.getHits().getHits());

            List<Book> results = new ArrayList<>();

            /* Search in result */
            searchHits.forEach(
                    hit -> results
                            .add(JSON.parseObject(hit.getSourceAsString(), Book.class)));

        } catch (Exception e) {
            e.printStackTrace();

            return Response
                    .status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("[ElasticsearchService] esSearch - Server Error")
                    .build();
        }

        return Response
                .status(Response.Status.OK)
                .entity("[ElasticsearchService] esSearch - OK")
                .build();
    }
}
