package com.productcatalog.domain.ports.out;

import com.productcatalog.domain.model.StatusUpdateEvent;

public interface StatusUpdatePublisher {
    void publish(StatusUpdateEvent event);
}