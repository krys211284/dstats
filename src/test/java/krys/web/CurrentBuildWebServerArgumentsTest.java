package krys.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentBuildWebServerArgumentsTest {
    @Test
    void bez_hosta_i_adresu_powinien_uzyc_lokalnego_domyslnego_bindu() {
        CurrentBuildWebServer.ServerArguments arguments = CurrentBuildWebServer.parseArguments(new String[0]);

        assertEquals("127.0.0.1", arguments.host());
        assertEquals(8080, arguments.port());
    }

    @Test
    void host_i_address_powinny_ustawiac_ten_sam_bind_host() {
        assertEquals("0.0.0.0", CurrentBuildWebServer.parseArguments(new String[]{"--host", "0.0.0.0"}).host());
        assertEquals("0.0.0.0", CurrentBuildWebServer.parseArguments(new String[]{"--address", "0.0.0.0"}).host());
        assertEquals("192.168.1.51", CurrentBuildWebServer.parseArguments(new String[]{"--host", "192.168.1.51"}).host());
        assertEquals("localhost", CurrentBuildWebServer.parseArguments(new String[]{"--host", "localhost"}).host());
    }

    @Test
    void port_powinien_dzialac_z_hostem_i_addressem_w_dowolnej_kolejnosci() {
        CurrentBuildWebServer.ServerArguments hostFirst = CurrentBuildWebServer.parseArguments(new String[]{
                "--host", "0.0.0.0",
                "--port", "8080"
        });
        CurrentBuildWebServer.ServerArguments portFirst = CurrentBuildWebServer.parseArguments(new String[]{
                "--port", "8080",
                "--host", "0.0.0.0"
        });
        CurrentBuildWebServer.ServerArguments addressFirst = CurrentBuildWebServer.parseArguments(new String[]{
                "--address", "0.0.0.0",
                "--port", "8080"
        });
        CurrentBuildWebServer.ServerArguments addressAfterPort = CurrentBuildWebServer.parseArguments(new String[]{
                "--port", "8080",
                "--address", "0.0.0.0"
        });

        assertEquals("0.0.0.0", hostFirst.host());
        assertEquals(8080, hostFirst.port());
        assertEquals(hostFirst, portFirst);
        assertEquals(hostFirst, addressFirst);
        assertEquals(hostFirst, addressAfterPort);
    }

    @Test
    void konstruktor_powinien_zachowac_domyslny_host_lokalny() throws Exception {
        Path tempDirectory = Files.createTempDirectory("server-args");
        try (CurrentBuildWebServer webServer = new CurrentBuildWebServer(0, tempDirectory)) {
            assertEquals("127.0.0.1", webServer.getBindHost());
        }
    }

    @Test
    void bledne_argumenty_powinny_byc_odrzucane() {
        assertInvalid("--unknown");
        assertInvalid("--host");
        assertInvalid("--address");
        assertInvalid("--port");
        assertInvalid("--port", "abc");
        assertInvalid("--port", "0");
        assertInvalid("--port", "-1");
        assertInvalid("--port", "65536");
        assertInvalid("--host", "");
        assertInvalid("--address", "");
    }

    private static void assertInvalid(String... args) {
        assertThrows(IllegalArgumentException.class, () -> CurrentBuildWebServer.parseArguments(args));
    }
}
