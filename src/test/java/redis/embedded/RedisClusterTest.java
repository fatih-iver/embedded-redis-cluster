package redis.embedded;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.*;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class RedisClusterTest {
	private RedisCluster cluster;

	private RedisCluster ephemeralCluster;

    @Before
    public void setUp() throws Exception {
		final List<Integer> group1 = Arrays.asList(7001, 8001);
		final List<Integer> group2 = Arrays.asList(7002, 8002);
		final List<Integer> group3 = Arrays.asList(7003, 8003);
		/*
		 * creates a cluster with quorum size of 2 and 3 replication groups,
		 * each with one master and one slave
		 */
		cluster = RedisCluster.builder()
			.serverPorts(group1).replicationGroup("master1", 1)
			.serverPorts(group2).replicationGroup("master2", 1)
			.serverPorts(group3).replicationGroup("master3", 1)
			.build();
		cluster.start();

		// and for ephemeral cluster
		ephemeralCluster = RedisCluster.builder().ephemeral()
			.replicationGroup("master1", 1)
			.replicationGroup("master2", 1)
			.replicationGroup("master3", 1)
			.build();
		ephemeralCluster.start();
    }

    @Test
    public void testRedisClusterWithPredefinedPorts() throws Exception {
		JedisCluster jc = null;

		try {
			jc = new JedisCluster(new HostAndPort("127.0.0.1", 7001));
			jc.set("somekey", "somevalue");
			assertEquals("the value shoudl be equal", "somevalue", jc.get("somekey"));
		} finally {
			if (jc != null) {
				jc.close();
			}
		}
    }

	@Test
	public void testRedisClusterWithEphemeralPorts() throws Exception {
		JedisCluster jc = null;

		try {
			jc = new JedisCluster(new HostAndPort("127.0.0.1", this.ephemeralCluster.ports().get(0)));
			jc.set("somekey", "somevalue");
			assertEquals("the value shoudl be equal", "somevalue", jc.get("somekey"));
		} finally {
			if (jc != null) {
				jc.close();
			}
		}
	}

    @After
    public void tearDown() throws Exception {
		this.cluster.stop();
    }
}
