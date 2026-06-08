package catering.businesslogic.event;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import catering.businesslogic.user.User;
import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

public class Event {

    private int id;
    private String name;
    private Date dateStart;
    private Date dateEnd;
    private User chef;
    private User organizer;
    private ArrayList<Service> services;
    private int expectedPersons; // Nome corretto
    private String location;
    private String notes; // Nome corretto

    // Costruttori preesistenti, mantenuti per coerenza
    public Event() {
        services = new ArrayList<>();
    }

    public Event(String name, String loc, User org, Date sDate, Date eDate, int expPart, String note) {
        this.name = name;
        this.organizer = org;
        this.dateStart = sDate;
        this.dateEnd = eDate;
        this.location = loc;
        this.expectedPersons = expPart;
        if(note != null){
            this.notes=note;
        }
    }
    
    public Event(String name, User organizer) {
        this();
        this.name = name;
        this.organizer = organizer;
    }

    public Event(String name) {
        this();
        this.name = name;
    }

    // Basic getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getExpectedPersons() { return expectedPersons; } // Corretto
    public void setExpectedPersons(int expPersons) { this.expectedPersons = expPersons; } // Corretto
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Date getDateStart() { return dateStart; }
    public void setDateStart(Date dateStart) { this.dateStart = dateStart; }
    public Date getDateEnd() { return dateEnd; }
    public void setDateEnd(Date dateEnd) { this.dateEnd = dateEnd; }
    public User getChef() { return chef; }
    public int getChefId() { return chef != null ? chef.getId() : 0; }
    public int getOrganizerId() { return organizer != null ? organizer.getId() : 0; }
    public void setChef(User chef) { this.chef = chef; }
    public void setChefId(int chefId) { this.chef = User.load(chefId); }
    public ArrayList<Service> getServices() { return services; }
    public void setServices(ArrayList<Service> services) { this.services = services; }
    public void setOrganizer(User u) { this.organizer = u; }
    public User getOrganizer() { return this.organizer; }

    // Service management (preesistenti, invariati)
    public void addService(Service service) { services.add(service); }
    public void removeService(Service service) { if (services != null) services.remove(service); }
    public boolean containsService(Service service) { return services != null && services.contains(service); }

    // --- OPERAZIONI DI PERSISTENZA (MODIFICATE) ---
    public void saveNewEvent() {
        String query = "INSERT INTO Events (name, date_start, date_end, expected_persons, location, notes, organizer_id, chef_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        int chef_id = (this.chef != null) ? this.chef.getId() : 0;
        int organizer_id = (this.organizer != null) ? this.organizer.getId() : 0;
        PersistenceManager.executeUpdate(query, this.name, this.dateStart, this.dateEnd, this.expectedPersons, this.location, this.notes, organizer_id, chef_id);
        this.id = PersistenceManager.getLastId();
    }

    public void updateEvent() {
        String query = "UPDATE Events SET name = ?, date_start = ?, date_end = ?, expected_persons = ?, location = ?, notes = ?, organizer_id = ?, chef_id = ? WHERE id = ?";
        int chef_id = (this.chef != null) ? this.chef.getId() : 0;
        int organizer_id = (this.organizer != null) ? this.organizer.getId() : 0;
        PersistenceManager.executeUpdate(query, this.name, this.dateStart, this.dateEnd, this.expectedPersons, this.location, this.notes, organizer_id, chef_id, this.id);
    }

    public boolean deleteEvent() {
        for (Service service : services) {
            service.deleteService();
        }
        services.clear();
        String query = "DELETE FROM Events WHERE id = ?";
        return PersistenceManager.executeUpdate(query, id) > 0;
    }

    public static ArrayList<Event> loadAllEvents() {
    ArrayList<Event> events = new ArrayList<>();
    String query = "SELECT * FROM Events ORDER BY date_start DESC";
    PersistenceManager.executeQuery(query, new ResultHandler() {
        @Override
        public void handle(ResultSet rs) throws SQLException {
            Event e = new Event();
            e.id = rs.getInt("id");
            e.name = rs.getString("name");
            
            // RIPRISTINIAMO IL METODO DI LETTURA ORIGINALE (E CORRETTO)
            e.dateStart = Date.valueOf(rs.getString("date_start"));
            String dateEndString = rs.getString("date_end");
            if (dateEndString != null) {
                e.dateEnd = Date.valueOf(dateEndString);
            }

            e.expectedPersons = rs.getInt("expected_persons");
            e.location = rs.getString("location");
            e.notes = rs.getString("notes");
            
            int chefId = rs.getInt("chef_id");
            if (chefId > 0) e.chef = User.load(chefId);
            
            int organizerId = rs.getInt("organizer_id");
            if (organizerId > 0) e.organizer = User.load(organizerId);

            events.add(e);
        }
    });

    for (Event e : events) {
        e.services = Service.loadServicesForEvent(e.id);
    }
    return events;
}

private static Event loadEventByQuery(String query, Object param) {
    final Event[] eventHolder = new Event[1];
    PersistenceManager.executeQuery(query, new ResultHandler() {
        @Override
        public void handle(ResultSet rs) throws SQLException {
            Event e = new Event();
            e.id = rs.getInt("id");
            e.name = rs.getString("name");

            // RIPRISTINIAMO IL METODO DI LETTURA ORIGINALE (E CORRETTO)
            e.dateStart = Date.valueOf(rs.getString("date_start"));
            String dateEndString = rs.getString("date_end");
            if (dateEndString != null) {
                e.dateEnd = Date.valueOf(dateEndString);
            }

            e.expectedPersons = rs.getInt("expected_persons");
            e.location = rs.getString("location");
            e.notes = rs.getString("notes");
            
            int chefId = rs.getInt("chef_id");
            if (chefId > 0) e.chef = User.load(chefId);
            
            int organizerId = rs.getInt("organizer_id");
            if (organizerId > 0) e.organizer = User.load(organizerId);
            
            eventHolder[0] = e;
        }
    }, param);
    
    if (eventHolder[0] != null) {
        eventHolder[0].services = Service.loadServicesForEvent(eventHolder[0].id);
    }
    return eventHolder[0];
}

    public static Event loadById(int id) {
        String query = "SELECT * FROM Events WHERE id = ?";
        return loadEventByQuery(query, id);
    }

    public static Event loadByName(String name) {
        String query = "SELECT * FROM Events WHERE name = ?";
        return loadEventByQuery(query, name);
    }

    
    
    @Override
    public String toString() {
        return "Event [id=" + id + ", name=" + name + ", dateStart=" + dateStart +
               ", services=" + (services != null ? services.size() : 0) + "]";
    }
}
