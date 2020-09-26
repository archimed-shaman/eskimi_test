# Solution

## Intro

> ClickHouse is a fast open-source OLAP database management system. \
It is column-oriented and allows to generate analytical reports using SQL queries in real-time. \
// https://clickhouse.tech 

Technical task sounds like a typical task for analytics instrument, so I dare to offer ClickHouse-based solution.

### Advantages of ClickHouse
  - You can store raw data and make additional tables/views. In combination with sampling, preaggregating, sharding it allows to make real-time API which responds in seconds. Yandex Metrika (similar to Google Analytics), SEMrush Traffic Analytics are proves.
  - Mostly people is familiar with SQL unlikely with Spark. Analytics department of SEMrush had direct access to Traffic Analytics ClickHouse to perform researches.
  - You don't need to develop new Spark job for each new request. SQL queries are more appropriate.
  - It's free, open source and has a large community :) Developers are in-touch in different messengers.
  
### Disadvantages of ClickHouse
  - Weak scalability. CH has no well-working resharding mechanism, so you should strongly plan you data volumes.
  - Data skew. If you have data, sharded by domain, tons of events regarding google.com will be placed to the same shard.
  - No data mutations (no `UPDATE/DELETE`). You have only `DROP PARTITION`. It is not a real problem for analytics instruments.
  - No optimizations for `JOIN` operations. It is not a problem, if you make optiomizations 'by hand' in SQL query (e.g. filter by key).
  - It may be hard to change table structures - you need to perform data migrations.

### Conclusion

