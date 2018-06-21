package ampedandwired.bamboo.plugin.mutex;

import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.chains.ChainExecution;
import com.atlassian.bamboo.chains.ChainResultsSummary;
import com.atlassian.bamboo.chains.plugins.PostChainAction;
import com.atlassian.bamboo.chains.plugins.PreChainAction;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.PlanKey;
import com.atlassian.bamboo.plan.PlanKeys;
import com.atlassian.bamboo.plan.PlanManager;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PlanMutexPreAndPostChainAction implements PreChainAction, PostChainAction {
    public static final Logger log = Logger.getLogger(PlanMutexPreAndPostChainAction.class);
    public static final String PLAN_MUTEX_KEY = "custom.bamboo.planMutex.list";
    private PlanManager planManager;

    private static ConcurrentMap<String, UUID> runningPlans = new ConcurrentHashMap<String, UUID>();
    
    @Override
    public void execute(@NotNull Chain chain, @NotNull ChainExecution chainExecution) throws Exception {
        PlanKey thisPlanKey = PlanKeys.getPlanKey(chain.getKey());
        UUID uuid = java.util.UUID.randomUUID();
        
        Map<String, String> customConfig = chain.getBuildDefinition().getCustomConfiguration();
        String planMutexKey = customConfig.get(PLAN_MUTEX_KEY);
        
        if (planMutexKey != null && !planMutexKey.trim().isEmpty()) {
          while(runningPlans.putIfAbsent(planMutexKey, uuid) != uuid)
          {
            log.info("Still waiting for mutex '" + planMutexKey + "' with plan " + thisPlanKey);
            Thread.sleep(1000);
          }
          
          log.info("Locked mutex '" + planMutexKey + "' with plan " + thisPlanKey);
        }
    }

    @Override
    public void execute(Chain chain, ChainResultsSummary chainResultsSummary, ChainExecution chainExecution) throws InterruptedException, Exception {
        PlanKey thisPlanKey = PlanKeys.getPlanKey(chain.getKey());
        Map<String, String> customConfig = chain.getBuildDefinition().getCustomConfiguration();
        String planMutexKey = customConfig.get(PLAN_MUTEX_KEY);
        
        if (planMutexKey != null && !planMutexKey.trim().isEmpty()) {
          runningPlans.remove(planMutexKey);
          log.info("Released mutex '" + planMutexKey + "' with plan " + thisPlanKey);
        }
    }
}
