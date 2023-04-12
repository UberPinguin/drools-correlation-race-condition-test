package org.example.CorrelationRaceCondition;

import java.util.Collection;
import java.util.Date;
import org.drools.core.common.InternalFactHandle;
import org.kie.api.KieServices;
import org.kie.api.definition.KiePackage;
import org.kie.api.event.rule.DebugAgendaEventListener;
import org.kie.api.event.rule.DebugRuleRuntimeEventListener;
import org.kie.api.logger.KieRuntimeLogger;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.ObjectFilter;

import org.junit.After;
import org.junit.Before;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
* Unit test for simple App.
*/
public class CorrelationRaceConditionTest extends TestCase {
  private KieServices ks;
  private KieContainer kc;
  private KieSession kieSession;
  private KieRuntimeLogger logger;
  private Thread droolsThread;

  @Before
  public void setUp() {
    ks = KieServices.get();
    kc = ks.getKieClasspathContainer();
    kieSession = kc.newKieSession("CorrelationRaceConditionKS");
    kieSession.addEventListener(new DebugAgendaEventListener());
    kieSession.addEventListener(new DebugRuleRuntimeEventListener());
    logger = KieServices.get().getLoggers().newThreadedFileLogger(kieSession, "./target/CorrelationRaceCondition", 1000);

    droolsThread = new Thread() {
      public void run(){
        kieSession.fireUntilHalt();
      }
    };
    droolsThread.start();
  }

  @After
  public void tearDown() throws InterruptedException {
    kieSession.halt();
    logger.close();
    kieSession.dispose();
    droolsThread.join();
  }

  /**
  * Create the test case
  *
  * @param testName name of the test case
  */
  public CorrelationRaceConditionTest(String testName) {
    super(testName);
  }


  /**
  * @return the suite of tests being tested
  */
  public static Test suite() {
    return new TestSuite(CorrelationRaceConditionTest.class);
  }

  public void testCloneEvent() {
    final Event event = new Event(new Date(), "DeviceOffline", "loc001", "device01");
    Event testClone = event.clone();
    assertEquals(event, testClone);
  }

  public void testSingleOfflineEvent() throws InterruptedException {
    final Event event = new Event(new Date(), "DeviceOffline", "loc001", "device01");
    kieSession.insert(event);
    /*
    * If we don't sleep for at least 150ms here, the Drools rules will not have finished firing
    * when we attempt to verify the expected end state.
    */
    Thread.sleep(150);
    listRulesInSession();
    printFactHandleDetails();
    assertEquals("Wrong number of Event facts", 1, countFactHandleByClass(Event.class::isInstance));
    assertEquals("Wrong number of DeviceOfflineAffliction facts", 1,
            countFactHandleByClass(DeviceOfflineAffliction.class::isInstance));
    DeviceOfflineAffliction aff = getDeviceOfflineAffliction();
    assertNotNull(aff);
  }

  /**
  * Insert 14 DeviceOffline events, followed by 14 DeviceAdopted events.
  * The DeviceNames in each set differ slightly, with the set of DeviceOffline
  * events omitting device014 and the set of DeviceAdopted events omitting
  * device08.
  * At the end, there should be a DeviceOfflineAffliction with exactly one
  * device name in it, and that name should be "device08".
  * @throws InterruptedException
  */
  public void testMultipleOfflineAndAdoptedEventsNoSleepBetweenEvents() throws InterruptedException {
    final Event templateOfflineEvent = new Event(new Date(), "DeviceOffline", "loc001", "device01");
    final Event templateAdoptedEvent = new Event(new Date(), "DeviceAdopted", "loc001", "device01");

    // insert a set of offline events
    for (int i = 1; i < 15; i++) {
      if (i == 14) {
        continue;
      }
      Event offlineEvent = templateOfflineEvent.clone().setTime(new Date())
          .setDeviceName(templateOfflineEvent.getDeviceName().replace("1",String.valueOf(i)));
      kieSession.insert(offlineEvent);
    }
    // insert a set of adopted events
    for (int i = 1; i < 15; i++) {
      if (i == 8) {
        continue;
      }
      Event adoptedEvent = templateAdoptedEvent.clone().setTime(new Date())
          .setDeviceName(templateAdoptedEvent.getDeviceName().replace("1",String.valueOf(i)));
      kieSession.insert(adoptedEvent);
    }
    /*
    * If we don't sleep for at least 250ms here, the Drools rules may not have finished firing
    * when we attempt to verify the expected end state.
    */
    Thread.sleep(250);
    listRulesInSession();
    printFactHandleDetails();
    assertEquals("Wrong number of Event facts", 1, countFactHandleByClass(Event.class::isInstance));
    assertEquals("Wrong number of DeviceOfflineAffliction facts", 1,
            countFactHandleByClass(DeviceOfflineAffliction.class::isInstance));
    DeviceOfflineAffliction aff = getDeviceOfflineAffliction();
    assertNotNull(aff);
  }

  public void testMultipleOfflineAndAdoptedEventsWithSleepBetweenEvents() throws InterruptedException {
    final Event templateOfflineEvent = new Event(new Date(), "DeviceOffline", "loc001", "device01");
    final Event templateAdoptedEvent = new Event(new Date(), "DeviceAdopted", "loc001", "device01");

    // insert a set of offline events
    for (int i = 1; i < 15; i++) {
      if (i == 14) {
        continue;
      }
      Thread.sleep(35);
      Event offlineEvent = templateOfflineEvent.clone().setTime(new Date())
          .setDeviceName(templateOfflineEvent.getDeviceName().replace("1",String.valueOf(i)));
      kieSession.insert(offlineEvent);
    }
    // insert a set of adopted events
    for (int i = 1; i < 15; i++) {
      if (i == 8) {
        continue;
      }
      Thread.sleep(35);
      Event adoptedEvent = templateAdoptedEvent.clone().setTime(new Date())
          .setDeviceName(templateAdoptedEvent.getDeviceName().replace("1",String.valueOf(i)));
      kieSession.insert(adoptedEvent);
    }
    /*
     * If we don't sleep for at least 150ms here, the Drools rules will not have finished firing
     * when we attempt to verify the expected end state.
     */
    Thread.sleep(150);
    listRulesInSession();
    printFactHandleDetails();
    assertEquals("Wrong number of Event facts", 1, countFactHandleByClass(Event.class::isInstance));
    assertEquals("Wrong number of DeviceOfflineAffliction facts", 1,
            countFactHandleByClass(DeviceOfflineAffliction.class::isInstance));
    DeviceOfflineAffliction aff = getDeviceOfflineAffliction();
    assertNotNull(aff);
  }
  
  private void listRulesInSession() {
    Collection<KiePackage> packages = kieSession.getKieBase().getKiePackages();
    System.err.println("printing inventory of rules");
    packages.forEach(p -> p.getRules().forEach(r -> System.err.println("\t" + r.getName())));
    System.err.println("end of rules inventory");
  }

  private void printFactHandleDetails() {
    System.err.println("printing details of facts");
    kieSession.getFactHandles().forEach(fh -> {
      InternalFactHandle ifh = (InternalFactHandle) fh;
      System.err.println("\t" + ifh.getObject().toString());
    });
    System.err.println("end of fact class name inventory");
  }
  
  private int countFactHandleByClass(ObjectFilter objFilter) {
    return kieSession.getObjects(objFilter).size();
  }

  private DeviceOfflineAffliction getDeviceOfflineAffliction() {
    return (DeviceOfflineAffliction) kieSession.getObjects(DeviceOfflineAffliction.class::isInstance).iterator().next();
  }

}