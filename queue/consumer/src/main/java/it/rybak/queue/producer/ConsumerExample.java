package it.rybak.queue.producer;

import javax.jms.*;
import javax.naming.InitialContext;

public class ConsumerExample {
    public static void main(final String[] args) throws Exception {
        Connection connection = null;
        InitialContext initialContext = null;
        try {
            // Step 1. Create an initial context to perform the JNDI lookup.
            initialContext = new InitialContext();

            // Step 2. Perform a lookup on the queue
            Queue queue = (Queue) initialContext.lookup("queue/exampleQueue");

            // Step 3. Perform a lookup on the Connection Factory
            ConnectionFactory cf = (ConnectionFactory) initialContext.lookup("ConnectionFactory");

            // Step 4.Create a JMS Connection
            connection = cf.createConnection();

            // Step 5. Create a JMS Session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

            // Step 6. Create a JMS Message Consumer
            MessageConsumer messageConsumer = session.createConsumer(queue);

            // Step 7. Start the Connection
            connection.start();

            // Step 8. Receive the message
            TextMessage messageReceived = (TextMessage) messageConsumer.receive(5000);

            System.out.println("Received message: " + messageReceived.getText());
        } finally {
            // Step 9. Be sure to close our JMS resources!
            if (initialContext != null) {
                initialContext.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }
}
