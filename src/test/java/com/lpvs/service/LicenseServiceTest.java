/**
 * Copyright (c) 2022, Samsung Electronics Co., Ltd. All rights reserved.
 *
 * Use of this source code is governed by a MIT license that can be
 * found in the LICENSE file.
 */
 
package com.lpvs.service;

import com.lpvs.entity.LPVSFile;
import com.lpvs.entity.LPVSLicense;
import com.lpvs.entity.config.WebhookConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LicenseServiceTest {
    @Test
    public void testFindConflicts() {
        WebhookConfig webhookConfig = new WebhookConfig();
        webhookConfig.setRepositoryName("LPVS");
        webhookConfig.setRepositoryOrganization("Samsung");
        webhookConfig.setRepositoryLicense("Apache-2.0");
        webhookConfig.setPullRequestAPIUrl("http://url.com");
        LPVSFile file = new LPVSFile();
        LPVSLicense license = new LPVSLicense(){{
            setChecklist_url("");
            setAccess("unrviewed");
            setSpdxId("LPGL-2.1-or-later");
        }};
        file.setLicenses(new HashSet<LPVSLicense>(){{
            add(license);
        }});
        file.setFilePath("");
        file.setComponent("");
        file.setSnippetMatch("");
        file.setMatchedLines("");

        LPVSFile file1 = new LPVSFile();
        LPVSLicense license1 = new LPVSLicense(){{
            setChecklist_url("");
            setAccess("unrviewed");
            setSpdxId("Apache-2.0");
        }};
        file1.setLicenses(new HashSet<LPVSLicense>(){{
            add(license1);
        }});
        file1.setFilePath("");
        file1.setComponent("");
        file1.setSnippetMatch("");
        file1.setMatchedLines("");

        LPVSFile file2 = new LPVSFile();
        LPVSLicense license2 = new LPVSLicense(){{
            setChecklist_url("");
            setAccess("unrviewed");
            setSpdxId("MIT");
        }};
        file2.setLicenses(new HashSet<LPVSLicense>(){{
            add(license2);
        }});
        file2.setFilePath("");
        file2.setComponent("");
        file2.setSnippetMatch("");
        file2.setMatchedLines("");

        List<LPVSFile> fileList = new ArrayList<LPVSFile>(){{
            add(file);
            add(file1);
            add(file2);
        }};
        LicenseService licenseService = new LicenseService("", "");
        ReflectionTestUtils.setField(licenseService, "licenseConflicts",
                new ArrayList<LicenseService.Conflict<String, String>>()
                {{ add(new LicenseService.Conflict<>("", "")); }});
        Assertions.assertNotNull(licenseService.findConflicts(webhookConfig, fileList));
    }
    
    @Nested
    class TestFindLicenseBySPDXFindLicenseByName {
        LicenseService licenseService;

        // `lpvs_license_1`
        LPVSLicense lpvs_license_1;
        final String license_name_1 = "MIT License";
        final String spdx_id_1 = "MIT";

        // `lpvs_license_2`
        LPVSLicense lpvs_license_2;
        final String license_name_2 = "Apache-2.0 License";
        final String spdx_id_2 = "Apache-2.0";

        @BeforeEach
        void setUp() throws NoSuchFieldException, IllegalAccessException {
            licenseService = new LicenseService(null, null);

            lpvs_license_1 = new LPVSLicense(1L, license_name_1, spdx_id_1, null, null, null);
            lpvs_license_2 = new LPVSLicense(2L, license_name_2, spdx_id_2, null, null, null);

            Field conflicts_field = licenseService.getClass().getDeclaredField("licenses");
            conflicts_field.setAccessible(true);
            conflicts_field.set(licenseService, List.of(lpvs_license_1, lpvs_license_2));
        }

        @Test
        public void testFindLicenseBySPDXFindLicenseByName() {
            assertEquals(lpvs_license_1, licenseService.findLicenseBySPDX(spdx_id_1));
            assertEquals(lpvs_license_2, licenseService.findLicenseBySPDX(spdx_id_2));
            assertNull(licenseService.findLicenseBySPDX("Apache-1.1"));

            assertEquals(lpvs_license_1, licenseService.findLicenseByName(license_name_1));
            assertEquals(lpvs_license_2, licenseService.findLicenseByName(license_name_2));
            assertNull(licenseService.findLicenseByName("Apache-1.1 License"));
        }
    }

    @Nested
    class TestAddLicenseConflict {
        LicenseService licenseService;
        LicenseService.Conflict<String, String> conflict_1 = new LicenseService.Conflict<>("MIT", "Apache-1.1");
        LicenseService.Conflict<String, String> conflict_2 = new LicenseService.Conflict<>("MIT", "Apache-2.0");

        @BeforeEach
        void setUp() throws NoSuchFieldException, IllegalAccessException {
            licenseService = new LicenseService(null, null);
            Field conflicts_field = licenseService.getClass().getDeclaredField("licenseConflicts");
            conflicts_field.setAccessible(true);
            conflicts_field.set(licenseService, new ArrayList<>());
        }

        @Test
        public void testAddLicenseConflict() throws NoSuchFieldException, IllegalAccessException {
            licenseService.addLicenseConflict("MIT", "Apache-1.1");
            licenseService.addLicenseConflict("Apache-1.1", "MIT");
            licenseService.addLicenseConflict("MIT", "Apache-2.0");

            Field conflicts_field = licenseService.getClass().getDeclaredField("licenseConflicts");
            conflicts_field.setAccessible(true);
            List<LicenseService.Conflict<String, String>> conflicts_list
                    = (List<LicenseService.Conflict<String, String>>) conflicts_field.get(licenseService);

            assertEquals(List.of(conflict_1, conflict_2), conflicts_list);
        }
    }


    @Nested
    class TestCheckLicense {
        LicenseService licenseService;

        // `lpvs_license_1`
        LPVSLicense lpvs_license_1;
        final String spdx_id_1 = "MIT";
        // `lpvs_license_2`
        LPVSLicense lpvs_license_2;
        final String spdx_id_2 = "Apache-1.1-or-later";
        // `lpvs_license_3`
        LPVSLicense lpvs_license_3;
        final String spdx_id_3 = "Apache-2.0-only";

        @BeforeEach
        void setUp() throws NoSuchFieldException, IllegalAccessException {
            licenseService = new LicenseService(null, null);

            lpvs_license_1 = new LPVSLicense(1L, null, spdx_id_1, null, null, null);
            lpvs_license_2 = new LPVSLicense(1L, null, spdx_id_2, null, null, null);
            lpvs_license_3 = new LPVSLicense(1L, null, spdx_id_3, null, null, null);

            Field conflicts_field = licenseService.getClass().getDeclaredField("licenses");
            conflicts_field.setAccessible(true);
            conflicts_field.set(licenseService, List.of(lpvs_license_1, lpvs_license_2, lpvs_license_3));
        }

        @Test
        public void testCheckLicense() {
            assertEquals(lpvs_license_1, licenseService.checkLicense("MIT"));
            assertEquals(lpvs_license_2, licenseService.checkLicense("Apache-1.1+"));
            assertEquals(lpvs_license_3, licenseService.checkLicense("Apache-2.0+"));
            assertNull(licenseService.checkLicense("non-MIT"));
        }
    }

    @Nested
    class TestFindConflicts_fullExecution {
        LicenseService licenseService;
        LicenseService.Conflict<String, String> conflict_1 = new LicenseService.Conflict<>("MIT", "Apache-2.0");

        // `lpvs_file_1`
        LPVSFile lpvs_file_1;
        LPVSLicense lpvs_license_1;
        final String spdx_id_1 = "MIT";

        // `lpvs_file_2`
        LPVSFile lpvs_file_2;
        LPVSLicense lpvs_license_2;
        final String spdx_id_2 = "Apache-2.0";

        WebhookConfig webhookConfig;

        @BeforeEach
        void setUp() throws NoSuchFieldException, IllegalAccessException {
            licenseService = new LicenseService(null, null);
            Field conflicts_field = licenseService.getClass().getDeclaredField("licenseConflicts");
            conflicts_field.setAccessible(true);
            conflicts_field.set(licenseService, List.of(conflict_1));

            lpvs_license_1 = new LPVSLicense(1L, null, spdx_id_1, null, null, null);
            lpvs_file_1 = new LPVSFile(1L, null, null, null, null, Set.of(lpvs_license_1), null);

            lpvs_license_2 = new LPVSLicense(2L, null, spdx_id_2, null, null, null);
            lpvs_file_2 = new LPVSFile(2L, null, null, null, null, Set.of(lpvs_license_2), null);

            webhookConfig = new WebhookConfig();
            webhookConfig.setRepositoryLicense("MIT");
        }

        @Test
        public void testFindConflicts_fullExecution() {
            List<LicenseService.Conflict<String, String>> expected = List.of(conflict_1, conflict_1);
            List<LicenseService.Conflict<String, String>> actual = licenseService.findConflicts(webhookConfig, List.of(lpvs_file_1, lpvs_file_2));

            assertEquals(expected, actual);
        }
    }
}
