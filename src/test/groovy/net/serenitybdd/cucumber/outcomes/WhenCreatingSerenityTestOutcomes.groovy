package net.serenitybdd.cucumber.outcomes

import com.github.goldin.spock.extensions.tempdir.TempDir
import net.serenitybdd.cucumber.integration.FeatureWithNoName
import net.serenitybdd.cucumber.integration.ScenariosWithTableInBackgroundSteps
import net.thucydides.core.model.TestOutcome
import net.thucydides.core.model.TestResult
import net.thucydides.core.model.TestStep
import net.thucydides.core.model.TestTag
import net.thucydides.core.reports.OutcomeFormat
import net.thucydides.core.reports.TestOutcomeLoader
import net.serenitybdd.cucumber.integration.BasicArithemticScenario
import net.serenitybdd.cucumber.integration.FailingScenario
import net.serenitybdd.cucumber.integration.MultipleScenarios
import net.serenitybdd.cucumber.integration.MultipleScenariosWithPendingTag
import net.serenitybdd.cucumber.integration.MultipleScenariosWithSkippedTag
import net.serenitybdd.cucumber.integration.PendingScenario
import net.serenitybdd.cucumber.integration.ScenariosWithPendingTag
import net.serenitybdd.cucumber.integration.ScenariosWithSkippedTag
import net.serenitybdd.cucumber.integration.SimpleScenario
import net.serenitybdd.cucumber.integration.SimpleScenarioWithNarrativeTexts
import net.serenitybdd.cucumber.integration.SimpleScenarioWithTags
import spock.lang.Specification

import static net.serenitybdd.cucumber.util.CucumberRunner.serenityRunnerForCucumberTestRunner

/**
 * Created by john on 23/07/2014.
 */
class WhenCreatingSerenityTestOutcomes extends Specification {

    @TempDir
    File outputDirectory

    /*
    Feature: A simple feature

      Scenario: A simple scenario
        Given I want to purchase 2 widgets
        And a widget costs $5
        When I buy the widgets
        Then I should be billed $10
     */
    def "should generate a well-structured Thucydides test outcome for each executed Cucumber scenario"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(SimpleScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]
        def steps = testOutcome.testSteps.collect { step -> step.description }

        then:
        testOutcome.title == "A simple scenario"

        and:
        testOutcome.stepCount == 4
        steps == ['Given I want to purchase 2 widgets', 'And a widget costs $5', 'When I buy the widgets', 'Then I should be billed $10']
    }

    def "should record results for each step"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(SimpleScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]
        def stepResults = testOutcome.testSteps.collect { step -> step.result }

        then:
        testOutcome.result == TestResult.SUCCESS

        and:
        stepResults == [TestResult.SUCCESS,TestResult.SUCCESS,TestResult.SUCCESS,TestResult.SUCCESS]
    }

    def "should record failures for a failing scenario"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(FailingScenario.class, outputDirectory);

        when:
        runtime.run();
        List<TestOutcome>  recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        TestOutcome testOutcome = recordedTestOutcomes[0]
        List<TestStep> stepResults = testOutcome.testSteps.collect { step -> step.result }

        then:
        testOutcome.result == TestResult.FAILURE
        and:
        stepResults == [TestResult.SUCCESS,TestResult.SUCCESS,TestResult.SUCCESS,TestResult.FAILURE, TestResult.SKIPPED]
        and:
        testOutcome.testSteps[3].errorMessage.contains("expected:<[2]0> but was:<[1]0>")
    }

    def "should record a feature tag based on the name of the feature"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(SimpleScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.tags.contains(TestTag.withName("A simple feature").andType("feature"))
    }

    def "should record background steps"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(ScenariosWithTableInBackgroundSteps.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.stepCount == 4
    }

    def "should record table data in steps"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(ScenariosWithTableInBackgroundSteps.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.testSteps[0].description == """Given the following customers exist:
| Name | DOB | Mobile Phone | Home Phone | Work Phone | Address Line 1 | Address Line 2 |
| SEAN PAUL | 30/05/1978 | 860123334 | 1234567899 | 16422132 | ONE BBI ACC | BEACON SOUTH |
| TONY SMITH | 10/10/1975 | 86123335 | 11255555 | 16422132 | 1 MAIN STREET | BANKCENTRE |
| PETE FORD | 12/03/1970 | 865555551 | 15555551 | 15555551 | Q6B HILL ST | BLACKROCK |
| JOHN B JOVI | 22/08/1957 | 871274762 |  | 16422132 | BLAKBURN | TALLAGHT |
| JOHN ANFIELD | 20/05/1970 | 876565656 | 015555551 | 214555555 | DUBLIN | DUBLIN |"""
    }
    def "should default to the filename if the feature name is not specified in the feature file"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(FeatureWithNoName.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.tags.contains(TestTag.withName("Feature with no name").andType("feature"))
    }

