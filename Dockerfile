FROM relateiq/oracle-java8

RUN mkdir -p /usr/src/app
COPY . /usr/src/app/
WORKDIR /usr/src/app

# Download gradle wrapper and build cache
RUN ./gradlew build --no-daemon

CMD ["./gradlew", "run", "-Pmyargs=ws://cs:3000", "--no-daemon"]