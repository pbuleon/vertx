# cloner le projet https://github.com/big-data-europe/docker-hbase
# les port 50070 et 50075 sont reservé sous windows .. remplacer dans docker-hbase\docker-compose-distributed-local.yml
# '- 50070:50070' par '- 40070:50070'
# '- 50075:50075' par '- 40075:50075'

# vdeployer hbase zookeper hadoop ...
docker-compose -f docker-compose-distributed-local.yml up -d



# Creer les node zookeeper : 
docker exec -i zoo zkCli.sh
create /zk
create /zk/kafka
create /zk/kafka/prod
create /zk/warp
create /zk/warp/prod
create /zk/warp/prod/services
create /zk/warp/prod/plasma
create /zk/hbase
create /zk/hbase/prod

# lancer kafka (0.10.2.1,  le 0.8.2.2 n'est pas dispo ...): 
docker run --name kafka-server1 --network docker-hbase_default -e ALLOW_PLAINTEXT_LISTENER=yes -e KAFKA_CFG_ZOOKEEPER_CONNECT=zoo:2181 -e KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092  -p 9092:9092 -d bitnami/kafka:latest

# Reconfigurer kafka en 0.8.2 (pas possible en variable de conf):
# se connecter sur le container:
docker exec -it kafka-server1 bash
cd /opt/bitnami/kafka/config
echo 'log.message.format.version=0.8.2' >> server.properties
echo 'inter.broker.protocol.version=0.8.2' >> server.properties
exit
# redemarrer le container : 
docker restart kafka-server1

# creer les topic (ici sans replication et 1 partition):
docker exec -it kafka-server1 kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic metadata
docker exec -it kafka-server1 kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic data
docker exec -it kafka-server1 kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic throttling
docker exec -it kafka-server1 kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic webcall
docker exec -it kafka-server1 kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic runner
docker exec -it kafka-server1 kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic plasmafeXX


# Ajouter les custome filter de warp10 dans hbase
# apres avoir buildé warp10
docker cp ./hbaseFilters/build/libs/hbaseFilters-2.5.0-2-g3d3d966d.jar hbase-master:/opt/hbase-1.2.6/lib/hbaseFilters-2.5.0-2-g3d3d966d.jar

# redemarre hbase master
docker restart hbase-master

# ajouter la ligne
# <property><name>hbase.coprocessor.region.classes</name><value>org.apache.hadoop.hbase.coprocessor.example.BulkDeleteEndpoint</value></property>
# dans le ficchier /opt/hbase-1.2.6/conf/hbase-site.xml du container  hbase-regionserver

# redemarrer : hbase-regionserver
docker restart hbase-regionserver



# demarre warp10-ingress
docker run -it -v C:\cygwin64\home\buleon1\git\vertx\warp10Configs\template:/opt/warp10-2.4.0/etc/conf.d --network="docker-hbase_default" --name warp10_ingress warp10io/warp10:latest bash
java -cp /opt/warp10-2.4.0/bin/warp10-2.4.0.jar io.warp10.WarpDist /opt/warp10/etc/conf.d/*
