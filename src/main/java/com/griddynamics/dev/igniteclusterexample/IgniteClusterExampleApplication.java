package com.griddynamics.dev.igniteclusterexample;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.griddynamics.dev.igniteclusterexample.config.ApplicationConfig;
import com.griddynamics.dev.igniteclusterexample.entity.Product;
import com.griddynamics.dev.igniteclusterexample.task.ProductCountTask;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootApplication
public class IgniteClusterExampleApplication {

	private static final String CACHE_NAME = "product";

	public static void main(String[] args) throws IOException {
		Ignite ignite = startIgnite();

		loadCache(ignite);

		IgniteCache<String, Product> cache = ignite.getOrCreateCache(CACHE_NAME);

		long start = System.currentTimeMillis();
		Map<String, Integer> statistic = ignite.compute().execute(ProductCountTask.class, cache.iterator());
		System.out.println();
		System.out.println("Statistic [products=" + statistic + ", duration=" + (System.currentTimeMillis() - start) + "ms]");
	}

	private static Ignite startIgnite() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

		context.register(ApplicationConfig.class);
		context.refresh();

		IgniteConfiguration cfg = context.getBean(IgniteConfiguration.class);

		Ignition.setClientMode(true);
		return Ignition.start(cfg);
	}

	private static void loadCache(Ignite ignition) throws IOException {
		IgniteCache<String, Product> cache = ignition.getOrCreateCache(CACHE_NAME);
		if (cache.iterator().hasNext()) {
			System.out.println("Cache not empty and will not be reloaded.");
			return;
		}

		CsvSchema bootstrapSchema = CsvSchema.emptySchema().withHeader();
		CsvMapper mapper = new CsvMapper();
		File file = new ClassPathResource("data/products.csv").getFile();
		MappingIterator<Product> readValues =
				mapper.reader(Product.class).with(bootstrapSchema).readValues(file);
		List<Product> products = readValues.readAll();

		cache.putAll(products.stream()
				.collect(Collectors.toMap(Product::getId, v -> v)));
		System.out.format("%s products loaded in cache.\n", products.size());
	}
}

