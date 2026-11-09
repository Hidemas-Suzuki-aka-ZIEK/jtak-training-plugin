package jp.jtak.core.export;

import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import org.junit.Test;

/**
 * 詳細設計 05 の 2.4（6・7）。
 * <p>
 * Detailed design 05 §2.4 (items 6 and 7).
 */
public class ExportFileNameTest {
    private static final LocalDateTime TIME = LocalDateTime.of(2026, 11, 9, 15, 30, 12);

    @Test public void allKindsAndExtensionsUseSpecifiedNames() {
        String[] slugs = {"cas-brief", "routing", "check-in", "game-plan"};
        ExportKind[] kinds = ExportKind.values();
        assertEquals(4, kinds.length);
        for (int i = 0; i < kinds.length; i++) {
            assertEquals(slugs[i], kinds[i].slug());
            for (String extension : Arrays.asList("txt", "pdf")) {
                assertEquals("jtak-" + slugs[i] + "-20261109-153012." + extension,
                        ExportFileName.of(kinds[i], TIME, extension));
            }
        }
        assertEquals("jtak-routing-20260102-030405.txt", ExportFileName.of(ExportKind.ROUTING,
                LocalDateTime.of(2026, 1, 2, 3, 4, 5), "txt"));
    }

    @Test public void invalidArgumentsAreRejected() {
        for (String extension : Arrays.asList(null, "", "TXT", ".txt", "csv", "../pdf")) {
            assertThrows(IllegalArgumentException.class,
                    () -> ExportFileName.of(ExportKind.CAS_BRIEF, TIME, extension));
        }
        assertThrows(IllegalArgumentException.class, () -> ExportFileName.of(null, TIME, "txt"));
        assertThrows(IllegalArgumentException.class,
                () -> ExportFileName.of(ExportKind.ROUTING, null, "pdf"));
    }
}
