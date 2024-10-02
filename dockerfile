# Use an official OpenJDK image as the base image
FROM openjdk:23

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file created by the Maven build to the container
COPY chatbot/target/chatbot-0.0.1-SNAPSHOT.jar /app/chatbot.jar

# Expose the port your Spring Boot application runs on
EXPOSE 8080

# Command to run the JAR file
ENTRYPOINT ["java", "-jar", "/app/chatbot.jar"]