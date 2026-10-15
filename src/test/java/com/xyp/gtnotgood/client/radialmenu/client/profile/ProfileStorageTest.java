package com.xyp.gtnotgood.client.radialmenu.client.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.xyp.gtnotgood.client.radialmenu.core.model.Profile;

/**
 * Profile files on disk.
 *
 * <p>
 * Worth testing directly: the storage layer takes a plain directory, so none of this needs Minecraft running, and the
 * case-sensitivity trap it guards against only shows up on a real filesystem.
 */
public class ProfileStorageTest {

    @Rule
    public final TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        ProfileStorage.init(folder.getRoot());
    }

    private static void save(String name) {
        ProfileStorage.saveProfile(Profile.empty(name));
    }

    @Test
    public void savesAndListsProfiles() {
        save("alpha");
        save("beta");

        List<String> names = ProfileStorage.listProfileNames();
        assertEquals(2, names.size());
        assertEquals("alpha", names.get(0));
        assertEquals("beta", names.get(1));
    }

    @Test
    public void namesThatCannotBeFileNamesAreCleanedUp() {
        assertEquals("a_b", ProfileStorage.sanitize("a/b"));
        assertEquals("a_b", ProfileStorage.sanitize("a:b"));
        assertEquals("default", ProfileStorage.sanitize("   "));
        assertEquals("default", ProfileStorage.sanitize(null));
    }

    @Test
    public void namesDifferingOnlyInCaseAreTheSameFile() {
        // The whole point: on Windows they are, and the rename path has to know that.
        assertTrue(ProfileStorage.isSameFile("default", "Default"));
        assertTrue(ProfileStorage.isSameFile("Default", "DEFAULT"));
        assertFalse(ProfileStorage.isSameFile("default", "other"));
    }

    @Test
    public void renameChangesTheFileOnDisk() {
        save("alpha");

        assertTrue(ProfileStorage.renameFile("alpha", "beta"));

        assertFalse(ProfileStorage.exists("alpha"));
        assertTrue(ProfileStorage.exists("beta"));
    }

    @Test
    public void renameCanChangeOnlyTheCapitalisation() {
        // Regression: renaming default to Default reported that the profile already existed, because on a
        // case-insensitive filesystem the target file is the source file.
        save("default");

        assertTrue(ProfileStorage.renameFile("default", "Default"));

        List<String> names = ProfileStorage.listProfileNames();
        assertEquals(1, names.size());
        assertEquals("Default", names.get(0));
    }

    @Test
    public void renameLeavesNoTemporaryFileBehind() {
        save("default");
        ProfileStorage.renameFile("default", "Default");

        for (File file : ProfileStorage.profilesDir().listFiles()) {
            assertFalse(file.getName(), file.getName().endsWith(".tmp"));
        }
    }

    @Test
    public void renamingSomethingThatIsNotThereFails() {
        assertFalse(ProfileStorage.renameFile("missing", "other"));
    }

    @Test
    public void aProfileRoundTripsThroughDisk() {
        Profile written = Profile.empty("alpha");
        written.root.layout.slots = 12;
        ProfileStorage.saveProfile(written);

        Profile read = ProfileStorage.loadProfile("alpha");
        assertEquals("alpha", read.name);
        assertEquals(12, read.root.slotCount());
    }

    @Test
    public void loadingSomethingThatIsNotThereReturnsNothing() {
        assertEquals(null, ProfileStorage.loadProfile("missing"));
    }

    @Test
    public void eachSaveKeepsTheVersionItWroteOver() {
        saveTitled("alpha", "first");
        saveTitled("alpha", "second");
        saveTitled("alpha", "third");

        // The live file is the newest, and the backups count backwards from it.
        assertEquals("third", ProfileStorage.loadProfile("alpha").root.title);
        assertEquals("second", backupTitle("alpha", 1));
        assertEquals("first", backupTitle("alpha", 2));
    }

    @Test
    public void onlyThreeVersionsAreKept() {
        for (int i = 1; i <= 6; i++) {
            saveTitled("alpha", "v" + i);
        }

        assertEquals("v5", backupTitle("alpha", 1));
        assertEquals("v4", backupTitle("alpha", 2));
        assertEquals("v3", backupTitle("alpha", 3));
        assertFalse(backup("alpha", 4).isFile());
    }

    @Test
    public void savingTheSameThingAgainDoesNotSpendTheHistory() {
        saveTitled("alpha", "first");
        saveTitled("alpha", "second");
        // An editor saves whenever it closes; three of those must not push the one real previous version out.
        saveTitled("alpha", "second");
        saveTitled("alpha", "second");
        saveTitled("alpha", "second");

        assertEquals("first", backupTitle("alpha", 1));
        assertFalse(backup("alpha", 2).isFile());
    }

    @Test
    public void thereIsNothingToKeepUntilSomethingIsOverwritten() {
        saveTitled("alpha", "first");

        assertFalse(backup("alpha", 1).isFile());
    }

    @Test
    public void theHistoryFollowsARename() {
        saveTitled("alpha", "first");
        saveTitled("alpha", "second");

        assertTrue(ProfileStorage.renameFile("alpha", "omega"));

        assertEquals("first", backupTitle("omega", 1));
        assertFalse(backup("alpha", 1).isFile());
    }

    @Test
    public void aDeletedProfileLeavesItsHistoryBehind() {
        // The case the backups exist for: deleted by mistake is exactly when they must still be there.
        saveTitled("alpha", "first");
        saveTitled("alpha", "second");

        assertTrue(ProfileStorage.deleteProfile("alpha"));

        assertEquals("first", backupTitle("alpha", 1));
    }

    private static void saveTitled(String name, String title) {
        Profile profile = Profile.empty(name);
        profile.root.title = title;
        ProfileStorage.saveProfile(profile);
    }

    private static File backup(String name, int age) {
        return new File(ProfileStorage.backupsDir(), name + "." + age + ".json");
    }

    /** Reads a backup the same way the mod reads a profile, so the test checks a file that still loads. */
    private String backupTitle(String name, int age) {
        File file = backup(name, age);
        assertTrue("no backup " + file.getName(), file.isFile());
        try {
            java.io.Reader reader = new java.io.InputStreamReader(new java.io.FileInputStream(file),
                java.nio.charset.Charset.forName("UTF-8"));
            try {
                return com.xyp.gtnotgood.client.radialmenu.core.json.ConfigCodec.readProfile(reader).root.title;
            } finally {
                reader.close();
            }
        } catch (Exception failure) {
            throw new AssertionError(failure);
        }
    }
}
