package it.rybak.activemq.artemis.topic;

import javax.jms.*;
import javax.naming.InitialContext;

public class SubscriberExample {
    public static void main(final String[] args) throws Exception {
        Connection connection = null;
        InitialContext initialContext = null;
        try {
            // /Step 1. Create an initial context to perform the JNDI lookup.
            initialContext = new InitialContext();

            // Step 2. perform a lookup on the topic
            Topic topic = (Topic) initialContext.lookup("topic/exampleTopic");

            // Step 3. perform a lookup on the Connection Factory
            ConnectionFactory cf = (ConnectionFactory) initialContext.lookup("ConnectionFactory");

            // Step 4. Create a JMS Connection
            connection = cf.createConnection();

            // Step 5. Create a JMS Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Step 6. Create a JMS Message Consumer
            MessageConsumer messageConsumer1 = session.createConsumer(topic);

            // Step 7. Start the Connection
            connection.start();

            // Step 8. Receive the message
            TextMessage messageReceived = (TextMessage) messageConsumer1.receive();

            System.out.println("Consumer 1 Received message: " + messageReceived.getText());
        } finally {
            // Step 9. Be sure to close our JMS resources!
            if (connection != null) {
                connection.close();
            }

            // Also the initialContext
            if (initialContext != null) {
                initialContext.close();
            }
        }
    }
}
