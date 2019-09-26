embedded-redis-cluster
======================

Embedded Redis Cluster for Java integration testing

Fork Notes
==============
This repository clones from [kstyrc](https://github.com/kstyrc/embedded-redis) original repository and [ozimov](https://github.com/ozimov/embedded-redis) clone repository.
The main goal of this fork is to provide *real* redis cluster support, allowing access with redis-cluster API, such as JedisCluster or (more standard) redis-cli `-c` option.

Maven dependency
==============

Not yet

Usage
==============

Running Simple RedisServer is as simple as:
```java
RedisServer redisServer = new RedisServer(6379);
redisServer.start();
// do some work
redisServer.stop();
```

You can also provide RedisServer with your own executable:
```java
// 1) given explicit file (os-independence broken!)
RedisServer redisServer = new RedisServer("/path/to/your/redis", 6379);

// 2) given os-independent matrix
RedisExecProvider customProvider = RedisExecProvider.defaultProvider()
  .override(OS.UNIX, "/path/to/unix/redis")
  .override(OS.WINDOWS, Architecture.x86, "/path/to/windows/redis")
  .override(OS.Windows, Architecture.x86_64, "/path/to/windows/redis")
  .override(OS.MAC_OS_X, Architecture.x86, "/path/to/macosx/redis")
  .override(OS.MAC_OS_X, Architecture.x86_64, "/path/to/macosx/redis")
  
RedisServer redisServer = new RedisServer(customProvider, 6379);
```

You can also use fluent API to create RedisServer:
```java
RedisServer redisServer = RedisServer.builder()
  .redisExecProvider(customRedisProvider)
  .port(6379)
  .slaveOf("locahost", 6378)
  .configFile("/path/to/your/redis.conf")
  .build();
```

Or even create simple redis.conf file from scratch:
```java
RedisServer redisServer = RedisServer.builder()
  .redisExecProvider(customRedisProvider)
  .port(6379)
  .setting("bind 127.0.0.1") // good for local development on Windows to prevent security popups
  .slaveOf("locahost", 6378)
  .setting("daemonize no")
  .setting("appendonly no")
  .setting("maxmemory 128M")
  .build();
```

## Setting up a cluster

This Redis cluster supports multi-node master and slave. Behind the scene, it will automatically setup the cluster using `CLUSTER MEET` and `CLUSTER REPLICATE`

#### Using ephemeral ports
A simple redis integration test with Redis cluster on ephemeral ports, with setup similar to that from production would look like this:
```java
public class SomeIntegrationTestThatRequiresRedis {
  private RedisCluster cluster;

  @Before
  public void setup() throws Exception {
    /*
     * creates a cluster with quorum size of 2 and 3 replication groups, 
     * each with one master and one slave
     */
    cluster = RedisCluster.builder().ephemeral()
                    .replicationGroup("master1", 1)
                    .replicationGroup("master2", 1)
                    .replicationGroup("master3", 1)
                    .build();
    cluster.start();
  }
  
  @Test
  public void test() throws Exception {
    JedisCluster jc = new JedisCluster(new HostAndPort("127.0.0.1", cluster.ports().get(0)));
    // test your code
  }
  
  @After
  public void tearDown() throws Exception {
    cluster.stop();
  }
}
```

#### Retrieving ports
The above example starts Redis cluster on ephemeral ports, which you can later get with ```cluster.ports()```,
which will return a list of all ports of the cluster. You can also get ports of sentinels with ```cluster.sentinelPorts()```
or servers with ```cluster.serverPorts()```. ```JedisUtil``` class contains utility methods for use with Jedis client.

#### Using predefined ports
You can also start Redis cluster on predefined ports and even mix both approaches:
```java
public class SomeIntegrationTestThatRequiresRedis {
  private RedisCluster cluster;

  @Before
  public void setup() throws Exception {
    final List<Integer> sentinels = Arrays.asList(26739, 26912);
    final List<Integer> group1 = Arrays.asList(6667, 6668);
    final List<Integer> group2 = Arrays.asList(6387, 6379);
    /*
     * creates a cluster with quorum size of 2 and 3 replication groups, 
     * each with one master and one slave
     */
    cluster = RedisCluster.builder()
          .serverPorts(group1).replicationGroup("master1", 1)
          .serverPorts(group2).replicationGroup("master2", 1)
          .ephemeralServers().replicationGroup("master3", 1)
          .build();
    cluster.start();
  }
}
```
The above will create and start a cluster with sentinels on ports ```26739, 26912```, first replication group on ```6667, 6668```,
second replication group on ```6387, 6379``` and third replication group on ephemeral ports.

Redis version
==============

Because this fork aims to support <i>real</i> redis cluster, the redis used should be version 3 or higher

The default built-in redis here are as follows:
- Linux/Unix (Not yet)
- OSX/macOS (Not yet)
- Windows: 3.0.503

However, you should provide RedisServer with redis executable if you need specific version.

License
==============
Licensed under the Apache License, Version 2.0

Contributors
==============
 * Krzysztof Styrc ([@kstyrc](http://github.com/kstyrc))
 * Piotr Turek ([@turu](http://github.com/turu))
 * anthonyu ([@anthonyu](http://github.com/anthonyu))
 * Artem Orobets ([@enisher](http://github.com/enisher))
 * Sean Simonsen ([@SeanSimonsen](http://github.com/SeanSimonsen))
 * Rob Winch ([@rwinch](http://github.com/rwinch))

Changelog
==============

### 0.6
 * Support JDK 6 +

### 0.5
 * OS detection fix
 * redis binary per OS/arch pair
 * Updated to 2.8.19 binary for Windows

### 0.4 
 * Updated for Java 8
 * Added Sentinel support
 * Ability to create arbitrary clusters on arbitrary (ephemeral) ports
 * Updated to latest guava 
 * Throw an exception if redis has not been started
 * Redis errorStream logged to System.out

### 0.3
 * Fluent API for RedisServer creation

### 0.2
 * Initial decent release
