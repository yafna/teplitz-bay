package some.data.event.listener;

import some.data.event.GenericEvent;
import some.data.provider.CustomDataSource;

public class EventCreatedListener {

    private CustomDataSource dataSource;

    public EventCreatedListener(CustomDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void onCreate(GenericEvent event){
        dataSource.saveEvent(event);
    }
}
