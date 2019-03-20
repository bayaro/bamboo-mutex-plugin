package ampedandwired.bamboo.plugin.mutex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Map;

import com.atlassian.bamboo.event.*;
import com.atlassian.bamboo.v2.build.events.*;
import com.atlassian.bamboo.deployments.execution.events.*;

import com.atlassian.event.Event;

import com.atlassian.event.api.EventListener;
//import com.atlassian.event.api.InlineEventListener;

public class PlanMutexActivityListener { //implements InlineEventListener {
    private static PlanMutexProcessor pmProcessor = PlanMutexProcessor.getInstance();

	@EventListener
	public void onDeploymentFinishedEvent(final DeploymentFinishedEvent event) {
        String mutexInfoKey = "" + event.getDeploymentResultId();
        String mutexInfo = pmProcessor.deploymentMutexes.get( mutexInfoKey );
        String[] mutex = mutexInfo.split( ":" );
        pmProcessor.releaseMutex( mutex[0], mutex[1] );
        pmProcessor.deploymentMutexes.remove( mutexInfoKey );
    }
}
