package redis.embedded;

import redis.embedded.ports.EphemeralPortProvider;
import redis.embedded.ports.PredefinedPortProvider;
import redis.embedded.ports.SequencePortProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class RedisClusterBuilder {
    private RedisServerBuilder serverBuilder = new RedisServerBuilder();
    private PortProvider replicationGroupPortProvider = new SequencePortProvider(6379);
    private final List<ReplicationGroup> groups = new LinkedList<ReplicationGroup>();

	private final List<Integer> mastersPorts = new ArrayList<>();
	private final List<Integer> slavesPorts = new ArrayList<>();

    public RedisClusterBuilder withServerBuilder(RedisServerBuilder serverBuilder) {
        this.serverBuilder = serverBuilder;
        return this;
    }

    public RedisClusterBuilder serverPorts(Collection<Integer> ports) {
        this.replicationGroupPortProvider = new PredefinedPortProvider(ports);
        return this;
    }

    public RedisClusterBuilder ephemeralServers() {
        this.replicationGroupPortProvider = new EphemeralPortProvider();
        return this;
    }


    public RedisClusterBuilder ephemeral() {
        ephemeralServers();
        return this;
    }

    public RedisClusterBuilder replicationGroup(String masterName, int slaveCount) {
        this.groups.add(new ReplicationGroup(masterName, slaveCount, this.replicationGroupPortProvider));
        return this;
    }

    public RedisCluster build() {
        final List<Redis> servers = buildServers();
        return new RedisCluster(servers,
			this.mastersPorts, this.slavesPorts);
    }

    private List<Redis> buildServers() {
        List<Redis> servers = new ArrayList<Redis>();
        for(ReplicationGroup g : groups) {
			servers.add(buildMaster(g));
			buildSlaves(servers, g);
        }
        return servers;
    }

    private void buildSlaves(List<Redis> servers, ReplicationGroup g) {
        for (Integer slavePort : g.slavePorts) {
        	this.slavesPorts.add(slavePort);
            serverBuilder.reset();
            serverBuilder.port(slavePort);
            final RedisServer slave = serverBuilder.
				setting("cluster-enabled yes").
				setting("cluster-config-file nodes-slave-" + slavePort + ".conf").
				setting("cluster-node-timeout 5000").
				setting("appendonly no").
				build();
            servers.add(slave);
        }
    }

    private Redis buildMaster(ReplicationGroup g) {
		this.mastersPorts.add(g.masterPort);
        serverBuilder.reset();
        return serverBuilder.
			port(g.masterPort).
			setting("cluster-enabled yes").
			setting("cluster-config-file nodes-slave-" + g.masterPort + ".conf").
			setting("cluster-node-timeout 5000").
			setting("appendonly no").
			build();
    }

    private static class ReplicationGroup {
        private final String masterName;
        private final int masterPort;
        private final List<Integer> slavePorts = new LinkedList<Integer>();

        private ReplicationGroup(String masterName, int slaveCount, PortProvider portProvider) {
            this.masterName = masterName;
            masterPort = portProvider.next();
            while (slaveCount-- > 0) {
                slavePorts.add(portProvider.next());
            }
        }
    }
}
