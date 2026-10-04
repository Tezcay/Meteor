package dev.meteor.delivery.mapper;

import dev.meteor.delivery.mapper.projection.DeliveryManifestProjection;
import dev.meteor.delivery.mapper.projection.ManifestItemProjection;
import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface DeliveryManifestMapper {

    @Select("""
            SELECT dm.id,
                   dm.release_id,
                   r.version_label,
                   dm.created_at
            FROM delivery_manifests dm
            INNER JOIN releases r ON r.id = dm.release_id
            WHERE r.id = #{releaseId}
              AND r.package_id = #{packageId}
              AND r.status = 'PUBLISHED'
            """)
    Optional<DeliveryManifestProjection> findPublishedByReleaseIdAndPackageId(
            @Param("releaseId") long releaseId,
            @Param("packageId") long packageId
    );

    @Select("""
            SELECT mi.id,
                   mi.item_position AS position,
                   mi.role,
                   mi.display_name,
                   fo.original_filename AS filename,
                   fo.media_type,
                   fo.size_bytes,
                   fo.content_sha256 AS sha256
            FROM manifest_items mi
            INNER JOIN file_objects fo ON fo.id = mi.file_object_id
            WHERE mi.manifest_id = #{manifestId}
            ORDER BY mi.item_position, mi.id
            """)
    List<ManifestItemProjection> findItemsByManifestId(@Param("manifestId") long manifestId);
}
