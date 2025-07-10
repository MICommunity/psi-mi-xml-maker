package uk.ac.ebi.intact.psi.mi.xmlmaker.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VersionUtilsTest {

    @Test
    public void olderMajorVersion() {
        assertTrue(VersionUtils.isCurrentVersionOlderThanLatestRelease("1", "2"));
    }

    @Test
    public void sameMajorVersion() {
        assertFalse(VersionUtils.isCurrentVersionOlderThanLatestRelease("1", "1"));
    }

    @Test
    public void newerMajorVersion() {
        assertFalse(VersionUtils.isCurrentVersionOlderThanLatestRelease("2", "1"));
    }

    @Test
    public void newerMajorSnapshotVersion() {
        assertFalse(VersionUtils.isCurrentVersionOlderThanLatestRelease("2-SNAPSHOT", "1"));
    }

    @Test
    public void olderMinorVersion() {
        assertTrue(VersionUtils.isCurrentVersionOlderThanLatestRelease("1.1", "1.2"));
    }

    @Test
    public void sameMinorVersion() {
        assertFalse(VersionUtils.isCurrentVersionOlderThanLatestRelease("1.1", "1.1"));
    }

    @Test
    public void newerMinorVersion() {
        assertFalse(VersionUtils.isCurrentVersionOlderThanLatestRelease("1.2", "1.1"));
    }

    @Test
    public void newerMinorSnapshotVersion() {
        assertFalse(VersionUtils.isCurrentVersionOlderThanLatestRelease("1.2-SNAPSHOT", "1.1"));
    }

    @Test
    public void olderPatchVersion() {
        assertTrue(VersionUtils.isCurrentVersionOlderThanLatestRelease("1.1.1", "1.1.2"));
    }

    @Test
    public void samePatchVersion() {
        assertFalse(VersionUtils.isCurrentVersionOlderThanLatestRelease("1.1.1", "1.1.1"));
    }

    @Test
    public void newerPatchVersion() {
        assertFalse(VersionUtils.isCurrentVersionOlderThanLatestRelease("1.1.2", "1.1.1"));
    }

    @Test
    public void newerPatchSnapshotVersion() {
        assertFalse(VersionUtils.isCurrentVersionOlderThanLatestRelease("1.1.2-SNAPSHOT", "1.1.1"));
    }

    @Test
    public void newerVersionWithPatch() {
        assertFalse(VersionUtils.isCurrentVersionOlderThanLatestRelease("1.1.1", "1.1"));
    }

    @Test
    public void olderVersionWithPatch() {
        assertTrue(VersionUtils.isCurrentVersionOlderThanLatestRelease("1.1.1", "1.2"));
    }

    @Test
    public void sameVersionCurrentSnapshotReleasedNotSnapshot() {
        assertTrue(VersionUtils.isCurrentVersionOlderThanLatestRelease("1.1.2-SNAPSHOT", "1.1.2"));
    }

    @Test
    public void sameVersionReleasedSnapshotCurrentNotSnapshot() {
        assertFalse(VersionUtils.isCurrentVersionOlderThanLatestRelease("1.1.2", "1.1.2-SNAPSHOT"));
    }
}

