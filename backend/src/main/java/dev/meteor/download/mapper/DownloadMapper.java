package dev.meteor.download.mapper;

import dev.meteor.download.mapper.param.DownloadAuditInsert;
import dev.meteor.download.mapper.param.DownloadTicketInsert;
import dev.meteor.download.mapper.projection.DownloadableFileProjection;
import dev.meteor.download.mapper.projection.DownloadTicketProjection;
import java.time.Instant;
import java.util.Optional;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface DownloadMapper {

    @Select("""
            SELECT eg.id AS grant_id,
                   r.id AS release_id,
                   dm.id AS manifest_id,
                   mi.id AS manifest_item_id,
                   fo.id AS file_object_id,
                   fo.storage_provider,
                   fo.storage_key,
                   fo.original_filename,
                   fo.media_type,
                   fo.size_bytes,
                   fo.content_sha256
            FROM entitlement_grants eg
            JOIN releases r ON r.package_id = eg.package_id
            JOIN delivery_manifests dm ON dm.release_id = r.id
            JOIN manifest_items mi ON mi.manifest_id = dm.id
            JOIN file_objects fo ON fo.id = mi.file_object_id
            WHERE eg.id = #{grantId}
              AND eg.user_id = #{userId}
              AND eg.status = 'ACTIVE'
              AND r.id = #{releaseId}
              AND r.status = 'PUBLISHED'
              AND mi.id = #{manifestItemId}
            """)
    Optional<DownloadableFileProjection> findAccessibleFile(
            @Param("userId") long userId,
            @Param("grantId") long grantId,
            @Param("releaseId") long releaseId,
            @Param("manifestItemId") long manifestItemId
    );

    @Insert("""
            INSERT INTO download_tickets (
                token_hash,
                user_id,
                entitlement_grant_id,
                release_id,
                manifest_item_id,
                file_object_id,
                status,
                created_at,
                expires_at
            ) VALUES (
                #{tokenHash},
                #{userId},
                #{grantId},
                #{releaseId},
                #{manifestItemId},
                #{fileObjectId},
                #{status},
                #{createdAt},
                #{expiresAt}
            )
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insertTicket(DownloadTicketInsert ticket);

    @Select("""
            SELECT id,
                   user_id,
                   entitlement_grant_id AS grant_id,
                   release_id,
                   manifest_item_id,
                   file_object_id,
                   status,
                   expires_at,
                   consumed_at
            FROM download_tickets
            WHERE token_hash = #{tokenHash}
              AND user_id = #{userId}
            FOR UPDATE
            """)
    Optional<DownloadTicketProjection> findTicketForUpdate(
            @Param("tokenHash") String tokenHash,
            @Param("userId") long userId
    );

    @Update("""
            UPDATE download_tickets
            SET status = 'CONSUMED',
                consumed_at = #{consumedAt}
            WHERE id = #{ticketId}
              AND user_id = #{userId}
              AND status = 'ISSUED'
            """)
    int markTicketConsumed(
            @Param("ticketId") long ticketId,
            @Param("userId") long userId,
            @Param("consumedAt") Instant consumedAt
    );

    @Insert("""
            INSERT INTO download_audits (
                download_ticket_id,
                user_id,
                entitlement_grant_id,
                release_id,
                manifest_item_id,
                file_object_id,
                outcome,
                response_bytes,
                content_sha256
            ) VALUES (
                #{ticketId},
                #{userId},
                #{grantId},
                #{releaseId},
                #{manifestItemId},
                #{fileObjectId},
                #{outcome},
                #{responseBytes},
                #{contentSha256}
            )
            """)
    int insertAudit(DownloadAuditInsert audit);
}
