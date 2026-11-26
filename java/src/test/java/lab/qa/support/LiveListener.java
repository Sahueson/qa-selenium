package lab.qa.support;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

/** Avisa a la pantalla en vivo de cuántos tests van a correr y espera a los últimos eventos al acabar. */
public class LiveListener implements TestExecutionListener {
  @Override
  public void testPlanExecutionStarted(TestPlan plan) {
    LiveEvents.emit("plan", String.valueOf(plan.countTestIdentifiers(TestIdentifier::isTest)));
  }

  @Override
  public void testPlanExecutionFinished(TestPlan plan) {
    LiveEvents.flush();
  }
}
