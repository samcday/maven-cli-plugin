package org.twdata.maven.cli;

import java.util.HashSet;
import java.util.Set;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import org.junit.runner.RunWith;

@RunWith(JDaveRunner.class)
public class ExecutePhaseCommandSpec extends Specification<Void> {
    public class WhenRunningRequests {
        private Set<String> modules = new HashSet<String>();
        private PhaseCallBuilder mockBuilder = mock(PhaseCallBuilder.class);
        private PhaseCallRunner mockRunner = mock(PhaseCallRunner.class);
        private CliConsole mockConsole = mock(CliConsole.class);
        private ExecutePhaseCommand command =
                new ExecutePhaseCommand(modules, mockBuilder, mockRunner, mockConsole);

        public void shouldNotMatchRequestIfAnyTheTokensIsInvalid() {
            specify(command.matchesRequest("-o build"), should.equal(false));
        }

        public void shouleNotRejectKeyValuesOfProperties() {
            specify(command.matchesRequest("-Dabcd=def"), should.equal(true));
        }

        public void shouldNotRejectProfileInput() {
            specify(command.matchesRequest("-Pprofile"), should.equal(true));
        }

        public void shouldNotRejectIfMatchesModuleWildCardInput() {
            modules.add("module");
            specify(command.matchesRequest("mod*"), should.equal(true));
        }
    }
}
