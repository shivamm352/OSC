package com.springosc.session.kakastreamconfig;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.stereotype.Service;
import java.util.Properties;


@Slf4j
@Service
public class KafkaKTableImpl {
    private final KafkaStreams streams;
    public ReadOnlyKeyValueStore<String, String> userSessionStore;

    public KafkaKTableImpl() {

        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "ktable");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> userSessionsStream = builder.stream("session-stream",
                Consumed.with(Serdes.String(), Serdes.String()));

        userSessionsStream.foreach((key, value) -> System.out.println("==Key:== " + key + ",== Value:== " + value));

        KTable<String, String> sessionTable = userSessionsStream
                .groupByKey()
                .reduce((oldValue, newValue) -> newValue, Materialized.as("user-session-store"));

        streams = new KafkaStreams(builder.build(), properties);
        streams.setStateListener((newState, oldState) -> {
            if (newState == KafkaStreams.State.RUNNING && oldState == KafkaStreams.State.REBALANCING) {
                userSessionStore = streams.store(StoreQueryParameters.
                        fromNameAndType("user-session-store",
                                QueryableStoreTypes.keyValueStore()));
            }
        });

        streams.start();
    }

    public void close() {
        if (streams != null) {
            streams.close();
        }
    }

    public boolean  getSessionStatus(String userId, String device)  {
        String key = userId + "_" + device;
        String sessionData = userSessionStore.get(key);
        if(sessionData!= null) {
            return sessionData.equals("true") ? true : false;
        }
        return false;
    }
}

