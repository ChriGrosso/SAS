package catering.persistence;

import catering.businesslogic.event.Event;
import catering.businesslogic.event.EventReceiver;
import catering.businesslogic.event.Service;
import catering.businesslogic.menu.Menu;
import catering.businesslogic.shift.Shift;
import catering.businesslogic.user.User;

/**
 * Persistence class for Event operations.
 * Delegates to Event and Service classes for actual persistence.
 */
public class EventPersistence implements EventReceiver {

    @Override
    public void updateEventCreated(Event event) {
        event.saveNewEvent();
    }

    @Override
    public void updateEventModified(Event event) {
        event.updateEvent();
    }

    @Override
    public void updateEventDeleted(Event event) {
        event.deleteEvent();
    }

    @Override
    public void updateServiceCreated(Event event, Service service) {
        service.saveNewService();
    }

    @Override
    public void updateServiceModified(Service service) {
        service.updateService();
    }

    @Override
    public void updateServiceDeleted(Service service) {
        service.deleteService();
    }

    @Override
    public void updateMenuAssigned(Service service, Menu menu) {
        service.assignMenuToService(menu);
    }

    @Override
    public void updateMenuRemoved(Service service) {
        service.removeMenuFromService();
    }

    @Override
    public void updateStaffAssigned(Service service, Shift shift, User staff) {
    }

    @Override
    public void updateStaffRemoved(Service service, Shift shift, User staff) {
    }
}