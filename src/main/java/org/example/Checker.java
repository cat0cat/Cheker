package org.example;

import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

public class Checker {

    private int period;
    private String hostName;
    private String dequeName;
    private int amqpPort;

    public Checker(){
        // считываем настройки из файла
        try (FileReader reader = new FileReader("settings.txt")) {
            Properties props = new Properties();
            props.load(reader);
            period = Integer.parseInt(props.getProperty("PERIOD"));
            hostName = props.getProperty("HOST_NAME/IP");
            dequeName = props.getProperty("DEQUE_NAME");
            amqpPort = Integer.parseInt(props.getProperty("AMQP_PORT"));
        } catch (IOException ex) {
            ex.printStackTrace(System.out);
        }

        new Send().start();
        new Receive().start();
    }

    public static void main(String[] args) {
        new Checker();
    }

    private class Send extends Thread {
        @Override
        public void run() {
            // устанавливаем соединение с сервером
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(hostName);
            factory.setPort(amqpPort);
            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {
                channel.queueDeclare(dequeName, false, false, false, null);
                String message = "Hello World!";
                channel.basicPublish("", dequeName, null, message.getBytes());
                System.out.println(" [x] Sent '" + message + "'");
            } catch (IOException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private class Receive extends Thread {
        @Override
        public void run() {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(hostName);
            factory.setPort(amqpPort);
            try (Connection connection = factory.newConnection();
                 Channel channel = connection.createChannel()) {
                channel.queueDeclare(dequeName, false, false, false, null);
                System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                    System.out.println(" [x] Received '" + message + "'");
                };
                channel.basicConsume(dequeName, true, deliverCallback, consumerTag -> { });
            } catch (IOException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
