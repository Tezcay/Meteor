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
import java.nio.charset.StandardCharsets;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        Instant now = clock.instant();
        DownloadTicketProjection ticket = downloadMapper
                .findTicketForUpdate(sha256(token.getBytes(StandardCharsets.UTF_8)), userId)
                .orElseThrow(DownloadNotFoundException::new);
        if (ticket.status() != DownloadTicketStatus.ISSUED
                || ticket.consumedAt() != null
                || !ticket.expiresAt().isAfter(now)) {
            throw new DownloadNotFoundException();
        }

        DownloadableFileProjection file = findAccessibleFile(
                userId,
                ticket.grantId(),
                ticket.releaseId(),
                ticket.manifestItemId()
        );
        if (file.fileObjectId() != ticket.fileObjectId()) {
            throw new DownloadNotFoundException();
        }

        byte[] content = readAndVerify(file);
        if (downloadMapper.markTicketConsumed(ticket.id(), userId, now) != 1) {
            throw new DownloadNotFoundException();
        }
        int insertedAudits = downloadMapper.insertAudit(new DownloadAuditInsert(
                ticket.id(),
                userId,
                file.grantId(),
                file.releaseId(),
                file.manifestItemId(),
                file.fileObjectId(),
                DownloadAuditOutcome.FILE_RESPONSE_PREPARED,
                content.length,
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
                content
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

    private byte[] readAndVerify(DownloadableFileProjection file) {
        if (!CLASSPATH_PROVIDER.equals(file.storageProvider())) {
            throw new DownloadStorageException(
                    new IllegalStateException("Unsupported storage provider")
            );
        }
        try (InputStream input = new ClassPathResource(file.storageKey()).getInputStream()) {
            byte[] content = input.readAllBytes();
            if (content.length != file.sizeBytes()
                    || !sha256(content).equalsIgnoreCase(file.contentSha256())) {
                throw new DownloadIntegrityException();
            }
            return content;
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
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
