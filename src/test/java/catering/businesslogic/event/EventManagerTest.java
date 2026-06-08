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

    // TEST 1: Creazione evento come Organizzatore
    @Test
    public void testCreateEvent_SuccessAsOrganizer() throws UseCaseLogicException {
        catERing.getUserManager().fakeLogin("Francesca");
        User organizer = catERing.getUserManager().getCurrentUser();
        Assertions.assertTrue(organizer.isOrganizer());

        Date testDate = new Date(System.currentTimeMillis());
        // MODIFICA: Passiamo una data di fine valida invece di null
        Event createdEvent = eventManager.createEvent("Nuovo Evento da Francesca", testDate, testDate, organizer);

        Assertions.assertNotNull(createdEvent);
        Assertions.assertTrue(createdEvent.getId() > 1);
    }

    // TEST 2: Creazione evento come NON Organizzatore
    @Test
    public void testCreateEvent_FailureAsNonOrganizer() throws UseCaseLogicException {
        catERing.getUserManager().fakeLogin("Antonio");
        User nonOrganizer = catERing.getUserManager().getCurrentUser();
        Assertions.assertFalse(nonOrganizer.isOrganizer());
        
        Date testDate = new Date(System.currentTimeMillis());
        Assertions.assertThrows(UseCaseLogicException.class, () -> {
            // MODIFICA: Passiamo una data di fine valida invece di null
            eventManager.createEvent("Evento non autorizzato", testDate, testDate, nonOrganizer);
        });
    }

    

    // TEST 4: Modifica evento come NON Proprietario
    @Test
    public void testModifyEvent_FailureAsNonOwner() throws UseCaseLogicException  {
        catERing.getUserManager().fakeLogin("Antonio");
        
        Assertions.assertThrows(UseCaseLogicException.class, () -> {
            eventManager.modifyEvent(1, "Tentativo di modifica illecito", new Date(System.currentTimeMillis()));
        });
    }

    
}
