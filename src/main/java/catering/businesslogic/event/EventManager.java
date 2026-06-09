package catering.businesslogic.event;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;

import catering.businesslogic.CatERing;
import catering.businesslogic.UseCaseLogicException;
import catering.businesslogic.kitchen.KitchenTask;
import catering.businesslogic.menu.Menu;
import catering.businesslogic.shift.Shift;
import catering.businesslogic.user.User;

public class EventManager {

    private ArrayList<EventReceiver> eventReceivers;
    private Event selectedEvent;
    private Service currentService;

    public EventManager() {
        eventReceivers = new ArrayList<>();
    }

    public void addEventReceiver(EventReceiver receiver) {
        if (receiver != null && !eventReceivers.contains(receiver)) {
            eventReceivers.add(receiver);
        }
    }

    public void removeEventReceiver(EventReceiver receiver) {
        eventReceivers.remove(receiver);
    }

    public ArrayList<Event> getEvents() {
        return Event.loadAllEvents();
    }

    public void setSelectedServiceIndex(int serviceId) {
        if (selectedEvent != null && selectedEvent.getServices() != null) {
            for (Service si : selectedEvent.getServices()) {
                if (si.getId() == serviceId) {
                    currentService = si;
                    return;
                }
            }
        }
    }

    public void setCurrentService(Service service) {
        this.currentService = service;
    }

    public Service getCurrentService() {
        return this.currentService;
    }

    public Event getEventFromID(int idEvento) {
        return Event.loadById(idEvento);
    }

    public Event getSelectedEvent() {
        return selectedEvent;
    }

    public void setSelectedEvent(Event event) {
        this.selectedEvent = event;
    }

    public Event createEvent(String name, Date dateStart, Date dateEnd, User organizer) throws UseCaseLogicException {
        if (organizer == null || !organizer.isOrganizer()) {
            throw new UseCaseLogicException("Solo un responsabile di servizio può creare un evento.");
        }
        Event event = new Event(name, null, organizer, dateStart, dateEnd, 150, null);
        notifyEventCreated(event);
        this.selectedEvent = event;
        this.currentService = null;
        return event;
    }

    public Event createEvent(String name, Date dateStart, Date dateEnd, int part, String loc, String note, User organizer) throws UseCaseLogicException {
        if (organizer == null || !organizer.isOrganizer()) {
            throw new UseCaseLogicException("Solo un responsabile di servizio può creare un evento.");
        }
        Event event = new Event(name, loc, organizer, dateStart, dateEnd, part, note);
        notifyEventCreated(event);
        this.selectedEvent = event;
        this.currentService = null;
        return event;
    }

    public void selectEvent(Event event) {
        this.selectedEvent = event;
        this.currentService = null;
    }

    public Service createService(String name, Date date, Time timeStart, Time timeEnd, String location)
            throws UseCaseLogicException {
        if (selectedEvent == null) {
            throw new UseCaseLogicException("Cannot create service: no event selected");
        }
        try {
            Service service = new Service();
            service.setName(name);
            service.setDate(date);
            service.setTimeStart(timeStart);
            service.setTimeEnd(timeEnd);
            service.setLocation(location);
            service.setEventId(selectedEvent.getId());
            notifyServiceCreated(service);
            selectedEvent.addService(service);
            this.currentService = service;
            return service;
        } catch (Exception e) {
            return null;
        }
    }

    public void modifyEvent(int eventId, String newName, Date newDate) throws UseCaseLogicException {
        Event event = Event.loadById(eventId);
        if (event == null) {
            throw new UseCaseLogicException("Evento non trovato.");
        }
        User currentUser = CatERing.getInstance().getUserManager().getCurrentUser();
        if (event.getOrganizer() == null || currentUser == null || currentUser.getId() != event.getOrganizer().getId()) {
            throw new UseCaseLogicException("L'utente non è il proprietario dell'evento e non può modificarlo.");
        }
        event.setName(newName);
        event.setDateStart(newDate);
        this.notifyEventModified(event);
    }

