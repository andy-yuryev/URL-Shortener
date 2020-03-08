package org.example.resources;

import com.mongodb.MongoClient;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Random;

import static com.mongodb.client.model.Updates.inc;

public class LinkResource {

    private static final String ID_KEY = "id";
    private static final String URL_KEY = "url";
    private static final String EXPIRE_AT_KEY = "expireAt";
    private static final String CLICKS_COUNT_KEY = "clicksCount";

    private static final Response ERROR_404 = Response.status(Response.Status.NOT_FOUND).build();

    private static final MongoCollection<Document> LINKS_COLLECTION;

    static {
        MongoClient client = new MongoClient();
        MongoDatabase database = client.getDatabase("short_links");
        LINKS_COLLECTION = database.getCollection("links");
    }

    @PUT
    @Path("put")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public static Response shortenUrl(String url) {
        if (url == null || url.isEmpty()) {
            return Response.status(Response.Status.NOT_ACCEPTABLE).tag("The empty URL can't be shortened.").build();
        }

        int attempt = 0;
        while (attempt < 5) {
            String id = getRandomId();
            Document document = new Document();
            document.put(ID_KEY, id);
            document.put(URL_KEY, url);
            document.put(EXPIRE_AT_KEY, LocalDateTime.now(ZoneOffset.UTC).plusYears(1));
            document.put(CLICKS_COUNT_KEY, 0);
            try {
                LINKS_COLLECTION.insertOne(document);
                return Response.ok("http://localhost:8080/" + id).build();
            } catch (MongoWriteException ignored) {
            }
            attempt++;
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.TEXT_HTML)
    public static Response getUrlById(@PathParam("id") String id) {
        Document document = LINKS_COLLECTION.findOneAndUpdate(new Document(ID_KEY, id), inc(CLICKS_COUNT_KEY, 1));
        if (document == null) {
            return ERROR_404;
        }

        String url = document.getString(URL_KEY);
        if (url == null || url.isEmpty()) {
            return ERROR_404;
        }

        return Response.seeOther(URI.create(url)).build();
    }

    private static String getRandomId() {
        String possibleCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefjhijklmnopqrstuvwxyz0123456789";
        StringBuilder builder = new StringBuilder();
        Random random = new Random();

        while (builder.length() < 5) {
            int index = (int) (random.nextFloat() * possibleCharacters.length());
            builder.append(possibleCharacters.charAt(index));
        }

        return builder.toString();
    }
}
