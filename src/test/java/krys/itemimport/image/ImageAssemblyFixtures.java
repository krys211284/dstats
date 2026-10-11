package krys.itemimport.image;

import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.image.opencv.OpenCvItemImageAssembler;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Współdzielony input dalszych testów vision; sam assembler i E2E są testowane bez cache. */
final class ImageAssemblyFixtures {
    private static final ItemImageAssembler SHARED = memoized(new OpenCvItemImageAssembler());

    private ImageAssemblyFixtures() {
    }

    static AssembledItemImage assemble(List<ItemImageImportRequest> requests) {
        return SHARED.assemble(requests);
    }

    static ItemImageAssembler memoized(ItemImageAssembler delegate) {
        var images = new ConcurrentHashMap<List<Source>, AssembledItemImage>();
        return requests -> {
            List<ItemImageImportRequest> input = List.copyOf(requests);
            List<Source> key = input.stream().map(Source::from).toList();
            return images.computeIfAbsent(key, ignored -> delegate.assemble(input));
        };
    }

    private record Source(String filename, String contentType, String sha256) {
        private static Source from(ItemImageImportRequest request) {
            try {
                byte[] hash = MessageDigest.getInstance("SHA-256").digest(request.getImageBytes());
                return new Source(request.getOriginalFilename(), request.getContentType(), HexFormat.of().formatHex(hash));
            } catch (NoSuchAlgorithmException exception) {
                throw new IllegalStateException(exception);
            }
        }
    }
}
