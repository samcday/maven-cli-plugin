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
            assertCommands(builder.parseCommand("clean test"),
                    aCommandCall().hasPhases("clean", "test").hasProjects(defaultProject));
        }
    }

    public class WhenInputContainsPhasesAndOfflineSwitch {
        public void shouldBuildOnlyOneCommandWhenSwitchIsSpecifiedAfterPhases() {
            assertCommands(builder.parseCommand("clean test -o"),
                    aCommandCall().hasPhases("clean", "test").runsOffline()
                            .hasProjects(defaultProject));
        }

        public void shouldBuildTwoCommandWhenSwitchIsSpecifiedBeforePhases() {
            assertCommands(builder.parseCommand("-o clean test"),
                    aCommandCall().runsOffline(),
                    aCommandCall().hasPhases("clean", "test").hasProjects(defaultProject));
        }

        public void shouldBuildOneCommandEvenWhenSwitchIsSpecifiedBetweenPhases() {
            assertCommands(builder.parseCommand("clean -o test"),
                    aCommandCall().hasPhases("clean", "test").runsOffline()
                            .hasProjects(defaultProject));
        }
    }

    public class WhenOnlySwitchesAreSpecified {
        public void willBuildOneCommandWhenOnlyOfflineSwitchIsSpecified() {
            assertCommands(builder.parseCommand("-o"), aCommandCall().runsOffline());
        }

        public void willBuildOneCommandWhenOnlyDoNotRecurseSwitchIsSpecified() {
            assertCommands(builder.parseCommand("-N"), aCommandCall().notRecursing());
        }

        public void willBuildOneCommandWhenOnlySkipTestsSwitchIsSpecified() {
            assertCommands(builder.parseCommand("-S"), aCommandCall().skippingTests());
        }

        public void willBuildOneCommandWhenOnlyProfileSwitchIsSpecified() {
            assertCommands(builder.parseCommand("-Pprofile"),
                    aCommandCall().hasProfiles("profile"));
        }

        public void willBuildOneCommandWhenOnlyPropertySwitchIsSpecified() {
            assertCommands(builder.parseCommand("-Dabcd=def"),
                    aCommandCall().hasProperties("abcd=def"));
        }

        public void willBuildTwoCommandsWhenTwoPropertiesAreSpecified() {
            assertCommands(builder.parseCommand("-Dabcd=def -Ddefg=ghi"),
                    aCommandCall().hasProperties("abcd=def"),
                    aCommandCall().hasProperties("defg=ghi"));
        }

        public void willBuildTwoCommandsWhenTwoSwitchesAreSpecified() {
            assertCommands(builder.parseCommand("-Dabcd=def -o"),
                    aCommandCall().hasProperties("abcd=def"),
                    aCommandCall().runsOffline());
        }

        public void willBuildNothingWhenNoKeyValueSpecifiedAfterPropertySwitch() {
            specify(builder.parseCommand("-D").size(), should.equal(0));
        }

        public void willBuildNothingWhenNoValueIsSpecifiedAfterPropertySwitch() {
            specify(builder.parseCommand("-Dabc").size(), should.equal(0));
        }

        public void willBuildNothingWhenNoValueIsSpecifiedAfterPropertySwitchWithEqualSign() {
            specify(builder.parseCommand("-Dabc=").size(), should.equal(0));
        }

        public void willBuildNothingWhenNoKeyIsSpecifiedAfterPropertySwitch() {
            specify(builder.parseCommand("-D=def").size(), should.equal(0));
        }
    }

    public class WhenSpecifyingModules {
        public void willBuildCommandWithNoValidLifecyleToRunIfInputHasNoPhasesSpecified() {
            MavenProject submodule = new MavenProject();
            modules.put("module", submodule);

            assertCommands(builder.parseCommand("module"),
                    aCommandCall().hasProjects(submodule));
        }

        public void willBuildCommandWithModulesMatchingTheWildCardSpecified() {
            MavenProject submodule1 = new MavenProject();
            MavenProject submodule2 = new MavenProject();

            modules.put("submodule1", submodule1);
            modules.put("submodule2", submodule1);
            modules.put("module", new MavenProject());

            assertCommands(builder.parseCommand("sub*"),
                    aCommandCall().hasProjects(submodule1, submodule2));
        }

        public void willNotBuildCommandIfWildCardSpecifiedMatchesNothing() {
            modules.put("mod1", new MavenProject());
            modules.put("mod2", new MavenProject());

            specify(builder.parseCommand("sub*").size(), should.equal(0));
        }
    }

    public class WhenUserAliasesAreSpecified {
        public void willExpandAliasAsDefinedInPom() {
            userAliases.put("explode", "clean compile");

            assertCommands(builder.parseCommand("explode"),
                    aCommandCall().hasPhases("clean", "compile").hasProjects(defaultProject));
        }

        public void willJoinTheUsualParsingAfterExpandingTheAlias() {
            userAliases.put("explode", "clean compile");

            assertCommands(builder.parseCommand("explode -N test"),
                    aCommandCall().hasPhases("clean", "compile", "test")
                        .hasProjects(defaultProject).notRecursing());
        }
    }

    private void assertCommands(List<CommandCall> actual,
            CommandCallTestDataBuilder... expected) {
        specify(actual.size(), should.equal(expected.length));

        for (int i = 0; i < actual.size(); i++) {
            assertReflectionEquals(expected[i].build(), actual.get(i));
        }
    }
}
