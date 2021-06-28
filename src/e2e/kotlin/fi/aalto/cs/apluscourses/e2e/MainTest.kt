package fi.aalto.cs.apluscourses.e2e

import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.attempt
import com.intellij.remoterobot.utils.keyboard
import com.intellij.remoterobot.utils.waitFor
import fi.aalto.cs.apluscourses.e2e.fixtures.dialog
import fi.aalto.cs.apluscourses.e2e.fixtures.ideFrame
import fi.aalto.cs.apluscourses.e2e.steps.CommonSteps
import fi.aalto.cs.apluscourses.e2e.utils.StepLoggerInitializer
import fi.aalto.cs.apluscourses.e2e.utils.containsText
import fi.aalto.cs.apluscourses.e2e.utils.getVersion
import fi.aalto.cs.apluscourses.e2e.utils.uiTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class MainTest {
    init {
        StepLoggerInitializer.init()
    }

    @Test
    fun mainTest() = uiTest {
        // step 2
        CommonSteps(this).createProject()
        // step 4
        ideFrame().waitForSmartMode()
        step("Cancel") {
            attempt(5) {
                CommonSteps(this).openAPlusProjectWindow()
                with(dialog("Select Course")) {
                    button("Cancel").click()
                }
            }
        }
        step("Select course") {
            attempt(5) {
                CommonSteps(this).openAPlusProjectWindow()
                with(dialog("Select Course")) {
                    findText("O1").click()
                    button("OK").click()
                }
            }
        }
        step("Choose settings") {
            CommonSteps(this).aPlusSettings(true)
        }
        step("Skip the potential \"Code with me\" popup") {
            with(ideFrame()) {
                codeWithMeButton()?.click()
            }
        }
        step("Assertions") {
            with(ideFrame()) {
                with(projectViewTree()) {
                    waitFor(
                        Duration.ofSeconds(60),
                        Duration.ofSeconds(1),
                        "O1Library not found in project view tree"
                    ) { hasText("O1Library") }
                }
                with(modules()) {
                    waitFor(
                        Duration.ofSeconds(60),
                        Duration.ofSeconds(1),
                        "No modules installed in modules list"
                    ) { containsText("[Installed]") }
                    assertTrue(
                        "O1Library is in the modules tree",
                        hasText("O1Library")
                    )
                }
            }
        }
        // step 8
        step("Authenticate with wrong token") {
            CommonSteps(this).setAPlusToken("")
            with(dialog("A+ Token")) {
                assertTrue("Token dialog still showing after clicking OK with empty token", isShowing)
                passwordField().text = "token"
                button("OK").click()
                assertTrue("Token dialog still showing after clicking OK with wrong token", isShowing)
                button("Cancel").click()
            }
        }
        step("Authenticate with right token") {
            CommonSteps(this).setAPlusToken(System.getenv("APLUS_TEST_TOKEN"))
            with(ideFrame()) {
                with(assignments()) {
                    waitFor(
                        Duration.ofSeconds(60),
                        Duration.ofSeconds(1),
                        "Week 1 wasn't found in the assignments tree"
                    ) { hasText("Week 1") }
                }
            }
        }
        step("Searching modules") {
            with(ideFrame()) {
                with(modules()) {
                    waitFor(
                        Duration.ofSeconds(60),
                        Duration.ofSeconds(1),
                        "Viinaharava not found in modules list"
                    ) { hasText("Viinaharava") }
                    click()
                    keyboard {
                        enterText("viina")
                    }
                    assertEquals("Only one item is selected", selectedItems.size, 1)
                    assertTrue("Selected module is correct", selectedItems[0].contains("Viinaharava", true))
                }
            }
        }
        step("Searching assignments") {
            with(ideFrame()) {
                with(assignments()) {
                    waitFor(
                        Duration.ofSeconds(60),
                        Duration.ofSeconds(1),
                        "Week 1 not found in assignments list"
                    ) { hasText("Week 1") }

                    // searching for assignments uses "containsText" rather than "hasText" because of
                    // platform-dependant quirks such as splitting the assignment number and name into two strings
                    assertFalse("'Files' assignment should be collapsed", containsText("Files"))
                    click()
                    keyboard {
                        enterText("files")
                        escape()
                    }
                    assertTrue("'Files' assignment should be visible", containsText("Files"))
                    keyboard {
                        escape()
                    }
                }
            }
        }
        // Week 11 is now expanded in the tree view because of the previous test
        step("Filtering assignments") {
            with(ideFrame()) {
                // just check the initial state (nothing is filtered out)
                filterButton().click()
                with(dropDownMenu()) {
                    assertTrue("By default, nothing is filtered out", hasText("Deselect all"))
                }
                keyboard {
                    escape() // close the menu without selecting anything
                }

                with(assignments()) {
                    assertTrue("'Files' assignment should be visible", containsText("Files"))
                }

                // filter out optional tasks
                filterButton().click()
                dropDownMenu().selectItemContains("Optional")
                filterButton().click()
                with(dropDownMenu()) {
                    assertTrue("Something should be filtered out", hasText("Select all"))
                }
                keyboard {
                    escape()
                }

                with(assignments()) {
                    assertFalse("'Files' assignment should now be hidden", containsText("Files"))
                    assertTrue("Feedback submissions should be visible", containsText("Feedback"))
                }

                // filter out non-submittable tasks
                filterButton().click()
                dropDownMenu().selectItemContains("Non-submittable")

                with(assignments()) {
                    assertFalse("Feedback submissions should now be hidden", hasText("Feedback"))
                    assertTrue("Closed Week 1 should be visible", hasText("Week 1"))
                }

                // filter out closed sections
                filterButton().click()
                dropDownMenu().selectItemContains("Closed")

                with(assignments()) {
                    assertFalse("Closed Week 1 should be visible", hasText("Week 1"))
                }

                // disable filtering altogether
                filterButton().click()
                dropDownMenu().selectItemContains("Select all")
                filterButton().click()
                with(dropDownMenu()) {
                    assertTrue("Nothing is filtered out anymore", hasText("Deselect all"))
                }
                keyboard {
                    escape()
                }

                assignments().click()
                keyboard {
                    enterText("files")
                    escape()
                }

                // check that various assignments are visible
                with(assignments()) {
                    assertTrue("'Files' assignment should be visible", containsText("Files"))
                    assertTrue("Feedback submissions should be visible", containsText("Feedback"))
                    assertTrue("Closed Week 1 should be visible", hasText("Week 1"))
                }
            }
        }
        step("User dropdown") {
            with(ideFrame()) {
                userButton().click()
                with(dropDownMenu()) {
                    assertTrue(
                        "The user dropdown contains the user's name",
                        containsText("TESTI-Opiskelija")
                    )
                    selectItemContains("Log out")
                }
                assertTrue(
                    "The assignments tree gets cleared after logging out",
                    containsText("Set your A+ token")
                )
                userButton().click()
                with(dropDownMenu()) {
                    assertTrue(
                        "The user dropdown shows that the user isn't logged in",
                        containsText("Not logged in")
                    )
                    selectItemContains("Log in")
                }
                dialog("A+ Token").button("Cancel").click()
            }
        }
    }

    @Test
    fun aboutDialogTest() = uiTest {
        step("About dialog") {
            CommonSteps(this).openAboutDialog()
            with(dialog("A+ Courses", Duration.ofSeconds(20))) {
                step("Check the version") {
                    assertTrue("Version is correct", hasText("Version: ${getVersion()}"))
                }
                step("Check the links") {
                    assertTrue(hasText("A+ Courses plugin website"))
                    assertTrue(hasText("A+ Courses plugin GitHub"))
                    assertTrue(hasText("A+ website"))
                    assertTrue(hasText("Apache Commons IO"))
                    assertTrue(hasText("IntelliJ Scala Plugin"))
                    assertTrue(hasText("json.org"))
                    assertTrue(hasText("Scala Standard Library 2.13.4"))
                    assertTrue(hasText("zip4j"))
                }
                button("OK").click()
            }
        }
    }
}
