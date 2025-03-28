# Build common first
docker build -t common:latest -f common/Dockerfile .

# Then build client/server
docker-compose build

# spin up only client and server
docker-compose up --no-deps client server