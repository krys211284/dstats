package krys.itemimport.image;

import krys.itemimport.ItemImageImportRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageAssemblyFixturesTest {
    private static final AssembledItemImage OUTPUT = new AssembledItemImage(
            new byte[]{1}, "PNG", 1, 1, 1, "wynik kontrolowanego delegata");

    @Test
    void shouldAssembleIdenticalInputOnlyOnceAcrossConcurrentConsumers() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ItemImageAssembler fixtures = ImageAssemblyFixtures.memoized(requests -> {
            calls.incrementAndGet();
            return OUTPUT;
        });
        List<ItemImageImportRequest> input = List.of(request("fixture.png", "image/png", new byte[]{1}));
        try (var workers = Executors.newFixedThreadPool(8)) {
            List<Callable<AssembledItemImage>> jobs = IntStream.range(0, 32)
                    .mapToObj(i -> (Callable<AssembledItemImage>) () -> fixtures.assemble(input)).toList();
            for (var result : workers.invokeAll(jobs)) assertSame(OUTPUT, result.get());
        }
        assertEquals(1, calls.get());
    }

    @Test
    void shouldKeepDifferentPixelsNamesMimeAndSourceOrderAsIndependentInputs() {
        AtomicInteger calls = new AtomicInteger();
        ItemImageAssembler fixtures = ImageAssemblyFixtures.memoized(requests -> {
            calls.incrementAndGet();
            return OUTPUT;
        });
        var first = request("first.png", "image/png", new byte[]{1});
        var second = request("second.png", "image/png", new byte[]{2});
        var variants = List.of(
                List.of(first),
                List.of(request("first.png", "image/png", new byte[]{2})),
                List.of(request("renamed.png", "image/png", new byte[]{1})),
                List.of(request("first.png", "image/jpeg", new byte[]{1})),
                List.of(first, second), List.of(second, first));
        for (var input : variants) {
            fixtures.assemble(input);
            fixtures.assemble(input);
        }
        assertEquals(variants.size(), calls.get());
    }

    @Test
    void shouldPropagateAssemblyFailureWithoutCachingIt() {
        ItemImageAssembler fixtures = ImageAssemblyFixtures.memoized(requests -> {
            throw new IllegalArgumentException("STITCH_NOT_CONFIDENT");
        });
        var input = List.of(request("broken.png", "image/png", new byte[]{1}));
        assertEquals("STITCH_NOT_CONFIDENT", assertThrows(IllegalArgumentException.class,
                () -> fixtures.assemble(input)).getMessage());
    }

    private static ItemImageImportRequest request(String name, String mime, byte[] bytes) {
        return new ItemImageImportRequest(name, mime, bytes);
    }
}
