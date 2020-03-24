docker run -d --name warp10 -p 8080:8080 -p 8081:8081 warp10io/warp10:latest
sleep 5
docker exec -u warp10 -t warp10 warp10-standalone.sh worf test 3600000 > data/warp10/token.json


