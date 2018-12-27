package com.griddynamics.dev.igniteclusterexample.app;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.griddynamics.dev.igniteclusterexample.entity.Product;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class LoadCacheApp {

    public static void main(String[] args) throws IOException {
        ClientConfiguration cfg = new ClientConfiguration().setAddresses("127.0.0.1:10800");
        IgniteClient client = Ignition.startClient(cfg);
        ClientCache<String, Product> cache = client.getOrCreateCache("product");

        List<Product> products = loadObjectList(Product.class, "data/products.csv");
        cache.putAll(products.stream().collect(Collectors.toMap(Product::getId, v -> v)));
        System.out.format("%s products loaded in cache.", products.size());;
    }

    private static <T> List<T> loadObjectList(Class<T> type, String fileName) throws IOException {
        CsvSchema bootstrapSchema = CsvSchema.emptySchema().withHeader();
        CsvMapper mapper = new CsvMapper();
        File file = new ClassPathResource(fileName).getFile();
        MappingIterator<T> readValues =
                mapper.reader(type).with(bootstrapSchema).readValues(file);
        return readValues.readAll();
    }
}
