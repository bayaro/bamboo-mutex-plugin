package ampedandwired.bamboo.plugin.mutex;

import org.jetbrains.annotations.NotNull;

import com.atlassian.bamboo.chains.Chain;
import com.atlassian.bamboo.chains.ChainExecution;
import com.atlassian.bamboo.chains.ChainResultsSummary;
import com.atlassian.bamboo.chains.plugins.PostChainAction;
import com.atlassian.bamboo.chains.plugins.PreChainAction;

import java.util.Map;
import java.util.UUID;

import com.atlassian.bamboo.v2.build.agent.BuildAgentRequirementFilter;
import com.atlassian.bamboo.v2.build.agent.BuildAgent;
import com.atlassian.bamboo.v2.build.CommonContext;
import com.atlassian.bamboo.v2.build.agent.capability.MinimalRequirementSet;
import java.util.Collection;
import com.atlassian.bamboo.deployments.execution.DeploymentContext;

public class PlanMutexPreAndPostChainAction implements PreChainAction, PostChainAction, BuildAgentRequirementFilter {
    public static final String PLAN_MUTEX_KEY = "custom.bamboo.planMutex.list";

    private static PlanMutexProcessor pmProcessor = PlanMutexProcessor.getInstance();

    public Collection<BuildAgent> filter(CommonContext commonContext,
            Collection<BuildAgent> availableAgents, MinimalRequirementSet requirements) {
        if ( !(commonContext instanceof DeploymentContext) ) {
            return availableAgents;
        }
        String id = commonContext.getEntityKey().toString() + "-" + ((DeploymentContext)commonContext).getDeploymentProjectName();
        String planMutexKey = ((DeploymentContext)commonContext).getEnvironmentName();
        // kludge, there are no way to get the environment from DeploymentFinishedEvent,
        // so we store in mutex map the environment under DeploymentResultId key
        pmProcessor.deploymentMutexes.put( "" + ((DeploymentContext)commonContext).getDeploymentResultId(), planMutexKey + ":" + id );
        pmProcessor.lockMutex( planMutexKey, id );
        return availableAgents;
    }

    @NotNull
    @Override
    public void execute(@NotNull Chain chain, @NotNull ChainExecution chainExecution) {
        String id = chain.getPlanKey().toString();
        Map<String, String> customConfig = chain.getBuildDefinition().getCustomConfiguration();
        String planMutexKey = customConfig.get(PLAN_MUTEX_KEY);

        if (planMutexKey != null && !planMutexKey.trim().isEmpty()) {
            pmProcessor.lockMutex( planMutexKey, id );
            // we have the mutex, check if we are still in running state
            if(chainExecution.isStopping() || chainExecution.isStopRequested()) {
                pmProcessor.releaseMutex( planMutexKey, id );
            }
        }
    }

    @Override
    public void execute(Chain chain, ChainResultsSummary chainResultsSummary, ChainExecution chainExecution) {
        String id = chain.getPlanKey().toString();
        Map<String, String> customConfig = chain.getBuildDefinition().getCustomConfiguration();
        String planMutexKey = customConfig.get(PLAN_MUTEX_KEY);

        if (planMutexKey != null && !planMutexKey.trim().isEmpty()) {
            pmProcessor.releaseMutex( planMutexKey, id );
        }
    }
}
