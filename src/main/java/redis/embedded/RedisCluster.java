package redis.embedded;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.embedded.exceptions.EmbeddedRedisException;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.IntStream;

public class RedisCluster implements Redis {
    private final List<Redis> servers = new LinkedList<Redis>();

	private List<Integer> mastersPorts;
	private List<Integer> slavesPorts;

    RedisCluster(
		List<Redis> servers,
		List<Integer> mastersPorts,
		List<Integer> slavesPorts) {
        this.servers.addAll(servers);
        this.mastersPorts = mastersPorts;
        this.slavesPorts = slavesPorts;
    }

    @Override
    public boolean isActive() {
        for(Redis redis : servers) {
            if(!redis.isActive()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void start() throws EmbeddedRedisException, InterruptedException {
        for(Redis redis : servers) {
            redis.start();
        }

        /*
         * Here, we need to manually setup the cluster
         * Because we dont want to add another dependency
         * like ruby's gem create-cluster
         */
		Integer clusterMeetTarget = mastersPorts.get(0);
		Jedis j = null;

		/*
		 * for every master
		 * meet them (except the `seed` master)
		 * and add their slots manually
		 * using pipeline for faster execution
		 */
		try{
			for(Integer i = 0; i < mastersPorts.size(); i++) {
				try {
					j = new Jedis("127.0.0.1", mastersPorts.get(i));

					if(!i.equals(clusterMeetTarget)){
						j.clusterMeet("127.0.0.1", clusterMeetTarget);
					}

					Integer finalI = i;
					Pipeline jp = j.pipelined();
					IntStream.range(0, 16384).filter(
						is -> Integer.valueOf(is % mastersPorts.size()).equals(finalI)
					).forEach(jp::clusterAddSlots);
					jp.sync();

				} catch (Exception e) {
					EmbeddedRedisException err = new EmbeddedRedisException(
						"Failed creating master instance at port: "+ mastersPorts.get(i));
					err.setStackTrace(e.getStackTrace());
					throw err;
				} finally {
					if(j!=null) {
						j.close();
						j=null;
					}
				}
			}
		} catch (EmbeddedRedisException e) {
			this.stop();
			throw e;
		}


		/*
		 * Preventing timing issues
		 * Because redis cluster setup need time
		 * it is NOT instantaneous
		 */
		Thread.sleep(mastersPorts.size() * 300);

		/*
		 * meet every slave to the MEET target
		 */
		try {
			for(Integer sp : slavesPorts) {
				try {
					j = new Jedis("127.0.0.1", sp);
					j.clusterMeet("127.0.0.1", clusterMeetTarget);
				} catch (Exception e) {
					EmbeddedRedisException err = new EmbeddedRedisException(
						"Failed creating slave instance at port: "+ sp);
					err.setStackTrace(e.getStackTrace());
					throw err;
				} finally {
					if(j!=null) {
						j.close();
						j=null;
					}
				}
			}
		} catch (EmbeddedRedisException e) {
			this.stop();
			throw e;
		}


		/*
		 * also prevent timing issues
		 */
		Thread.sleep(500);
    }

    @Override
    public void stop() throws EmbeddedRedisException {
        for(Redis redis : servers) {
            redis.stop();
        }
    }

    @Override
    public List<Integer> ports() {
        List<Integer> ports = new ArrayList<Integer>();
        ports.addAll(serverPorts());
        return ports;
    }

    public List<Integer> serverPorts() {
        List<Integer> ports = new ArrayList<Integer>();
        ports.addAll(mastersPorts);
        ports.addAll(slavesPorts);
        return ports;
    }

    public static RedisClusterBuilder builder() {
        return new RedisClusterBuilder();
    }
}