Hadoop/Spark is more useful in case of various one-off researches which deals with heterogeneous data with no demand to execution time/latency. ClickHouse is good for real-time analytics instruments with defined set of reports (something like Google Analytics). The usual CH data flow includes preliminary data processing, e.g. with the help of Spark - it may help to parse, filter and uniformize data. \
\
[ClickHouse Documentation](https://clickhouse.tech/docs/en/)

# Infrastructure

## ClickHouse server
Official Docker images are available at https://hub.docker.com/r/yandex/clickhouse-server/ \
\
Run local clickhouse cluster with docker-compose command:
```
docker-compose up
```

## ClickHouse client
Run official clickhouse client and connect to the first node with command
```
docker run -it --rm --net clickhouse --link eskimi_test_ch_shard1_1:clickhouse-server yandex/clickhouse-client --host eskimi_test_ch_shard1_1 --port 9000
```
Or to the second through the first one:
```
docker run -it --rm --net clickhouse --link eskimi_test_ch_shard1_1:clickhouse-server yandex/clickhouse-client --host eskimi_test_ch_shard2_1 --port 9000
```

## DB configuration
Test ClickHouse database has two shards to demonstrate mechanism of distributed queries. In real life usually there are several shard with two (or more) replicas within. ClickHouse needs ZooKeeper cluster for replication. Yandex is considering Etcd as alternative to ZooKeeper at the moment.
```
Shard_1 Replica_1 - Shard_1 Replica_2 
Shard_2 Replica_1 - Shard_2 Replica_2
...
Shard_N Replica_1 - Shard_N Replica_2
```

# Run

  1. Run ClickHouse & ZooKeper. Just run 

```
$ docker-compose up
``` 
This command will download all necessary containers from DockerHub and launch one ZooKeeper and two ClickHouse servers with all necessary configs.

  2. To simplify proceess, make some aliases: 
```
$ alias clickhouse-client="docker run -it --rm --net clickhouse --link eskimi_test_ch_shard1_1:clickhouse-server yandex/clickhouse-client"
$ alias clickhouse-pipe="docker run -a stdin -a stdout -i --rm --net clickhouse --link eskimi_test_ch_shard1_1:clickhouse-server yandex/clickhouse-client"
$ clickhouse-client --host eskimi_test_ch_shard1_1 --port 9000
ClickHouse client version 20.9.2.20 (official build).
Connecting to eskimi_test_ch_shard1_1:9000 as user default.
Connected to ClickHouse server version 20.9.2 revision 54439.
ch1 :)
```
    
  3. Create databases and tables. Just run sql script: 
```
$ clickhouse-pipe --host eskimi_test_ch_shard1_1 --port 9000 --multiquery < sql/constructors.sql
```
  4. Build data generator
```
$ cd generator && sbt assembly
```
    
  5. Generate data (e.g. full 2020.09), by default - 10000000 events per day.
```
$ for i in {01..30}; do scala generator/target/scala-2.13/generator-assembly-0.1.0-SNAPSHOT.jar 2020-09-$i 10000000; done
2020-09-26 13:04:54.920 [main] INFO  com.github.generator.main$ - generator started with args [day: Tue Sep 01 00:00:00 MSK 2020; rows: 10000000; dir: generated]
2020-09-26 13:05:06.214 [scala-execution-context-global-30] INFO  com.github.generator.main$ - batch 5000000 complete
2020-09-26 13:05:06.273 [scala-execution-context-global-22] INFO  com.github.generator.main$ - batch 6000000 complete
2020-09-26 13:05:06.330 [scala-execution-context-global-23] INFO  com.github.generator.main$ - batch 3000000 complete
2020-09-26 13:05:06.362 [scala-execution-context-global-25] INFO  com.github.generator.main$ - batch 2000000 complete
2020-09-26 13:05:06.363 [scala-execution-context-global-28] INFO  com.github.generator.main$ - batch 9000000 complete
2020-09-26 13:05:06.379 [scala-execution-context-global-26] INFO  com.github.generator.main$ - batch 7000000 complete
2020-09-26 13:05:06.393 [scala-execution-context-global-24] INFO  com.github.generator.main$ - batch 8000000 complete
2020-09-26 13:05:06.395 [scala-execution-context-global-21] INFO  com.github.generator.main$ - batch 1000000 complete
2020-09-26 13:05:06.424 [scala-execution-context-global-29] INFO  com.github.generator.main$ - batch 4000000 complete
2020-09-26 13:05:06.445 [scala-execution-context-global-27] INFO  com.github.generator.main$ - batch 10000000 complete
2020-09-26 13:05:06.454 [main] INFO  com.github.generator.main$ - generator stopped

...

$ du -h --max-depth=1 generated 
795M    generated/2020-09-26
795M    generated/2020-09-09
795M    generated/2020-09-15
795M    generated/2020-09-04
795M    generated/2020-09-20
795M    generated/2020-09-08
795M    generated/2020-09-24
795M    generated/2020-09-22
795M    generated/2020-09-27
795M    generated/2020-09-21
795M    generated/2020-09-06
795M    generated/2020-09-28
795M    generated/2020-09-12
795M    generated/2020-09-25
795M    generated/2020-09-02
795M    generated/2020-09-11
795M    generated/2020-09-23
795M    generated/2020-09-10
795M    generated/2020-09-18
795M    generated/2020-09-29
795M    generated/2020-09-13
795M    generated/2020-09-01
795M    generated/2020-09-03
795M    generated/2020-09-14
795M    generated/2020-09-19
795M    generated/2020-09-07
795M    generated/2020-09-16
795M    generated/2020-09-05
795M    generated/2020-09-17
795M    generated/2020-09-30
24G     generated
```
    
  6. Insert data into ClickHouse:
```
$ for day in {01..30}; do for file in $(ls generator/generated/2020-09-$day/part_*); do cat $file | clickhouse-pipe --host eskimi_test_ch_shard1_1 --port 9000 --query="INSERT INTO logs.visit_log_all FORMAT CSVWithNames"; done; done
```

# Data reports

Now, let's check data
```
ch1 :) select count() from logs.visit_log_all

SELECT count()
FROM logs.visit_log_all

┌───count()─┐
│ 300000000 │
└───────────┘

1 rows in set. Elapsed: 0.013 sec.
```

## Which are the most visited sites last month?

```
SELECT 
    site_id,
    uniq(dmp_id) AS users,
    count() AS visits
FROM logs.visit_log_all
PREWHERE toStartOfMonth(event_date) = '2020-09-01'
GROUP BY site_id
ORDER BY users DESC
LIMIT 10
SETTINGS max_threads = 8

┌─site_id─┬──users─┬─visits─┐
│      -1 │ 559398 │ 811610 │
│   12563 │  24150 │  24417 │
│   12664 │  24066 │  24346 │
│   12516 │  24062 │  24353 │
│   12372 │  24049 │  24343 │
│   12356 │  24032 │  24336 │
│   12175 │  24030 │  24305 │
│   12702 │  24013 │  24314 │
│   12090 │  23979 │  24273 │
│   12603 │  23979 │  24260 │
└─────────┴────────┴────────┘

10 rows in set. Elapsed: 34.330 sec. Processed 300.00 million rows, 6.00 GB (8.74 million rows/s., 174.77 MB/s.) 
```

It's not quite fast, but it is not optimized snapshot. Let's make pre-aggregated metrics for sites day-by-day. Distributed-to-distributed insert reshards data by siteID. It makes data imbalanced, e.g. for google. On the other hand, this makes possible some tricks, like `distributed_group_by_no_merge` option.
```
$ for day in {01..30}; do clickhouse-client --host eskimi_test_ch_shard1_1 --port 9000 --query="INSERT INTO logs.site_metrics_agr_all SELECT toDate(event_date) AS date, site_id, gender, country, city, uniqState(dmp_id) AS users_agr, countState() AS visits_agr FROM logs.visit_log_all PREWHERE date = '2020-09-$day' GROUP BY date, site_id, gender, country, city"; done
```
Now:
```
SELECT *
FROM 
(
    SELECT 
        toStartOfMonth(date) AS month,
        site_id,
        uniqMerge(users_agr) AS users,
        countMerge(visits_agr) AS visits
    FROM logs.site_metrics_agr_all
    PREWHERE month = '2020-09-01'
    GROUP BY 
        month,
        site_id
    SETTINGS distributed_group_by_no_merge = 1, max_threads = 8, distributed_aggregation_memory_efficient = 1
)
ORDER BY users DESC
LIMIT 10
SETTINGS distributed_aggregation_memory_efficient = 1

┌──────month─┬─site_id─┬──users─┬─visits─┐
│ 2020-09-01 │      -1 │ 559398 │ 811610 │
│ 2020-09-01 │   12563 │  24150 │  24417 │
│ 2020-09-01 │   12664 │  24066 │  24346 │
│ 2020-09-01 │   12516 │  24062 │  24353 │
│ 2020-09-01 │   12372 │  24049 │  24343 │
│ 2020-09-01 │   12356 │  24032 │  24336 │
│ 2020-09-01 │   12175 │  24030 │  24305 │
│ 2020-09-01 │   12702 │  24013 │  24314 │
│ 2020-09-01 │   12090 │  23979 │  24273 │
│ 2020-09-01 │   12603 │  23979 │  24260 │
└────────────┴─────────┴────────┴────────┘

10 rows in set. Elapsed: 13.368 sec. Processed 223.75 million rows, 36.67 GB (16.74 million rows/s., 2.74 GB/s.) 
```

Looks better. However, data was not generated perfectly - site_metrics_agr_all contains 223751273 entries, which is 74% of total data. Anyway, some data reduce and repartition made it faster twice. Also, rank is "heavy" request, which needs likely fullscan and usually requires mounthly precalculated table for real-time reports.

## How many unique users we have who are from Lithuania are males and have visited siteId=37 in last month?

```
SELECT 
    toStartOfMonth(date) AS month,
    site_id,
    uniqMerge(users_agr) AS users
FROM logs.site_metrics_agr_all
PREWHERE site_id = 12563
WHERE (country = 'LI') AND (gender = 1)
GROUP BY 
    month,
    site_id

┌──────month─┬─site_id─┬─users─┐
│ 2020-09-01 │   12563 │    36 │
└────────────┴─────────┴───────┘

1 rows in set. Elapsed: 0.050 sec. Processed 17.15 thousand rows, 1.16 MB (340.47 thousand rows/s., 22.95 MB/s.)
```

## How many unique users did not visit siteId=13 for the last 14 days?

```
SELECT 
    uniq(dmp_id) AS visited,
    uniqIf(dmp_id, mark = 0) AS non_visited
FROM 
(
    SELECT 
        dmp_id,
        countIf(site_id = 12563) AS mark
    FROM logs.visit_log_all
    PREWHERE toDate(event_date) BETWEEN '2020-09-11' AND '2020-09-26'
    GROUP BY dmp_id
    SETTINGS distributed_group_by_no_merge = 1, max_threads = 8, distributed_aggregation_memory_efficient = 1
)

┌─visited─┬─non_visited─┐
│  999657 │      987030 │
└─────────┴─────────────┘

1 rows in set. Elapsed: 3.306 sec. Processed 160.02 million rows, 3.20 GB (48.40 million rows/s., 968.01 MB/s.)
```

## Which keywords were mostly used last month?

Let's find it as is. It is full scan by 300M rows. In real life, sampling or additional table/view can be applied.
```
SELECT 
    keyword,
    count() AS usage
FROM 
(
    SELECT arrayJoin(keywords) AS keyword
    FROM logs.visit_log_all
    PREWHERE toStartOfMonth(event_date) = '2020-09-01'
)
GROUP BY keyword
ORDER BY usage DESC
LIMIT 10
SETTINGS max_threads = 8

┌─keyword─┬───usage─┐
│ 405     │ 1623702 │
│ 619     │ 1623508 │
│ 498     │ 1623356 │
│ 545     │ 1623331 │
│ 239     │ 1623284 │
│ 159     │ 1623243 │
│ 973     │ 1623142 │
│ 162     │ 1623127 │
│ 733     │ 1623101 │
│ 577     │ 1623011 │
└─────────┴─────────┘

10 rows in set. Elapsed: 25.374 sec. Processed 300.00 million rows, 22.86 GB (11.82 million rows/s., 901.00 MB/s.)
```
