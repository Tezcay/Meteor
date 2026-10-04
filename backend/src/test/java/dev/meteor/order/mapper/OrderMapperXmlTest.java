package dev.meteor.order.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

class OrderMapperXmlTest {

    private static final String NAMESPACE = "dev.meteor.order.mapper.OrderMapper";

    @Test
    void declaresEveryMapperStatementAndKeepsLockingQueriesDistinct() throws Exception {
        Configuration configuration = parseMapper();

        assertThat(configuration.getMappedStatementNames())
                .contains(
                        statement("findByUserIdAndIdempotencyKey"),
                        statement("findByUserIdAndIdempotencyKeyForUpdate"),
                        statement("findByIdAndUserId"),
                        statement("findByIdAndUserIdForUpdate"),
                        statement("findAllByUserId"),
                        statement("findDetailByIdAndUserId"),
                        statement("insertOrder"),
                        statement("insertOrderItem"),
                        statement("markPaid")
                );

        String unlockedSql = sql(configuration, "findByIdAndUserId");
        String lockedSql = sql(configuration, "findByIdAndUserIdForUpdate");

        assertThat(unlockedSql)
                .contains("FROM orders o JOIN order_items oi ON oi.order_id = o.id")
                .doesNotContain("FOR UPDATE");
        assertThat(lockedSql).endsWith("FOR UPDATE");

        MappedStatement insertOrder = configuration.getMappedStatement(statement("insertOrder"));
        assertThat(insertOrder.getKeyProperties()).containsExactly("id");
    }

    private Configuration parseMapper() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mapper/OrderMapper.xml";

        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
        }

        return configuration;
    }

    private String sql(Configuration configuration, String methodName) {
        return configuration
                .getMappedStatement(statement(methodName))
                .getBoundSql(Map.of("orderId", 1L, "userId", 1L))
                .getSql()
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String statement(String methodName) {
        return NAMESPACE + "." + methodName;
    }
}
