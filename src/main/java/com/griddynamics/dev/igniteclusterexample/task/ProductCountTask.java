package com.griddynamics.dev.igniteclusterexample.task;

import com.griddynamics.dev.igniteclusterexample.entity.Product;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteException;
import org.apache.ignite.compute.ComputeJob;
import org.apache.ignite.compute.ComputeJobAdapter;
import org.apache.ignite.compute.ComputeJobResult;
import org.apache.ignite.compute.ComputeTaskSplitAdapter;
import org.apache.ignite.resources.IgniteInstanceResource;

import javax.cache.Cache;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

public class ProductCountTask extends ComputeTaskSplitAdapter<Iterator<Cache.Entry<String, Product>>, Map<String, Integer>> {

    private static final int PARTITION_SIZE = 100;

    @IgniteInstanceResource
    private Ignite ignite;

    @Override
    protected Collection<? extends ComputeJob> split(int gridSize,
                                                     Iterator<Cache.Entry<String, Product>> iterator) throws IgniteException {
        List<ComputeJob> jobs = new ArrayList<>();
        Set<String> partOfKeys = new HashSet<>();
        System.out.println(">>> Started jobs creation...");
        while (iterator.hasNext()) {
            String key = iterator.next().getKey();
            partOfKeys.add(key);
            if (partOfKeys.size() == PARTITION_SIZE || !iterator.hasNext()) {
                IgniteCache<String, Product> cache = ignite.getOrCreateCache("product");
                Collection<Product> products = cache.getAll(partOfKeys).values();
                jobs.add(new ComputeJobAdapter() {

                    @Override
                    public Object execute() throws IgniteException {
                        System.out.format(">>> Started job for %s products.\n", products.size());

                        return products.stream()
                                .map(Product::getSalePrice)
                                .collect(Collectors.toList());
                    }
                });
                partOfKeys.clear();
            }
        }
        return jobs;
    }

    @Override
    public Map<String, Integer> reduce(List<ComputeJobResult> results) throws IgniteException {
        Map<String, Integer> map = new HashMap<>();
        for (ComputeJobResult result : results) {
            List<String> stringPrices = result.getData();
            for (String stringPrice: stringPrices) {
                BigDecimal price;
                try {
                    price = new BigDecimal(stringPrice);
                } catch (NumberFormatException e) {
                    map.put("wrong format", map.getOrDefault("wrong format", 0) + 1);
                    continue;
                }
                if (BigDecimal.ZERO.compareTo(price) < 1 && new BigDecimal(50).compareTo(price) > 0) {
                    map.put("0-49.99", map.getOrDefault("0-49.99", 0) + 1);
                    continue;
                }
                if (new BigDecimal(50).compareTo(price) < 1 && new BigDecimal(100).compareTo(price) > 0) {
                    map.put("50-99.99", map.getOrDefault("50-99.99", 0) + 1);
                    continue;
                }
                if (new BigDecimal(100).compareTo(price) < 1) {
                    map.put("100+", map.getOrDefault("100+", 0) + 1);
                    continue;
                }
                map.put("less 0", map.getOrDefault("less 0", 0) + 1);
            }
        }
        return map;
    }
}
