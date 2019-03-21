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
    private static ConcurrentMap<String, Map<String, Long>> lockOrder = new ConcurrentHashMap<String, Map<String, Long>>();
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

    public void lockMutex( String planMutexKey, String id ) {
        log.info( ":::::: " + runningPlans.toString() );
        String msg;
        String lockedBy = runningPlans.get( planMutexKey );
        if ( lockedBy != null ) {
            if ( lockedBy.equals( id ) ) {
                msg = "====== Mutex '" + planMutexKey + "' already locked by " + lockedBy;
                log.info( msg );
                return;
            }
            msg = "====== Waiting for mutex '" + planMutexKey + "' locked by " + lockedBy;
            log.info( msg );
        }
        int i = 0;
        long myTime = System.currentTimeMillis();
        boolean imFirst = true;
        lockOrder.putIfAbsent( planMutexKey, new HashMap<String, Long>() );
        lockOrder.get( planMutexKey ).putIfAbsent( id, myTime );
        while ( true ) {
            if ( i % 10 == 0 ) {
                log.info( "****** " + i + " " + planMutexKey + ":" + id + " >>> " + runningPlans.toString() + " >>> " + lockOrder.toString() );
            }
            i++;
            if ( i > 3600 ) return; // 1 hour

            Iterator it = lockOrder.get( planMutexKey ).entrySet().iterator();
            imFirst = true;
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                if ( (Long)(pair.getValue()) < myTime ) {
                    imFirst = false;
                    break;
                }
            }
            if ( imFirst && ( runningPlans.putIfAbsent(planMutexKey, id) == id) ) {
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                return;
            }
        }
        msg = "====== Mutex '" + planMutexKey + "' was locked with id " + id;
        log.info( msg );
    }

    public void releaseMutex( String planMutexKey, String id ) {
        log.info( ":::::: " + runningPlans.toString() );
        String msg;
        if (runningPlans.remove(planMutexKey, id)) {
            msg = "====== Mutex '" + planMutexKey + "' released with id " + id;
            log.info( msg );
        } else {
            msg = "====== Mutex '" + planMutexKey + "' did not released with id " + id + " since it was claimed by " + runningPlans.get(planMutexKey);
            log.error( msg );
            //runningPlans.remove(planMutexKey, runningPlans.get(planMutexKey) );
            //msg = "=!=!=!= Mutex '" + planMutexKey + "' released with id " + runningPlans.get(planMutexKey);
        }
        lockOrder.get( planMutexKey ).remove( id );
    }

    public String get( String planMutexKey ) {
        return runningPlans.get( planMutexKey );
    }
}
