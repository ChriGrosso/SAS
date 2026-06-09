package catering.businesslogic.event;

import catering.businesslogic.CatERing;
import catering.businesslogic.UseCaseLogicException;
import catering.businesslogic.user.User;
import catering.businesslogic.user.UserManager;
import catering.persistence.PersistenceManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.Date;
import java.sql.Time;

public class EventManagerTest {

    private CatERing catERing;
    private EventManager eventManager;

    @BeforeEach
    public void setUp() {
        PersistenceManager.initializeDatabase("database/catering_init_sqlite.sql");
        this.catERing = CatERing.getInstance();
        this.catERing.setUserManager(new UserManager());
        this.eventManager = new EventManager();
        this.eventManager.addEventReceiver(new catering.persistence.EventPersistence());
        this.catERing.setEventManager(this.eventManager);
    }

    @Test
    public void testCreateEvent_SuccessAsOrganizer() throws UseCaseLogicException {
        catERing.getUserManager().fakeLogin("Francesca");
        User organizer = catERing.getUserManager().getCurrentUser();
        Assertions.assertTrue(organizer.isOrganizer());

        Date testDate = new Date(System.currentTimeMillis());
        Event createdEvent = eventManager.createEvent("Nuovo Evento da Francesca", testDate, testDate, organizer);

        Assertions.assertNotNull(createdEvent);
        Assertions.assertTrue(createdEvent.getId() > 0);
    }

    @Test
    public void testCreateEvent_FailureAsNonOrganizer() throws UseCaseLogicException {
        catERing.getUserManager().fakeLogin("Antonio");
        User nonOrganizer = catERing.getUserManager().getCurrentUser();
        Assertions.assertFalse(nonOrganizer.isOrganizer());
        
        Date testDate = new Date(System.currentTimeMillis());
        Assertions.assertThrows(UseCaseLogicException.class, () -> {
            eventManager.createEvent("Evento non autorizzato", testDate, testDate, nonOrganizer);
        });
    }

    @Test
    public void testModifyEvent_FailureAsNonOwner() throws UseCaseLogicException  {
        catERing.getUserManager().fakeLogin("Antonio");
        Assertions.assertThrows(UseCaseLogicException.class, () -> {
            eventManager.modifyEvent(1, "Tentativo di modifica illecito", new Date(System.currentTimeMillis()));
        });
    }

    @Test
    public void testCopyEvent_Success() throws UseCaseLogicException {
        catERing.getUserManager().fakeLogin("Francesca");
        User organizer = catERing.getUserManager().getCurrentUser();
        Date testDate = new Date(System.currentTimeMillis());

        Event originalEvent = eventManager.createEvent("Evento Originale", testDate, testDate, organizer);
        Event copiedEvent = eventManager.copyEvent(originalEvent);

        Assertions.assertNotNull(copiedEvent);
        Assertions.assertNotEquals(originalEvent.getId(), copiedEvent.getId()); 
        Assertions.assertEquals(organizer, copiedEvent.getOrganizer()); 
    }

    @Test
    public void testAssignStaff_Success() throws Exception {
        catERing.getUserManager().fakeLogin("Francesca");
        User organizer = catERing.getUserManager().getCurrentUser();
        Date testDate = new Date(System.currentTimeMillis());
        Time startTime = Time.valueOf("08:00:00");
        Time endTime = Time.valueOf("14:00:00");

        Event event = eventManager.createEvent("Evento Assegnazione Staff", testDate, testDate, organizer);
        eventManager.setSelectedEvent(event);

        Service dummyService = new Service("Pranzo di Gala");
        User dummyStaff = new User("Cameriere_Test"); 

        catering.businesslogic.shift.Shift assignedShift = eventManager.assignStaff(dummyService, null, dummyStaff, testDate, startTime, endTime, "SERVIZIO");

        Assertions.assertNotNull(assignedShift);
        Assertions.assertTrue(assignedShift.isBooked(dummyStaff));
    }

    @Test
    public void testRemoveStaff_Success() throws Exception {
        catERing.getUserManager().fakeLogin("Francesca");
        User organizer = catERing.getUserManager().getCurrentUser();
        Date testDate = new Date(System.currentTimeMillis());
        Time startTime = Time.valueOf("18:00:00");
        Time endTime = Time.valueOf("23:00:00");

        Event event = eventManager.createEvent("Evento Rimozione Staff", testDate, testDate, organizer);
        eventManager.setSelectedEvent(event);

        Service dummyService = new Service("Cena Aziendale");
        User dummyStaff = new User("Cameriere_Test");

        catering.businesslogic.shift.Shift assignedShift = eventManager.assignStaff(dummyService, null, dummyStaff, testDate, startTime, endTime, "SERVIZIO");
        
        eventManager.removeStaff(dummyService, null, dummyStaff, assignedShift);

        Assertions.assertFalse(assignedShift.isBooked(dummyStaff));
    }
}