    public Service modifyService(int serviceId, String name, Date date, String location, int menuId) {
        Service service = findServiceById(serviceId);
        if (service != null) {
            service.setName(name);
            service.setDate(date);
            service.setLocation(location);
            if (menuId > 0 && (service.getMenuId() == 0 || service.getMenuId() != menuId)) {
                try {
                    Menu menu = Menu.load(menuId);
                    if (menu != null) {
                        service.setMenu(menu);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading menu: " + e.getMessage());
                }
            }
            notifyServiceModified(service);
            if (currentService != null && currentService.getId() == serviceId) {
                currentService = service;
            }
        }
        return service;
    }

    public boolean deleteService(int serviceId) {
        try {
            if (selectedEvent == null) return false;
            Service serviceToDelete = findServiceById(serviceId);
            if (serviceToDelete == null) return false;
            
            selectedEvent.removeService(serviceToDelete);
            if (currentService != null && currentService.getId() == serviceId) {
                currentService = null;
            }
            notifyServiceDeleted(serviceToDelete);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean deleteEvent(int eventId) {
        try {
            Event eventToDelete = Event.loadById(eventId);
            if (eventToDelete == null) return false;
            if (selectedEvent != null && selectedEvent.getId() == eventId) {
                selectedEvent = null;
                currentService = null;
            }
            notifyEventDeleted(eventToDelete);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void assignMenu(Menu menu) throws UseCaseLogicException {
        if (selectedEvent == null || currentService == null) {
            throw new UseCaseLogicException("Cannot assign menu: no event or service selected");
        }
        currentService.setMenu(menu);
        notifyMenuAssigned(currentService, menu);
    }

    public boolean removeMenu() {
        if (currentService == null) return false;
        currentService.removeMenu();
        notifyMenuRemoved(currentService);
        return true;
    }

    private Service findServiceById(int serviceId) {
        if (selectedEvent == null || selectedEvent.getServices() == null) return null;
        for (Service s : selectedEvent.getServices()) {
            if (s.getId() == serviceId) return s;
        }
        return null;
    }

    
    // DSD 1c.1, DSD 6 e DSD 7 
    

    public Event copyEvent(Event event) throws UseCaseLogicException {
        if (event == null) {
            throw new UseCaseLogicException("Nessun evento da copiare fornito.");
        }
        User currentUser = CatERing.getInstance().getUserManager().getCurrentUser();
        if (currentUser == null || !currentUser.isOrganizer()) {
            throw new UseCaseLogicException("Solo un organizzatore può copiare un evento.");
        }

        String newName = event.getName() + " (Copia)";

        Event copiedEvent = new Event(
            newName, 
            event.getLocation(), 
            currentUser, 
            event.getDateStart(), 
            event.getDateEnd(), 
            event.getExpectedPersons(), 
            event.getNotes()
        );

        notifyEventCreated(copiedEvent);
        this.selectedEvent = copiedEvent;
        this.currentService = null;
        return copiedEvent;
    }

    public Shift assignStaff(Service service, KitchenTask task, User staff, Date date, Time startTime, Time endTime, String role) throws UseCaseLogicException {
        User currentUser = CatERing.getInstance().getUserManager().getCurrentUser();
        if (currentUser == null || !currentUser.isOrganizer()) {
            throw new UseCaseLogicException("Solo un organizzatore può assegnare il personale.");
        }
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento attivo in corso per assegnare lo staff.");
        }

        
        Shift shift = Shift.createShift(date, startTime, endTime);

        
        if (staff != null) {
            shift.addBooking(staff); 
        }

        notifyStaffAssigned(service, shift, staff);
        return shift;
    }

    public void removeStaff(Service service, KitchenTask task, User staff, Shift shift) throws UseCaseLogicException {
        User currentUser = CatERing.getInstance().getUserManager().getCurrentUser();
        if (currentUser == null || !currentUser.isOrganizer()) {
            throw new UseCaseLogicException("Solo un organizzatore può rimuovere il personale.");
        }
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento attivo in corso da cui rimuovere lo staff.");
        }

        
        if (staff != null && shift != null) {
            shift.removeBookedUser(staff);
        }

        notifyStaffRemoved(service, shift, staff);
    }

    
    // CONTRATTI EXTRA
    
    
    
    public void assignChef(User chef) throws UseCaseLogicException {
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento selezionato per assegnare lo chef.");
        }
        
        
        this.selectedEvent.setChef(chef);
        
        
        this.notifyEventModified(this.selectedEvent);
    }

    
    public void confirmEvent() throws UseCaseLogicException {
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento selezionato da confermare.");
        }
        
        
        this.notifyEventModified(this.selectedEvent);
    }

    
    public void closeEvent(String notes, String documentation) throws UseCaseLogicException {
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento selezionato da chiudere.");
        }
        
        if (notes != null && !notes.isEmpty()) {
            String noteAttuali = this.selectedEvent.getNotes();
            if (noteAttuali == null) {
                noteAttuali = "";
            } else {
                noteAttuali += "\n";
            }
            this.selectedEvent.setNotes(noteAttuali + "Note di chiusura: " + notes);
        }
        
        
        this.notifyEventModified(this.selectedEvent);
    }

    
    // METODI DI NOTIFICA
    

    private void notifyEventCreated(Event event) {
        for (EventReceiver receiver : eventReceivers) { receiver.updateEventCreated(event); }
    }
    private void notifyEventModified(Event event) {
        for (EventReceiver receiver : eventReceivers) { receiver.updateEventModified(event); }
    }
    private void notifyEventDeleted(Event event) {
        for (EventReceiver receiver : eventReceivers) { receiver.updateEventDeleted(event); }
    }
    private void notifyServiceCreated(Service service) {
        for (EventReceiver receiver : eventReceivers) { receiver.updateServiceCreated(selectedEvent, service); }
    }
    private void notifyServiceModified(Service service) {
        for (EventReceiver receiver : eventReceivers) { receiver.updateServiceModified(service); }
    }
    private void notifyServiceDeleted(Service service) {
        for (EventReceiver receiver : eventReceivers) { receiver.updateServiceDeleted(service); }
    }
    private void notifyMenuAssigned(Service service, Menu menu) {
        for (EventReceiver receiver : eventReceivers) { receiver.updateMenuAssigned(service, menu); }
    }
    private void notifyMenuRemoved(Service service) {
        for (EventReceiver receiver : eventReceivers) { receiver.updateMenuRemoved(service); }
    }
    private void notifyStaffAssigned(Service service, Shift shift, User staff) {
        for (EventReceiver receiver : eventReceivers) { receiver.updateStaffAssigned(service, shift, staff); }
    }
    private void notifyStaffRemoved(Service service, Shift shift, User staff) {
        for (EventReceiver receiver : eventReceivers) { receiver.updateStaffRemoved(service, shift, staff); }
    }
}