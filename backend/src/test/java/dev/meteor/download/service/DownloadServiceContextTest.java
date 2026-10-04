package dev.meteor.download.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.meteor.download.mapper.DownloadMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig
@ContextConfiguration(classes = DownloadService.class)
class DownloadServiceContextTest {

    @MockitoBean
    private DownloadMapper downloadMapper;

    @Autowired
    private DownloadService downloadService;

    @Test
    void createsServiceWithItsProductionConstructor() {
        assertThat(downloadService).isNotNull();
        assertThat(downloadMapper).isNotNull();
    }
}
