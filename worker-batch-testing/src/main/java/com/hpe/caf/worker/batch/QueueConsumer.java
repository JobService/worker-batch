/*
 * Copyright 2016-2020 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.worker.batch;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class QueueConsumer extends DefaultConsumer
{
    private final LinkedBlockingQueue<Delivery> messages;
    public QueueConsumer(final Channel channel)
    {
        super(channel);
        this.messages = new LinkedBlockingQueue<>();
    }

    @Override
    public void handleDelivery(final String consumerTag,
                               final Envelope envelope,
                               final AMQP.BasicProperties properties,
                               final byte[] body)
            throws IOException
    {
        messages.add(new Delivery(envelope, body));
    }

    public Delivery nextDelivery() throws InterruptedException
    {
        return messages.take();
    }

    public Delivery nextDelivery(final long timeout) throws InterruptedException
    {
        return messages.poll(timeout, TimeUnit.MILLISECONDS);
    }

    public static class Delivery {
        private final Envelope envelope;
        private final byte[] body;

        public Delivery(final Envelope envelope, final byte[] body)
        {
            this.envelope = envelope;
            this.body = body;
        }

        public Envelope getEnvelope()
        {
            return envelope;
        }

        public byte[] getBody()
        {
            return body;
        }
    }
}
