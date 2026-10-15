package dev.meteor.download.service;

import dev.meteor.download.dto.DownloadAuthorizationResponse;
import dev.meteor.download.dto.DownloadFilePayload;
import dev.meteor.download.mapper.DownloadMapper;
import dev.meteor.download.mapper.param.DownloadAuditInsert;
import dev.meteor.download.mapper.param.DownloadTicketInsert;
import dev.meteor.download.mapper.projection.DownloadableFileProjection;
import dev.meteor.download.mapper.projection.DownloadTicketProjection;
import dev.meteor.download.model.DownloadAuditOutcome;
import dev.meteor.download.model.DownloadTicketStatus;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 签发短时一次性票据；消费时重新授权，并在同一事务内消费票据、记录响应准备审计。 */
@Service
public class DownloadService {

    private static final Duration TICKET_LIFETIME = Duration.ofMinutes(5);
    private static final int TOKEN_BYTES = 32;
    private static final int MAX_TOKEN_LENGTH = 200;
    private static final String CLASSPATH_PROVIDER = "CLASSPATH";

    private final DownloadMapper downloadMapper;
    private final Clock clock;
    private final SecureRandom secureRandom;

    @Autowired
    public DownloadService(DownloadMapper downloadMapper) {
        this(downloadMapper, Clock.systemUTC(), new SecureRandom());
    }

    DownloadService(DownloadMapper downloadMapper, Clock clock, SecureRandom secureRandom) {
        this.downloadMapper = downloadMapper;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    @Transactional
    public DownloadAuthorizationResponse authorize(
            long userId,
            long grantId,
            long releaseId,
            long manifestItemId
    ) {
        DownloadableFileProjection file = findAccessibleFile(userId, grantId, releaseId, manifestItemId);
        String token = generateToken();
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(TICKET_LIFETIME);
        // 原始随机 token 只交给客户端，数据库仅存 SHA-256；数据库哈希不能直接当下载凭证。
        DownloadTicketInsert ticket = new DownloadTicketInsert(
                sha256(token.getBytes(StandardCharsets.UTF_8)),
                userId,
                file.grantId(),
                file.releaseId(),
                file.manifestItemId(),
                file.fileObjectId(),
                DownloadTicketStatus.ISSUED,
                issuedAt,
                expiresAt
        );
        if (downloadMapper.insertTicket(ticket) != 1) {
            throw new DownloadStorageException(
                    new IllegalStateException("Download ticket was not persisted")
            );
        }
        return new DownloadAuthorizationResponse(
                ticket.getId(),
                "/api/downloads/" + token,
                expiresAt,
                file.originalFilename()
        );
    }

    @Transactional
    public DownloadFilePayload consume(long userId, String token) {
        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH) {
            throw new DownloadNotFoundException();
        }

        // 按 token 哈希和当前用户锁住票据，同一票据的并发消费在数据库处排队。
        DownloadTicketProjection ticket = downloadMapper
                .findTicketForUpdate(sha256(token.getBytes(StandardCharsets.UTF_8)), userId)
                .orElseThrow(DownloadNotFoundException::new);
        // 等待行锁期间可能到期，必须取得锁后再取时间；恰好到期也不能消费。
        Instant now = clock.instant();
        if (ticket.status() != DownloadTicketStatus.ISSUED
                || ticket.consumedAt() != null
                || !ticket.expiresAt().isAfter(now)) {
            throw new DownloadNotFoundException();
        }

        // 签发后的撤权、版本禁用或文件关联变化必须立即生效，不能仅信任票据里的 ID。
        DownloadableFileProjection file = findAccessibleFile(
                userId,
                ticket.grantId(),
                ticket.releaseId(),
                ticket.manifestItemId()
        );
        if (file.fileObjectId() != ticket.fileObjectId()) {
            throw new DownloadNotFoundException();
        }

        // 先确认文件可读且摘要正确，再消费；文件错误不会烧掉票据。
        Resource resource = verifyClasspathResource(file);
        if (downloadMapper.markTicketConsumed(ticket.id(), userId, now) != 1) {
            throw new DownloadNotFoundException();
        }
        // 审计失败也回滚票据消费。PREPARED 仅代表服务端准备响应，不证明浏览器已保存。
        int insertedAudits = downloadMapper.insertAudit(new DownloadAuditInsert(
                ticket.id(),
                userId,
                file.grantId(),
                file.releaseId(),
                file.manifestItemId(),
                file.fileObjectId(),
                DownloadAuditOutcome.FILE_RESPONSE_PREPARED,
                file.sizeBytes(),
                file.contentSha256()
        ));
        if (insertedAudits != 1) {
            throw new DownloadStorageException(
                    new IllegalStateException("Download audit was not persisted")
            );
        }
        return new DownloadFilePayload(
                file.originalFilename(),
                file.mediaType(),
                file.contentSha256(),
                file.sizeBytes(),
                resource
        );
    }

    private DownloadableFileProjection findAccessibleFile(
            long userId,
            long grantId,
            long releaseId,
            long manifestItemId
    ) {
        return downloadMapper.findAccessibleFile(userId, grantId, releaseId, manifestItemId)
                .orElseThrow(DownloadNotFoundException::new);
    }

    private Resource verifyClasspathResource(DownloadableFileProjection file) {
        if (!CLASSPATH_PROVIDER.equals(file.storageProvider())) {
            throw new DownloadStorageException(
                    new IllegalStateException("Unsupported storage provider")
            );
        }
        ClassPathResource resource = new ClassPathResource(file.storageKey());
        try (InputStream source = resource.getInputStream()) {
            MessageDigest digest = sha256Digest();
            long sizeBytes;
            // 分块计算摘要和字节数，避免整文件进堆；Controller 随后会再次读取 Resource 写响应。
            try (DigestInputStream input = new DigestInputStream(source, digest)) {
                sizeBytes = input.transferTo(OutputStream.nullOutputStream());
            }
            String contentSha256 = HexFormat.of().formatHex(digest.digest());
            if (sizeBytes != file.sizeBytes()
                    || !contentSha256.equalsIgnoreCase(file.contentSha256())) {
                throw new DownloadIntegrityException();
            }
            return resource;
        } catch (DownloadIntegrityException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new DownloadStorageException(exception);
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(byte[] value) {
        return HexFormat.of().formatHex(sha256Digest().digest(value));
    }

    private static MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
