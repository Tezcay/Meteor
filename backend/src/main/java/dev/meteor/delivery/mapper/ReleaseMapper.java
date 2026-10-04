package dev.meteor.delivery.mapper;

import dev.meteor.delivery.mapper.projection.ReleaseProjection;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ReleaseMapper {

    @Select("""
            SELECT id,
                   package_id,
                   release_number,
                   version_label,
                   summary,
                   published_at
            FROM releases
            WHERE package_id = #{packageId}
              AND status = 'PUBLISHED'
            ORDER BY release_number DESC, id DESC
            """)
    List<ReleaseProjection> findPublishedByPackageId(@Param("packageId") long packageId);
}
