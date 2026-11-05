package com.oriolvc.topicman.messaging.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.oriolvc.topicman.messaging.application.model.ListBrokersCommand;
import com.oriolvc.topicman.messaging.application.model.SupportedBroker;
import com.oriolvc.topicman.shared.domain.BrokerType;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListBrokersUseCaseTest {

    @Test
    void listsTheCapabilitiesOfEveryBrokerWithAProducerAndAReader() {
        var useCase = new ListBrokersUseCase(
                List.of(new FakeMessageProducer(BrokerType.RABBITMQ), new FakeMessageProducer(BrokerType.KAFKA)),
                List.of(new FakeMessageReader(BrokerType.KAFKA), new FakeMessageReader(BrokerType.RABBITMQ)));

        var result = useCase.execute(new ListBrokersCommand());

        assertThat(result.brokers())
                .containsExactly(
                        new SupportedBroker(BrokerType.KAFKA, SupportedBroker.DestinationKind.TOPIC, true, true),
                        new SupportedBroker(
                                BrokerType.RABBITMQ, SupportedBroker.DestinationKind.EXCHANGE, false, false));
    }

    @Test
    void leavesOutABrokerThatCanOnlyProduceOrOnlyRead() {
        var useCase = new ListBrokersUseCase(
                List.of(new FakeMessageProducer(BrokerType.KAFKA), new FakeMessageProducer(BrokerType.PULSAR)),
                List.of(new FakeMessageReader(BrokerType.KAFKA), new FakeMessageReader(BrokerType.RABBITMQ)));

        var result = useCase.execute(new ListBrokersCommand());

        assertThat(result.brokers()).extracting(SupportedBroker::type).containsExactly(BrokerType.KAFKA);
    }
}
