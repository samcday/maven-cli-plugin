package org.twdata.maven.cli;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import org.apache.maven.project.MavenProject;
import org.junit.runner.RunWith;
import static org.unitils.reflectionassert.ReflectionAssert.assertReflectionEquals;
import static org.twdata.maven.cli.CommandCallTestDataBuilder.aCommandCall;

@RunWith(JDaveRunner.class)
public class CommandCallBuilderSpec extends Specification<CommandCallBuilder> {
    private MavenProject defaultProject = new MavenProject();
    private Map<String, MavenProject> modules = new HashMap<String, MavenProject>();
    private Map<String, String> userAliases = new HashMap<String, String>();
    private CommandCallBuilder builder =
            new CommandCallBuilder(defaultProject, modules, userAliases);

    public class WhenInputOnlyContainsPhases {
        public void shouldBuildOnlyOneCommandContainingAllThePhases() {
            CommandCall expected = aCommandCall().hasPhases("clean", "test")
                    .hasProjects(defaultProject).build();

            assertCommands(builder.parseCommand("clean test"), expected);
        }
    }

    public class WhenInputContainsPhasesAndOfflineSwitch {
        public void shouldBuildOnlyOneCommandWhenSwitchIsSpecifiedAfterPhases() {
            CommandCall expected = aCommandCall().hasPhases("clean", "test")
                    .runsOffline().hasProjects(defaultProject).build();

            assertCommands(builder.parseCommand("clean test -o"), expected);
        }

        public void shouldBuildTwoCommandWhenSwitchIsSpecifiedBeforePhases() {
            CommandCall expectedOffline = aCommandCall().runsOffline().build();
            CommandCall expectedPhases = aCommandCall().hasPhases("clean", "test")
                    .hasProjects(defaultProject).build();

            assertCommands(builder.parseCommand("-o clean test"),
                    expectedOffline, expectedPhases);
        }

        public void shouldBuildOneCommandEvenWhenSwitchIsSpecifiedBetweenPhases() {
            CommandCall expected = aCommandCall().hasPhases("clean", "test")
                    .runsOffline().hasProjects(defaultProject).build();

            assertCommands(builder.parseCommand("clean -o test"), expected);
        }
    }

    private void assertCommands(List<CommandCall> actual, CommandCall... expected) {
        specify(actual.size(), should.equal(expected.length));

        for (int i = 0; i < actual.size(); i++) {
            assertReflectionEquals(expected[i], actual.get(i));
        }
    }
}
