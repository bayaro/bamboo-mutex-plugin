package ampedandwired.bamboo.plugin.mutex;

import org.apache.log4j.Logger;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public final class PlanMutexProcessor {
    private static final Logger log = Logger.getLogger(PlanMutexProcessor.class);
    private static ConcurrentMap<String, String> runningPlans = new ConcurrentHashMap<String, String>();
    public static Map<String, String> deploymentMutexes = new HashMap<String, String>();

    public static volatile PlanMutexProcessor _instance = null;

    public PlanMutexProcessor() {}

    public static synchronized PlanMutexProcessor getInstance() {
        if (_instance == null)
            synchronized (PlanMutexProcessor.class) {
                if (_instance == null)
                    _instance = new PlanMutexProcessor();
            }
        return _instance;
    }

    private void logg( String msg ) {
        log.info( msg );
    }

    public void lockMutex( String planMutexKey, String id ) {
        logg( "****** L:" + planMutexKey + ":" + id );
        logg( ":::::: deploymentMutexes: " + deploymentMutexes.toString() );
        logg( ":::::: runningPlans: " + runningPlans.toString() );
        String lockedBy = runningPlans.get( planMutexKey );
        if ( lockedBy != null ) {
            if ( lockedBy.equals( id ) ) {
                logg( "====== Mutex '" + planMutexKey + "' already locked by " + lockedBy );
                return;
            }
            logg( "====== Waiting for mutex '" + planMutexKey + "' locked by " + lockedBy );
        }
        int i = 0;
        while ( true ) {
            if ( i % 10 == 0 ) {
                logg( "****** " + i + " " + planMutexKey + ":" + id );
                logg( ":::::: runningPlans: " + runningPlans.toString() );
            }
            i++;
            if ( i > 3600 ) return; // 1 hour

            if ( runningPlans.putIfAbsent(planMutexKey, id) == id ) {
                logg( ":::::: runningPlans: " + runningPlans.toString() );
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                return;
            }
        }
        logg( "====== Mutex '" + planMutexKey + "' was locked with id " + id );
    }

    public void releaseMutex( String planMutexKey, String id ) {
        logg( "****** R:" + planMutexKey + ":" + id );
        logg( ":::::: deploymentMutexes: " + deploymentMutexes.toString() );
        logg( ":::::: runningPlans: " + runningPlans.toString() );
        if (runningPlans.remove(planMutexKey, id)) {
            logg( "====== Mutex '" + planMutexKey + "' released with id " + id );
        } else {
            log.error( "====== Mutex '" + planMutexKey + "' did not released with id " + id + " since it was claimed by " + runningPlans.get(planMutexKey) );
            //runningPlans.remove(planMutexKey, runningPlans.get(planMutexKey) );
            //msg = "=!=!=!= Mutex '" + planMutexKey + "' released with id " + runningPlans.get(planMutexKey);
        }
    }
}
