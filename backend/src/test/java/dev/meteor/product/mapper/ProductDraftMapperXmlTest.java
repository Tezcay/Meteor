package dev.meteor.product.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Map;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

/** 不连接数据库的 XML 契约检查；真实插入与回滚由容器测试验证。 */
class ProductDraftMapperXmlTest {
    @Test
    void keepsOwnershipFilteringLockingAndGeneratedKeys() throws Exception {
        Configuration config = new Configuration();
        String resource = "mapper/ProductDraftMapper.xml";
        try (InputStream input = Resources.getResourceAsStream(resource)) {
            new XMLMapperBuilder(input, config, resource, config.getSqlFragments()).parse();
        }
        String namespace = "dev.meteor.product.mapper.ProductDraftMapper.";
        String shopSql = config.getMappedStatement(namespace + "findActiveShopForUpdate")
                .getBoundSql(Map.of("userId", 1L)).getSql().replaceAll("\\s+", " ").trim();
        assertThat(shopSql).contains("owner_user_id = ?", "status = 'ACTIVE'").endsWith("FOR UPDATE");
        String listSql = config.getMappedStatement(namespace + "findOwnDrafts")
                .getBoundSql(Map.of("userId", 1L)).getSql().replaceAll("\\s+", " ").trim();
        assertThat(listSql).contains("s.owner_user_id = ?", "pr.status = 'DRAFT'").endsWith("LIMIT 50");
        assertThat(config.getMappedStatement(namespace + "insertProduct").getKeyProperties())
                .containsExactly("productId");
        assertThat(config.getMappedStatement(namespace + "insertRevision").getKeyProperties())
                .containsExactly("revisionId");
    }
}
