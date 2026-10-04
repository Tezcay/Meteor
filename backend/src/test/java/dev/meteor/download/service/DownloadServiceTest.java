package dev.meteor.download.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import dev.meteor.download.dto.DownloadAuthorizationResponse;
import dev.meteor.download.dto.DownloadFilePayload;
import dev.meteor.download.mapper.DownloadMapper;
import dev.meteor.download.mapper.param.DownloadAuditInsert;
import dev.meteor.download.mapper.param.DownloadTicketInsert;
import dev.meteor.download.mapper.projection.DownloadableFileProjection;
import dev.meteor.download.mapper.projection.DownloadTicketProjection;
import dev.meteor.download.model.DownloadAuditOutcome;
import dev.meteor.download.model.DownloadTicketStatus;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class DownloadServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-03T12:00:00Z");
    private static final String FILE_HASH = "d56ccbeda6e7ca6610a9cb5b12fd7fb9231ec12787707b3577dfa87d56430f21";

    private final StubDownloadMapper mapper = new StubDownloadMapper();
    private final DownloadService service = new DownloadService(
            mapper,
            Clock.fixed(NOW, ZoneOffset.UTC),
            new SecureRandom()
    );

    @Test
    void issuesFiveMinuteTicketAndStoresOnlyTokenHash() {
        mapper.file = Optional.of(file());

        DownloadAuthorizationResponse result = service.authorize(2L, 5001L, 3001L, 6001L);

        assertThat(result.ticketId()).isEqualTo(9001L);
        assertThat(result.downloadUrl()).startsWith("/api/downloads/");
        assertThat(result.filename()).isEqualTo("basic-resume-template.html");
        assertThat(result.expiresAt()).isEqualTo(NOW.plusSeconds(300));
        assertThat(mapper.ticket.getTokenHash()).hasSize(64);
        assertThat(result.downloadUrl()).doesNotContain(mapper.ticket.getTokenHash());
        assertThat(mapper.ticket.getStatus()).isEqualTo(DownloadTicketStatus.ISSUED);
        assertThat(mapper.ticket.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void consumesVerifiedClasspathFileOnceAndRecordsPreparedResponse() {
        mapper.file = Optional.of(file());
        mapper.ticketProjection = Optional.of(ticket(DownloadTicketStatus.ISSUED, NOW.plusSeconds(60), null));

        DownloadFilePayload result = service.consume(2L, "valid-token");

        assertThat(result.filename()).isEqualTo("basic-resume-template.html");
        assertThat(result.mediaType()).isEqualTo("text/html");
        assertThat(result.sha256()).isEqualTo(FILE_HASH);
        assertThat(result.sizeBytes()).isEqualTo(634L);
        assertThat(result.resource()).isInstanceOf(ClassPathResource.class);
        assertThat(result.resource().isReadable()).isTrue();
        assertThat(mapper.consumedTicketId).isEqualTo(9001L);
        assertThat(mapper.consumedAt).isEqualTo(NOW);
        assertThat(mapper.audit.outcome()).isEqualTo(DownloadAuditOutcome.FILE_RESPONSE_PREPARED);
        assertThat(mapper.audit.responseBytes()).isEqualTo(634L);
    }

    @Test
    void rejectsExpiredTicketBeforeReadingFile() {
        mapper.ticketProjection = Optional.of(ticket(DownloadTicketStatus.ISSUED, NOW, null));

        assertThatThrownBy(() -> service.consume(2L, "expired-token"))
                .isInstanceOf(DownloadNotFoundException.class);

        assertThat(mapper.accessibleFileCalls).isZero();
        assertThat(mapper.markConsumedCalls).isZero();
    }

    @Test
    void rejectsAlreadyConsumedTicket() {
        mapper.ticketProjection = Optional.of(ticket(
                DownloadTicketStatus.CONSUMED,
                NOW.plusSeconds(60),
                NOW.minusSeconds(1)
        ));

        assertThatThrownBy(() -> service.consume(2L, "used-token"))
                .isInstanceOf(DownloadNotFoundException.class);

        assertThat(mapper.accessibleFileCalls).isZero();
    }

    @Test
    void revalidatesCurrentGrantAndReleaseWhenTicketIsConsumed() {
        mapper.ticketProjection = Optional.of(ticket(DownloadTicketStatus.ISSUED, NOW.plusSeconds(60), null));

        assertThatThrownBy(() -> service.consume(2L, "revoked-token"))
                .isInstanceOf(DownloadNotFoundException.class);

        assertThat(mapper.accessibleFileCalls).isEqualTo(1);
        assertThat(mapper.markConsumedCalls).isZero();
        assertThat(mapper.audit).isNull();
    }

    @Test
    void rejectsStoredFileWhenTrustedSizeOrHashDoesNotMatch() {
        mapper.file = Optional.of(new DownloadableFileProjection(
                5001L,
                3001L,
                4001L,
                6001L,
                5001L,
                "CLASSPATH",
                "delivery-fixtures/basic/resume-template.html",
                "basic-resume-template.html",
                "text/html",
                635L,
                FILE_HASH
        ));
        mapper.ticketProjection = Optional.of(ticket(DownloadTicketStatus.ISSUED, NOW.plusSeconds(60), null));

        assertThatThrownBy(() -> service.consume(2L, "integrity-token"))
                .isInstanceOf(DownloadIntegrityException.class);

        assertThat(mapper.markConsumedCalls).isZero();
        assertThat(mapper.audit).isNull();
    }

    private static DownloadableFileProjection file() {
        return new DownloadableFileProjection(
                5001L,
                3001L,
                4001L,
                6001L,
                5001L,
                "CLASSPATH",
                "delivery-fixtures/basic/resume-template.html",
                "basic-resume-template.html",
                "text/html",
                634L,
                FILE_HASH
        );
    }

    private static DownloadTicketProjection ticket(
            DownloadTicketStatus status,
            Instant expiresAt,
            Instant consumedAt
    ) {
        return new DownloadTicketProjection(
                9001L,
                2L,
                5001L,
                3001L,
                6001L,
                5001L,
                status,
                expiresAt,
                consumedAt
        );
    }

    private static final class StubDownloadMapper implements DownloadMapper {
        private Optional<DownloadableFileProjection> file = Optional.empty();
        private Optional<DownloadTicketProjection> ticketProjection = Optional.empty();
        private DownloadTicketInsert ticket;
        private DownloadAuditInsert audit;
        private int accessibleFileCalls;
        private int markConsumedCalls;
        private long consumedTicketId;
        private Instant consumedAt;

        @Override
        public Optional<DownloadableFileProjection> findAccessibleFile(
                long userId,
                long grantId,
                long releaseId,
                long manifestItemId
        ) {
            accessibleFileCalls++;
            return file;
        }

        @Override
        public int insertTicket(DownloadTicketInsert ticket) {
            this.ticket = ticket;
            ticket.setId(9001L);
            return 1;
        }

        @Override
        public Optional<DownloadTicketProjection> findTicketForUpdate(String tokenHash, long userId) {
            return ticketProjection;
        }

        @Override
        public int markTicketConsumed(long ticketId, long userId, Instant consumedAt) {
            markConsumedCalls++;
            consumedTicketId = ticketId;
            this.consumedAt = consumedAt;
            return 1;
        }

        @Override
        public int insertAudit(DownloadAuditInsert audit) {
            this.audit = audit;
            return 1;
        }
    }
}
