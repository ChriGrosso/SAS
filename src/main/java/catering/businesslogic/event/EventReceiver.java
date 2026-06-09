package catering.businesslogic.event;

import catering.businesslogic.menu.Menu;
import catering.businesslogic.shift.Shift;
import catering.businesslogic.user.User;


public interface EventReceiver {

    void updateEventCreated(Event event);

    void updateEventModified(Event event);

    void updateEventDeleted(Event event);

    void updateServiceCreated(Event event, Service service);

    void updateServiceModified(Service service);

    void updateServiceDeleted(Service service);

    void updateMenuAssigned(Service service, Menu menu);

    void updateMenuRemoved(Service service);

    // Nuovi metodi per DSD Assegnamento Personale
    void updateStaffAssigned(Service service, Shift shift, User staff);

    void updateStaffRemoved(Service service, Shift shift, User staff);
}