/*
@flavor:strawberry
Feature: A simple feature with tags
  This is about selling widgets
  @shouldPass
  @color:red
  @in-progress
  ...
 */
    def "should record any provided tags"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(SimpleScenarioWithTags.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.tags.size() == 6
        and:
        testOutcome.tags.contains(TestTag.withName("A simple feature with tags").andType("feature"))
        testOutcome.tags.contains(TestTag.withName("strawberry").andType("flavor"))
        testOutcome.tags.contains(TestTag.withName("red").andType("color"))
        testOutcome.tags.contains(TestTag.withName("shouldPass").andType("tag"))
        testOutcome.tags.contains(TestTag.withName("in-progress").andType("tag"))
        testOutcome.tags.contains(TestTag.withName("Samples/Simple scenario with tags").andType("story"))
    }

    def "should record the narrative text"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(SimpleScenarioWithNarrativeTexts.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.userStory.narrative == "This is about selling widgets"
    }

    def "should record the scenario description text for a scenario"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(SimpleScenarioWithNarrativeTexts.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        def testOutcome = recordedTestOutcomes[0]

        then:
        testOutcome.description == """A description of this scenario
It goes for two lines"""
    }

    def "should record pending and skipped steps for a pending scenario"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(PendingScenario.class, outputDirectory);

        when:
        runtime.run();
        List<TestOutcome>  recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory);
        TestOutcome testOutcome = recordedTestOutcomes[0]
        List<TestStep> stepResults = testOutcome.testSteps.collect { step -> step.result }

        then:
        testOutcome.result == TestResult.PENDING
        and:
        stepResults == [TestResult.SUCCESS,TestResult.SUCCESS,TestResult.PENDING,TestResult.IGNORED]
    }

    def "should generate a well-structured Thucydides test outcome for feature files with several Cucumber scenario"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(MultipleScenarios.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory)

        then:

        recordedTestOutcomes.size() == 2

        def testOutcome1 = recordedTestOutcomes[0]
        def steps1 = testOutcome1.testSteps.collect { step -> step.description }

        def testOutcome2 = recordedTestOutcomes[1]
        def steps2 = testOutcome2.testSteps.collect { step -> step.description }

        and:
        testOutcome1.title == "Simple scenario 1"
        testOutcome1.result == TestResult.FAILURE

        and:
        testOutcome2.title == "Simple scenario 2"
        testOutcome2.result == TestResult.SUCCESS

        and:
        steps1 == ['Given I want to purchase 2 widgets', 'And a widget costs $5', 'When I buy the widgets', 'Then I should be billed $50']
        steps2 == ['Given I want to purchase 4 widgets', 'And a widget costs $3', 'When I buy the widgets', 'Then I should be billed $12']
    }

    def "should generate outcomes for scenarios with a background section"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(BasicArithemticScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory)

        then:
        recordedTestOutcomes.size() == 4

        and:
        recordedTestOutcomes.collect { it.name } == ["Addition", "Another Addition","Many additions","Many additions"]
        and:
        recordedTestOutcomes[0].stepCount == 3
        recordedTestOutcomes[1].stepCount == 3
        recordedTestOutcomes[2].stepCount == 5
        recordedTestOutcomes[3].stepCount == 5
    }

    def "should read @issue tags"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(BasicArithemticScenario.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory)

        then:
        recordedTestOutcomes.each { outcome ->
            outcome.tags.contains(TestTag.withName("ISSUE-123").andType("issue"))
        }
        and:
        recordedTestOutcomes[0].tags.contains(TestTag.withName("ISSUE-456").andType("issue"))
    }

    def "scenarios with the @pending tag should be reported as Pending"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(MultipleScenariosWithPendingTag.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory)

        then:

        recordedTestOutcomes.size() == 2

        def testOutcome1 = recordedTestOutcomes[0]
        def steps1 = testOutcome1.testSteps.collect { step -> step.description }

        def testOutcome2 = recordedTestOutcomes[1]
        def steps2 = testOutcome2.testSteps.collect { step -> step.description }

        and:
        testOutcome1.title == "Simple scenario 1"
        testOutcome1.result == TestResult.PENDING

        and:
        testOutcome2.title == "Simple scenario 2"
        testOutcome2.result == TestResult.PENDING

        and:
        steps1 == ['Given I want to purchase 2 widgets', 'And a widget costs $5', 'When I buy the widgets', 'Then I should be billed $50']
        steps2 == ['Given I want to purchase 4 widgets', 'And a widget costs $3', 'When I buy the widgets', 'Then I should be billed $12']
    }


    def "individual scenarios with the @pending tag should be reported as Pending"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(ScenariosWithPendingTag.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory)

        then:

        recordedTestOutcomes.size() == 3
        def testOutcome1 = recordedTestOutcomes[0]
        def testOutcome2 = recordedTestOutcomes[1]
        def testOutcome3 = recordedTestOutcomes[2]

        and:
        testOutcome1.result == TestResult.SUCCESS
        testOutcome2.result == TestResult.PENDING
        testOutcome3.result == TestResult.SUCCESS
    }

    def "individual scenarios with the @wip tag should be reported as Skipped"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(ScenariosWithSkippedTag.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory)

        then:

        recordedTestOutcomes.size() == 3
        def testOutcome1 = recordedTestOutcomes[0]
        def testOutcome2 = recordedTestOutcomes[1]
        def testOutcome3 = recordedTestOutcomes[2]

        and:
        testOutcome1.result == TestResult.SUCCESS
        testOutcome2.result == TestResult.SKIPPED
        testOutcome3.result == TestResult.SUCCESS
    }

    def "scenarios with the @wip tag should be reported as skipped"() {
        given:
        def runtime = serenityRunnerForCucumberTestRunner(MultipleScenariosWithSkippedTag.class, outputDirectory);

        when:
        runtime.run();
        def recordedTestOutcomes = new TestOutcomeLoader().forFormat(OutcomeFormat.JSON).loadFrom(outputDirectory)

        then:

        recordedTestOutcomes.size() == 2

        def testOutcome1 = recordedTestOutcomes[0]
        def steps1 = testOutcome1.testSteps.collect { step -> step.description }

        def testOutcome2 = recordedTestOutcomes[1]
        def steps2 = testOutcome2.testSteps.collect { step -> step.description }

        and:
        testOutcome1.title == "Simple scenario 1"
        testOutcome1.result == TestResult.SKIPPED

        and:
        testOutcome2.title == "Simple scenario 2"
        testOutcome2.result == TestResult.SKIPPED

        and:
        steps1 == ['Given I want to purchase 2 widgets', 'And a widget costs $5', 'When I buy the widgets', 'Then I should be billed $50']
        steps2 == ['Given I want to purchase 4 widgets', 'And a widget costs $3', 'When I buy the widgets', 'Then I should be billed $12']
    }

}