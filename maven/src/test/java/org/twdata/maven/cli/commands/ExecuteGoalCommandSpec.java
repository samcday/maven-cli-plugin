package org.twdata.maven.cli.commands;

import java.util.HashMap;
import jdave.Specification;
import jdave.junit4.JDaveRunner;
import org.junit.runner.RunWith;

@RunWith(JDaveRunner.class)
public class ExecuteGoalCommandSpec extends Specification<Void> {
    public class WhenMatchingRequests {
        private ExecuteGoalCommand command;

        public void create() {
            HashMap<String, String> userDefinedGoal = new HashMap<String, String>();
            userDefinedGoal.put("all", "clean compile test");

            //TODO: This last parameter should be a MavenPluginManager, but how do we get that in a unit test?
            command = new ExecuteGoalCommand(null, null, userDefinedGoal, null);
        }

        public void shouldMatchWhenSpecifiedRequestIsOneOfThePredefinedGoals() {
            specify(command.matchesRequest("clean"), should.equal(true));
        }

        public void shouldMatchWhenSpecifiedRequestIsOneOfTheUserDefinedGoals() {
            specify(command.matchesRequest("all"), should.equal(true));
        }

        public void shouldMatchWhenSpecifiedRequestRunningASpecifiedPluginGoal() {
            specify(command.matchesRequest("group:artifact:goal"), should.equal(true));
        }

        public void shouldNotMatchWhenRequestForSpecifiedPluginOmitsGoal() {
            specify(command.matchesRequest("group:artifact"), should.equal(false));
        }
    }
}
