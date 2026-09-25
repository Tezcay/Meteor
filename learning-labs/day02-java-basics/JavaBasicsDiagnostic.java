import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class JavaBasicsDiagnostic {

    public static void main(String[] args) {
        ProductFile basicGuide = new ProductFile(101L, "basic-guide.pdf");
        ProductFile sameIdentity = new ProductFile(101L, "renamed-guide.pdf");
        ProductFile proAssets = new ProductFile(202L, "pro-assets.zip");

        List<ProductFile> deliveryOrder = new ArrayList<>();
        deliveryOrder.add(basicGuide);
        deliveryOrder.add(sameIdentity);
        deliveryOrder.add(proAssets);

        Set<ProductFile> uniqueFiles = new HashSet<>(deliveryOrder);

        Map<Long, ProductFile> filesById = new HashMap<>();
        for (ProductFile file : deliveryOrder) {
            filesById.put(file.getId(), file);
        }

        System.out.println("list size = " + deliveryOrder.size());
        System.out.println("set size = " + uniqueFiles.size());
        System.out.println("map size = " + filesById.size());
        System.out.println("file 101 = " + filesById.get(101L));

        demonstrateExceptionStack();
    }

    private static void demonstrateExceptionStack() {
        try {
            parseRequestedRelease("release-abc");
        } catch (IllegalArgumentException exception) {
            System.out.println("caught: " + exception.getMessage());
            exception.printStackTrace(System.out);
        }
    }

    private static long parseRequestedRelease(String rawReleaseId) {
        return parsePositiveId(rawReleaseId);
    }

    private static long parsePositiveId(String rawValue) {
        try {
            long value = Long.parseLong(rawValue);
            if (value <= 0) {
                throw new IllegalArgumentException("id must be positive");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("release id must be a number", exception);
        }
    }

    private static final class ProductFile {
        private final long id;
        private final String name;

        private ProductFile(long id, String name) {
            this.id = id;
            this.name = name;
        }

        private long getId() {
            return id;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ProductFile that)) {
                return false;
            }
            return id == that.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "ProductFile{id=" + id + ", name='" + name + "'}";
        }
    }
}
