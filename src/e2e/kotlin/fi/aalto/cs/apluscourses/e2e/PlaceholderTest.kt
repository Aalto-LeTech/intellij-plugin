package fi.aalto.cs.apluscourses.e2e

import com.intellij.remoterobot.stepsProcessing.step
import com.intellij.remoterobot.utils.waitFor
import fi.aalto.cs.apluscourses.e2e.fixtures.dialog
import fi.aalto.cs.apluscourses.e2e.fixtures.ideFrame
import fi.aalto.cs.apluscourses.e2e.steps.CommonSteps
import fi.aalto.cs.apluscourses.e2e.utils.StepLoggerInitializer
import fi.aalto.cs.apluscourses.e2e.utils.uiTest
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.time.Duration
import java.util.Properties

class PlaceholderTest {
  init {
    StepLoggerInitializer.init()
  }

  @Test
  fun mainTest() = uiTest {
    CommonSteps(this).createProject()
    CommonSteps(this).openAPlusProjectWindow()
    step("Cancel") {
      with(dialog("Select Course")) {
        button("Cancel").click()
      }
    }
    CommonSteps(this).openAPlusProjectWindow()
    step("Select course") {
      with(dialog("Select Course")) {
        findText("O1").click()
        button("OK").click()
      }
    }
    step("Choose settings") {
      CommonSteps(this).aPlusSettings(true)
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
        aPlusStripeButton().click()
        with(modules()) {
          waitFor(
            Duration.ofSeconds(60),
            Duration.ofSeconds(1),
            "O1Library not found in modules list"
          ) { hasText("O1Library") }
          assertTrue("A module is installed", hasText("  [Installed]"))
        }
      }
    }
  }

  @Test
  fun aboutDialogTest() = uiTest {
    step("About dialog") {
      CommonSteps(this).openAboutDialog()
      with(dialog("A+ Courses")) {
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
  private fun getVersion(): String {
    val versionProperties = Properties()
    val projectDir = File("").absolutePath;
    versionProperties.load(FileInputStream("$projectDir/build/resources/main/build-info.properties"))
    return versionProperties.getProperty("version")
  }
}
